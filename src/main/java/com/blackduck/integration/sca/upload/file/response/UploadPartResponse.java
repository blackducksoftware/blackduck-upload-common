/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.file.response;

import java.util.Optional;

import com.blackduck.integration.rest.exception.IntegrationRestException;
import com.blackduck.integration.rest.response.Response;

public class UploadPartResponse {
    private final int httpStatusCode;
    private final String httpStatusMessage;
    private final boolean hasResponse;
    private final Response response;

    private UploadPartResponse(
        final int httpStatusCode,
        final String httpStatusMessage,
        final boolean hasResponse,
        final Response response
    ) {
        this.httpStatusCode = httpStatusCode;
        this.httpStatusMessage = httpStatusMessage;
        this.hasResponse = hasResponse;
        this.response = response;
    }

    public static UploadPartResponse fromException(IntegrationRestException integrationRestException) {
        return new UploadPartResponse(integrationRestException.getHttpStatusCode(), integrationRestException.getHttpStatusMessage(), false, null);
    }

    public static UploadPartResponse fromResponse(Response response) {
        return new UploadPartResponse(response.getStatusCode(), response.getStatusMessage(), true, response);
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getHttpStatusMessage() {
        return httpStatusMessage;
    }

    public boolean hasResponse() {
        return hasResponse;
    }

    public Optional<Response> getResponse() {
        return Optional.ofNullable(response);
    }
}
