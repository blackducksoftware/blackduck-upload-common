package com.synopsys.blackduck.upload.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.blackduck.integration.exception.IntegrationException;

/**
 * Thrown to indicate that an error or multiple errors have occurred during validation of the upload.
 */
public class UploaderValidationException extends IntegrationException implements Serializable {
    private static final long serialVersionUID = 1904821297808062717L;
    private final List<UploadError> uploadErrors;

    /**
     * Constructs a new exception with the upload errors, and it's cause. In addition to this is accepts additional parameters inherited from {@link Exception}.
     * @param uploadErrors       A list of errors that occurred prior to creating the exception.
     * @param cause              the cause of the exception.
     * @param enableSuppression  whether or not suppression is enabled or disabled.
     * @param writableStackTrace whether or not the stack trace should be writable.
     */
    public UploaderValidationException(List<UploadError> uploadErrors, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(printMessages(uploadErrors), cause, enableSuppression, writableStackTrace);
        this.uploadErrors = uploadErrors;
    }

    /**
     * Constructs a new exception with the upload errors, and it's cause.
     * @param uploadErrors A list of errors that occurred prior to creating the exception.
     * @param cause        the cause of the exception.
     */
    public UploaderValidationException(List<UploadError> uploadErrors, Throwable cause) {
        super(printMessages(uploadErrors), cause);
        this.uploadErrors = uploadErrors;
    }

    /**
     * Constructs a new exception with the upload errors, and it's cause.
     * @param uploadErrors A list of errors that occurred prior to creating the exception.
     */
    public UploaderValidationException(List<UploadError> uploadErrors) {
        super(printMessages(uploadErrors));
        this.uploadErrors = uploadErrors;
    }

    /**
     * Constructs an exception containing only the cause.
     * @param cause the cause of the exception.
     */
    public UploaderValidationException(Throwable cause) {
        super(cause);
        this.uploadErrors = new ArrayList<>();
    }

    /**
     * Constructs an empty exception.
     */
    public UploaderValidationException() {
        this.uploadErrors = new ArrayList<>();
    }

    /**
     * Retrieves a list of all errors associated with the exception.
     * @return {@link List} of errors that occurred during an upload.
     */
    public List<UploadError> getUploadErrors() {
        return uploadErrors;
    }

    /**
     * Determine if an error exists in the list of errors within the exception.
     * @return True if a specific {@link ErrorCode} exists in the list of errors, false otherwise.
     * @see ErrorCode
     */
    public boolean hasError(ErrorCode expectedErrorCode) {
        return uploadErrors.stream()
            .map(UploadError::getErrorCode)
            .anyMatch(errorCode -> errorCode.equals(expectedErrorCode));
    }

    private static String printMessages(List<UploadError> errors) {
        List<String> formattedErrorMessages = errors
            .stream()
            .map(UploadError::createFormattedErrorMessage)
            .collect(Collectors.toList());
        return String.format("Uploader Errors (%s):%n%s", errors.size(), String.join("\n", formattedErrorMessages));
    }

}