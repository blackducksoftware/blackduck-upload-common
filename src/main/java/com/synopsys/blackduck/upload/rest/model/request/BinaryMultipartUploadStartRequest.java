/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.blackduck.upload.rest.model.request;

import com.synopsys.blackduck.upload.client.model.BinaryScanRequestData;

/**
 * Data model object used as the content of a start multipart upload request for a binary file.
 * This object is serialized as JSON and sent as the body content of an HTTP request to start a binary upload.
 * @see BinaryScanRequestData
 * @see MultipartUploadStartRequest
 */
public class BinaryMultipartUploadStartRequest extends MultipartUploadStartRequest {
    private final BinaryScanRequestData binaryScanRequestData;

    /**
     * Constructor for the upload start request data of a binary file.
     * @param fileSize              The size of the file to be uploaded.
     * @param checksum              The base 64 encoded MD5 checksum of the file to be uploaded.
     * @param binaryScanRequestData The data needed to initiate a binary scan request on the Black Duck server.
     */
    public BinaryMultipartUploadStartRequest(long fileSize, String checksum, BinaryScanRequestData binaryScanRequestData) {
        super(fileSize, checksum);
        this.binaryScanRequestData = binaryScanRequestData;
    }

    /**
     * Retrieve the data needed to initiate a binary scan request on the Black Duck server.
     * @return data needed to initiate a binary scan.
     */
    public BinaryScanRequestData getBinaryScanRequestData() {
        return binaryScanRequestData;
    }
}
