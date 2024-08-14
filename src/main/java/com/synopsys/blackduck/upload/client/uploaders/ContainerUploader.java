package com.synopsys.blackduck.upload.client.uploaders;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

import com.synopsys.blackduck.upload.file.FileUploader;
import com.synopsys.blackduck.upload.file.model.MultipartUploadFileMetadata;
import com.synopsys.blackduck.upload.rest.model.ContentTypes;
import com.synopsys.blackduck.upload.rest.model.request.MultipartUploadStartRequest;
import com.synopsys.blackduck.upload.rest.status.ContainerUploadStatus;
import com.synopsys.blackduck.upload.rest.status.MutableResponseStatus;
import com.synopsys.blackduck.upload.rest.status.UploadStatus;
import com.synopsys.blackduck.upload.validation.UploadValidator;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.function.ThrowingFunction;
import com.synopsys.integration.rest.body.BodyContent;
import com.synopsys.integration.rest.body.EntityBodyContent;
import com.synopsys.integration.rest.response.Response;

/**
 * Uploader implementation for Container uploads.
 *
 * @see UploadStatus
 * @see FileUploader
 * @see UploadValidator
 */
public class ContainerUploader extends AbstractUploader<ContainerUploadStatus> {

    /**
     * Constructor for Container uploads.
     *
     * @param chunkSize The maximum size per chunk for a multipart upload.
     * @param fileUploader The class which uploads the file to the Black Duck server.
     * @param uploadValidator The class that provides validation for file splitting and uploader configuration.
     */
    ContainerUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        super(chunkSize, fileUploader, uploadValidator);
    }

    /**
     * Construct the body content for the Container HTTP request body for a standard upload.
     *
     * @param filePath The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    @Override
    protected BodyContent createBodyContent(Path filePath) {
        FileEntity entity = new FileEntity(filePath.toFile(), ContentType.create(ContentTypes.APPLICATION_CONTAINER_SCAN_DATA_V1));
        return new EntityBodyContent(entity);
    }


    /**
     * Retrieve the HTTP request headers used for starting Container multipart upload requests.
     *
     * @return a map of HTTP request headers.
     */
    @Override
    protected Map<String, String> getMultipartUploadStartRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, getMultipartUploadStartContentType());
        return headers;
    }

    /**
     * Retrieve the Content-Type for the multipart container upload start requests.
     *
     * @return the Content-Type for container upload start requests.
     */
    @Override
    protected String getMultipartUploadStartContentType() {
        return ContentTypes.APPLICATION_MULTIPART_UPLOAD_START_V1;
    }

    /**
     * Retrieve the body content for a Container HTTP multipart upload start request.
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
     * Construct the status object for a Container upload either containing content or error status.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     */
    @Override
    protected ThrowingFunction<Response, ContainerUploadStatus, IntegrationException> createUploadStatus() {
        return response -> {
            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();
            return new ContainerUploadStatus(statusCode, statusMessage, null);
        };
    }

    /**
     * Construct the status when a Container upload error occurs.
     *
     * @see UploadStatus
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, ContainerUploadStatus> createUploadStatusError() {
        return (response, exception) -> new ContainerUploadStatus(response.getStatusCode(), response.getStatusMessage(), exception);
    }
}
