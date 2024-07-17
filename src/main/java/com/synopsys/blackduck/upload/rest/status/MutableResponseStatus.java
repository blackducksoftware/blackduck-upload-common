package com.synopsys.blackduck.upload.rest.status;

import java.util.Objects;

/**
 * Object that maintains the current HTTP status code and message from HTTP requests in order to construct the correct {@link UploadStatus}
 * object when the multipart upload completes.  This is used as a container for the HTTP status values for each HTTP request made to the Black Duck server
 * for multipart uploads.
 *
 * The object is mutated as the upload proceeds in order to capture the most recent HTTP status code and message for the latest HTTP request.
 * This includes updating this object when the following HTTP requests are made:
 * <ul>
 *     <li>Start upload</li>
 *     <li>Upload part of the file</li>
 *     <li>Finish upload</li>
 * </ul>
 * This object is updated for each of those requests which allows the {@link UploadStatus} to be created with the correct HTTP status code and message
 * that caused the multipart upload to terminate.
 * <br/>
 * <br/>
 * The upload may terminate for the following reasons:
 * <ol>
 *     <li>Successful upload of the file</li>
 *     <li>Error occurred executing one of the types of HTTP requests mentioned above</li>
 * </ol>
 */
public class MutableResponseStatus {
    private int statusCode;
    private String statusMessage;

    /**
     * Constructor for the container object of the HTTP status code and message.
     * @param statusCode    The HTTP status code that is the end result of the multipart upload when it terminated regardless of whether the upload is successful or not.
     * @param statusMessage The HTTP status message that describes an error if an error occurred or a success message indicating the upload succeeded.
     */
    public MutableResponseStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Retrieve the HTTP status code for the latest recorded request in a multipart upload.
     * @return The HTTP status code for the latest recorded request.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Retrieve the HTTP status message for the latest recorded request in a multipart upload.
     * @return The HTTP status message for the latest recoded request.
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Replace the HTTP status code with the value from the latest HTTP request made in a multipart upload.
     * @param statusCode The HTTP status code to record.
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Replace the HTTP status message with the value from the latest HTTP request made in a multipart upload.
     * @param statusMessage The HTTP status message to record.
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        MutableResponseStatus that = (MutableResponseStatus) o;
        return statusCode == that.statusCode && Objects.equals(statusMessage, that.statusMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, statusMessage);
    }

    @Override
    public String toString() {
        return "MutableResponseStatus{" +
            "statusCode=" + statusCode +
            ", statusMessage='" + statusMessage + '\'' +
            '}';
    }
}