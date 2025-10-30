package com.blackduck.integration.sca.upload.file;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.StringBodyContent;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class BlackDuckFileUploader extends AbstractFileUploader {
    private final BlackDuckHttpClient httpClient;
    private final UploadRequestPaths uploadRequestPaths;

    public BlackDuckFileUploader(BlackDuckHttpClient httpClient, int multipartUploadPartRetryAttempts, long multipartUploadPartRetryInitialInterval, int multipartUploadTimeoutInMinutes, ExecutorService executorService, UploadRequestPaths uploadRequestPaths) {
        super(httpClient, multipartUploadPartRetryAttempts, multipartUploadPartRetryInitialInterval, multipartUploadTimeoutInMinutes, executorService);
        this.httpClient = httpClient;
        this.uploadRequestPaths = uploadRequestPaths;
    }

    @Override
    protected HttpUrl getUploadUrl() throws IntegrationException {
        return httpClient.getBlackDuckUrl().appendRelativeUrl(uploadRequestPaths.getUploadRequestPath());
    }

    @Override
    protected Request getMultipartUploadStartRequest(Map<String, String> startRequestHeaders, String multipartUploadStartContentType, MultipartUploadStartRequest multipartUploadStartRequest) throws IntegrationException {
        String requestPath = uploadRequestPaths.getMultipartUploadStartRequestPath();
        HttpUrl requestUrl = httpClient.getBlackDuckUrl().appendRelativeUrl(requestPath);

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .method(HttpMethod.POST)
                .headers(startRequestHeaders)
                .bodyContent(new StringBodyContent(
                        httpClient.getGson().toJson(multipartUploadStartRequest),
                        ContentType.create(multipartUploadStartContentType)
                ));

        return builder.build();
    }

    @Override
    protected Request.Builder getMultipartUploadPartRequestBuilder(MultipartUploadFileMetadata fileMetaData, String uploadUrl, MultipartUploadFilePart part) throws IntegrationException {
        HttpUrl requestUrl = new HttpUrl(uploadUrl);
        return  new Request.Builder()
                .url(requestUrl)
                .method(HttpMethod.PUT)
                .headers(createUploadHeaders(fileMetaData, part));
    }

    @Override
    protected Request getMultipartUploadFinishRequest(String uploadUrl, Map<Integer,String> tagOrderMap) throws IntegrationException {
        HttpMethod httpMethod = HttpMethod.POST;
        HttpUrl requestUrl = new HttpUrl(uploadUrl + "/completed");

        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .method(httpMethod)
                .addHeader(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_MULTIPART_UPLOAD_FINISH_V1);

        return builder.build();
    }

    private Map<String, String> createUploadHeaders(MultipartUploadFileMetadata fileMetaData, MultipartUploadFilePart part) {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(CONTENT_DIGEST_HEADER, String.format("md5=:%s:", part.getChecksum()));
        requestHeaders.put(
                HttpHeaders.CONTENT_RANGE,
                String.format("bytes %s-%s/%s", part.getStartByteRange(), part.getStartByteRange() + part.getChunkSize() - 1, fileMetaData.getFileSize())
        );
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_MULTIPART_UPLOAD_DATA_V1);

        return requestHeaders;
    }
}
