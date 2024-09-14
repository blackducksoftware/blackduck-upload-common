package com.synopsys.blackduck.upload.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.blackduck.upload.rest.BlackDuckHttpClient;
import com.synopsys.blackduck.upload.validation.UploadValidator;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.proxy.ProxyInfo;

class UploaderConfigTest {
    private static final ProxyInfo PROXY_INFO = ProxyInfo.NO_PROXY_INFO;
    private static final int UPLOAD_CHUNK_SIZE = 31339;
    private static final int BLACKDUCK_TIMEOUT_IN_SECONDS = 30;
    private static final boolean ALWAYS_TRUST_CERT = false;
    private static final String HTTP_URL_STRING = "https://somewhere.com";
    private static final String API_TOKEN = "ThisTsNotAValidToken";
    private static final Long MULTIPART_UPLOAD_THRESHOLD = 1024 * 1024 * 5L;

    private HttpUrl httpUrl;
    private String testPropertiesFile;

    @BeforeEach
    public void init() {
        httpUrl = assertDoesNotThrow(() -> new HttpUrl(HTTP_URL_STRING));
        File emptyPropertiesFile = assertDoesNotThrow(() -> File.createTempFile(RandomStringUtils.randomAlphanumeric(10), ".properties"));
        assertDoesNotThrow(emptyPropertiesFile::createNewFile);
        emptyPropertiesFile.deleteOnExit();

        testPropertiesFile = emptyPropertiesFile.getAbsolutePath();
    }

    @Test
    void testBuildValidationAllValid() {
        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfigFromProperties(PROXY_INFO, new Properties());
        uploaderConfigBuilder
            .setUploadChunkSize(UPLOAD_CHUNK_SIZE)
            .setBlackDuckTimeoutInSeconds(BLACKDUCK_TIMEOUT_IN_SECONDS)
            .setAlwaysTrustServerCertificate(ALWAYS_TRUST_CERT)
            .setBlackDuckUrl(httpUrl)
            .setApiToken(API_TOKEN)
            .setMultipartUploadThreshold(MULTIPART_UPLOAD_THRESHOLD)
            .setMultipartUploadPartRetryAttempts(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS)
            .setMultipartUploadPartRetryInitialInterval(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL)
            .setMultipartUploadTimeoutInMinutes(UploadValidator.DEFAULT_MULTIPART_UPLOAD_TIMEOUT_MINUTES);

        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        assertEquals(PROXY_INFO, uploaderConfig.getProxyInfo());
        assertEquals(UPLOAD_CHUNK_SIZE, uploaderConfig.getUploadChunkSize());
        assertEquals(BLACKDUCK_TIMEOUT_IN_SECONDS, uploaderConfig.getBlackDuckTimeoutInSeconds());
        assertEquals(ALWAYS_TRUST_CERT, uploaderConfig.isAlwaysTrustServerCertificate());
        assertEquals(httpUrl, uploaderConfig.getBlackDuckUrl());
        assertEquals(API_TOKEN, uploaderConfig.getApiToken());
        assertEquals(MULTIPART_UPLOAD_THRESHOLD, uploaderConfig.getMultipartUploadThreshold());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS, uploaderConfig.getMultipartUploadPartRetryAttempts());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL, uploaderConfig.getMultipartUploadPartRetryInitialInterval());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_TIMEOUT_MINUTES, uploaderConfig.getMultipartUploadTimeoutInMinutes());
    }

    @Test
    void testBuildValidationAllNull() {
        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfig(PROXY_INFO);
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        EnvironmentProperties.getRequiredPropertyKeys().forEach(key -> assertTrue(integrationException.getMessage().contains(key)));
    }

    @Test
    void testOnlyRequiredValuesNull() {
        // Blackduck URL, API Token, and trust cert are omitted from the builder
        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfig(PROXY_INFO);
        uploaderConfigBuilder
            .setUploadChunkSize(UPLOAD_CHUNK_SIZE)
            .setBlackDuckTimeoutInSeconds(BLACKDUCK_TIMEOUT_IN_SECONDS)
            .setMultipartUploadThreshold(MULTIPART_UPLOAD_THRESHOLD)
            .setMultipartUploadPartRetryAttempts(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS)
            .setMultipartUploadPartRetryInitialInterval(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL)
            .setMultipartUploadTimeoutInMinutes(UploadValidator.DEFAULT_MULTIPART_UPLOAD_TIMEOUT_MINUTES);

        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        EnvironmentProperties.getRequiredPropertyKeys().forEach(key -> assertTrue(integrationException.getMessage().contains(key)));
    }

    @Test
    void testNonRequiredValuesNull() {
        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfig(PROXY_INFO);
        uploaderConfigBuilder
            .setAlwaysTrustServerCertificate(ALWAYS_TRUST_CERT)
            .setBlackDuckUrl(httpUrl)
            .setApiToken(API_TOKEN);

        UploaderConfig uploaderConfig = assertDoesNotThrow(uploaderConfigBuilder::build);
        assertEquals(PROXY_INFO, uploaderConfig.getProxyInfo());
        assertEquals(ALWAYS_TRUST_CERT, uploaderConfig.isAlwaysTrustServerCertificate());
        assertEquals(httpUrl, uploaderConfig.getBlackDuckUrl());
        assertEquals(API_TOKEN, uploaderConfig.getApiToken());
        assertEquals(UploadValidator.DEFAULT_UPLOAD_CHUNK_SIZE, uploaderConfig.getUploadChunkSize());
        assertEquals(BlackDuckHttpClient.DEFAULT_BLACKDUCK_TIMEOUT_SECONDS, uploaderConfig.getBlackDuckTimeoutInSeconds());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD, uploaderConfig.getMultipartUploadThreshold());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS, uploaderConfig.getMultipartUploadPartRetryAttempts());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL, uploaderConfig.getMultipartUploadPartRetryInitialInterval());
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_TIMEOUT_MINUTES, uploaderConfig.getMultipartUploadTimeoutInMinutes());
    }

    @Test
    void testBuildValidationChunkSize() {
        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfigFromEnvironment(PROXY_INFO);
        uploaderConfigBuilder.setUploadChunkSize(UPLOAD_CHUNK_SIZE);
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_UPLOAD_CHUNK_SIZE.getPropertyKey()));
    }

    @Test
    void testBuildValidationTimeout() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        uploaderConfigBuilder.setBlackDuckTimeoutInSeconds(BLACKDUCK_TIMEOUT_IN_SECONDS);
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_TIMEOUT_SECONDS.getPropertyKey()));
    }

    @Test
    void testBuildValidationAlwaysTrust() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        uploaderConfigBuilder.setAlwaysTrustServerCertificate(ALWAYS_TRUST_CERT);
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_TRUST_CERT.getPropertyKey()));
    }

    @Test
    void testBuildValidationBlackduckUrl() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        uploaderConfigBuilder.setBlackDuckUrl(httpUrl);
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_URL.getPropertyKey()));
    }

    @Test
    void testBuildValidationApiToken() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        uploaderConfigBuilder.setApiToken(API_TOKEN);
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_API_TOKEN.getPropertyKey()));
    }

    @Test
    void testOptionalBuildValidationMultipartUploadThreshold() {
        // blackduck.multipart.upload.threshold is an optional property, therefore if it is omitted it shouldn't be included in the validation error messages.
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_THRESHOLD.getPropertyKey()));
    }

    @Test
    void testOptionalBuildValidationMultipartUploadPartRetryAttempts() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS.getPropertyKey()));
    }

    @Test
    void testOptionalBuildValidationMultipartUploadPartRetryInitialInterval() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL.getPropertyKey()));
    }

    @Test
    void testOptionalBuildValidationMultipartUploadTimeoutMinutes() {
        UploaderConfig.Builder uploaderConfigBuilder = assertDoesNotThrow(() -> UploaderConfig.createConfigFromFile(PROXY_INFO, testPropertiesFile));
        IntegrationException integrationException = assertThrows(IntegrationException.class, uploaderConfigBuilder::build);
        assertFalse(integrationException.getMessage().contains(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_TIMEOUT_MINUTES.getPropertyKey()));

    }
}
