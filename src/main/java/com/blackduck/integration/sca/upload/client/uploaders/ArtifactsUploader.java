/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.blackduck.integration.sca.upload.file.FileUploader;
import org.apache.http.HttpHeaders;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.DefaultFileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

/**
 * Uploader implementation for Artifact uploads.
 *
 * @see UploadStatus
 * @see FileUploader
 * @see UploadValidator
 */
public class ArtifactsUploader extends AbstractBlackDuckUploader<UploadStatus> {

    /**
     * Constructor for Artifact uploads.
     *
     * @param chunkSize The maximum size per chunk for a multipart upload.
     * @param fileUploader The class which uploads the file to the Black Duck server.
     * @param uploadValidator The class that provides validation for file splitting and uploader configuration.
     */
    ArtifactsUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        super(chunkSize, fileUploader, uploadValidator);
    }

    /**
     * Construct the body content for the Artifact HTTP request body for a standard upload.
     *
     * @param filePath The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    @Override
    protected BodyContent createBodyContent(Path filePath) {
        throw new UnsupportedOperationException("Default upload for data type not implemented");
        // TODO: Implement for tools upload. custom octet stream content type similar to binary
    }

    /**
     * Retrieve the HTTP request headers used for starting Artifact multipart upload requests.
     *
     * @return a map of HTTP request headers.
     */
    @Override
    protected Map<String, String> getMultipartUploadStartRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, getMultipartUploadStartContentType());
        return headers;
    }

    /**
     * Retrieve the Content-Type for the multipart artifacts upload start requests.
     *
     * @return the Content-Type for artifacts upload start requests.
     */
    @Override
    protected String getMultipartUploadStartContentType() {
        return ContentTypes.APPLICATION_MULTIPART_UPLOAD_START_V1;
    }

    /**
     * Retrieve the body content for an Artifact HTTP multipart upload start request.
     * This is serialized as JSON to the Black Duck server.
     *
     * @see MultipartUploadStartRequest
     * @see MultipartUploadFileMetadata
     * @param uploadFileMetaDataSupplier The supplier of metadata used to create a start request.
     * @return the multipart start request body content.
     */
    @Override
    protected MultipartUploadStartRequest getMultipartUploadStartRequest(Supplier<MultipartUploadFileMetadata> uploadFileMetaDataSupplier) {
        MultipartUploadFileMetadata multipartUploadFileMetadata = uploadFileMetaDataSupplier.get();
        return new MultipartUploadStartRequest(multipartUploadFileMetadata.getFileSize(), multipartUploadFileMetadata.getChecksum());
    }

    /**
     * Construct the status object for an Artifact upload either containing content or error status.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     */
    @Override
    protected ThrowingFunction<Response, UploadStatus, IntegrationException> createUploadStatus() {
        //TODO: to be implemented in phase 2
        throw new UnsupportedOperationException("This function is not yet supported for this uploader type");
    }

    /**
     * Construct the status when an Artifact upload error occurs.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, UploadStatus> createUploadStatusError() {
        //TODO: to be implemented in phase 2
        throw new UnsupportedOperationException("This function is not yet supported for this uploader type");
    }
}
