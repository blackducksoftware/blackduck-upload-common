/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.rest.status;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.exception.IntegrationException;

/**
 * Class that represents the overall status of a multipart upload of a file that is returned to a user.
 * This includes the HTTP status codes when the upload terminated, which can be found with the {@link UploadStatus#getStatusCode()} method.
 * If there is a failure due to an exception, then the exception is captured and accessible with the {@link UploadStatus#getException()} method.
 * The exception is used to determine if the status represents a failure or not.
 */
public abstract class UploadStatus implements Serializable {
    private static final long serialVersionUID = 5731404099313813486L;
    private final int statusCode;
    private final String statusMessage;

    private final IntegrationException exception;

    /**
     * Constructor for the status of the multipart upload.
     * @param statusCode    The HTTP status code that is the end result of the multipart upload when it terminated regardless of whether the upload is successful or not.
     * @param statusMessage The HTTP status message that describes an error if an error occurred or a success message indicating the upload succeeded.
     * @param exception     The exception that caused a failure of the upload if present, otherwise it is null for successful uploads.
     */
    protected UploadStatus(int statusCode, String statusMessage, @Nullable IntegrationException exception) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.exception = exception;
    }

    /**
     * Retrieve the HTTP status code when the multipart upload terminated.
     * @return The HTTP status code where the upload terminated regardless of success or failure.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Retrieve the HTTP status message describing how the upload terminated whether success or the failure cause.
     * @return The HTTP status message where the upload terminated regardless of success or failure.
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Determine if the status of the upload has an error.
     * @return True if the upload terminated due to an error, false otherwise.
     */
    public boolean isError() {
        return getException().isPresent();
    }

    /**
     * Retrieve the exception that caused the upload to terminate if present.
     * @return The exception if present, otherwise {@link Optional#empty()}.
     */
    public Optional<IntegrationException> getException() {
        return Optional.ofNullable(exception);
    }

    /**
     * Determine if the status contains content as a result of performing a multipart upload.
     * @return True if the status contains content from the multipart upload, false otherwise.
     */
    public abstract boolean hasContent();

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UploadStatus that = (UploadStatus) o;
        return statusCode == that.statusCode && Objects.equals(statusMessage, that.statusMessage) && Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, statusMessage, exception);
    }

    @Override
    public String toString() {
        return "UploadStatus{" +
            "statusCode=" + statusCode +
            ", statusMessage='" + statusMessage + '\'' +
            ", exception=" + exception +
            '}';
    }
}