package com.blackduck.integration.sca.upload.file;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;

import java.util.Map;
import java.util.function.BiFunction;

public interface FileUploader {

    /**
     * Performs a standard file upload to Black Duck.
     *
     * @param bodyContent The {@link BodyContent} of the upload file request.
     * @param uploadStatusFunction {@link ThrowingFunction} that generates the {@link UploadStatus} from the response.
     * @param uploadStatusErrorFunction {@link BiFunction} that generates the error {@link UploadStatus} from the response and exception thrown.
     * @return {@link UploadStatus} status of the upload.
     * @param <T> {@link UploadStatus} status of the upload for the file type.
     * @throws IntegrationException if an error occurred while making the request to Black Duck.
     */
     <T extends UploadStatus> T upload(
            BodyContent bodyContent,
            ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction,
            BiFunction<MutableResponseStatus, IntegrationException, T> uploadStatusErrorFunction
    ) throws IntegrationException;

    /**
     * Performs a multipart file upload to Black Duck.
     *
     * @param multipartUploadFileMetadata The {@link MultipartUploadFileMetadata} for the file to upload.
     * @param multipartUploadStartRequestHeaders A {@link Map} of headers for the multipart upload start request.
     * @param multipartUploadStartContentType The Content-Type for the start request body.
     * @param multipartUploadStartRequest The data object for multipart upload start request.
     * @param uploadStatusFunction {@link ThrowingFunction} that generates the {@link UploadStatus} from the response.
     * @param uploadStatusErrorFunction {@link BiFunction} that generates the error {@link UploadStatus} from the response and exception thrown.
     * @return {@link UploadStatus} status of the upload.
     * @param <T> status of the upload for the file type.
     */
    <T extends UploadStatus> T multipartUpload(
            MultipartUploadFileMetadata multipartUploadFileMetadata,
            Map<String, String> multipartUploadStartRequestHeaders,
            String multipartUploadStartContentType,
            MultipartUploadStartRequest multipartUploadStartRequest,
            ThrowingFunction<Response, T, IntegrationException> uploadStatusFunction,
            BiFunction<MutableResponseStatus, IntegrationException, T> uploadStatusErrorFunction
    );
}
