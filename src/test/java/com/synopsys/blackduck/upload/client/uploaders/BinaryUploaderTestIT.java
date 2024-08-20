package com.synopsys.blackduck.upload.client.uploaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.blackduck.upload.client.UploaderConfig;
import com.synopsys.blackduck.upload.client.model.BinaryScanRequestData;
import com.synopsys.blackduck.upload.generator.RandomByteContentFileGenerator;
import com.synopsys.blackduck.upload.rest.status.BinaryUploadStatus;
import com.synopsys.blackduck.upload.test.TestPropertyKey;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.properties.TestPropertiesManager;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.proxy.ProxyInfo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BinaryUploaderTestIT {
    private static final TestPropertiesManager testPropertiesManager = TestPropertyKey.getPropertiesManager();
    private static final Logger logger = LoggerFactory.getLogger(BinaryUploaderTestIT.class);
    private static final IntLogger intLogger = new Slf4jIntLogger(logger);
    private static final String BINARY_URL_PREFIX = "/api/uploads";

    private final Gson gson = new Gson();

    private UploaderFactory uploaderFactory;
    private Path generatedSampleFilePath;

    private UploaderConfig.Builder uploaderConfigBuilder;
    private BinaryScanRequestData binaryScanRequestData = new BinaryScanRequestData(
        "project name",
        "project version",
        "code location name",
        "https://code-location-uri"
    );

    @BeforeAll
    void createSampleFile() throws IOException {
        RandomByteContentFileGenerator randomByteContentFileGenerator = new RandomByteContentFileGenerator();
        long fileSize = 1024 * 1024 * 15L;
        generatedSampleFilePath = randomByteContentFileGenerator.generateFile(fileSize, ".bin").orElseThrow(() -> new IOException("Could not generate file"));
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


        //Set default values for tests
        uploaderConfigBuilder = UploaderConfig.createConfigFromProperties(ProxyInfo.NO_PROXY_INFO, new Properties());
        uploaderConfigBuilder
            .setUploadChunkSize(100)
            .setAlwaysTrustServerCertificate(true)
            .setBlackDuckUrl(blackduckUrl)
            .setApiToken(blackduckApiToken);
    }

    @Test
    void testStandardUpload() {
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);

        BinaryUploader binaryUploader = uploaderFactory.createBinaryUploader(BINARY_URL_PREFIX, binaryScanRequestData);
        BinaryUploadStatus uploadStatus = assertDoesNotThrow(() -> binaryUploader.upload(generatedSampleFilePath));
        assertFalse(uploadStatus.isError());
        assertEquals(HttpStatus.SC_CREATED, uploadStatus.getStatusCode());
        assertTrue(uploadStatus.hasContent());
    }

    @Test
    void testMultipartUpload() {
        // set threshold to 1 byte to always perform a multipart upload
        uploaderConfigBuilder
            .setUploadChunkSize(1024 * 1024 * 5)
            .setMultipartUploadThreshold(1L);
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);

        BinaryUploader binaryUploader = uploaderFactory.createBinaryUploader(BINARY_URL_PREFIX, binaryScanRequestData);
        BinaryUploadStatus uploadStatus = assertDoesNotThrow(() -> binaryUploader.upload(generatedSampleFilePath));
        assertFalse(uploadStatus.isError());
        assertEquals(HttpStatus.SC_NO_CONTENT, uploadStatus.getStatusCode());
        assertTrue(uploadStatus.hasContent());
    }

    @Test
    void testMultipartUploadValidationError() {
        // Chunk size is 100 bytes, which is lower than the minimum supported
        uploaderConfigBuilder
            .setMultipartUploadThreshold(1L);
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, gson);

        BinaryUploader binaryUploader = uploaderFactory.createBinaryUploader(BINARY_URL_PREFIX, binaryScanRequestData);
        assertThrows(IntegrationException.class, () -> binaryUploader.upload(generatedSampleFilePath));
    }
}
