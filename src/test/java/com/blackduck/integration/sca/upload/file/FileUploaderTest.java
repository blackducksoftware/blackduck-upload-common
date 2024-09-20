package com.blackduck.integration.sca.upload.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.MultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.test.TestPropertyKey;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.properties.TestPropertiesManager;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.google.gson.Gson;

class FileUploaderTest {
    private static final TestPropertiesManager testPropertiesManager = TestPropertyKey.getPropertiesManager();
    private static final int CHUNK_SIZE = 1024 * 1024 * 5; // 5MB
    private static final String SAMPLE_FILE_PATH = "src/test/resources/sample_file_100MB.txt";

    private static MultipartUploadFileMetadata metaData;
    private final UploadRequestPaths uploadRequestPaths = new UploadRequestPaths("/api/uploads/");

    private Response mockFailureResponse;
    private Response mockSuccessResponse;
    private BlackDuckHttpClient mockHttpClient;
    private MutableResponseStatus mutableResponseStatus;

    @BeforeAll
    static void initAll() throws IOException {
        metaData = new FileSplitter().splitFile(Path.of(SAMPLE_FILE_PATH), CHUNK_SIZE);
    }

    @BeforeEach
    void initEach() {
        mockFailureResponse = Mockito.mock(Response.class);
        mockSuccessResponse = Mockito.mock(Response.class);
        mockHttpClient = Mockito.mock(BlackDuckHttpClient.class);

        Mockito.when(mockSuccessResponse.isStatusCodeSuccess()).thenReturn(true);

        mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");
    }

    @Test
    void testMultipartUploadPartsRetryFails() throws Exception {
        chainFailureResponses(1);
        // Set retry attempts to 0
        FileUploader fileUploader = new FileUploader(mockHttpClient, uploadRequestPaths, 0, 0, 10);
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, "https://invalid");
        // All parts fail when one failed
        assertEquals(0, partsMap.size());
        // Execute upload part is called twice, once for first upload and second for canceling upload
        Mockito.verify(mockHttpClient, Mockito.times(2)).execute(Mockito.any(Request.class));
    }

    @ParameterizedTest
    @MethodSource("retryableStatusCodes")
    void testMultipartUploadPartsRetrySucceeds(int statusCode) throws Exception {
        chainFailureResponses(1);
        // Retry for all retryable status codes
        Mockito.when(mockFailureResponse.getStatusCode()).thenReturn(statusCode);
        // Set retry attempts
        FileUploader fileUploader = new FileUploader(mockHttpClient, uploadRequestPaths, 1, 0, 10);
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, "https://invalid");
        // All parts returns
        assertEquals(20, partsMap.size());
    }

    private static Set<Integer> retryableStatusCodes() {
        return UploadValidator.MULTIPART_UPLOAD_PART_RETRY_STATUS_CODES;
    }

    @Test
    void testMultipartUploadPartsMultipleRetriesSucceeds() throws Exception {
        chainFailureResponses(50);
        // Retry for all retryable status codes
        Mockito.when(mockFailureResponse.getStatusCode()).thenReturn(HttpStatus.SC_GATEWAY_TIMEOUT);
        // Set retry attempts to 50
        FileUploader fileUploader = new FileUploader(mockHttpClient, uploadRequestPaths, 50, 0, 10);
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, "https://invalid");
        // All parts returns
        assertEquals(20, partsMap.size());
    }

    @Test
    void testMultipartUploadPartsRetryFromProperty() throws Exception {
        String retryAttemptsProperty =
            testPropertiesManager.getProperty(TestPropertyKey.MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS.getPropertyKey()).orElse("0");
        int retryAttempts = Integer.parseInt(retryAttemptsProperty);

        // Return gateway timeout for failures
        Mockito.when(mockFailureResponse.getStatusCode()).thenReturn(HttpStatus.SC_BAD_GATEWAY);
        // Fail according retry attempt property
        chainFailureResponses(retryAttempts);

        // Set retry attempts to the number of retryable status codes
        FileUploader fileUploader = new FileUploader(mockHttpClient, uploadRequestPaths, retryAttempts, 0, 10);
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, "https://invalid");
        // All parts returns
        assertEquals(20, partsMap.size());
        // Verify the number of times execute was called is number of retries (failure) + 20 (success)
        Mockito.verify(mockHttpClient, Mockito.times(retryAttempts + 20)).execute(Mockito.any(Request.class));
    }

    @Test
    void testMultipartUploadVerifyFailure() throws Exception {
        Map<String, String> startRequestHeaders = Map.of("Content-Type", "Test");
        MultipartUploadStartRequest multipartUploadStartRequest = new MultipartUploadStartRequest(1L, "abc123");
        BiFunction<MutableResponseStatus, IntegrationException, BinaryUploadStatus> testStatusErrorFunction = (response, exception) ->
            new BinaryUploadStatus(1, "statusMessage", exception, null);

        // Mock general setup
        Mockito.when(mockHttpClient.getGson()).thenReturn(new Gson());

        // Mock start request
        Mockito.when(mockHttpClient.getBlackDuckUrl()).thenReturn(new HttpUrl("https://someUrl"));
        Response mockStartResponse = Mockito.mock(Response.class);
        Mockito.when(mockStartResponse.getStatusCode()).thenReturn(200);
        Mockito.when(mockStartResponse.getStatusMessage()).thenReturn("statusMessage");
        Mockito.when(mockStartResponse.getHeaders()).thenReturn(Map.of(HttpHeaders.LOCATION, "https://urlToUploadTo"));
        Mockito.when(mockHttpClient.execute(Mockito.any(Request.class))).thenReturn(mockStartResponse);
        // No mocks for the upload parts. This means that all 20 part uploads will be skipped, but no error is logged.
        // This simulates a scenario where a thread is dropped and is unrecoverable before an exception is created.

        FileUploader fileUploader = new FileUploader(mockHttpClient, uploadRequestPaths, 1, 0, 10);
        BinaryUploadStatus binaryUploadStatus = fileUploader.multipartUpload(
            metaData,
            startRequestHeaders,
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            multipartUploadStartRequest,
            null,
            testStatusErrorFunction
        );

        assertTrue(binaryUploadStatus.isError());
        IntegrationException exception = binaryUploadStatus.getException().orElseThrow(() -> new AssertionError("Could not get response content when it was expected."));
        assertTrue(exception.getMessage().contains("The number of parts uploaded does not match the number of parts uploaded."));
    }

    // Chain the number of failures as specified and succeed afterward
    private void chainFailureResponses(int failureCount) throws IntegrationException {
        OngoingStubbing<Response> stub = Mockito.when(mockHttpClient.execute(Mockito.any(Request.class)));
        for (int i = 0; i < failureCount; i++) {
            stub = stub.thenReturn(mockFailureResponse);
        }
        stub.thenReturn(mockSuccessResponse);
    }
}
