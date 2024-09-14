/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.blackduck.upload.rest.status;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.blackduck.upload.rest.model.response.BinaryFinishResponseContent;
import com.blackduck.integration.exception.IntegrationException;

/**
 * Class that represents the overall status of a multipart upload of a binary file that is returned to a user.
 * @see BinaryFinishResponseContent
 * @see UploadStatus
 */
public class BinaryUploadStatus extends UploadStatus implements Serializable {
    private static final long serialVersionUID = 3589110143128921002L;
    private final BinaryFinishResponseContent responseContent;

    /**
     * Constructor for the status of the binary multipart upload.
     * @param statusCode      The HTTP status code that is the end result of the multipart upload when it terminated regardless of whether the upload is successful or not.
     * @param statusMessage   The HTTP status message that describes an error if an error occurred or a success message indicating the upload succeeded.
     * @param exception       The exception that caused a failure of the upload if present, otherwise it is null for successful uploads.
     * @param responseContent The content when the multipart binary upload succeeds.
     */
    public BinaryUploadStatus(
        int statusCode,
        String statusMessage,
        @Nullable IntegrationException exception,
        @Nullable BinaryFinishResponseContent responseContent
    ) {
        super(statusCode, statusMessage, exception);
        this.responseContent = responseContent;
    }

    /**
     * Retrieve the content object when the binary multipart upload succeeds.
     * @return The {@link BinaryFinishResponseContent} containing the URL location and entity tag of the binary scan.
     */
    public Optional<BinaryFinishResponseContent> getResponseContent() {
        return Optional.ofNullable(responseContent);
    }

    @Override
    public boolean hasContent() {
        return getResponseContent().isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        BinaryUploadStatus that = (BinaryUploadStatus) o;
        return Objects.equals(responseContent, that.responseContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), responseContent);
    }

    @Override
    public String toString() {
        return "BinaryUploadStatus{" +
            "statusCode=" + getStatusCode() +
            ", statusMessage='" + getStatusMessage() + '\'' +
            ", responseContent='" + responseContent + '\'' +
            ", exception=" + getException() +
            '}';
    }
}