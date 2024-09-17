/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.blackduck.upload.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains state for errors as they occur in the library. Can store multiple errors if they are recoverable or terminate the execution by throwing a {@link UploaderValidationException}.
 */
public class UploadStateManager {
    private final List<UploadError> uploadErrors;

    /**
     * Constructs a manager to maintain state for errors that occur during validation or uploading of files.
     */
    public UploadStateManager() {
        this.uploadErrors = new ArrayList<>();
    }

    /**
     * Adds an error code and message to the manager.
     * @param errorCode    The {@link ErrorCode} enum of the error that occurred.
     * @param errorMessage The custom message for the error.
     */
    public void addError(ErrorCode errorCode, String errorMessage) {
        uploadErrors.add(new UploadError(errorCode, errorMessage));
    }

    /**
     * Adds an error code and message to the manager, then throws an {@link UploaderValidationException}
     * @param errorCode    The {@link ErrorCode} enum of the error that occurred.
     * @param errorMessage The custom message for the error.
     * @throws UploaderValidationException containing all exceptions saved in the manager.
     */
    public void addErrorAndTerminate(ErrorCode errorCode, String errorMessage) throws UploaderValidationException {
        uploadErrors.add(new UploadError(errorCode, errorMessage));
        throw createUploaderValidationException();
    }

    /**
     * Retrieve the list of upload errors.
     * @return {@link List} of upload errors.
     */
    public List<UploadError> getUploadErrors() {
        return uploadErrors;
    }

    /**
     * Create an {@link UploaderValidationException} containing all the upload errors saved in the manager.
     * @return a validation exception containing all the upload errors saved in the manager.
     */
    public UploaderValidationException createUploaderValidationException() {
        return new UploaderValidationException(uploadErrors);
    }

    /**
     * Determine if the manager has any errors.
     * @return True if the status at least one error has been persisted, false otherwise.
     */
    public boolean hasErrors() {
        return !uploadErrors.isEmpty();
    }
}
