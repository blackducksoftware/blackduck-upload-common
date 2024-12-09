package com.blackduck.integration.sca.upload.file;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import com.blackduck.integration.rest.RestConstants;
import com.blackduck.integration.rest.exception.IntegrationRestException;
import com.blackduck.integration.rest.response.Response;

public class FileUploaderResponse implements Response {
    private final IntegrationRestException integrationRestException;

    public FileUploaderResponse(IntegrationRestException integrationRestException) {
        this.integrationRestException = integrationRestException;
    }

    @Override
    public HttpUriRequest getRequest() {
        throw new NotImplementedException();
    }

    @Override
    public int getStatusCode() {
        return integrationRestException.getHttpStatusCode();
    }

    @Override
    public boolean isStatusCodeSuccess() {
        return getStatusCode() >= RestConstants.OK_200 && getStatusCode() < RestConstants.MULT_CHOICE_300;
    }

    @Override
    public boolean isStatusCodeError() {
        return getStatusCode() >= RestConstants.BAD_REQUEST_400;
    }

    @Override
    public String getStatusMessage() {
        return integrationRestException.getHttpStatusMessage();
    }

    @Override
    public InputStream getContent() {
        throw new NotImplementedException();
    }

    @Override
    public String getContentString() {
        return integrationRestException.getHttpResponseContent();
    }

    @Override
    public String getContentString(Charset encoding) {
        throw new NotImplementedException();
    }

    @Override
    public Long getContentLength() {
        throw new NotImplementedException();
    }

    @Override
    public String getContentEncoding() {
        throw new NotImplementedException();
    }

    @Override
    public String getContentType() {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, String> getHeaders() {
        throw new NotImplementedException();
    }

    @Override
    public String getHeaderValue(String name) {
        throw new NotImplementedException();
    }

    @Override
    public CloseableHttpResponse getActualResponse() {
        throw new NotImplementedException();
    }

    @Override
    public void close() {
        // Implementation is based off of exception and does not contain a response
        // therefor nothing to do here.
    }

    @Override
    public long getLastModified() {
        throw new NotImplementedException();
    }

    @Override
    public void throwExceptionForError() {
        // Implementation is based off of exception and does not contain a response
        // therefor nothing to do here.
    }
}
