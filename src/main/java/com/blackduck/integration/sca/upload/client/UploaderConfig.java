/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.validation.ErrorCode;
import com.blackduck.integration.sca.upload.validation.UploadError;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import com.blackduck.integration.sca.upload.validation.UploaderValidationException;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.properties.PropertiesManager;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.proxy.ProxyInfo;

/**
 * Data object to contain configuration needed for multipart uploads.
 *
 * @see HttpUrl
 */
public class UploaderConfig {
    private final ProxyInfo proxyInfo;
    private final int uploadChunkSize;
    private final int blackduckTimeoutInSeconds;
    private final boolean alwaysTrustServerCertificate;
    private final HttpUrl blackDuckUrl;
    private final String apiToken;
    private final Long multipartUploadThreshold;
    private final int multipartUploadPartRetryAttempts;
    private final long multipartUploadPartRetryInitialInterval;
    private final int multipartUploadTimeoutInMinutes;

    /**
     * Static constructor to instantiate Builder using just {@link ProxyInfo}.
     *
     * @param proxyInfo The proxy info for upload.
     *
     * @return builder.
     */
    public static Builder createConfig(ProxyInfo proxyInfo) {
        return createConfigFromProperties(proxyInfo, new Properties());
    }

    /**
     * Static constructor to instantiate Builder using {@link ProxyInfo} and {@link Properties}.
     *
     * @param proxyInfo The proxy info for upload.
     * @param properties The properties for upload.
     *
     * @return builder.
     */
    public static Builder createConfigFromProperties(ProxyInfo proxyInfo, Properties properties) {
        return new Builder(proxyInfo, PropertiesManager.loadProperties(properties));
    }

    /**
     * Static constructor to instantiate Builder using {@link ProxyInfo} and a properties file.
     * Validation is performed against the properties file, but not it's contents.
     *
     * @param proxyInfo The proxy info for upload.
     * @param propertiesFileLocation The file which contains the properties for upload.
     *
     * @return builder.
     *
     * @throws IntegrationException if the properties file is invalid, does not exist, is a directory or is unreadable.
     * @throws IOException if an I/O error occurs when reading from the stream.
     */
    public static Builder createConfigFromFile(ProxyInfo proxyInfo, String propertiesFileLocation)
        throws IntegrationException, IOException {
        return new Builder(proxyInfo, PropertiesManager.loadFromFile(propertiesFileLocation));
    }

    /**
     * Static constructor to instantiate Builder using {@link ProxyInfo} and from the environment.
     * If the variable exists in the environment without a value, this empty value will still be added.
     *
     * @param proxyInfo The proxy info for upload.
     *
     * @return builder.
     */
    public static Builder createConfigFromEnvironment(ProxyInfo proxyInfo) {
        return new Builder(proxyInfo, PropertiesManager.loadFromEnvironment(EnvironmentProperties.getAsMap()));
    }

    /**
     * Static constructor to instantiate Builder using {@link ProxyInfo} and file with properties.
     * Load properties from file and environment. For properties that exist in both, environment will take precedence.
     * Validation is performed against the properties file, but not it's contents.
     * If the variable exists in the environment without a value, this empty value will still be added to the properties.
     *
     * @param proxyInfo The proxy info for upload.
     * @param propertiesFileLocation The file which contains the properties for upload.
     *
     * @return builder.
     *
     * @throws IntegrationException if the properties file is invalid, does not exist, is a directory or is unreadable.
     * @throws IOException if an I/O error occurs when reading from the stream.
     */
    public static Builder createConfigWithOverrides(ProxyInfo proxyInfo, String propertiesFileLocation)
        throws IntegrationException, IOException {
        return new Builder(proxyInfo, PropertiesManager.loadWithOverrides(propertiesFileLocation, EnvironmentProperties.getAsMap()));
    }

    private UploaderConfig(
        ProxyInfo proxyInfo,
        int uploadChunkSize,
        int blackduckTimeoutInSeconds,
        boolean alwaysTrustServerCertificate,
        HttpUrl blackDuckUrl,
        String apiToken,
        Long multipartUploadThreshold,
        int multipartUploadPartRetryAttempts,
        long multipartUploadPartRetryInitialInterval,
        int multipartUploadTimeoutInMinutes
    ) {
        this.proxyInfo = proxyInfo;
        this.uploadChunkSize = uploadChunkSize;
        this.blackduckTimeoutInSeconds = blackduckTimeoutInSeconds;
        this.alwaysTrustServerCertificate = alwaysTrustServerCertificate;
        this.blackDuckUrl = blackDuckUrl;
        this.apiToken = apiToken;
        this.multipartUploadThreshold = multipartUploadThreshold;
        this.multipartUploadPartRetryAttempts = multipartUploadPartRetryAttempts;
        this.multipartUploadPartRetryInitialInterval = multipartUploadPartRetryInitialInterval;
        this.multipartUploadTimeoutInMinutes = multipartUploadTimeoutInMinutes;
    }

    /**
     * Retrieve the {@link ProxyInfo}.
     *
     * @return proxy info.
     */
    public ProxyInfo getProxyInfo() {
        return proxyInfo;
    }

    /**
     * Retrieve the size for uploading chunks to Black Duck.
     *
     * @return upload chunk size.
     */
    public int getUploadChunkSize() {
        return uploadChunkSize;
    }

    /**
     * Retrieve the timeout when communicating with Black Duck.
     *
     * @return timeout in seconds.
     */
    public int getBlackDuckTimeoutInSeconds() {
        return blackduckTimeoutInSeconds;
    }

    /**
     * Determine the value for trusting the Black Duck server certificate.
     *
     * @return trust server certificate.
     */
    public boolean isAlwaysTrustServerCertificate() {
        return alwaysTrustServerCertificate;
    }

    /**
     * Retrieve the Black Duck {@link HttpUrl}.
     *
     * @return url.
     */
    public HttpUrl getBlackDuckUrl() {
        return blackDuckUrl;
    }

    /**
     * Retrieve the Black Duck api token.
     *
     * @return api token.
     */
    public String getApiToken() {
        return apiToken;
    }

    /**
     * Retrieve the multipart upload threshold.
     *
     * @return multipart upload threshold.
     */
    public Long getMultipartUploadThreshold() {
        return multipartUploadThreshold;
    }

    /**
     * Retrieve the multipart upload retry attempts.
     *
     * @return multipart upload retry attempts.
     */
    public Integer getMultipartUploadPartRetryAttempts() {
        return multipartUploadPartRetryAttempts;
    }

    /**
     * Retrieve the multipart upload retry interval.
     *
     * @return multipart upload retry interval.
     */
    public Long getMultipartUploadPartRetryInitialInterval() {
        return multipartUploadPartRetryInitialInterval;
    }

    /**
     * Retrieve the multipart upload timeout minutes.
     *
     * @return multipart upload timeout minutes.
     */
    public int getMultipartUploadTimeoutInMinutes() {
        return multipartUploadTimeoutInMinutes;
    }

    /**
     * Builder class used to validate and create an instance of {@link UploaderConfig}.
     */
    public static class Builder {
        ProxyInfo proxyInfo;
        PropertiesManager propertiesManager;

        private Builder(ProxyInfo proxyInfo, PropertiesManager propertiesManager) {
            this.proxyInfo = proxyInfo;
            this.propertiesManager = propertiesManager;
        }

        /**
         * Validate and construct an instance of {@link UploaderConfig}.
         *
         * @return uploader configuration.
         *
         * @throws IntegrationException if any required properties do not exist or are null, or if the property for the Black Duck URL is not a valid URL.
         */
        public UploaderConfig build() throws IntegrationException {
            validate();

            // Black Duck URL and API token are validated prior to construction, therefore those values should never be null.
            return new UploaderConfig(
                proxyInfo,
                getUploadChunkSize(),
                getBlackDuckTimeoutInSeconds(),
                isAlwaysTrustServerCertificate(),
                getBlackDuckUrl().orElse(null),
                getApiToken().orElse(null),
                getMultipartUploadThreshold(),
                getMultipartUploadPartRetryAttempts(),
                getMultipartUploadPartRetryInitialInterval(),
                getMultipartUploadTimeoutInMinutes()
            );
        }

        private void validate() throws IntegrationException {
            List<UploadError> nullPropertyKeys = EnvironmentProperties.getRequiredPropertyKeys()
                .stream()
                .filter(propertyKey -> getPropertyValue(propertyKey) == null)
                .map(property -> new UploadError(ErrorCode.MISSING_REQUIRED_PROPERTY_ERROR, property))
                .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(nullPropertyKeys)) {
                throw new UploaderValidationException(nullPropertyKeys);
            }
        }


        /**
         * Retrieve current builder value for the size of uploading chunks to Black Duck.
         *
         * @return configured or default upload chunk size.
         */
        public int getUploadChunkSize() {
            Optional<String> uploadChunkSizeProperty = Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_UPLOAD_CHUNK_SIZE.getPropertyKey()));
            return uploadChunkSizeProperty.map(Integer::parseInt).orElse(UploadValidator.DEFAULT_UPLOAD_CHUNK_SIZE);
        }

        /**
         * Retrieve current builder value for the timeout when communicating with Black Duck.
         *
         * @return configured or default timeout in seconds.
         */
        public int getBlackDuckTimeoutInSeconds() {
            Optional<String> timeoutInSeconds = Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_TIMEOUT_SECONDS.getPropertyKey()));
            return timeoutInSeconds.map(Integer::parseInt).orElse(BlackDuckHttpClient.DEFAULT_BLACKDUCK_TIMEOUT_SECONDS);
        }

        /**
         * Retrieve current builder value for trusting the Black Duck server certificate.
         *
         * @return configured value to trust server certificate.
         */
        public boolean isAlwaysTrustServerCertificate() {
            return Boolean.parseBoolean(getPropertyValue(EnvironmentProperties.BLACKDUCK_TRUST_CERT.getPropertyKey()));
        }

        /**
         * Retrieve current builder value for the Black Duck {@link HttpUrl}.
         *
         * @return {@link Optional} configured Black Duck {@link HttpUrl}.
         */
        public Optional<HttpUrl> getBlackDuckUrl() throws IntegrationException {
            Optional<String> blackduckUrlProperty = Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_URL.getPropertyKey()));
            if (blackduckUrlProperty.isPresent()) {
                return Optional.of(new HttpUrl(blackduckUrlProperty.get()));
            }
            return Optional.empty();
        }

        /**
         * Retrieve current builder value for the Black Duck API token.
         *
         * @return {@link Optional} configured api token.
         */
        public Optional<String> getApiToken() {
            return Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_API_TOKEN.getPropertyKey()));
        }

        /**
         * Retrieve current builder value for the multipart upload threshold.
         *
         * @return configured or default multipart upload threshold.
         */
        public Long getMultipartUploadThreshold() {
            Optional<String> multipartUploadThresholdProperty = Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_THRESHOLD.getPropertyKey()));
            return multipartUploadThresholdProperty.map(Long::parseLong)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD);
        }

        /**
         * Retrieve current builder value for the multipart retry attempts.
         *
         * @return configured or default multipart retry attempts.
         */
        public Integer getMultipartUploadPartRetryAttempts() {
            Optional<String> multipartUploadPartRetryAttemptsProperty =
                Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS.getPropertyKey()));
            return multipartUploadPartRetryAttemptsProperty.map(Integer::parseInt)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS);
        }

        /**
         * Retrieve current builder value for the multipart retry initial interval.
         *
         * @return configured or default multipart retry initial interval.
         */
        public Long getMultipartUploadPartRetryInitialInterval() {
            Optional<String> multipartUploadPartRetryInitialIntervalProperty =
                Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL.getPropertyKey()));
            return multipartUploadPartRetryInitialIntervalProperty.map(Long::parseLong)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL);
        }

        /**
         * Retrieve current builder value for the multipart upload timeout in minutes.
         *
         * @return configured or default multipart upload timeout in minutes.
         */
        public Integer getMultipartUploadTimeoutInMinutes() {
            Optional<String> multipartUploadTimeoutMinutesProperty =
                Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_TIMEOUT_MINUTES.getPropertyKey()));
            return multipartUploadTimeoutMinutesProperty.map(Integer::parseInt)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_TIMEOUT_MINUTES);
        }

        private String getPropertyValue(String propertyKey) {
            return propertiesManager.getProperty(propertyKey).orElse(null);
        }

        /**
         * Replace the value for a specified property within {@link EnvironmentProperties}.
         *
         * @param environmentProperty The property key to replace.
         * @param propertyValue The value of the property.
         */
        public void setPropertyValue(EnvironmentProperties environmentProperty, String propertyValue) {
            propertiesManager.getProperties().put(environmentProperty.getPropertyKey(), propertyValue);
        }

        /**
         * Replace the size for uploading chunks to Black Duck.
         *
         * @param chunkSize The size of each individual chunk for upload.
         *
         * @return builder.
         */
        public Builder setUploadChunkSize(int chunkSize) {
            return setUploadChunkSize(String.valueOf(chunkSize));
        }

        /**
         * Replace the size for uploading chunks to Black Duck.
         *
         * @param chunkSize The size of each individual chunk for upload.
         *
         * @return builder.
         */
        public Builder setUploadChunkSize(String chunkSize) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_UPLOAD_CHUNK_SIZE, chunkSize);
            return this;
        }

        /**
         * Replace the timeout when communicating with Black Duck.
         *
         * @param blackduckTimeoutInSeconds The time to wait for a response.
         *
         * @return builder.
         */
        public Builder setBlackDuckTimeoutInSeconds(int blackduckTimeoutInSeconds) {
            return setBlackDuckTimeoutInSeconds(String.valueOf(blackduckTimeoutInSeconds));
        }

        /**
         * Replace the timeout when communicating with Black Duck.
         *
         * @param blackduckTimeoutInSeconds The time to wait for a response.
         *
         * @return builder.
         */
        public Builder setBlackDuckTimeoutInSeconds(String blackduckTimeoutInSeconds) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_TIMEOUT_SECONDS, blackduckTimeoutInSeconds);
            return this;
        }

        /**
         * Replace the value for trusting the Black Duck server certificate.
         *
         * @param alwaysTrustServerCertificate If the Black Duck certificate should be trusted.
         *
         * @return builder.
         */
        public Builder setAlwaysTrustServerCertificate(boolean alwaysTrustServerCertificate) {
            return setAlwaysTrustServerCertificate(String.valueOf(alwaysTrustServerCertificate));
        }

        /**
         * Replace the value for trusting the Black Duck server certificate.
         *
         * @param alwaysTrustServerCertificate If the Black Duck certificate should be trusted.
         *
         * @return builder.
         */
        public Builder setAlwaysTrustServerCertificate(String alwaysTrustServerCertificate) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_TRUST_CERT, alwaysTrustServerCertificate);
            return this;
        }

        /**
         * Replace the Black Duck {@link HttpUrl}.
         *
         * @param blackDuckUrl The Black Duck URL.
         *
         * @return builder.
         */
        public Builder setBlackDuckUrl(HttpUrl blackDuckUrl) {
            return setBlackDuckUrl(blackDuckUrl.string());
        }

        /**
         * Replace the Black Duck {@link HttpUrl}.
         *
         * @param blackDuckUrl The Black Duck URL.
         *
         * @return builder.
         */
        public Builder setBlackDuckUrl(String blackDuckUrl) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_URL, blackDuckUrl);
            return this;
        }

        /**
         * Replace the Black Duck api token.
         *
         * @param apiToken The token used for api calls to Black Duck.
         *
         * @return Builder.
         */
        public Builder setApiToken(String apiToken) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_API_TOKEN, apiToken);
            return this;
        }

        /**
         * Replace the multipart upload threshold.
         *
         * @param multipartUploadThreshold The minimum size for each upload.
         *
         * @return builder.
         */
        public Builder setMultipartUploadThreshold(Long multipartUploadThreshold) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_THRESHOLD, String.valueOf(multipartUploadThreshold));
            return this;
        }

        /**
         * Replace the multipart upload retry attempts.
         *
         * @param multipartUploadPartRetryAttempts The maximum number of retries for failed uploads.
         *
         * @return builder.
         */
        public Builder setMultipartUploadPartRetryAttempts(Integer multipartUploadPartRetryAttempts) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS, String.valueOf(multipartUploadPartRetryAttempts));
            return this;
        }

        /**
         * Replace the multipart upload retry interval.
         *
         * @param multipartUploadPartRetryInitialInterval The time to wait before retrying an upload.
         *
         * @return builder.
         */
        public Builder setMultipartUploadPartRetryInitialInterval(Long multipartUploadPartRetryInitialInterval) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL, String.valueOf(multipartUploadPartRetryInitialInterval));
            return this;
        }

        public Builder setMultipartUploadTimeoutInMinutes(Integer multipartUploadTimeoutInMinutes) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_TIMEOUT_MINUTES, String.valueOf(multipartUploadTimeoutInMinutes));
            return this;
        }
    }
}
