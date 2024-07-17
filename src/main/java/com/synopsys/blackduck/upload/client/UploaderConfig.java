package com.synopsys.blackduck.upload.client;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.synopsys.blackduck.upload.validation.ErrorCode;
import com.synopsys.blackduck.upload.validation.UploadError;
import com.synopsys.blackduck.upload.validation.UploadValidator;
import com.synopsys.blackduck.upload.validation.UploaderValidationException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.properties.PropertiesManager;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.proxy.ProxyInfo;

/**
 * Data object to contain configuration needed for multipart uploads.
 *
 * @see HttpUrl
 */
public class UploaderConfig {
    private final ProxyInfo proxyInfo;
    private final int uploadChunkSize;
    private final int timeoutInSeconds;
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
        int timeoutInSeconds,
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
        this.timeoutInSeconds = timeoutInSeconds;
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
    public int getTimeoutInSeconds() {
        return timeoutInSeconds;
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

            return new UploaderConfig(
                proxyInfo,
                getUploadChunkSize(),
                getTimeoutInSeconds(),
                isAlwaysTrustServerCertificate(),
                getBlackDuckUrl(),
                getApiToken(),
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

        private int getUploadChunkSize() {
            return Integer.parseInt(getPropertyValue(EnvironmentProperties.BLACKDUCK_UPLOAD_CHUNK_SIZE.getPropertyKey()));
        }

        private int getTimeoutInSeconds() {
            return Integer.parseInt(getPropertyValue(EnvironmentProperties.BLACKDUCK_TIMEOUT_SECONDS.getPropertyKey()));
        }

        private boolean isAlwaysTrustServerCertificate() {
            return Boolean.parseBoolean(getPropertyValue(EnvironmentProperties.BLACKDUCK_TRUST_CERT.getPropertyKey()));
        }

        private HttpUrl getBlackDuckUrl() throws IntegrationException {
            return new HttpUrl(getPropertyValue(EnvironmentProperties.BLACKDUCK_URL.getPropertyKey()));
        }

        private String getApiToken() {
            return getPropertyValue(EnvironmentProperties.BLACKDUCK_API_TOKEN.getPropertyKey());
        }

        private Long getMultipartUploadThreshold() {
            Optional<String> multipartUploadThresholdProperty = Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_THRESHOLD.getPropertyKey()));
            return multipartUploadThresholdProperty.map(Long::parseLong)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD);
        }

        private Integer getMultipartUploadPartRetryAttempts() {
            Optional<String> multipartUploadPartRetryAttemptsProperty =
                Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS.getPropertyKey()));
            return multipartUploadPartRetryAttemptsProperty.map(Integer::parseInt)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS);
        }

        private Long getMultipartUploadPartRetryInitialInterval() {
            Optional<String> multipartUploadPartRetryInitialIntervalProperty =
                Optional.ofNullable(getPropertyValue(EnvironmentProperties.BLACKDUCK_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL.getPropertyKey()));
            return multipartUploadPartRetryInitialIntervalProperty.map(Long::parseLong)
                .orElse(UploadValidator.DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL);
        }

        private Integer getMultipartUploadTimeoutInMinutes() {
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
         * @param timeoutInSeconds The time to wait for a response.
         *
         * @return builder.
         */
        public Builder setTimeoutInSeconds(int timeoutInSeconds) {
            return setTimeoutInSeconds(String.valueOf(timeoutInSeconds));
        }

        /**
         * Replace the timeout when communicating with Black Duck.
         *
         * @param timeoutInSeconds The time to wait for a response.
         *
         * @return builder.
         */
        public Builder setTimeoutInSeconds(String timeoutInSeconds) {
            setPropertyValue(EnvironmentProperties.BLACKDUCK_TIMEOUT_SECONDS, timeoutInSeconds);
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
