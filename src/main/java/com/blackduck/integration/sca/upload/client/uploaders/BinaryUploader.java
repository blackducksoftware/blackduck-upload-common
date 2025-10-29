/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.blackduck.integration.sca.upload.file.FileUploader;
import org.apache.http.HttpHeaders;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.body.MultipartBodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.model.BinaryScanRequestData;
import com.blackduck.integration.sca.upload.file.DefaultFileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.BinaryMultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.model.response.BinaryFinishResponseContent;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

/**
 * Uploader implementation for Binary uploads.
 *
 * @see BinaryUploadStatus
 * @see BinaryScanRequestData
 * @see FileUploader
 * @see UploadValidator
 */
public class BinaryUploader extends AbstractUploader<BinaryUploadStatus> {
    private final BinaryScanRequestData binaryScanRequestData;

    /**
     * Constructor for Binary uploader.
     *
     * @param chunkSize The maximum size per chunk for a multipart upload.
     * @param fileUploader The class which uploads the file to the Black Duck server.
     * @param uploadValidator The class that provides validation for file splitting and uploader configuration.
     * @param binaryScanRequestData The class that provides the required binary specific data for uploads.
     */
    BinaryUploader(
        int chunkSize,
        FileUploader fileUploader,
        UploadValidator uploadValidator,
        BinaryScanRequestData binaryScanRequestData
    ) {
        super(chunkSize, fileUploader, uploadValidator);
        this.binaryScanRequestData = binaryScanRequestData;
    }

    /**
     * Construct the body content for the Binary HTTP request body for a standard upload.
     *
     * @param filePath The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    @Override
    protected BodyContent createBodyContent(Path filePath) {
        final Map<String, File> contentMap = new HashMap<>();
        contentMap.put("fileupload", filePath.toFile());
        final Map<String, String> metaDataMap = new HashMap<>();
        metaDataMap.put("projectName", binaryScanRequestData.getProjectName());
        metaDataMap.put("version", binaryScanRequestData.getVersion());
        metaDataMap.put("codeLocationName", binaryScanRequestData.getCodeLocationName().orElse(""));
        metaDataMap.put("codeLocationUri", binaryScanRequestData.getCodeLocationUri().orElse(""));
        return new MultipartBodyContent(contentMap, metaDataMap);
    }

    /**
     * Retrieve the HTTP request headers used for starting Binary multipart upload requests.
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
     * Retrieve the Content-Type for the multipart binary upload start requests.
     *
     * @return the Content-Type for binary upload start requests.
     */
    @Override
    protected String getMultipartUploadStartContentType() {
        return ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1;
    }

    /**
     * Retrieve the body content for a Binary HTTP multipart upload start request.
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
        return new BinaryMultipartUploadStartRequest(
            multipartUploadFileMetadata.getFileSize(),
            multipartUploadFileMetadata.getChecksum(),
            binaryScanRequestData
        );
    }

    /**
     * Construct the status object for a Binary upload either containing content or error status.
     *
     * @see BinaryUploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     */
    @Override
    protected ThrowingFunction<Response, BinaryUploadStatus, IntegrationException> createUploadStatus() {
        return response -> {
            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();

            Map<String, String> responseHeaders = response.getHeaders();
            String location = Optional.ofNullable(responseHeaders.get(HttpHeaders.LOCATION)).orElseThrow(() -> new IntegrationException("Could not find Location header."));
            String eTag = responseHeaders.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(HttpHeaders.ETAG))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IntegrationException("Could not find ETag header."));

            BinaryFinishResponseContent binaryFinishResponseContent = new BinaryFinishResponseContent(location, eTag);

            return new BinaryUploadStatus(statusCode, statusMessage, null, binaryFinishResponseContent);
        };
    }

    /**
     * Construct the status when a Binary upload error occurs.
     *
     * @see BinaryUploadStatus
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, BinaryUploadStatus> createUploadStatusError() {
        return (response, exception) -> new BinaryUploadStatus(response.getStatusCode(), response.getStatusMessage(), exception, null);
    }
}
