package com.blackduck.integration.sca.upload.client.uploaders;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.LogLevel;
import com.blackduck.integration.log.PrintStreamIntLogger;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.FileBodyContent;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.google.gson.Gson;

public class ScassUploader {

    public UploadStatus upload(String signedUrl, Map<String, String> headers, Path uploadFilePath) throws IntegrationException {

        PrintStreamIntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.DEBUG);
        IntHttpClient client = new IntHttpClient(logger, new Gson(), 600, true, ProxyInfo.NO_PROXY_INFO);
        HttpUrl requestUrl = new HttpUrl(signedUrl);

        FileBodyContent bodyContent = new FileBodyContent(uploadFilePath.toFile(), null);
        Request.Builder builder = new Request.Builder()
                .url(requestUrl)
                .headers(headers)
                .method(HttpMethod.PUT)
                .bodyContent(bodyContent);

        Request request = builder.build();
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status");
        try (Response response = client.execute(request)) {
            mutableResponseStatus.setStatusCode(response.getStatusCode());
            mutableResponseStatus.setStatusMessage(response.getStatusMessage());
            // Handle errors
            client.throwExceptionForError(response);

            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();

            return new DefaultUploadStatus(statusCode, statusMessage, null);
        } catch (IOException | IntegrationException ex) {
            return new DefaultUploadStatus(mutableResponseStatus.getStatusCode(), mutableResponseStatus.getStatusMessage(),
                    new IntegrationException(FileUploader.CLOSE_RESPONSE_OBJECT_MESSAGE + ex.getCause(), ex));
        }
    }

}
