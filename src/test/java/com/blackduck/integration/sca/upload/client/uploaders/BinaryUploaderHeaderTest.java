package com.blackduck.integration.sca.upload.client.uploaders;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.model.BinaryScanRequestData;
import com.blackduck.integration.sca.upload.file.FileUploader;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.sca.upload.validation.UploadValidator;

class BinaryUploaderHeaderTest {
    
    private static final String LOCATION_URL = "https://example.com/upload/123";
    private static final String EXPECTED_ETAG = "\"a5dd8dec-cb28-473c-a269-738faa6a2b84\"";
    private static final int EXPECTED_STATUS_CODE = 201;
    private static final String EXPECTED_STATUS_MESSAGE = "Created";
    
    private static final String[] ETAG_VARIANTS = {"ETag", "Etag"}; // Case variations for testing

    @Test
    void testCreateUploadStatus_CaseInsensitiveETag() throws Exception {
        FileUploader mockFileUploader = mock(FileUploader.class);
        UploadValidator mockUploadValidator = mock(UploadValidator.class);
        BinaryScanRequestData mockBinaryScanRequestData = mock(BinaryScanRequestData.class);
        Response mockResponse = mock(Response.class);

        BinaryUploader binaryUploader = new BinaryUploader(1000, mockFileUploader, mockUploadValidator, mockBinaryScanRequestData);

        int expectedHeadersCalls = ETAG_VARIANTS.length;

        for (String eTagVariant : ETAG_VARIANTS) {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.LOCATION, LOCATION_URL);
            headers.put(eTagVariant, EXPECTED_ETAG); // Varying ETag case

            when(mockResponse.getStatusCode()).thenReturn(EXPECTED_STATUS_CODE);
            when(mockResponse.getStatusMessage()).thenReturn(EXPECTED_STATUS_MESSAGE);
            when(mockResponse.getHeaders()).thenReturn(headers);

            BinaryUploadStatus status = binaryUploader.createUploadStatus().apply(mockResponse);

            assertEquals(EXPECTED_STATUS_CODE, status.getStatusCode());
            assertEquals(EXPECTED_STATUS_MESSAGE, status.getStatusMessage());
            assertTrue(status.getResponseContent().isPresent(), "Response content should be present");
            assertEquals(LOCATION_URL, status.getResponseContent().orElseThrow().getLocation());
            assertEquals(EXPECTED_ETAG, status.getResponseContent().orElseThrow().getETag());
        }

        verify(mockResponse, times(expectedHeadersCalls)).getHeaders();
    }
}
