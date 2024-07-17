package com.synopsys.blackduck.upload.rest.model.request;

/**
 * Data model object used as the content of a start multipart upload request.
 * This object is serialized as JSON and sent as the body content of an HTTP request to start a multipart upload.
 */
public class MultipartUploadStartRequest {
    private final long fileSize;
    private final String checksum;

    /**
     * Constructor for the upload start request data.
     * @param fileSize The size of the file to be uploaded.
     * @param checksum The base 64 encoded MD5 checksum of the file to be uploaded.
     */
    public MultipartUploadStartRequest(long fileSize, String checksum) {
        this.fileSize = fileSize;
        this.checksum = checksum;
    }

    /**
     * Retrieve the size of the file to be uploaded.
     * @return file size.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Retrieve the base 64 encoded MD5 checksum of the file.
     * @return file checksum.
     */
    public String getChecksum() {
        return checksum;
    }
}
