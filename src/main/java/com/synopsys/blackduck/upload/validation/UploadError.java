/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.blackduck.upload.validation;

import java.io.Serializable;

/**
 * A class for associating {@link ErrorCode} with custom error messages.
 * @see UploaderValidationException
 */
public class UploadError implements Serializable {
    private static final long serialVersionUID = 7250402374345082683L;
    private final ErrorCode errorCode;
    private final String errorMessage;

    /**
     * Constructor for the upload error.
     * @param errorCode    The {@link ErrorCode} enum of the error that occurred.
     * @param errorMessage The custom message for the error.
     */
    public UploadError(ErrorCode errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Retrieve the {@link ErrorCode} of the error that occurred.
     * @return the error code enum.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Retrieve the custom error message
     * @return the error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Produces a formatted message combining both the error code and the custom message.
     * @return the formatted message.
     */
    public String createFormattedErrorMessage() {
        return String.format("%s: %s", errorCode.getDescription(), errorMessage);
    }
}
