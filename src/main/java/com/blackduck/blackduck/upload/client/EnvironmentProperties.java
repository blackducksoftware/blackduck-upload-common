/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.blackduck.upload.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enum to handle properties coming from environment.
 */
public enum EnvironmentProperties {
    BLACKDUCK_UPLOAD_CHUNK_SIZE("blackduck.upload.chunk.size", false),
    BLACKDUCK_TIMEOUT_SECONDS("blackduck.timeout.seconds", false),
    BLACKDUCK_TRUST_CERT("blackduck.trust.cert", true),
    BLACKDUCK_URL("blackduck.url", true),
    BLACKDUCK_API_TOKEN("blackduck.api.token", true),
    BLACKDUCK_MULTIPART_UPLOAD_THRESHOLD("blackduck.multipart.upload.threshold", false),
    BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS("blackduck.multipart.upload.part.retry.attempts", false),
    BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL("blackduck.multipart.upload.part.retry.initial.interval", false),
    BLACKDUCK_MULTIPART_UPLOAD_TIMEOUT_MINUTES("blackduck.multipart.upload.timeout.minutes", false);

    private final String propertyKey;
    private final boolean isRequired;

    /**
     * Constructor for the environment properties.
     *
     * @param propertyKey The name of the property within the environment.
     * @param isRequired Flag to determine if property is required or not.
     */
    EnvironmentProperties(String propertyKey, boolean isRequired) {
        this.propertyKey = propertyKey;
        this.isRequired = isRequired;
    }

    /**
     * Retrieve the name of the property.
     *
     * @return property name.
     */
    public String getPropertyKey() {
        return propertyKey;
    }

    /**
     * Retrieve the flag controlling if property is required.
     *
     * @return is required.
     */
    public boolean isRequired() {
        return isRequired;
    }

    /**
     * Retrieve all property names with property key.
     *
     * @return map.
     */
    public static Map<String, String> getAsMap() {
        Map<String, String> environmentMap = new HashMap<>();
        for (EnvironmentProperties value : EnvironmentProperties.values()) {
            environmentMap.put(value.toString(), value.getPropertyKey());
        }
        return environmentMap;
    }

    /**
     * Retrieve all property keys which are required.
     *
     * @return list.
     */
    public static List<String> getRequiredPropertyKeys() {
        return Arrays.stream(EnvironmentProperties.values())
            .filter(EnvironmentProperties::isRequired)
            .map(EnvironmentProperties::getPropertyKey)
            .collect(Collectors.toList());
    }

    /**
     * Retrieve all environment properties which are not required.
     *
     * @return list.
     */
    public static List<EnvironmentProperties> getNonRequiredProperties() {
        return Arrays.stream(EnvironmentProperties.values())
            .filter(value -> !value.isRequired())
            .collect(Collectors.toList());
    }
}
