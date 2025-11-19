package com.blackduck.integration.sca.upload.file.model;

import java.util.List;
import java.util.Map;

public class MultipartUrlData {

    private String url;
    private String method;
    private Map<String, String> allHeaders;

    public String getUrl() {
        return url;
    }
    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return allHeaders;
    }
}
