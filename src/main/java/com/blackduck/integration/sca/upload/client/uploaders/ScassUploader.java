package com.blackduck.integration.sca.upload.client.uploaders;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.EntityBodyContent;
import com.blackduck.integration.rest.body.FileBodyContent;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

/**
 * Class to use for uploading files to SCASS
 */
public class ScassUploader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String X_GOOG_RESUMABLE_HEADER = "x-goog-resumable";

    private final static int PERMANENT_REDIRECT = 308;

    private final IntHttpClient client;

    private final UploadValidator uploadValidator;

    private final int chunkSize;

    private final long multipartUploadPartRetryInitialInterval;

    private final int multipartUploadPartRetryAttempts;

    public ScassUploader(IntHttpClient client, UploadValidator uploadValidator, int chunkSize, long multipartUploadPartRetryInitialInterval,
            int multipartUploadPartRetryAttempts) {
        this.client = client;
        this.uploadValidator = uploadValidator;
        this.chunkSize = chunkSize;
        this.multipartUploadPartRetryInitialInterval = multipartUploadPartRetryInitialInterval;
        this.multipartUploadPartRetryAttempts = multipartUploadPartRetryAttempts;
    }

    public UploadStatus upload(HttpMethod method, String signedUrl, Map<String, String> headers, Path uploadFilePath) throws IOException, IntegrationException {
        if (HttpMethod.POST.equals(method)) {
            return resumableUpload(signedUrl, headers, uploadFilePath);
        }

        if (HttpMethod.PUT.equals(method)) {
            return upload(signedUrl, headers, uploadFilePath);
        }

        throw new IllegalArgumentException("Http method " + method + " is not supported. Http method must be either POST or PUT");
    }

    public UploadStatus upload(String signedUrl, Map<String, String> headers, Path uploadFilePath) throws IntegrationException {
        validate(uploadFilePath);

        HttpUrl requestUrl = new HttpUrl(signedUrl);

        FileBodyContent bodyContent = new FileBodyContent(uploadFilePath.toFile(), null);
        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(headers)
                .method(HttpMethod.PUT)
                .bodyContent(bodyContent);

        Request request = builder.build();
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status");
        try (Response response = client.execute(request)) {
            mutableResponseStatus.setStatusCode(response.getStatusCode());
            mutableResponseStatus.setStatusMessage(response.getStatusMessage());
            // Handle errors
            client.throwExceptionForError(response);

            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();

            return new DefaultUploadStatus(statusCode, statusMessage, null);
        } catch (IOException | IntegrationException ex) {
            return new DefaultUploadStatus(mutableResponseStatus.getStatusCode(), mutableResponseStatus.getStatusMessage(),
                    new IntegrationException(FileUploader.CLOSE_RESPONSE_OBJECT_MESSAGE + ex.getCause(), ex));
        }
    }

    public UploadStatus resumableUpload(String signedUrl, Map<String, String> headers, Path uploadFilePath) throws IOException, IntegrationException {
        validate(uploadFilePath);

        String uploadUrl = initiateResumableUpload(signedUrl, headers);

        File file = uploadFilePath.toFile();
        long fileSize = file.length();
        MutableLong offset = new MutableLong(0);
        int bytesToRead = computeBytesToRead(fileSize, 0);

        int bytesRead;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            byte[] chunk = new byte[bytesToRead];
            while ((bytesRead = randomAccessFile.read(chunk, 0, bytesToRead)) != -1) {
                Map<String, String> chunkHeaders = new HashMap<>();
                String rangeValue = String.format("bytes %s-%s/%s", offset, (offset.getValue() + bytesRead - 1), fileSize);
                chunkHeaders.put(HttpHeaders.CONTENT_RANGE, rangeValue);

                int status = uploadChunk(uploadUrl, chunkHeaders, chunk, offset);
                if (status == HttpStatus.SC_OK) {
                    break;
                }

                // at this point offset was reset, so reset file pointer and reinitialize buffer
                randomAccessFile.seek(offset.getValue());
                bytesToRead = computeBytesToRead(fileSize, offset.getValue());
                chunk = new byte[bytesToRead];
            }
        } catch (InterruptedException e) {
            throw new IntegrationException("The thread was interrupted.");
        }

        String message = String.format("Resumable upload was successful for the file: %s", uploadFilePath.toAbsolutePath().toString());
        return new DefaultUploadStatus(HttpStatus.SC_OK, message, null);
    }

    private void validate(Path uploadFilePath) throws IntegrationException {
        uploadValidator.validateUploadFile(uploadFilePath);
        uploadValidator.validateUploaderConfiguration(uploadFilePath, chunkSize);
    }

    public String initiateResumableUpload(String signedUrl, Map<String, String> headers) throws IOException, IntegrationException {
        HttpUrl requestUrl = new HttpUrl(signedUrl);

        Map<String, String> requestHeaders = new HashMap<>(headers);
        requestHeaders.put(X_GOOG_RESUMABLE_HEADER, "start");
        requestHeaders.put(HttpHeaders.CONTENT_LENGTH, "0");

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(requestHeaders)
                .method(HttpMethod.POST);

        Request request = builder.build();
        try (Response response = client.execute(request)) {
            if (response.getStatusCode() == HttpStatus.SC_CREATED) {
                return response.getHeaders().get(HttpHeaders.LOCATION); // This is the resumable session URL
            } else {
                String errorMessage = String.format("Failed to initiate resumable upload. Returned status is %s. Returned status message is %s.",
                        response.getStatusCode(), response.getStatusMessage());

                throw new IntegrationException(errorMessage);
            }
        }
    }

    private int uploadChunk(String uploadUrl, Map<String, String> headers, byte[] chunk, MutableLong offset)
            throws IntegrationException, IOException, InterruptedException {
        HttpUrl requestUrl = new HttpUrl(uploadUrl);
        HttpEntity entity = new ByteArrayEntity(chunk);
        EntityBodyContent bodyContent = new EntityBodyContent(entity);

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(headers)
                .method(HttpMethod.PUT)
                .bodyContent(bodyContent);

        Request request = builder.build();

        String chunkId = headers.get(HttpHeaders.CONTENT_RANGE);
        long interval = multipartUploadPartRetryInitialInterval;
        int retryCount = 0;
        int status = 0;
        while (retryCount <= multipartUploadPartRetryAttempts) {

            try (Response response = client.execute(request)) {
                if (response.getStatusCode() == HttpStatus.SC_OK || response.getStatusCode() == HttpStatus.SC_CREATED) {
                    status = HttpStatus.SC_OK;
                    break;
                } else if (response.getStatusCode() == PERMANENT_REDIRECT) {
                    // Chunk was uploaded successfully, GCS waits for next chunk
                    Map<String, String> responseHeaders = response.getHeaders();
                    String range = responseHeaders.get(HttpHeaders.RANGE);
                    if (range != null) {
                        String[] rangeParts = range.split("-");
                        // offset should be taken from response, since that indicates the last byte that was really stored in GCS bucket
                        // offset could be different from previous offset value plus bytesRead - 1
                        offset.setValue(Long.parseLong(rangeParts[1]) + 1);
                    } else {
                        throw new IntegrationException(String.format("Response Range was not provided for chunk %s", chunkId));
                    }

                    status = response.getStatusCode();
                    break;
                } else {
                    String errorMessage = String.format("Failed to upload chunk %s. Returned status is %s. Returned status message is %s.",
                            chunkId, response.getStatusCode(), response.getStatusMessage());

                    throw new IntegrationException(errorMessage);
                }
            } catch (Exception ex) {
                logger.error("Exception occurred while uploading chunk {}", chunkId);
                logger.error("Cause: {}", ex.getMessage());
                logger.debug("Cause: ", ex);

                if (retryCount >= multipartUploadPartRetryAttempts) {
                    String errorMessage =
                            String.format("Failed to upload chunk after %s attempts. Error message is: %s", multipartUploadPartRetryAttempts, ex.getMessage());
                    throw new IntegrationException(errorMessage);
                }

                Thread.sleep(interval);
                interval = 2 * interval;
                retryCount++;
            }

        }

        return status;
    }

    private int computeBytesToRead(long fileSize, long offset) {
        long remainingBytes = fileSize - offset;

        // safe to cast since remaining bytes are less than chunk size, which is int
        return remainingBytes < chunkSize ? (int) remainingBytes : chunkSize;
    }

}
