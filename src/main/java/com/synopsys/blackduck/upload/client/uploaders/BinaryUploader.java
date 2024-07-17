package com.synopsys.blackduck.upload.client.uploaders;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.http.HttpHeaders;

import com.synopsys.blackduck.upload.client.model.BinaryScanRequestData;
import com.synopsys.blackduck.upload.file.FileUploader;
import com.synopsys.blackduck.upload.file.model.MultipartUploadFileMetadata;
import com.synopsys.blackduck.upload.rest.model.ContentTypes;
import com.synopsys.blackduck.upload.rest.model.request.BinaryMultipartUploadStartRequest;
import com.synopsys.blackduck.upload.rest.model.request.MultipartUploadStartRequest;
import com.synopsys.blackduck.upload.rest.model.response.BinaryFinishResponseContent;
import com.synopsys.blackduck.upload.rest.status.BinaryUploadStatus;
import com.synopsys.blackduck.upload.rest.status.MutableResponseStatus;
import com.synopsys.blackduck.upload.rest.status.UploadStatus;
import com.synopsys.blackduck.upload.validation.UploadValidator;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.function.ThrowingFunction;
import com.synopsys.integration.rest.body.BodyContent;
import com.synopsys.integration.rest.body.MultipartBodyContent;
import com.synopsys.integration.rest.response.Response;

/**
 * Uploader implementation for Binary uploads.
 *
 * @see BinaryUploadStatus
 * @see BinaryScanRequestData
 * @see FileUploader
 * @see UploadValidator
 */
public class BinaryUploader extends AbstractUploader<BinaryUploadStatus> {
    private final BinaryScanRequestData binaryScanRequestData;

    /**
     * Constructor for Binary uploader.
     *
     * @param chunkSize The maximum size per chunk for a multipart upload.
     * @param fileUploader The class which uploads the file to the Black Duck server.
     * @param uploadValidator The class that provides validation for file splitting and uploader configuration.
     * @param binaryScanRequestData The class that provides the required binary specific data for uploads.
     */
    BinaryUploader(
        int chunkSize,
        FileUploader fileUploader,
        UploadValidator uploadValidator,
        BinaryScanRequestData binaryScanRequestData
    ) {
        super(chunkSize, fileUploader, uploadValidator);
        this.binaryScanRequestData = binaryScanRequestData;
    }

    /**
     * Construct the body content for the Binary HTTP request body for a standard upload.
     *
     * @param filePath The path to the file being uploaded.
     * @return the {@link BodyContent} used for upload.
     */
    @Override
    protected BodyContent createBodyContent(Path filePath) {
        return new MultipartBodyContent(
            Map.of("fileupload", filePath.toFile()),
            Map.of(
                "projectName", binaryScanRequestData.getProjectName(),
                "version", binaryScanRequestData.getVersion(),
                "codeLocationName", binaryScanRequestData.getCodeLocationName().orElse(""),
                "codeLocationUri", binaryScanRequestData.getCodeLocationUri().orElse("")
            )
        );
    }

    /**
     * Retrieve the HTTP request headers used for starting Binary multipart upload requests.
     *
     * @return a map of HTTP request headers.
     */
    @Override
    protected Map<String, String> getMultipartUploadStartRequestHeaders() {
        return Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1);
    }

    /**
     * Retrieve the body content for a Binary HTTP multipart upload start request.
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
        return new BinaryMultipartUploadStartRequest(
            multipartUploadFileMetadata.getFileSize(),
            multipartUploadFileMetadata.getChecksum(),
            binaryScanRequestData
        );
    }

    /**
     * Construct the status object for a Binary upload either containing content or error status.
     *
     * @see BinaryUploadStatus
     * @return a function that produces the {@link UploadStatus} or throws an exception.
     */
    @Override
    protected ThrowingFunction<Response, BinaryUploadStatus, IntegrationException> createUploadStatus() {
        return response -> {
            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();

            Map<String, String> responseHeaders = response.getHeaders();
            String location = Optional.ofNullable(responseHeaders.get(HttpHeaders.LOCATION)).orElseThrow(() -> new IntegrationException("Could not find Location header."));
            String eTag = Optional.ofNullable(responseHeaders.get(HttpHeaders.ETAG)).orElseThrow(() -> new IntegrationException("Could not find Etag header."));
            BinaryFinishResponseContent binaryFinishResponseContent = new BinaryFinishResponseContent(location, eTag);

            return new BinaryUploadStatus(statusCode, statusMessage, null, binaryFinishResponseContent);
        };
    }

    /**
     * Construct the status when a Binary upload error occurs.
     *
     * @see BinaryUploadStatus
     * @return a function that produces the {@link UploadStatus} when an error has occurred.
     */
    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, BinaryUploadStatus> createUploadStatusError() {
        return (response, exception) -> new BinaryUploadStatus(response.getStatusCode(), response.getStatusMessage(), exception, null);
    }
}