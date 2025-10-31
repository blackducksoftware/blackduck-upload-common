package com.blackduck.integration.sca.upload.client.uploaders;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.rest.body.BodyContent;
import com.blackduck.integration.rest.body.EntityBodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadStartRequestData;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ScassMultipartUploader extends AbstractUploader<DefaultUploadStatus> {

    public ScassMultipartUploader(int chunkSize, FileUploader fileUploader, UploadValidator uploadValidator) {
        super(chunkSize, fileUploader, uploadValidator);
    }

    @Override
    protected BodyContent createBodyContent(Path filePath) {
        FileEntity entity = new FileEntity(filePath.toFile(), ContentType.create(ContentTypes.APPLICATION_CONTAINER_SCAN_DATA_V1));
        return new EntityBodyContent(entity);
    }

    @Override
    protected Supplier<MultipartUploadStartRequestData> createUploadStartRequestData(MultipartUploadFileMetadata multipartUploadFileMetadata) {
        return null;
    }

    @Override
    protected ThrowingFunction<Response, DefaultUploadStatus, IntegrationException> createUploadStatus() throws IntegrationException {
        return response -> {
            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();
            return new DefaultUploadStatus(statusCode, statusMessage, null);
        };
    }

    @Override
    protected BiFunction<MutableResponseStatus, IntegrationException, DefaultUploadStatus> createUploadStatusError() {
        return (response, exception) -> new DefaultUploadStatus(response.getStatusCode(), response.getStatusMessage(), exception);
    }
}
