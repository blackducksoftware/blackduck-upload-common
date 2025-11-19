package com.blackduck.integration.sca.upload.file;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.body.EntityBodyContent;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.exception.IntegrationRestException;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadStartRequestData;
import com.blackduck.integration.sca.upload.file.model.MultipartUrlData;
import com.blackduck.integration.sca.upload.file.response.UploadPartResponse;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class AbstractFileUploader implements FileUploader {
    public static final String CLOSE_RESPONSE_OBJECT_MESSAGE = "Was unable to close response object: ";
    public static final String CONTENT_DIGEST_HEADER = "Content-Digest";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final IntHttpClient httpClient;
    private AtomicBoolean isCanceled = new AtomicBoolean(false);
    private final int multipartUploadPartRetryAttempts;
    // Retry interval in milliseconds
    private final long multipartUploadPartRetryInitialInterval;
    private final int multipartUploadTimeoutInMinutes;
    private final ExecutorService executorService;

    /**
     * Constructor for the file uploader.
     *
     * @param httpClient The {@link BlackDuckHttpClient} used to authenticate with and make requests to Black Duck.
     * @param multipartUploadPartRetryAttempts The number of retry attempts for uploading a file part.
     * @param multipartUploadPartRetryInitialInterval The initial interval to wait for the first retry of a file part upload.
     */
    public AbstractFileUploader(
            IntHttpClient httpClient,
            int multipartUploadPartRetryAttempts,
            long multipartUploadPartRetryInitialInterval,
            int multipartUploadTimeoutInMinutes,
            ExecutorService executorService
    ) {
        this.httpClient = httpClient;
        this.multipartUploadPartRetryAttempts = multipartUploadPartRetryAttempts;
        this.multipartUploadPartRetryInitialInterval = multipartUploadPartRetryInitialInterval;
        this.multipartUploadTimeoutInMinutes = multipartUploadTimeoutInMinutes;
        this.executorService = executorService;
    }

    protected abstract HttpUrl getUploadUrl() throws IntegrationException;
    protected abstract Request getMultipartUploadStartRequest(MultipartUploadStartRequestData uploadStartRequestData) throws IntegrationException;

    protected abstract Request.Builder getMultipartUploadPartRequestBuilder(MultipartUploadFileMetadata fileMetaData,
                                                                            String uploadUrl,
                                                                            MultipartUploadFilePart part) throws IntegrationException;
    protected abstract Request getMultipartUploadFinishRequest(String uploadUrl, Map<Integer,String> tagOrderMap, MultipartUrlData completeUploadUrl) throws IntegrationException;

    /**
     * Performs a standard file upload to Black Duck.
     *
     * @param bodyContent The {@link BodyContent} of the upload file request.
     * @param uploadStatusFunction {@link ThrowingFunction} that generates the {@link UploadStatus} from the response.
     * @param uploadStatusErrorFunction {@link BiFunction} that generates the error {@link UploadStatus} from the response and exception thrown.
     * @return {@link UploadStatus} status of the upload.
     * @param <T> {@link UploadStatus} status of the upload for the file type.
     * @throws IntegrationException if an error occurred while making the request to Black Duck.
     */
    public <T extends UploadStatus> T upload(
            BodyContent bodyContent,
            ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction,
            BiFunction<MutableResponseStatus, IntegrationException, T> uploadStatusErrorFunction
    ) throws IntegrationException {
        HttpUrl requestUrl = getUploadUrl();

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .method(HttpMethod.POST)
                .bodyContent(bodyContent);

        Request request = builder.build();
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status");
        try (Response response = httpClient.execute(request)) {
            mutableResponseStatus.setStatusCode(response.getStatusCode());
            mutableResponseStatus.setStatusMessage(response.getStatusMessage());
            // Handle errors
            httpClient.throwExceptionForError(response);
            return uploadStatusFunction.apply(response);
        } catch (IOException | IntegrationException ex) {
            return uploadStatusErrorFunction.apply(mutableResponseStatus, new IntegrationException(CLOSE_RESPONSE_OBJECT_MESSAGE + ex.getCause(), ex));
        }
    }

    /**
     * Performs a multipart file upload to Black Duck.
     *
     * @param multipartUploadFileMetadata The {@link MultipartUploadFileMetadata} for the file to upload.
     * @param uploadStatusFunction {@link ThrowingFunction} that generates the {@link UploadStatus} from the response.
     * @param uploadStatusErrorFunction {@link BiFunction} that generates the error {@link UploadStatus} from the response and exception thrown.
     * @return {@link UploadStatus} status of the upload.
     * @param <T> status of the upload for the file type.
     */
    public <T extends UploadStatus> T multipartUpload(
            MultipartUploadFileMetadata multipartUploadFileMetadata,
            Supplier<MultipartUploadStartRequestData>  startUploadRequestSupplier,
            ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction,
            BiFunction<MutableResponseStatus, IntegrationException, T> uploadStatusErrorFunction
    ) {
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status");
        try {
            String uploadUrl = startMultipartUpload(mutableResponseStatus, startUploadRequestSupplier);
            Map<Integer, String> uploadedParts = multipartUploadParts(mutableResponseStatus, multipartUploadFileMetadata, uploadUrl);
            verifyAllPartsUploaded(multipartUploadFileMetadata, uploadedParts);
            return finishMultipartUpload(mutableResponseStatus, uploadUrl, uploadStatusFunction, uploadedParts, multipartUploadFileMetadata.getCompleteUploadUrl(), multipartUploadFileMetadata.getAbortUploadUrl());
        } catch (IntegrationException ex) {
            return uploadStatusErrorFunction.apply(mutableResponseStatus, ex);
        }
    }

    /**
     * Initiates the start of a multipart upload.
     *
     * @param mutableResponseStatus A {@link MutableResponseStatus} with the status of the multipart upload.
     * @return Value of the upload url from Black Duck to be used for part uploads and assembly.
     * @throws IntegrationException if an error occurred while making the request to Black Duck.
     */
    protected String startMultipartUpload(
            MutableResponseStatus mutableResponseStatus,
            Supplier<MultipartUploadStartRequestData> startRequestDataSupplier
    ) throws IntegrationException {
        Request request = getMultipartUploadStartRequest(startRequestDataSupplier.get());
        try (Response response = httpClient.execute(request)) {
            mutableResponseStatus.setStatusCode(response.getStatusCode());
            mutableResponseStatus.setStatusMessage(response.getStatusMessage());
            // Handle errors
            httpClient.throwExceptionForError(response);

            Map<String, String> responseHeaders = response.getHeaders();
            return Optional.ofNullable(responseHeaders.get(HttpHeaders.LOCATION)).orElseThrow(() -> new IntegrationException("Could not find Location header."));
        } catch (IOException ex) {
            throw new IntegrationException(CLOSE_RESPONSE_OBJECT_MESSAGE + ex.getCause(), ex);
        }
    }

    /**
     * Performs the uploading of file parts for a multipart upload.
     *
     * @param mutableResponseStatus A {@link MutableResponseStatus} with the status of the multipart upload.
     * @param multipartUploadFileMetadata The {@link MultipartUploadFileMetadata} for the file to upload.
     * @param uploadUrl Url from Black Duck to be used for part uploads.
     * @return A {@link Map} of uploaded part indexes and their tag ids.
     * @throws IntegrationException If an error occurred during the upload of parts.
     */
    protected Map<Integer, String> multipartUploadParts(
            MutableResponseStatus mutableResponseStatus,
            MultipartUploadFileMetadata multipartUploadFileMetadata,
            String uploadUrl
    ) throws IntegrationException {
        logger.info("Starting multipart file upload for {}.", multipartUploadFileMetadata.getUploadId());
        Map<Integer, String> tagOrderMap = new ConcurrentHashMap<>(multipartUploadFileMetadata.getFileChunks().size());
        try {
            logger.debug("Submitting {} upload requests into executor service.", multipartUploadFileMetadata.getFileChunks().size());
            for (MultipartUploadFilePart part : multipartUploadFileMetadata.getFileChunks()) {
                executorService.submit(() -> {
                    boolean partUploaded = false;
                    try {
                        partUploaded = retryableExecuteUploadPart(mutableResponseStatus, tagOrderMap, multipartUploadFileMetadata, uploadUrl, part);
                    } catch (InterruptedException e) {
                        logger.error("Thread was interrupted during upload of part: ", e);
                        Thread.currentThread().interrupt();
                    } catch (IntegrationException | IOException e) {
                        logger.error("Error uploading part: ", e);
                    }
                    if (!partUploaded) {
                        try {
                            cancelUpload(uploadUrl, multipartUploadFileMetadata.getAbortUploadUrl());
                        } catch (IntegrationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            logger.debug("All {} upload requests submitted into executor service.", multipartUploadFileMetadata.getFileChunks().size());
            executorService.shutdown();
            logger.debug("Awaiting for executor service to complete or timeout of {} minutes occurs.", multipartUploadTimeoutInMinutes);
            boolean success = executorService.awaitTermination(multipartUploadTimeoutInMinutes, TimeUnit.MINUTES);
            logger.debug("Executor service terminated: {}", success);
            // if the timeout occurred cancel the upload.
            if(!success) {
                logger.error("Upload timed out. Cancelling upload.");
                logger.debug(partsUploadedString(tagOrderMap.size(), multipartUploadFileMetadata.getFileChunks().size()));
                cancelUpload(uploadUrl, multipartUploadFileMetadata.getAbortUploadUrl());
                throw new IntegrationTimeoutException("Executor service timed out.");
            }
            if (isCanceled.get()) {
                logger.info("Upload was cancelled. Check log for errors.");
            } else if (success) {
                logger.info("All part requests submitted successfully.");
            } else {
                logger.info("There were errors submitting part upload requests.");
            }
            return tagOrderMap;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // TODO: We likely need to improve handling interrupted threads, including cleaning up the thread pool in the event of an interrupt.
            //  See HUB-42207 for implementing this work.
            throw new IntegrationException("An error occurred while uploading parts: " + ex.getCause(), ex);
        }
    }

    // Performs the upload of a part and retries based on status code. Attempts and wait interval between each retry are specified by properties.
    private boolean retryableExecuteUploadPart(
            MutableResponseStatus mutableResponseStatus,
            Map<Integer, String> tagOrderMap,
            MultipartUploadFileMetadata fileMetaData,
            String uploadUrl,
            MultipartUploadFilePart part
    ) throws IOException, InterruptedException, IntegrationException {
        int retryCount = 0; // Initial upload is 0
        long interval = multipartUploadPartRetryInitialInterval;
        Request.Builder requestBuilder = getMultipartUploadPartRequestBuilder(fileMetaData, uploadUrl, part);

        while (retryCount <= multipartUploadPartRetryAttempts && !isCanceled.get()) {
            if (retryCount > 0) {
                logger.info("Retry attempt {} for uploading of part {}", retryCount, part);
                if (multipartUploadPartRetryInitialInterval > 0) {
                    Thread.sleep(interval);
                }
            }

            Optional<UploadPartResponse> optionalPartResponse;
            try (RandomAccessFile uploadFile = new RandomAccessFile(part.getFilePath().toFile(), "r");
                 FileByteRangeInputStream fileByteRangeInputStream = new FileByteRangeInputStream(uploadFile, part.getStartByteRange(), part.getChunkSize())) {
                EntityBodyContent content = createUploadBodyContent(part, fileByteRangeInputStream);
                requestBuilder.bodyContent(content);
                optionalPartResponse = executeUploadPart(requestBuilder.build(), part);
            }
            if (optionalPartResponse.isPresent()) {
                UploadPartResponse uploadPartResponse = optionalPartResponse.get();
                mutableResponseStatus.setStatusCode(uploadPartResponse.getHttpStatusCode());
                mutableResponseStatus.setStatusMessage(uploadPartResponse.getHttpStatusMessage());

                Optional<Response> optionalResponse = uploadPartResponse.getResponse();

                if (optionalResponse.isPresent()) {
                    try (Response response = optionalResponse.get()) {
                        if (response.isStatusCodeSuccess()) {
                            tagOrderMap.put(part.getIndex(), part.getTagId().toString());
                            return true;
                        } else if (UploadValidator.MULTIPART_UPLOAD_PART_RETRY_STATUS_CODES.contains(response.getStatusCode())) {
                            logger.debug("Received {} response code during uploading of part: {}", response.getStatusCode(), response.getStatusMessage());
                            if (retryCount > 0) {
                                // Double the retry interval
                                interval = 2 * interval;
                            }
                        } else {
                            logger.error("Aborting upload part due to {} status code {}", response.getStatusCode(), response.getStatusMessage());
                            return false;
                        }
                    }
                }
            } else {
                logger.error("Aborting upload part due to no or non-valid response");
                return false;
            }
            retryCount += 1;
        }

        String status = isCanceled.get() ? "cancelled" : "failed";
        logger.error("Upload of part {} {}", status, part);
        return false;
    }

    private EntityBodyContent createUploadBodyContent(MultipartUploadFilePart part, FileByteRangeInputStream fileByteRangeInputStream) {
        InputStreamEntity entity = new InputStreamEntity(fileByteRangeInputStream, part.getChunkSize(), ContentType.create(ContentTypes.APPLICATION_MULTIPART_UPLOAD_DATA_V1));
        return new EntityBodyContent(entity);
    }

    private Optional<UploadPartResponse> executeUploadPart(Request request, MultipartUploadFilePart part) {
        if (isCanceled.get()) {
            logger.debug("Multipart upload has been canceled, not starting upload for part {}, beginning with byte {}.", part.getIndex(), part.getStartByteRange());
            return Optional.empty();
        }

        logger.debug("Starting upload for part {}, beginning with byte {}.", part.getIndex(), part.getStartByteRange());

        try {
            try (Response response = httpClient.execute(request)) {
                return Optional.of(UploadPartResponse.fromResponse(response));
            }
        } catch (IntegrationRestException ex) {
            return Optional.of(UploadPartResponse.fromException(ex));
        } catch (IOException | IntegrationException ex) {
            logger.error("Exception occurred whiling uploading part {}", part);
            logger.error("Cause: {}", ex.getMessage());
            logger.debug("Cause: ", ex);
        }
        return Optional.empty();
    }

    /**
     * Notify Black Duck the complete upload of file parts and initiates reassembly of the file.
     *
     * @param mutableResponseStatus A {@link MutableResponseStatus} with the status of the multipart upload.
     * @param uploadUrl Url from Black Duck to be used for finishing the multipart upload.
     * @param uploadStatusFunction {@link ThrowingFunction} that generates the {@link UploadStatus} from the response.
     * @param tagOrderMap A {@link Map} of uploaded part indexes and their tag ids.
     * @return {@link UploadStatus} status of the upload.
     * @param <T> status of the upload for the file type.
     * @throws IntegrationException if an error occurred while making the request to Black Duck.
     */
    public <T extends UploadStatus> T finishMultipartUpload(
            MutableResponseStatus mutableResponseStatus,
            String uploadUrl,
            ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction,
            Map<Integer, String> tagOrderMap,
            MultipartUrlData completeUploadUrl,
            MultipartUrlData abortUploadUrl
    ) throws IntegrationException {
        Request request = getMultipartUploadFinishRequest(uploadUrl, tagOrderMap, completeUploadUrl);
        HttpMethod httpMethod= request.getMethod();
        HttpUrl requestUrl = request.getUrl();

        if (isCanceled.get()) {
            logger.debug("Upload has been canceled, not calling {} against {}", httpMethod, requestUrl);
            throw new IntegrationException("Upload has been canceled, not calling {} against {}");
        }

        logger.info("Finishing multipart file upload.");
        try (Response response = httpClient.execute(request)) {
            mutableResponseStatus.setStatusCode(response.getStatusCode());
            mutableResponseStatus.setStatusMessage(response.getStatusMessage());
            // Handle errors
            httpClient.throwExceptionForError(response);
            return uploadStatusFunction.apply(response);
        } catch (IOException ex) {
            cancelUpload(uploadUrl, abortUploadUrl);
            throw new IntegrationException(CLOSE_RESPONSE_OBJECT_MESSAGE + ex.getCause(), ex);
        } catch (IntegrationException ex) {
            cancelUpload(uploadUrl, abortUploadUrl);
            throw ex;
        }
    }

    // Notifies Black Duck or Cloud Storage server of an upload cancellation and blocks further uploads of parts by the uploader.
    private void cancelUpload(String uploadUrl, MultipartUrlData abortUploadUrl) throws IntegrationException {
        if (isCanceled.get()) {
            logger.debug("Upload already cancelled.");
            return;
        }

        logger.info("Canceling multipart file upload.");
        Request request;

        if(abortUploadUrl != null) {
            HttpUrl requestUrl = new HttpUrl(abortUploadUrl.getUrl());
            Map<String, String> requestHeaders = abortUploadUrl.getHeaders();

            Request.Builder requestBuilder = new Request.Builder()
                    .url(requestUrl)
                    .headers(requestHeaders)
                    .method(HttpMethod.DELETE);

            request = requestBuilder.build();
        } else {
            HttpUrl requestUrl = new HttpUrl(uploadUrl);

            Request.Builder builder = new Request.Builder()
                    .url(requestUrl)
                    .method(HttpMethod.DELETE);

            request = builder.build();
        }

        try {
            // Handle errors
            Response response = httpClient.execute(request);
            httpClient.throwExceptionForError(response);
        } catch (IntegrationException ex) {
            logger.error("Error canceling upload");
            logger.error("Cause: {}", ex.getMessage());
            logger.debug("Cause: ", ex);
        }
        isCanceled.set(true);
    }

    private void verifyAllPartsUploaded(MultipartUploadFileMetadata multipartUploadFileMetaData, Map<Integer, String> uploadedParts) throws IntegrationException {
        int actual = uploadedParts.size();
        int expected = multipartUploadFileMetaData.getFileChunks().size();
        if (expected != actual) {
            String message = "The number of parts uploaded does not match the number of parts created. " + partsUploadedString(actual, expected);
            logger.error(message);
            throw new IntegrationException("The number of parts uploaded does not match the number of parts uploaded. Expected: " + expected + ", Actual: " + actual);
        }
    }

    private String partsUploadedString(int actual, int expected) {
        return String.format(
                "%d of %d expected parts were uploaded.",
                actual,
                expected
        );
    }


}
