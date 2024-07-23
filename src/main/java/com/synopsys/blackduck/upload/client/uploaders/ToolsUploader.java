package com.synopsys.blackduck.upload.client.uploaders;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.http.HttpHeaders;

import com.synopsys.blackduck.upload.file.FileUploader;
import com.synopsys.blackduck.upload.file.model.MultipartUploadFileMetadata;
import com.synopsys.blackduck.upload.rest.model.ContentTypes;
import com.synopsys.blackduck.upload.rest.model.request.MultipartUploadStartRequest;
import com.synopsys.blackduck.upload.rest.status.MutableResponseStatus;
import com.synopsys.blackduck.upload.rest.status.UploadStatus;
import com.synopsys.blackduck.upload.validation.UploadValidator;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.function.ThrowingFunction;
import com.synopsys.integration.rest.body.BodyContent;
import com.synopsys.integration.rest.response.Response;

/**
 * Uploader implementation for Tools uploads.
 *
 * @see UploadStatus
 * @see FileUploader
 * @see UploadValidator
 */
public class ToolsUploader extends AbstractUploader<UploadStatus> {

    /**
     * Constructor for Tools uploads.
     *
     * @param chunkSize The maximum size per chunk for a multipart upload.
     * @param fileUploader The class which uploads the file to the Black Duck server.
     * @param uploadValidator The class that provides validation for file splitting and uploader configuration.
     */
    ToolsUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        super(chunkSize, fileUploader, uploadValidator);
    }

    /**
     * Construct the body content for the ToolsHTTP request body for a standard upload.
     *
     * @param filePath The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    @Override
    protected BodyContent createBodyContent(Path filePath) {
        throw new UnsupportedOperationException("Default upload for data type not implemented");
        // TODO: Implement for tools upload. multipart content type similar to binary
    }

    /**
     * Retrieve the HTTP request headers used for starting Tools multipart upload requests.
     *
     * @return a map of HTTP request headers.
     */
    @Override
    protected Map<String, String> getMultipartUploadStartRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_MULTIPART_UPLOAD_START_V1);
        return headers;
    }

    /**
     * Retrieve the body content for a Tools HTTP multipart upload start request.
     * This is serialized as JSON to the Black Duck server.
     *
     * @see MultipartUploadStartRequest
     * @see MultipartUploadFileMetadata
     * @param uploadFileMetaDataSupplier The supplier of metadata used to create a start request.
     * @return the multipart start request body content.
     */
    @Override
    protected MultipartUploadStartRequest getMultipartUploadStartRequest(Supplier<MultipartUploadFileMetadata> uploadFileMetaDataSupplier) {
        MultipartUploadFileMetadata multipartUploadFileMetadata = uploadFileMetaDataSupplier.get();
        return new MultipartUploadStartRequest(multipartUploadFileMetadata.getFileSize(), multipartUploadFileMetadata.getChecksum());
    }

    /**
     * Construct the status object for a Tools upload either containing content or error status.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     */
    @Override
    protected ThrowingFunction<Response, UploadStatus, IntegrationException> createUploadStatus() {
        //TODO: to be implemented in phase 2
        throw new UnsupportedOperationException("This function is not yet supported for this uploader type");
    }

    /**
     * Construct the status when a Tools upload error occurs.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, UploadStatus> createUploadStatusError() {
        //TODO: to be implemented in phase 2
        throw new UnsupportedOperationException("This function is not yet supported for this uploader type");
    }
}
