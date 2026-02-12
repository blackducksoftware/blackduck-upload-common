/*
 * blackduck-upload-common
 *
 * Copyright (c) 2026 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance
 * Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.util;

import java.util.Map;

/**
 * Utility class for HTTP header operations.
 */
public class HttpHeaderUtils {

    /**
     * Performs a case-insensitive lookup of a header value in a map.
     * HTTP headers are case-insensitive per RFC 2616, but HashMap is case-sensitive.
     * Tries exact match first for performance, then common variations, then full scan.
     *
     * @param headers the map of headers to search
     * @param headerName the name of the header to find
     * @return the header value if found, null otherwise
     */
    public static String getHeaderCaseInsensitive(Map<String, String> headers, String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }

        // Fast path: try exact match first
        String value = headers.get(headerName);
        if (value != null) {
            return value;
        }

        // Try common variations before full scan
        value = headers.get(headerName.toLowerCase());
        if (value != null) {
            return value;
        }

        value = headers.get(headerName.toUpperCase());
        if (value != null) {
            return value;
        }

        // Fall back to case-insensitive search only if needed
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
