/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance
 * Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.body.EntityBodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

/**
 * Uploader implementation for BDBA uploads.
 *
 * @see UploadStatus
 * @see FileUploader
 * @see UploadValidator
 */
public class BdbaUploader extends AbstractUploader<DefaultUploadStatus> {

    /**
     * Constructor for BDBA uploads.
     *
     * @param chunkSize
     *            The maximum size per chunk for a multipart upload.
     * @param fileUploader
     *            The class which uploads the file to the Black Duck server.
     * @param uploadValidator
     *            The class that provides validation for file splitting and uploader configuration.
     */
    BdbaUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        super(chunkSize, fileUploader, uploadValidator);
    }

    /**
     * Construct the body content for the BDBA HTTP request body for a standard upload.
     *
     * @param filePath
     *            The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    @Override
    protected BodyContent createBodyContent(Path filePath) {
        FileEntity entity = new FileEntity(filePath.toFile(), ContentType.create(ContentTypes.APPLICATION_BDBA_SCAN_DATA_V1));
        return new EntityBodyContent(entity);
    }

    /**
     * Retrieve the HTTP request headers used for starting BDBA multipart upload requests.
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
     * Retrieve the Content-Type for the multipart BDBA upload start requests.
     *
     * @return the Content-Type for BDBA upload start requests.
     */
    @Override
    protected String getMultipartUploadStartContentType() {
        return ContentTypes.APPLICATION_MULTIPART_UPLOAD_START_V1;
    }

    /**
     * Retrieve the body content for a BDBA HTTP multipart upload start request.
     * This is serialized as JSON to the Black Duck server.
     *
     * @see MultipartUploadStartRequest
     * @see MultipartUploadFileMetadata
     * @param uploadFileMetaDataSupplier
     *            The supplier of metadata used to create a start request.
     * @return the multipart start request body content.
     */
    @Override
    protected MultipartUploadStartRequest getMultipartUploadStartRequest(Supplier<MultipartUploadFileMetadata> uploadFileMetaDataSupplier) {
        MultipartUploadFileMetadata multipartUploadFileMetadata = uploadFileMetaDataSupplier.get();
        return new MultipartUploadStartRequest(multipartUploadFileMetadata.getFileSize(), multipartUploadFileMetadata.getChecksum());
    }

    /**
     * Construct the status object for a BDBA upload either containing content or error status.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     */
    @Override
    protected ThrowingFunction<Response, DefaultUploadStatus, IntegrationException> createUploadStatus() {
        return response -> {
            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();
            return new DefaultUploadStatus(statusCode, statusMessage, null);
        };
    }

    /**
     * Construct the status when a BDBA upload error occurs.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, DefaultUploadStatus> createUploadStatusError() {
        return (response, exception) -> new DefaultUploadStatus(response.getStatusCode(), response.getStatusMessage(), exception);
    }
}
