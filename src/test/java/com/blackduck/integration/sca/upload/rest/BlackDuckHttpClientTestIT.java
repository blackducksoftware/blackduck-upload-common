package com.blackduck.integration.sca.upload.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.properties.TestPropertiesManager;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.test.TestPropertyKey;
import com.google.gson.Gson;

public class BlackDuckHttpClientTestIT {
    private static final TestPropertiesManager testPropertiesManager = TestPropertyKey.getPropertiesManager();

    private BlackDuckHttpClient blackDuckHttpClient;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new Gson();
    private final IntLogger intLogger = new Slf4jIntLogger(logger);
    private final int timeoutInSeconds = 120;

    private static HttpUrl blackduckUrl;

    @BeforeAll
    public static void setup() {
        Optional<String> optionalUrl = testPropertiesManager.getProperty(TestPropertyKey.TEST_BLACKDUCK_URL.getPropertyKey());
        Assumptions.assumeTrue(optionalUrl.isPresent());
        String blackduckUrlString = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_URL.getPropertyKey()));
        blackduckUrl = assertDoesNotThrow(() -> new HttpUrl(blackduckUrlString));
    }

    @Test
    void testShouldFail() {
        blackDuckHttpClient = new BlackDuckHttpClient(
            intLogger,
            gson,
            timeoutInSeconds,
            true,
            ProxyInfo.NO_PROXY_INFO,
            blackduckUrl,
            RandomStringUtils.randomAlphanumeric(100)
        );
        Assumptions.assumeTrue(blackDuckHttpClient.canConnect());
        HttpUriRequest httpUriRequest = createRequest();
        assertFalse(blackDuckHttpClient.isAlreadyAuthenticated(httpUriRequest), "Expected to NOT be authenticated");

        try (Response response = blackDuckHttpClient.execute(httpUriRequest)) {
            fail("execute should have failed with an exception");
        } catch (IntegrationException e) {
            assertFalse(blackDuckHttpClient.isAlreadyAuthenticated(httpUriRequest), "Expected to NOT be authenticated");
        } catch (IOException e) {
            fail("An IntegrationException should have been thrown", e);
        }
    }

    @Test
    void testShouldSucceed() {
        String blackduckApiToken = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_API_TOKEN.getPropertyKey()));

        blackDuckHttpClient = new BlackDuckHttpClient(
            intLogger,
            gson,
            timeoutInSeconds,
            true,
            ProxyInfo.NO_PROXY_INFO,
            blackduckUrl,
            blackduckApiToken
        );
        Assumptions.assumeTrue(blackDuckHttpClient.canConnect());
        HttpUriRequest httpUriRequest = createRequest();
        assertFalse(blackDuckHttpClient.isAlreadyAuthenticated(httpUriRequest), "Expected to NOT be authenticated");

        try (Response response = blackDuckHttpClient.execute(httpUriRequest)) {
            assertTrue(response.isStatusCodeSuccess(), "Expected successful response");
            assertTrue(blackDuckHttpClient.isAlreadyAuthenticated(response.getRequest()), "Expected to be authenticated");
        } catch (Exception e) {
            fail("An exception should NOT have been thrown", e);
        }
    }

    private HttpUriRequest createRequest() {
        HttpUrl authenticationUrl = assertDoesNotThrow(() -> blackDuckHttpClient.getBlackDuckUrl().appendRelativeUrl("api/current-user"));

        RequestBuilder requestBuilder = assertDoesNotThrow(() -> blackDuckHttpClient.createRequestBuilder(HttpMethod.GET, new HashMap<>()));
        requestBuilder.setCharset(StandardCharsets.UTF_8);
        requestBuilder.setUri(authenticationUrl.string());
        requestBuilder.addHeader(HttpHeaders.CONTENT_LENGTH, "0");
        requestBuilder.addHeader("Content-Type", "application/json");

        return requestBuilder.build();
    }
}
