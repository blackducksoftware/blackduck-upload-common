package com.blackduck.integration.sca.upload.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import com.blackduck.integration.exception.IntegrationException;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.google.gson.Gson;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploaderHeaderTest {
    private static Map<String, String> responseHeaders;
    private BlackDuckHttpClient mockHttpClient;
    private FileUploader uploader;

    @BeforeAll
    static void setupHeaders() {
        responseHeaders = new HashMap<>();
        responseHeaders.put("date", "Tue, 18 Nov 2025 07:18:56 GMT");
        responseHeaders.put("server", "nginx");
        responseHeaders.put("content-length", "0");
        responseHeaders.put("expires", "0");
        responseHeaders.put("x-frame-options", "SAMEORIGIN");
        responseHeaders.put("pragma", "no-cache");
        responseHeaders.put("strict-transport-security", "max-age=31536000; includeSubDomains");
        responseHeaders.put("content-security-policy", "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'self'");
        responseHeaders.put("x-content-type-options", "nosniff, nosniff");
        responseHeaders.put("x-xss-protection", "1; mode=block, 1; mode=block");
        responseHeaders.put("referrer-policy", "no-referrer-when-downgrade");
        responseHeaders.put("connection", "keep-alive");
        responseHeaders.put("location", "https://mock-server.internal/api/storage/bdba/a7ca431b-6f3b-4ebf-ab00-6e998f2fe6ef/multipart");
        responseHeaders.put("cache-control", "no-cache, no-store, max-age=0, must-revalidate, no-store, no-cache, must-revalidate");
    }

    @BeforeEach
    public void setup() throws IntegrationException {
        mockHttpClient = mock(BlackDuckHttpClient.class);
        when(mockHttpClient.getBlackDuckUrl()).thenReturn(new HttpUrl("https://localhost"));
        when(mockHttpClient.getGson()).thenReturn(new Gson());
        uploader = new FileUploader(mockHttpClient, new UploadRequestPaths("/api/uploads/"), 0, 0, 1);
    }

    @Test
    void testStartMultipartUploadHandlesLocationHeaderCaseInsensitively() throws Exception {
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(201);
        when(mockResponse.getStatusMessage()).thenReturn("Created");
        when(mockResponse.getHeaders()).thenReturn(responseHeaders);
        when(mockResponse.isStatusCodeSuccess()).thenReturn(true);

        when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);
        doNothing().when(mockHttpClient).throwExceptionForError(any(Response.class));

        MutableResponseStatus status = new MutableResponseStatus(-1, "unknown");
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/vnd.blackducksoftware.multipart-upload-start-1+json");
        MultipartUploadStartRequest startRequest = new MultipartUploadStartRequest(39L, "checksum");

        String uploadUrl = uploader.startMultipartUpload(status, headers, "application/vnd.blackducksoftware.multipart-upload-start-1+json", startRequest);

        assertEquals("https://mock-server.internal/api/storage/bdba/a7ca431b-6f3b-4ebf-ab00-6e998f2fe6ef/multipart", uploadUrl);
        assertEquals(201, status.getStatusCode());
        assertEquals("Created", status.getStatusMessage());

        verify(mockResponse).getStatusCode();
        verify(mockResponse).getStatusMessage();
        verify(mockResponse).getHeaders();
    }

    @Test
    void testStartMultipartUploadMissingLocationHeader() throws Exception {
        Map<String, String> headersWithoutLocation = new HashMap<>();
        headersWithoutLocation.put("date", "Tue, 18 Nov 2025 07:18:56 GMT");
        headersWithoutLocation.put("server", "nginx");
        headersWithoutLocation.put("content-length", "0");

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(201);
        when(mockResponse.getStatusMessage()).thenReturn("Created");
        when(mockResponse.getHeaders()).thenReturn(headersWithoutLocation);
        when(mockResponse.isStatusCodeSuccess()).thenReturn(true);

        when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);
        doNothing().when(mockHttpClient).throwExceptionForError(any(Response.class));

        MutableResponseStatus status = new MutableResponseStatus(-1, "unknown");
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/vnd.blackducksoftware.multipart-upload-start-1+json");
        MultipartUploadStartRequest startRequest = new MultipartUploadStartRequest(39L, "checksum");

        IntegrationException exception = assertThrows(
            IntegrationException.class,
            () -> uploader.startMultipartUpload(status, headers, "application/vnd.blackducksoftware.multipart-upload-start-1+json", startRequest)
        );

        assertEquals("Could not find Location header.", exception.getMessage());

        verify(mockResponse).getStatusCode();
        verify(mockResponse).getStatusMessage();
        verify(mockResponse).getHeaders();
    }

    @Test
    void testStartMultipartUploadHttpError() throws Exception {
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(500);
        when(mockResponse.getStatusMessage()).thenReturn("Internal Server Error");
        when(mockResponse.getHeaders()).thenReturn(responseHeaders);
        when(mockResponse.isStatusCodeSuccess()).thenReturn(false);

        when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);
        doThrow(new IntegrationException("Server error")).when(mockHttpClient).throwExceptionForError(any(Response.class));

        MutableResponseStatus status = new MutableResponseStatus(-1, "unknown");
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/vnd.blackducksoftware.multipart-upload-start-1+json");
        MultipartUploadStartRequest startRequest = new MultipartUploadStartRequest(39L, "checksum");

        IntegrationException exception = assertThrows(
            IntegrationException.class,
            () -> uploader.startMultipartUpload(status, headers, "application/vnd.blackducksoftware.multipart-upload-start-1+json", startRequest)
        );

        assertEquals("Server error", exception.getMessage());
        assertEquals(500, status.getStatusCode());
        assertEquals("Internal Server Error", status.getStatusMessage());

        verify(mockResponse).getStatusCode();
        verify(mockResponse).getStatusMessage();
        verify(mockHttpClient).throwExceptionForError(mockResponse);
    }
}
