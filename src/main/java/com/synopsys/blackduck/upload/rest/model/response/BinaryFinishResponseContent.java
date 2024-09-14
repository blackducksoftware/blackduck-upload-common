/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.blackduck.upload.rest.model.response;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data model object containing the response content from a finish of a binary multipart upload.
 * This object is deserialized from JSON contained in the body content of an HTTP response finishing a binary
 * multipart upload.
 */
public class BinaryFinishResponseContent implements Serializable {
    private static final long serialVersionUID = -4720933923156829144L;
    private final String location;
    private final String eTag;

    /**
     * Constructor for the finish response of a binary multipart upload.
     * @param location The URL of the newly created resource as a response to the multipart upload completing.
     * @param eTag     The entity tag containing the identifier of the response resource from a multipart upload.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag">ETag Header</a>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Location">Location Header</a>
     */
    public BinaryFinishResponseContent(String location, String eTag) {
        this.location = location;
        this.eTag = eTag;
    }

    /**
     * Retrieve the location URL of the resource created from completing a multipart upload.
     * @return The URL of the resource.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Location">Location Header</a>
     */
    public String getLocation() {
        return location;
    }

    /**
     * Retrieve the entity tag of the resource created from completing a multipart upload.
     * @return The entity tag of the resource.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag">ETag Header</a>
     */
    public String getETag() {
        return eTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BinaryFinishResponseContent that = (BinaryFinishResponseContent) o;
        return Objects.equals(location, that.location) && Objects.equals(eTag, that.eTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, eTag);
    }

    @Override
    public String toString() {
        return "BinaryFinishResponseContent{" +
            "location='" + location + '\'' +
            ", eTag='" + eTag + '\'' +
            '}';
    }
}