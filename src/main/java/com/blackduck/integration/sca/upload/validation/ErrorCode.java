/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.validation;

/**
 * Defines the error codes produced during validation or execution of uploading files.
 * @see UploadError
 */
public enum ErrorCode {
    FILE_SIZE_ERROR("File size error"),
    CHUNK_SIZE_ERROR("Chunk size error"),
    SOURCE_FILE_MISSING_ERROR("Source file to partition is missing"),
    SOURCE_FILE_NOT_A_FILE_ERROR("Source file is not a file"),
    SOURCE_FILE_READ_PERMISSION_ERROR("Source file does not have read permission"),
    CALCULATE_MD5_CHECKSUM_ERROR("Error calculating MD5 checksum"),

    UPLOAD_START_ERROR("Error starting multipart upload"),
    UPLOAD_PART_ERROR("Error uploading file part"),
    MISSING_REQUIRED_PROPERTY_ERROR("Required property not found");

    private final String description;
    
    ErrorCode(String description) {
        this.description = description;
    }

    /**
     * Retrieve the description of the error code.
     * @return The error description.
     */
    public String getDescription() {
        return description;
    }

}
