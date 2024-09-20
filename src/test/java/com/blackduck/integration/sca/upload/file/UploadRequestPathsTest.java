package com.blackduck.integration.sca.upload.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UploadRequestPathsTest {
    private static final String EXPECTED_URL_START = "/a/b/c/";
    private static final String ID = RandomStringUtils.randomAlphanumeric(10);

    private static Stream<String> getParameters() {
        return Stream.of("/a/b/c", "/a/b/c/", "//a/b/c/", "/a//b/c/", "/a/b/c//");
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetUploadRequestPath(String testUrlPrefix) {
        UploadRequestPaths uploadRequestPaths = new UploadRequestPaths(testUrlPrefix);
        assertEquals(EXPECTED_URL_START, uploadRequestPaths.getUploadRequestPath());
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetMultiPartRequestPath(String testUrlPrefix) {
        UploadRequestPaths uploadRequestPaths = new UploadRequestPaths(testUrlPrefix);
        assertEquals(EXPECTED_URL_START + "multipart/" + ID, uploadRequestPaths.getMultipartUploadPartRequestPath(ID));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetStartRequestPath(String testUrlPrefix) {
        UploadRequestPaths uploadRequestPaths = new UploadRequestPaths(testUrlPrefix);
        assertEquals(EXPECTED_URL_START + "multipart", uploadRequestPaths.getMultipartUploadStartRequestPath());
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetFinishRequestPath(String testUrlPrefix) {
        UploadRequestPaths uploadRequestPaths = new UploadRequestPaths(testUrlPrefix);
        assertEquals(EXPECTED_URL_START + "multipart/" + ID + "/completed", uploadRequestPaths.getMultipartUploadFinishRequestPath(ID));
    }
}
