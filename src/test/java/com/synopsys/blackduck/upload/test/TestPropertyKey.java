package com.synopsys.blackduck.upload.test;

import java.util.HashMap;
import java.util.Map;

import com.synopsys.integration.properties.TestPropertiesManager;

public enum TestPropertyKey {
    TEST_BLACKDUCK_URL("blackduck.url"),
    TEST_BLACKDUCK_API_TOKEN("blackduck.api.token"),
    BDBA_CONTAINER_AVAILABLE("blackduck.bdba.container.available"),
    MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS("blackduck.multipart.upload.part.retry.attempts"),
    MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL("blackduck.multipart.upload.part.retry.initial.interval");

    private final String propertyKey;

    TestPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public static String getPropertyKey(TestPropertyKey testPropertyKey) {
        return testPropertyKey.getPropertyKey();
    }

    private static Map<String, String> getAsMap() {
        Map<String, String> environmentMap = new HashMap<>();
        for (TestPropertyKey value : TestPropertyKey.values()) {
            environmentMap.put(value.toString(), value.getPropertyKey());
        }
        return environmentMap;
    }

    public static TestPropertiesManager getPropertiesManager() {
        return TestPropertiesManager.loadFromEnvironmentAndDefaultFile(TestPropertyKey.getAsMap());
    }
}
