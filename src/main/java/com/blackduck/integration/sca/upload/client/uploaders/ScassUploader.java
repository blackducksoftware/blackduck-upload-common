package com.blackduck.integration.sca.upload.client.uploaders;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.EntityBodyContent;
import com.blackduck.integration.rest.body.FileBodyContent;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.google.gson.Gson;

/**
 * Class to use for uploading files to SCASS
 */
public class ScassUploader {

    private final static String CONTENT_RANGE_HEADER = "Content-Range";

    // chunk size is recommended to be multiple of 256 KB. Chunk size around 50 Mb seems to be good compromise between
    // performance and reliability
    private final static int CHUNK_SIZE = 262144 * 200;

    private final IntHttpClient client;

    public ScassUploader(IntLogger intLogger, Gson gson) {
        this.client = new IntHttpClient(intLogger, gson, 600, true, ProxyInfo.NO_PROXY_INFO);
    }

    public UploadStatus upload(String signedUrl, Map<String, String> headers, Path uploadFilePath) throws IntegrationException {

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
                String rangeValue = "bytes " + offset + "-" + (offset.getValue() + bytesRead - 1) + "/" + fileSize;
                chunkHeaders.put(CONTENT_RANGE_HEADER, rangeValue);

                int status = uploadChunk(uploadUrl, chunkHeaders, chunk, offset);
                if (status == HttpStatus.SC_OK) {
                    break;
                }

                // at this point offset was reset, so reset file pointer and reinitialize buffer
                randomAccessFile.seek(offset.getValue());
                bytesToRead = computeBytesToRead(fileSize, offset.getValue());
                chunk = new byte[bytesToRead];
            }
        }

        String message = String.format("Resumable upload was successful for the file: %s", uploadFilePath.toAbsolutePath().toString());
        return new DefaultUploadStatus(HttpStatus.SC_OK, message, null);
    }

    public String initiateResumableUpload(String signedUrl, Map<String, String> headers) throws IOException, IntegrationException {
        HttpUrl requestUrl = new HttpUrl(signedUrl);

        Map<String, String> requestHeaders = new HashMap<>(headers);
        requestHeaders.put("x-goog-resumable", "start");
        requestHeaders.put("Content-Length", "0");

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(requestHeaders)
                .method(HttpMethod.POST);

        Request request = builder.build();
        try (Response response = client.execute(request)) {
            if (response.getStatusCode() == HttpStatus.SC_CREATED) {
                return response.getHeaders().get("Location"); // This is the resumable session URL
            } else {
                String errorMessage = String.format("Failed to initiate resumable upload. Returned status is %s. Returned status message is %s.",
                        response.getStatusCode(), response.getStatusMessage());

                throw new IntegrationException(errorMessage);
            }
        }
    }

    private int uploadChunk(String uploadUrl, Map<String, String> headers, byte[] chunk, MutableLong offset) throws IntegrationException, IOException {
        HttpUrl requestUrl = new HttpUrl(uploadUrl);
        HttpEntity entity = new ByteArrayEntity(chunk);
        EntityBodyContent bodyContent = new EntityBodyContent(entity);

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(headers)
                .method(HttpMethod.PUT)
                .bodyContent(bodyContent);

        Request request = builder.build();
        try (Response response = client.execute(request)) {
            if (response.getStatusCode() == HttpStatus.SC_OK || response.getStatusCode() == HttpStatus.SC_CREATED) {
                return HttpStatus.SC_OK;
            } else if (response.getStatusCode() == 308) { // PERMANENT_REDIRECT
                // Chunk was uploaded successfully, GCS waits for next chunk
                Map<String, String> responseHeaders = response.getHeaders();
                String range = responseHeaders.get("Range");
                if (range != null) {
                    String[] rangeParts = range.split("-");
                    // offset should be taken from response, since that indicates the last byte that was really stored in GCS bucket
                    // offset could be different from previous offset value plus bytesRead - 1
                    offset.setValue(Long.parseLong(rangeParts[1]) + 1);
                } else {
                    throw new IntegrationException(String.format("Response Range was not provided for chunk %s", headers.get(CONTENT_RANGE_HEADER)));
                }

                return response.getStatusCode();
            } else {
                String errorMessage = String.format("Failed to upload chunk %s. Returned status is %s. Returned status message is %s.",
                        headers.get(CONTENT_RANGE_HEADER), response.getStatusCode(), response.getStatusMessage());

                throw new IntegrationException(errorMessage);
            }

        }
    }

    private int computeBytesToRead(long fileSize, long offset) {
        long remainingBytes = fileSize - offset;

        // safe to cast since remaining bytes are less than chunk size, which is int
        return remainingBytes < CHUNK_SIZE ? (int) remainingBytes : CHUNK_SIZE;
    }

}
