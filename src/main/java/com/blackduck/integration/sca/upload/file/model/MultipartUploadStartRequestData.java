package com.blackduck.integration.sca.upload.file.model;

import java.util.Map;
import java.util.Objects;

public class MultipartUploadStartRequestData {

    private final String baseUrl;
    private final String contentType;
    private final Map<String, String> headers;
    private final Object bodyContent;

    public MultipartUploadStartRequestData(String baseUrl, String contentType, Map<String, String> headers, Object bodyContent) {
        this.baseUrl = baseUrl;
        this.contentType = contentType;
        this.headers = headers;
        this.bodyContent = bodyContent;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getBodyContent() {
        return bodyContent;
    }

    public <T> T getBodyContentAs(Class<T> targetClass) throws ClassCastException {
        if(targetClass.isAssignableFrom(getBodyContent().getClass()))
        {
            return targetClass.cast(bodyContent);
        } else {
            throw new ClassCastException("Cannot cast " + bodyContent.getClass() + " to " + targetClass);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MultipartUploadStartRequestData that = (MultipartUploadStartRequestData) o;
        return Objects.equals(baseUrl, that.baseUrl) && Objects.equals(contentType, that.contentType) && Objects.equals(headers, that.headers) && Objects.equals(bodyContent, that.bodyContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, contentType, headers, bodyContent);
    }

    @Override
    public String toString() {
        return "MultipartUploadStartRequestData{" +
                "baseUrl='" + baseUrl + '\'' +
                ", contentType='" + contentType + '\'' +
                ", headers=" + headers +
                ", bodyContent=" + bodyContent +
                '}';
    }
}
