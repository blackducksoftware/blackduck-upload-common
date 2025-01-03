/*
 * Copyright (C) 2020 Synopsys Inc.
 * http://www.synopsys.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Synopsys ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Synopsys.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.rest.status.ScassUploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScassUploaderTest {

    private static final int CHUNK_SIZE = 100;

    private static final long MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL = 10;

    private static final int MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS = 2;

    private static final ScassUploadStatus OK_UPLOAD_STATUS = new ScassUploadStatus(200, "OK", null, "Success");

    private static final ScassUploadStatus BAD_REQUEST_UPLOAD_STATUS = new ScassUploadStatus(400, "Bad Request", null, "Error");

    private static final String ERROR_CONTENT = "Error Content";

    private static final Path UPLOADED_FILE_PATH = Paths.get("src/test/java/com/blackduck/integration/sca/upload/client/uploaders/ScassUploaderTest.java");

    private static final String SIGNED_URL = "https://example.com/upload";

    private static final Map<String, String> HEADERS = new HashMap<>();

    @Mock
    private IntHttpClient client;

    @Mock
    private UploadValidator uploadValidator;

    private ScassUploader scassUploader;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        scassUploader = new ScassUploader(client, uploadValidator, CHUNK_SIZE, MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL,
                MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS);
    }

    @Test
    public void testWhenPutUploadIsSuccessful() throws Exception {
        // Mock the response object
        Response response = Mockito.mock(Response.class);
        Mockito.when(client.execute(Mockito.any(Request.class))).thenReturn(response);
        mockResponse(response, 200, "OK", "Success");

        ScassUploadStatus status = scassUploader.upload(HttpMethod.PUT, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(OK_UPLOAD_STATUS, status);

        Mockito.verify(client, times(1)).execute(any(Request.class));
        Mockito.verify(response, times(1)).close();
    }

    @Test
    public void testWhenPutUploadIsNotSuccessfull() throws Exception {
        // Mock the response object
        Response response = Mockito.mock(Response.class);
        Mockito.when(client.execute(Mockito.any(Request.class))).thenReturn(response);
        mockResponse(response, BAD_REQUEST_UPLOAD_STATUS.getStatusCode(), BAD_REQUEST_UPLOAD_STATUS.getStatusMessage(), BAD_REQUEST_UPLOAD_STATUS.getContent());

        IntegrationException integException = new IntegrationException("errorMessage");
        Mockito.doThrow(integException).when(client).throwExceptionForError(Mockito.any(Response.class));

        ScassUploadStatus status = scassUploader.upload(HttpMethod.PUT, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(BAD_REQUEST_UPLOAD_STATUS.getStatusCode(), status.getStatusCode());
        assertEquals(BAD_REQUEST_UPLOAD_STATUS.getStatusMessage(), status.getStatusMessage());
        assertEquals(BAD_REQUEST_UPLOAD_STATUS.getContent(), status.getContent());
        assertEquals(integException, status.getException().get());

        Mockito.verify(client, times(1)).execute(any(Request.class));
        Mockito.verify(response, times(1)).close();
    }

    @Test
    public void testWhenPutUploadThrowsException() throws Exception {
        IntegrationException integException = new IntegrationException("errorMessage");
        Mockito.doThrow(integException).when(client).execute(Mockito.any(Request.class));

        ScassUploadStatus status = scassUploader.upload(HttpMethod.PUT, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(-1, status.getStatusCode());
        assertEquals(integException, status.getException().get());

        Mockito.verify(client, times(1)).execute(any(Request.class));
    }

    @Test
    public void testWhenPostUploadIsSuccessful() throws Exception {
        // Mock the response object for the initial resumable upload initiation
        Response initialResponse = Mockito.mock(Response.class);
        Mockito.when(initialResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        Mockito.when(initialResponse.getHeaders()).thenReturn(Map.of(HttpHeaders.LOCATION, "https://example.com/upload/resumable"));

        // Mock the response object for uploading first chunk
        Response chunkResponse1 = Mockito.mock(Response.class);
        Mockito.when(chunkResponse1.getStatusCode()).thenReturn(ScassUploader.PERMANENT_REDIRECT);
        Mockito.when(chunkResponse1.getHeaders()).thenReturn(Map.of(HttpHeaders.RANGE, "bytes=0-100"));

        // Mock the response object for uploading last chunk
        Response chunkResponse2 = Mockito.mock(Response.class);
        Mockito.when(chunkResponse2.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        Mockito.when(client.execute(any(Request.class))).thenReturn(initialResponse, chunkResponse1, chunkResponse2);

        ScassUploadStatus status = scassUploader.upload(HttpMethod.POST, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(HttpStatus.SC_OK, status.getStatusCode());

        Mockito.verify(client, times(3)).execute(any(Request.class));
        Mockito.verify(initialResponse, times(1)).close();
        Mockito.verify(chunkResponse1, times(1)).close();
        Mockito.verify(chunkResponse2, times(1)).close();
    }

    @Test
    // Test when initial upload fails
    public void testWhenPostUploadIsNotSuccessful1() throws Exception {
        // Mock the response object for the initial resumable upload initiation
        Response initialResponse = Mockito.mock(Response.class);
        Mockito.when(initialResponse.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        Mockito.when(initialResponse.getContentString()).thenReturn(ERROR_CONTENT);

        Mockito.when(client.execute(any(Request.class))).thenReturn(initialResponse);

        ScassUploadStatus status = scassUploader.upload(HttpMethod.POST, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(HttpStatus.SC_BAD_REQUEST, status.getStatusCode());
        assertEquals(ERROR_CONTENT, status.getContent());

        Mockito.verify(client, times(1)).execute(any(Request.class));
        Mockito.verify(initialResponse, times(1)).close();
    }

    @Test
    // Test when first chunk upload fails
    public void testWhenPostUploadIsNotSuccessful2() throws Exception {
        // Mock the response object for the initial resumable upload initiation
        Response initialResponse = Mockito.mock(Response.class);
        Mockito.when(initialResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        Mockito.when(initialResponse.getHeaders()).thenReturn(Map.of(HttpHeaders.LOCATION, "https://example.com/upload/resumable"));

        // Mock the response object for uploading first chunk
        Response chunkResponse1 = Mockito.mock(Response.class);
        Mockito.when(chunkResponse1.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        Mockito.when(chunkResponse1.getContentString()).thenReturn(ERROR_CONTENT);

        Mockito.when(client.execute(any(Request.class))).thenReturn(initialResponse, chunkResponse1, chunkResponse1, chunkResponse1);

        ScassUploadStatus status = scassUploader.upload(HttpMethod.POST, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(HttpStatus.SC_BAD_REQUEST, status.getStatusCode());
        assertEquals(ERROR_CONTENT, status.getContent());

        Mockito.verify(client, times(4)).execute(any(Request.class));
        Mockito.verify(initialResponse, times(1)).close();
        Mockito.verify(chunkResponse1, times(3)).close();
    }

    @Test
    // Test when first chunk upload fails at first attempt, but succeeds at second attempt
    public void testWhenPostUploadIsNotSuccessful3() throws Exception {
        // Mock the response object for the initial resumable upload initiation
        Response initialResponse = Mockito.mock(Response.class);
        Mockito.when(initialResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        Mockito.when(initialResponse.getHeaders()).thenReturn(Map.of(HttpHeaders.LOCATION, "https://example.com/upload/resumable"));

        // Mock the response object for uploading first chunk
        Response chunkResponse1 = Mockito.mock(Response.class);
        Mockito.when(chunkResponse1.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        Mockito.when(chunkResponse1.getContentString()).thenReturn(ERROR_CONTENT);

        Response chunkResponse2 = Mockito.mock(Response.class);
        Mockito.when(chunkResponse2.getStatusCode()).thenReturn(ScassUploader.PERMANENT_REDIRECT);
        Mockito.when(chunkResponse2.getHeaders()).thenReturn(Map.of(HttpHeaders.RANGE, "bytes=0-100"));

        // Mock the response object for uploading last chunk
        Response chunkResponse3 = Mockito.mock(Response.class);
        Mockito.when(chunkResponse3.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        Mockito.when(client.execute(any(Request.class))).thenReturn(initialResponse, chunkResponse1, chunkResponse2, chunkResponse3);

        ScassUploadStatus status = scassUploader.upload(HttpMethod.POST, SIGNED_URL, HEADERS, UPLOADED_FILE_PATH);

        assertEquals(HttpStatus.SC_OK, status.getStatusCode());

        Mockito.verify(client, times(4)).execute(any(Request.class));
        Mockito.verify(initialResponse, times(1)).close();
        Mockito.verify(chunkResponse1, times(1)).close();
        Mockito.verify(chunkResponse2, times(1)).close();
        Mockito.verify(chunkResponse3, times(1)).close();
    }

    private void mockResponse(Response response, int status, String statusMessage, String content) throws IntegrationException {
        Mockito.when(response.getStatusCode()).thenReturn(status);
        Mockito.when(response.getStatusMessage()).thenReturn(statusMessage);
        Mockito.when(response.getContentString()).thenReturn(content);
    }

}
