package com.synopsys.blackduck.upload.client.uploaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.blackduck.upload.client.UploaderConfig;
import com.synopsys.blackduck.upload.client.model.BinaryScanRequestData;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.proxy.ProxyInfo;

public class UploaderFactoryTest {
    private static final String DUMMY_PREFIX_URL = "a/b/c";
    private static UploaderFactory uploaderFactory;

    private static final Logger logger = LoggerFactory.getLogger(UploaderFactoryTest.class);
    private static final IntLogger intLogger = new Slf4jIntLogger(logger);

    @BeforeAll
    public static void init() {
        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfigFromProperties(ProxyInfo.NO_PROXY_INFO, new Properties());
        uploaderConfigBuilder
            .setUploadChunkSize(39)
            .setBlackDuckTimeoutInSeconds(13)
            .setAlwaysTrustServerCertificate(false)
            .setBlackDuckUrl("https://somewhere.com")
            .setApiToken("ThisTsNotAValidToken");
        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        uploaderFactory = new UploaderFactory(uploaderConfig, intLogger, new Gson());
    }

    //    @Test
    //    void testCreateArtifactsUploader() {
    //        assertNotNull(uploaderFactory.createArtifactsUploader(DUMMY_PREFIX_URL));
    //    }

    @Test
    void testCreateBinaryUploader() {
        assertNotNull(uploaderFactory.createBinaryUploader(DUMMY_PREFIX_URL, new BinaryScanRequestData("projectName", "version", "codeLocationName", "https://code-location-uri")));
    }

    @Test
    void testCreateContainerUploader() {
        assertNotNull(uploaderFactory.createContainerUploader(DUMMY_PREFIX_URL));
    }
    //
    //    @Test
    //    void testCreateReversingLabUploader() {
    //        assertNotNull(uploaderFactory.createReversingLabUploader(DUMMY_PREFIX_URL));
    //    }
    //
    //    @Test
    //    void testCreateToolsUploader() {
    //        assertNotNull(uploaderFactory.createToolsUploader(DUMMY_PREFIX_URL));
    //    }
}
