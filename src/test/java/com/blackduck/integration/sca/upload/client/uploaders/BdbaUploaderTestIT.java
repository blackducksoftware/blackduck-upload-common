/*
 * blackduck-upload-common
 *
 * Copyright (c) 2025 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance
 * Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.properties.TestPropertiesManager;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.FileBodyContent;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.UploaderConfig;
import com.blackduck.integration.sca.upload.generator.RandomByteContentFileGenerator;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.test.TestPropertyKey;
import com.google.gson.Gson;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BdbaUploaderTestIT {
    private static final TestPropertiesManager testPropertiesManager = TestPropertyKey.getPropertiesManager();
    private static final Logger logger = LoggerFactory.getLogger(BdbaUploaderTestIT.class);
    private static final IntLogger intLogger = new Slf4jIntLogger(logger);

    private final Gson gson = new Gson();

    private UploaderFactory uploaderFactory;
    private Path generatedSampleFilePath;

    private UploaderConfig.Builder uploaderConfigBuilder;

    @BeforeAll
    void createSampleFile() throws IOException {
        RandomByteContentFileGenerator randomByteContentFileGenerator = new RandomByteContentFileGenerator();
        long fileSize = 1024 * 1024 * 15L;
        generatedSampleFilePath = randomByteContentFileGenerator.generateFile(fileSize, ".tar").orElseThrow(() -> new IOException("Could not generate file"));
    }

    @BeforeEach
    void init() {
        System.setProperty("org.slf4j.simpleLogger.log.com.blackduck", "debug");

        boolean bdbaAvailable = testPropertiesManager.getProperty(TestPropertyKey.BDBA_CONTAINER_AVAILABLE.getPropertyKey())
                .map(Boolean::valueOf)
                .orElse(false);
            assumeTrue(bdbaAvailable);
            
        String blackduckUrlString = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_URL.getPropertyKey()));
        HttpUrl blackduckUrl = assertDoesNotThrow(() -> new HttpUrl(blackduckUrlString));
        String blackduckApiToken = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_API_TOKEN.getPropertyKey()));

        //Set default values for tests
        uploaderConfigBuilder = UploaderConfig.createConfigFromProperties(ProxyInfo.NO_PROXY_INFO, new Properties());
        uploaderConfigBuilder
            .setUploadChunkSize(100)
            .setAlwaysTrustServerCertificate(true)
            .setBlackDuckUrl(blackduckUrl)
            .setApiToken(blackduckApiToken);
    }

    @Test
    void testStandardUpload() throws Exception {
        UUID scanId = createScan();
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);

        BdbaUploader bdbaUploader = uploaderFactory.createBdbaUploader(String.format("/api/storage/bdba/%s", scanId));
        DefaultUploadStatus uploadStatus = assertDoesNotThrow(() -> bdbaUploader.upload(generatedSampleFilePath));
        assertFalse(uploadStatus.isError());
        assertEquals(HttpStatus.SC_CREATED, uploadStatus.getStatusCode());
        // A bdba upload does not contain a response body, therefore there should be no content returned other than a 201 response code.
        assertFalse(uploadStatus.hasContent());
    }

    @Test
    void testMultipartUpload() throws Exception {
        UUID scanId = createScan();

        // set threshold to 1 byte to always perform a multipart upload
        uploaderConfigBuilder
            .setUploadChunkSize(1024 * 1024 * 5)
            .setMultipartUploadThreshold(1L);
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);

        BdbaUploader bdbaUploader = uploaderFactory.createBdbaUploader(String.format("/api/storage/bdba/%s", scanId));
        DefaultUploadStatus uploadStatus = assertDoesNotThrow(() -> bdbaUploader.upload(generatedSampleFilePath));
        assertFalse(uploadStatus.isError());
        assertEquals(HttpStatus.SC_NO_CONTENT, uploadStatus.getStatusCode());
        assertFalse(uploadStatus.hasContent());
    }

    @Test
    void testMultipartUploadValidationError() {
        // Chunk size is 100 bytes, which is lower than the minimum supported
        uploaderConfigBuilder
            .setMultipartUploadThreshold(1L);
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);

        BdbaUploader bdbaUploader = uploaderFactory.createBdbaUploader(String.format("/api/storage/bdba/%s", UUID.randomUUID()));
        assertThrows(IntegrationException.class, () -> bdbaUploader.upload(generatedSampleFilePath));
    }

    private UUID createScan() throws Exception {
        UploaderConfig uploaderConfig = uploaderConfigBuilder.build();

        BlackDuckHttpClient httpClient = new BlackDuckHttpClient(
            intLogger,
            gson,
            uploaderConfig.getBlackDuckTimeoutInSeconds(),
            uploaderConfig.isAlwaysTrustServerCertificate(),
            uploaderConfig.getProxyInfo(),
            uploaderConfig.getBlackDuckUrl(),
            uploaderConfig.getApiToken()
        );

        HttpUrl requestUrl = httpClient.getBlackDuckUrl().appendRelativeUrl("api/intelligent-persistence-scans");
        File bdioHeader = new File("src/test/resources/bdio-header.pb");
        FileBodyContent bodyContent = new FileBodyContent(bdioHeader, null);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/vnd.blackducksoftware.intelligent-persistence-scan-3+protobuf");
        Request.Builder builder = new Request.Builder()
            .url(requestUrl)
            .headers(headers)
            .method(HttpMethod.POST)
            .bodyContent(bodyContent);

        Request request = builder.build();
        Response response = httpClient.execute(request);

        assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        return extractUUID(response.getHeaders().get(HttpHeaders.LOCATION));
    }

    private UUID extractUUID(String url) {
        assertNotNull(url, "No location header was found in response");

        String regex = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        boolean matchFound = matcher.find();

        assertTrue(matchFound, "Scan id was not found in returned location url");

        return UUID.fromString(matcher.group(1));
    }
}
