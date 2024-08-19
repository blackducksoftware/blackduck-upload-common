package com.synopsys.blackduck.upload.rest.model;

/**
 * This class contains constants that define the different content types that are sent with the HTTP requests for multipart uploads.
 * These constants are used as the values when the Content-Type header is created for an HTTP request to the Black Duck server for multipart uploads.
 */
public class ContentTypes {
    // Start upload content types.
    /**
     * The content type for starting a multipart upload request to the Black Duck server.
     * This should be the default content type for starting a multipart upload request if there isn't a specific content type defined for the file type.
     */
    public static final String APPLICATION_MULTIPART_UPLOAD_START_V1 = "application/vnd.blackducksoftware.multipart-upload-start-1+json";
    /**
     * The content type for starting a multipart upload request of a binary file to the Black Duck server.
     */
    public static final String APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1 = "application/vnd.blackducksoftware.binary-multipart-upload-start-1+json";

    /**
     * The content type for performing a standard upload request of a container file to the Black Duck server.
     */
    public static final String APPLICATION_CONTAINER_SCAN_DATA_V1 = "application/vnd.blackducksoftware.container-scan-data-1+octet-stream";

    // Upload parts content types.
    /**
     * The content type for an upload request of part/chunks of the file up to the Black Duck server.
     */
    public static final String APPLICATION_MULTIPART_UPLOAD_DATA_V1 = "application/vnd.blackducksoftware.multipart-upload-data-1+octet-stream";

    // Finish upload content types.
    /**
     * The content type for finishing a multipart upload request to the Black Duck server.
     */
    public static final String APPLICATION_MULTIPART_UPLOAD_FINISH_V1 = "application/vnd.blackducksoftware.multipart-upload-finish-1+json";

    // Make default constructor private so that this class cannot be instantiated since it only contains constants.
    private ContentTypes() {}
}
