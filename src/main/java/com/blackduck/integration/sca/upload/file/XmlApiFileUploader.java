package com.blackduck.integration.sca.upload.file;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class XmlApiFileUploader extends AbstractFileUploader {

    public XmlApiFileUploader(IntHttpClient httpClient, int multipartUploadPartRetryAttempts, long multipartUploadPartRetryInitialInterval, int multipartUploadTimeoutInMinutes, ExecutorService executorService) {
        super(httpClient, multipartUploadPartRetryAttempts, multipartUploadPartRetryInitialInterval, multipartUploadTimeoutInMinutes, executorService);
    }

    @Override
    protected HttpUrl getUploadUrl() throws IntegrationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Request getMultipartUploadStartRequest(Map<String, String> startRequestHeaders, String multipartUploadStartContentType, MultipartUploadStartRequest multipartUploadStartRequest) throws IntegrationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Request.Builder getMultipartUploadPartRequestBuilder(MultipartUploadFileMetadata fileMetaData, String uploadUrl, MultipartUploadFilePart part) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Request getMultipartUploadFinishRequest(String uploadUrl, Map<Integer, String> tagOrderMap) throws IntegrationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
