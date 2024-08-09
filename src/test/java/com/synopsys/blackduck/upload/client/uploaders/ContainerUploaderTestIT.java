package com.synopsys.blackduck.upload.client.uploaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.blackduck.upload.client.UploaderConfig;
import com.synopsys.blackduck.upload.generator.RandomByteContentFileGenerator;
import com.synopsys.blackduck.upload.rest.status.ContainerUploadStatus;
import com.synopsys.blackduck.upload.test.TestPropertyKey;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.properties.TestPropertiesManager;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.proxy.ProxyInfo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerUploaderTestIT {
    private static final TestPropertiesManager testPropertiesManager = TestPropertyKey.getPropertiesManager();
    private static final Logger logger = LoggerFactory.getLogger(ContainerUploaderTestIT.class);
    private static final IntLogger intLogger = new Slf4jIntLogger(logger);

    private final Gson gson = new Gson();

    private UploaderFactory uploaderFactory;
    private Path generatedSampleFilePath;

    @BeforeAll
    void createSampleFile() throws IOException {
        RandomByteContentFileGenerator randomByteContentFileGenerator = new RandomByteContentFileGenerator();
        long fileSize = 1024 * 1024 * 15L;
        generatedSampleFilePath = randomByteContentFileGenerator.generateFile(fileSize, ".tar").orElseThrow(() -> new IOException("Could not generate file"));
    }

    @BeforeEach
    void init() {
        System.setProperty("org.slf4j.simpleLogger.log.com.synopsys", "debug");
        boolean bdbaAvailable = testPropertiesManager.getProperty(TestPropertyKey.BDBA_CONTAINER_AVAILABLE.getPropertyKey())
            .map(Boolean::valueOf)
            .orElse(false);
        assumeTrue(bdbaAvailable);

        String blackduckUrlString = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_URL.getPropertyKey()));
        HttpUrl blackduckUrl = assertDoesNotThrow(() -> new HttpUrl(blackduckUrlString));
        String blackduckApiToken = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_API_TOKEN.getPropertyKey()));


        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfigFromProperties(ProxyInfo.NO_PROXY_INFO, new Properties());
        uploaderConfigBuilder
            .setUploadChunkSize(100)
            .setAlwaysTrustServerCertificate(true)
            .setBlackDuckUrl(blackduckUrl)
            .setApiToken(blackduckApiToken);
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);
    }

    @Test
    void testStandardUpload() {
        ContainerUploader containerUploader = uploaderFactory.createContainerUploader(String.format("/api/storage/containers/%s", UUID.randomUUID()));
        ContainerUploadStatus uploadStatus = assertDoesNotThrow(() -> containerUploader.upload(generatedSampleFilePath));
        assertFalse(uploadStatus.isError());
        assertEquals(HttpStatus.SC_CREATED, uploadStatus.getStatusCode());
        // A container upload does not contain a response body, therefore there should be no content returned other than a 201 response code.
        assertFalse(uploadStatus.hasContent());
    }

}
