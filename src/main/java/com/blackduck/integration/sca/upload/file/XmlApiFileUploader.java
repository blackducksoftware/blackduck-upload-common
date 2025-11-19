package com.blackduck.integration.sca.upload.file;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadStartRequestData;
import com.blackduck.integration.sca.upload.file.model.MultipartUrlData;

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
    protected Request getMultipartUploadStartRequest(MultipartUploadStartRequestData uploadStartRequestData) throws IntegrationException {
        Map<String, String> startHeaders = uploadStartRequestData.getHeaders();
        HttpUrl requestUrl = new HttpUrl(uploadStartRequestData.getBaseUrl());

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(startHeaders)
                .method(HttpMethod.POST);

        return builder.build();
    }

    @Override
    protected Request.Builder getMultipartUploadPartRequestBuilder(MultipartUploadFileMetadata fileMetaData, String uploadUrl, MultipartUploadFilePart part) throws IntegrationException {
        Map<String, String> allHeaders = part.getPartUploadData().getHeaders();
        HttpUrl partUrl = new HttpUrl(part.getPartUploadData().getUrl());

        return new Request.Builder()
                .url(partUrl)
                .method(HttpMethod.PUT)
                .headers(allHeaders);
    }

    @Override
    protected Request getMultipartUploadFinishRequest(String uploadUrl, Map<Integer, String> tagOrderMap, MultipartUrlData completeUploadUrl) throws IntegrationException {
        Map<String, String> finishHeaders = completeUploadUrl.getHeaders();
        HttpUrl requestUrl = new HttpUrl(completeUploadUrl.getUrl());

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(finishHeaders)
                .method(HttpMethod.POST);

        return builder.build();
    }
}
