/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.body.EntityBodyContent;
import com.blackduck.integration.rest.body.StringBodyContent;
import com.blackduck.integration.rest.exception.IntegrationRestException;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.file.response.UploadPartResponse;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import com.google.gson.Gson;

/**
 * Class used to perform an upload of a file to Black Duck.
 *
 * @see BlackDuckHttpClient
 * @see UploadRequestPaths
 */
public class FileUploader {
    public static final String CLOSE_RESPONSE_OBJECT_MESSAGE = "Was unable to close response object: ";
    public static final String CONTENT_DIGEST_HEADER = "Content-Digest";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BlackDuckHttpClient httpClient;
    private final Gson gson;
    private final UploadRequestPaths uploadRequestPaths;
    private boolean isCanceled = false;
    private final int multipartUploadPartRetryAttempts;
    // Retry interval in milliseconds
    private final long multipartUploadPartRetryInitialInterval;
    private final int multipartUploadTimeoutInMinutes;

    /**
     * Constructor for the file uploader.
     *
     * @param httpClient The {@link BlackDuckHttpClient} used to authenticate with and make requests to Black Duck.
     * @param uploadRequestPaths The {@link UploadRequestPaths} endpoints for performing upload and multipart uploads.
     * @param multipartUploadPartRetryAttempts The number of retry attempts for uploading a file part.
     * @param multipartUploadPartRetryInitialInterval The initial interval to wait for the first retry of a file part upload.
     */
    public FileUploader(
        BlackDuckHttpClient httpClient,
        UploadRequestPaths uploadRequestPaths,
        int multipartUploadPartRetryAttempts,
        long multipartUploadPartRetryInitialInterval,
        int multipartUploadTimeoutInMinutes
    ) {
        this.httpClient = httpClient;
        this.uploadRequestPaths = uploadRequestPaths;
        this.multipartUploadPartRetryAttempts = multipartUploadPartRetryAttempts;
        this.multipartUploadPartRetryInitialInterval = multipartUploadPartRetryInitialInterval;
        this.multipartUploadTimeoutInMinutes = multipartUploadTimeoutInMinutes;
        gson = httpClient.getGson();
    }

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
        String requestPath = uploadRequestPaths.getUploadRequestPath();
        HttpUrl requestUrl = httpClient.getBlackDuckUrl().appendRelativeUrl(requestPath);

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
     * @param multipartUploadStartRequestHeaders A {@link Map} of headers for the multipart upload start request.
     * @param multipartUploadStartContentType The Content-Type for the start request body.
     * @param multipartUploadStartRequest The data object for multipart upload start request.
     * @param uploadStatusFunction {@link ThrowingFunction} that generates the {@link UploadStatus} from the response.
     * @param uploadStatusErrorFunction {@link BiFunction} that generates the error {@link UploadStatus} from the response and exception thrown.
     * @return {@link UploadStatus} status of the upload.
     * @param <T> status of the upload for the file type.
     */
    public <T extends UploadStatus> T multipartUpload(
        MultipartUploadFileMetadata multipartUploadFileMetadata,
        Map<String, String> multipartUploadStartRequestHeaders,
        String multipartUploadStartContentType,
        MultipartUploadStartRequest multipartUploadStartRequest,
        ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction,
        BiFunction<MutableResponseStatus, IntegrationException, T> uploadStatusErrorFunction
    ) {
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status");
        try {
            String uploadUrl = startMultipartUpload(mutableResponseStatus, multipartUploadStartRequestHeaders, multipartUploadStartContentType, multipartUploadStartRequest);
            Map<Integer, String> uploadedParts = multipartUploadParts(mutableResponseStatus, multipartUploadFileMetadata, uploadUrl);
            verifyAllPartsUploaded(multipartUploadFileMetadata, uploadedParts);
            return finishMultipartUpload(mutableResponseStatus, uploadUrl, uploadStatusFunction);
        } catch (IntegrationException ex) {
            return uploadStatusErrorFunction.apply(mutableResponseStatus, ex);
        }
    }

    /**
     * Initiates the start of a multipart upload.
     *
     * @param mutableResponseStatus A {@link MutableResponseStatus} with the status of the multipart upload.
     * @param startRequestHeaders A {@link Map} of headers for the multipart upload start request.
     * @param multipartUploadStartContentType The Content-Type for the start request body.
     * @param multipartUploadStartRequest The data object for multipart upload start request.
     * @return Value of the upload url from Black Duck to be used for part uploads and assembly.
     * @throws IntegrationException if an error occurred while making the request to Black Duck.
     */
    protected String startMultipartUpload(
        MutableResponseStatus mutableResponseStatus,
        Map<String, String> startRequestHeaders,
        String multipartUploadStartContentType,
        MultipartUploadStartRequest multipartUploadStartRequest
    ) throws IntegrationException {
        String requestPath = uploadRequestPaths.getMultipartUploadStartRequestPath();
        HttpUrl requestUrl = httpClient.getBlackDuckUrl().appendRelativeUrl(requestPath);

        Request.Builder builder = new Request.Builder()
            .url(requestUrl)
            .method(HttpMethod.POST)
            .headers(startRequestHeaders)
            .bodyContent(new StringBodyContent(
                gson.toJson(multipartUploadStartRequest),
                ContentType.create(multipartUploadStartContentType)
            ));

        Request request = builder.build();
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
            // For GCS at the moment it must execute each upload request in order.  There are back end changes that need to be implemented to support GCS with multithreaded support.
            ExecutorService executorService = Executors.newSingleThreadExecutor();

            //TODO: By default, we want to use a multithreaded pool. We may want to have this configurable and enable this for the purposes of testing.
            // See HUB-42207 for more info. FileUploaderTest may also need to be updated for multithreaded cases.
            // ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), Executors.defaultThreadFactory());
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
                        cancelUpload(uploadUrl);
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
                cancelUpload(uploadUrl);
                throw new IntegrationTimeoutException("Executor service timed out.");
            }
            if (isCanceled) {
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
        HttpUrl requestUrl = new HttpUrl(uploadUrl);
        Request.Builder requestBuilder = new Request.Builder()
            .url(requestUrl)
            .method(HttpMethod.PUT)
            .headers(createUploadHeaders(fileMetaData, part));

        while (retryCount <= multipartUploadPartRetryAttempts && !isCanceled) {
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

        String status = isCanceled ? "cancelled" : "failed";
        logger.error("Upload of part {} {}", status, part);
        return false;
    }

    private EntityBodyContent createUploadBodyContent(MultipartUploadFilePart part, FileByteRangeInputStream fileByteRangeInputStream) {
        InputStreamEntity entity = new InputStreamEntity(fileByteRangeInputStream, part.getChunkSize(), ContentType.create(ContentTypes.APPLICATION_MULTIPART_UPLOAD_DATA_V1));
        return new EntityBodyContent(entity);
    }

    private Optional<UploadPartResponse> executeUploadPart(Request request, MultipartUploadFilePart part) {
        if (isCanceled) {
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
     * @return {@link UploadStatus} status of the upload.
     * @param <T> status of the upload for the file type.
     * @throws IntegrationException if an error occurred while making the request to Black Duck.
     */
    public <T extends UploadStatus> T finishMultipartUpload(
        MutableResponseStatus mutableResponseStatus,
        String uploadUrl,
        ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction
    ) throws IntegrationException {
        HttpMethod httpMethod = HttpMethod.POST;
        HttpUrl requestUrl = new HttpUrl(uploadUrl + "/completed");

        if (isCanceled) {
            logger.debug("Upload has been canceled, not calling {} against {}", httpMethod, requestUrl);
            throw new IntegrationException("Upload has been canceled, not calling {} against {}");
        }

        logger.info("Finishing multipart file upload.");

        Request.Builder builder = new Request.Builder()
            .url(requestUrl)
            .method(httpMethod)
            .addHeader(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_MULTIPART_UPLOAD_FINISH_V1);

        Request request = builder.build();
        try (Response response = httpClient.execute(request)) {
            mutableResponseStatus.setStatusCode(response.getStatusCode());
            mutableResponseStatus.setStatusMessage(response.getStatusMessage());
            // Handle errors
            httpClient.throwExceptionForError(response);
            return uploadStatusFunction.apply(response);
        } catch (IOException ex) {
            cancelUpload(uploadUrl);
            throw new IntegrationException(CLOSE_RESPONSE_OBJECT_MESSAGE + ex.getCause(), ex);
        } catch (IntegrationException ex) {
            cancelUpload(uploadUrl);
            throw ex;
        }
    }

    // Notifies Black Duck of an upload cancellation and blocks further uploads of parts by the uploader.
    private void cancelUpload(String uploadUrl) {
        if (isCanceled) {
            logger.debug("Upload already cancelled.");
            return;
        }

        logger.info("Canceling multipart file upload.");
        try {
            HttpUrl requestUrl = new HttpUrl(uploadUrl);

            Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .method(HttpMethod.DELETE);

            Request request = builder.build();
            try (Response response = httpClient.execute(request)) {
                // Handle errors
                httpClient.throwExceptionForError(response);
            }
        } catch (IntegrationException | IOException ex) {
            logger.error("Error canceling upload");
            logger.error("Cause: {}", ex.getMessage());
            logger.debug("Cause: ", ex);
        }
        isCanceled = true;
    }

    private Map<String, String> createUploadHeaders(MultipartUploadFileMetadata fileMetaData, MultipartUploadFilePart part) {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(CONTENT_DIGEST_HEADER, String.format("md5=:%s:", part.getChecksum()));
        requestHeaders.put(
            HttpHeaders.CONTENT_RANGE,
            String.format("bytes %s-%s/%s", part.getStartByteRange(), part.getStartByteRange() + part.getChunkSize() - 1, fileMetaData.getFileSize())
        );
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_MULTIPART_UPLOAD_DATA_V1);

        return requestHeaders;
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
