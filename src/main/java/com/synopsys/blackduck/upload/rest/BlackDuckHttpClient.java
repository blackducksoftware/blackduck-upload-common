package com.synopsys.blackduck.upload.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.client.AuthenticatingIntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.response.Response;
import com.synopsys.integration.rest.support.AuthenticationSupport;

/**
 * The class to execute Black Duck REST API requests to a Black Duck server.
 */
public class BlackDuckHttpClient extends AuthenticatingIntHttpClient {
    private static final String AUTHORIZATION_TYPE = "Bearer";
    public static final String AUTHENTICATION_SUFFIX = "api/tokens/authenticate";
    public static final String BEARER_RESPONSE_KEY = "bearerToken";

    private final Gson gson;
    private final HttpUrl blackDuckUrl;
    private final String apiToken;
    private final AuthenticationSupport authenticationSupport = new AuthenticationSupport();

    /**
     * Constructor for the HTTP client.
     * @param logger                       The {@link IntLogger} to log messages from the HTTP requests.
     * @param gson                         The object to serialize/deserialize data to and from JSON.
     * @param timeoutInSeconds             The timeout for the HTTP client to wait for a request to execute before failing the request with a timeout error.
     * @param alwaysTrustServerCertificate True to always trust server certificates including self-signed.  Not recommended for production.
     * @param proxyInfo                    The proxy information to use when sending the HTTP requests or {@link ProxyInfo#NO_PROXY_INFO} if a proxy isn't being used.
     * @param blackDuckUrl                 The {@link HttpUrl} of the Black Duck server in order to execute REST API requests.
     * @param apiToken                     The Black Duck API token to use for authentication in order to execute REST API requests.
     */
    public BlackDuckHttpClient(
        IntLogger logger,
        Gson gson,
        int timeoutInSeconds,
        boolean alwaysTrustServerCertificate,
        ProxyInfo proxyInfo,
        HttpUrl blackDuckUrl,
        String apiToken
    ) {
        super(logger, gson, timeoutInSeconds, alwaysTrustServerCertificate, proxyInfo);
        this.gson = gson;
        this.blackDuckUrl = blackDuckUrl;
        this.apiToken = apiToken;
    }

    /**
     * Constructor for the HTTP client.
     * @param logger           The {@link IntLogger} to log messages from the HTTP requests.
     * @param gson             The object to serialize/deserialize data to and from JSON.
     * @param timeoutInSeconds The timeout for the HTTP client to wait for a request to execute before failing the request with a timeout error.
     * @param proxyInfo        The proxy information to use when sending the HTTP requests or {@link ProxyInfo#NO_PROXY_INFO} if a proxy isn't being used.
     * @param sslContext       The {@link SSLContext} containing the trusted certificate information in order to establish SSL connections to the Black Duck server.
     * @param blackDuckUrl     The {@link HttpUrl} of the Black Duck server in order to execute REST API requests.
     * @param apiToken         The Black Duck API token to use for authentication in order to execute REST API requests.
     */
    public BlackDuckHttpClient(
        IntLogger logger,
        Gson gson,
        int timeoutInSeconds,
        ProxyInfo proxyInfo,
        SSLContext sslContext,
        HttpUrl blackDuckUrl,
        String apiToken
    ) {
        super(logger, gson, timeoutInSeconds, proxyInfo, sslContext);
        this.gson = gson;
        this.blackDuckUrl = blackDuckUrl;
        this.apiToken = apiToken;
    }

    /**
     * Constructor for the HTTP client.
     * @param logger                       The {@link IntLogger} to log messages from the HTTP requests.
     * @param gson                         The object to serialize/deserialize data to and from JSON.
     * @param timeoutInSeconds             The timeout for the HTTP client to wait for a request to execute before failing the request with a timeout error.
     * @param alwaysTrustServerCertificate True to always trust server certificates including self-signed.  Not recommended for production.
     * @param proxyInfo                    The proxy information to use when sending the HTTP requests or {@link ProxyInfo#NO_PROXY_INFO} if a proxy isn't being used.
     * @param credentialsProvider          The object responsible for providing the credentials to the HTTP request in order to authenticate with the Black Duck server.
     * @param clientBuilder                The builder of the HTTP client in order to construct an {@link HttpClient} to execute HTTP requests to the Black Duck server.
     * @param defaultRequestConfigBuilder  The builder to create a {@link RequestConfig} in order to contain information to control how
     *                                     an HTTP request is sent through the {@link HttpClient} to the Black Duck server.
     * @param commonRequestHeaders         The map containing HTTP request header names and values that will be supplied to each HTTP request.
     * @param blackDuckUrl                 The {@link HttpUrl} of the Black Duck server in order to execute REST API requests.
     * @param apiToken                     The Black Duck API token to use for authentication in order to execute REST API requests.
     */
    public BlackDuckHttpClient(
        IntLogger logger,
        Gson gson,
        int timeoutInSeconds,
        boolean alwaysTrustServerCertificate,
        ProxyInfo proxyInfo,
        CredentialsProvider credentialsProvider,
        HttpClientBuilder clientBuilder,
        RequestConfig.Builder defaultRequestConfigBuilder,
        Map<String, String> commonRequestHeaders,
        HttpUrl blackDuckUrl,
        String apiToken
    ) {
        super(logger, gson, timeoutInSeconds, alwaysTrustServerCertificate, proxyInfo, credentialsProvider, clientBuilder, defaultRequestConfigBuilder, commonRequestHeaders);
        this.gson = gson;
        this.blackDuckUrl = blackDuckUrl;
        this.apiToken = apiToken;
    }

    @Override
    public boolean isAlreadyAuthenticated(HttpUriRequest request) {
        return Arrays.stream(request.getHeaders(AuthenticationSupport.AUTHORIZATION_HEADER))
            .map(Header::getValue)
            .anyMatch(header -> header.startsWith(AUTHORIZATION_TYPE));
    }

    @Override
    public Response attemptAuthentication() throws IntegrationException {
        Map<String, String> headers = new HashMap<>();
        headers.put(AuthenticationSupport.AUTHORIZATION_HEADER, "token " + getApiToken());

        return authenticationSupport.attemptAuthentication(this, getBlackDuckUrl(), AUTHENTICATION_SUFFIX, headers);
    }

    @Override
    protected void completeAuthenticationRequest(HttpUriRequest request, Response response) {
        authenticationSupport.completeTokenAuthenticationRequest(request, response, logger, getGson(), this, BEARER_RESPONSE_KEY);
    }

    /**
     * Retrieve the {@link Gson} instance used to serialize/deserialize objects to/from JSON when communicating with a Black Duck server.
     * @return The instance of {@link Gson} for serializing and deserializing data objects.
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Retrieve the URL to the Black Duck server in order to execute REST API requests against the server.
     * @return The {@link HttpUrl} of the Black Duck server in order to execute REST API requests.
     */
    public HttpUrl getBlackDuckUrl() {
        return blackDuckUrl;
    }

    /**
     * Retrieve the API token used to authenticate with the Black Duck server.
     * @return The Black Duck API token to execute REST API requests.
     */
    public String getApiToken() {
        return apiToken;
    }

}
