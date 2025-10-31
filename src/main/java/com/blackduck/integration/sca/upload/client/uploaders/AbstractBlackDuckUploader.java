package com.blackduck.integration.sca.upload.client.uploaders;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingSupplier;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.StringBodyContent;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadStartRequestData;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import org.apache.http.entity.ContentType;

import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractBlackDuckUploader<T extends UploadStatus> extends AbstractUploader<T> {

    public AbstractBlackDuckUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        super(chunkSize, fileUploader, uploadValidator);
    }

    /**
     * Retrieve the HTTP request headers used for starting multipart upload requests.
     *
     * @return a map of HTTP request headers.
     */
    protected abstract Map<String, String> getMultipartUploadStartRequestHeaders();

    /**
     * Retrieve the Content-Type for the multipart upload start request.
     *
     * @return the uploader Content-Type for upload start requests.
     */
    protected abstract String getMultipartUploadStartContentType();

    /**
     * Retrieve the body content for an HTTP multipart upload start request.
     * This is serialized as JSON to the Black Duck server.
     *
     * @see MultipartUploadStartRequest
     * @see MultipartUploadFileMetadata
     * @param uploadFileMetaDataSupplier The supplier of metadata used to create a start request.
     * @return the multipart start request body content.
     */
    protected abstract MultipartUploadStartRequest getMultipartUploadStartRequest(Supplier<MultipartUploadFileMetadata> uploadFileMetaDataSupplier);

    @Override
    protected Supplier<MultipartUploadStartRequestData> createUploadStartRequestData(MultipartUploadFileMetadata multipartUploadFileMetadata) {
        return () -> new MultipartUploadStartRequestData(null, getMultipartUploadStartContentType(),getMultipartUploadStartRequestHeaders(), getMultipartUploadStartRequest(()-> multipartUploadFileMetadata));
    }
}
