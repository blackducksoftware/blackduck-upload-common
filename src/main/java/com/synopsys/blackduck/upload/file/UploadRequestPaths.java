package com.synopsys.blackduck.upload.file;

/**
 * Class containing paths to Black Duck file upload endpoints.
 */
public class UploadRequestPaths {
    private static final String MULTIPART_UPLOAD_START_REQUEST_FORMAT = "multipart";
    private static final String MULTIPART_UPLOAD_PART_REQUEST_FORMAT = "multipart/%s";
    private static final String MULTIPART_UPLOAD_FINISH_REQUEST_FORMAT = "multipart/%s/completed";

    private String prefix;

    /**
     * Constructor for the upload request paths object.
     *
     * @param prefix The full Black Duck upload url.
     */
    public UploadRequestPaths(String prefix) {
        setPrefix(prefix);
    }

    private void setPrefix(String rawPrefix) {
        this.prefix = (rawPrefix + "/").replaceAll("(/){2,}", "/");
    }

    /**
     * Retrieve the Black Duck url for a standard file upload.
     *
     * @return A string url.
     */
    public String getUploadRequestPath() {return this.prefix;}

    /**
     * Retrieve the Black Duck url for initiating a multipart file upload.
     *
     * @return A string url.
     */
    public String getMultipartUploadStartRequestPath() {
        return prefix + MULTIPART_UPLOAD_START_REQUEST_FORMAT;
    }

    /**
     * Retrieve the Black Duck url for uploading parts of a file for multipart file upload.
     *
     * @param id The identifier of multipart file upload.
     * @return A string url.
     */
    public String getMultipartUploadPartRequestPath(String id) {
        return String.format(prefix + MULTIPART_UPLOAD_PART_REQUEST_FORMAT, id);
    }

    /**
     * Retrieve the Black Duck url for finishing a multipart file upload.
     *
     * @param id The identifier of multipart file upload.
     * @return A string url.
     */
    public String getMultipartUploadFinishRequestPath(String id) {
        return String.format(prefix + MULTIPART_UPLOAD_FINISH_REQUEST_FORMAT, id);
    }
}
