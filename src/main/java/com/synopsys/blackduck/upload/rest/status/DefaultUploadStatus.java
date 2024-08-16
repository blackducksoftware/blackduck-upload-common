package com.synopsys.blackduck.upload.rest.status;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import com.synopsys.integration.exception.IntegrationException;

/**
 * Class that represents the overall status of a multipart upload file that is returned to a user.
 * @see UploadStatus
 */
public class DefaultUploadStatus extends UploadStatus implements Serializable {

    /**
     * Constructor for the status of the multipart upload.
     * @param statusCode    The HTTP status code that is the end result of the multipart upload when it terminated regardless of whether the upload is successful or not.
     * @param statusMessage The HTTP status message that describes an error if an error occurred or a success message indicating the upload succeeded.
     * @param exception     The exception that caused a failure of the upload if present, otherwise it is null for successful uploads.
     */
    public DefaultUploadStatus(final int statusCode, final String statusMessage, @Nullable final IntegrationException exception) {
        super(statusCode, statusMessage, exception);
    }

    @Override
    public boolean hasContent() {
        // No content is returned in finish requests do not have a response body with content, therefore we return false here.
        return false;
    }
}
