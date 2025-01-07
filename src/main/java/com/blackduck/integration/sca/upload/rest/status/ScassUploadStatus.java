/*
 * blackduck-upload-common
 *
 * Copyright (c) 2025 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance
 * Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.rest.status;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.exception.IntegrationException;

/**
 * Class that represents the overall status of a file upload to SCASS.
 * @see UploadStatus
 */
public class ScassUploadStatus extends UploadStatus {

    private static final long serialVersionUID = 1L;

    private String content;

    public ScassUploadStatus(
        int statusCode, String statusMessage, @Nullable IntegrationException exception,
        String content
    ) {
        super(statusCode, statusMessage, exception);
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean hasContent() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(content);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ScassUploadStatus other = (ScassUploadStatus) obj;
        return Objects.equals(content, other.content);
    }

}
