/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.blackduck.integration.function.ThrowingSupplier;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadStartRequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.EnvironmentProperties;
import com.blackduck.integration.sca.upload.file.FileSplitter;
import com.blackduck.integration.sca.upload.file.DefaultFileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

/**
 * Abstract class containing all common file upload methods.
 *
 * @see UploadStatus
 * @see FileSplitter
 * @see FileUploader
 * @see UploadValidator
 * @param <T> {@link UploadStatus} The result type from uploading a file.
 */
public abstract class AbstractUploader<T extends UploadStatus> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final FileSplitter fileSplitter = new FileSplitter();
    private final FileUploader fileUploader;
    private final UploadValidator uploadValidator;
    private final int chunkSize;

    /**
     * Constructor for the abstract uploader.
     *
     * @param chunkSize The maximum size per chunk for a multipart upload.
     * @param fileUploader The class which uploads the file to the Black Duck server.
     * @param uploadValidator The class that provides validation for file splitting and uploader configuration.
     */
    AbstractUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        this.chunkSize = chunkSize;
        this.fileUploader = fileUploader;
        this.uploadValidator = uploadValidator;
    }

    /**
     *
     * Performs upload of a file specified by a given path.
     * If the file is larger than the threshold specified by {@link EnvironmentProperties#BLACKDUCK_MULTIPART_UPLOAD_THRESHOLD},
     * a multipart upload is performed. Otherwise, a standard uploaded is performed.
     *
     * @param uploadFilePath The path of the file to upload.
     * @return the {@link UploadStatus} from uploading a file.
     */
    public T upload(Path uploadFilePath) throws IOException, IntegrationException {
        uploadValidator.validateUploadFile(uploadFilePath);

        if (uploadValidator.isFileForPartitioning(uploadFilePath)) {
            uploadValidator.validateUploaderConfiguration(uploadFilePath, chunkSize);
            return partitionAndUploadFile(uploadFilePath);
        }

        return fileUploader.upload(createBodyContent(uploadFilePath), createUploadStatus(), createUploadStatusError());
    }

    private T partitionAndUploadFile(Path uploadFilePath) throws IOException, IntegrationException {
        logger.info("Start of calculate for file offsets.");
        MultipartUploadFileMetadata multipartUploadFileMetadata = fileSplitter.splitFile(uploadFilePath, chunkSize);
        logger.info("Finish of calculate for file offsets.");
        return fileUploader.multipartUpload(
            multipartUploadFileMetadata,
            createUploadStartRequestData(multipartUploadFileMetadata),
            createUploadStatus(),
            createUploadStatusError()
        );
    }

    /**
     * Construct the body content for the HTTP request body for a standard upload.
     *
     * @param filePath The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    protected abstract BodyContent createBodyContent(Path filePath);

    protected abstract Supplier<MultipartUploadStartRequestData> createUploadStartRequestData(MultipartUploadFileMetadata multipartUploadFileMetadata);

    /**
     * Construct the status object for an upload either containing content or error status.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     * @throws IntegrationException if HTTP response does not contain the expected content.
     */
    protected abstract ThrowingFunction<Response, T, IntegrationException> createUploadStatus() throws IntegrationException;

    /**
     * Construct the status when an upload error occurs.
     *
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    protected abstract BiFunction<MutableResponseStatus, IntegrationException, T> createUploadStatusError();
}
