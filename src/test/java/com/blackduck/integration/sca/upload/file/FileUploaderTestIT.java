package com.blackduck.integration.sca.upload.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.function.ThrowingFunction;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.properties.TestPropertiesManager;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.MultipartBodyContent;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.model.BinaryScanRequestData;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;
import com.blackduck.integration.sca.upload.generator.RandomByteContentFileGenerator;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.rest.model.ContentTypes;
import com.blackduck.integration.sca.upload.rest.model.request.BinaryMultipartUploadStartRequest;
import com.blackduck.integration.sca.upload.rest.model.response.BinaryFinishResponseContent;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.MutableResponseStatus;
import com.blackduck.integration.sca.upload.test.TestPropertyKey;
import com.google.gson.Gson;

/**
 * Test the functionality of the FileUploader class using the Binary uploader.
 *
 * Note: In order to run this integration test, the configured Blackduck server must be running a bdba-worker. Without this, this test will fail attempting to communicate with
 * Blackduck. If bdba-worker is running and the TestPropertyKey.BDBA_CONTAINER_AVAILABLE is set to "true", then the tests in this class will run. Otherwise, these tests are skipped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploaderTestIT {
    private static final TestPropertiesManager testPropertiesManager = TestPropertyKey.getPropertiesManager();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UploadRequestPaths uploadRequestPaths = new UploadRequestPaths("/api/uploads/");
    private final int chunkSize = 1024 * 1024 * 5; // 5MB
    private final FileSplitter fileSplitter = new FileSplitter();
    private BlackDuckHttpClient httpClient;
    private BinaryScanRequestData binaryScanRequestData;
    private Path generatedSampleFilePath;
    // See unit tests for retry
    private int retryAttempts = 0;
    private long retryInitialInterval = 0;
    private final int uploadTimeoutMinutes = 10;

    @BeforeAll
    void createSampleFile() throws IOException {
        RandomByteContentFileGenerator randomByteContentFileGenerator = new RandomByteContentFileGenerator();
        long fileSize = 1024 * 1024 * 100L;
        generatedSampleFilePath = randomByteContentFileGenerator.generateFile(fileSize, ".bin").orElseThrow(() -> new IOException("Could not generate file"));
    }

    @BeforeEach
    void init() {
        System.setProperty("org.slf4j.simpleLogger.log.com.blackduck", "debug");
        boolean bdbaAvailable = testPropertiesManager.getProperty(TestPropertyKey.BDBA_CONTAINER_AVAILABLE.getPropertyKey())
            .map(Boolean::valueOf)
            .orElse(false);
        assumeTrue(bdbaAvailable);

        String blackduckUrlString = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_URL.getPropertyKey()));
        HttpUrl blackduckUrl = assertDoesNotThrow(() -> new HttpUrl(blackduckUrlString));
        String blackduckApiToken = assertDoesNotThrow(()
            -> testPropertiesManager.getRequiredProperty(TestPropertyKey.TEST_BLACKDUCK_API_TOKEN.getPropertyKey()));

        String retryAttemptsProperty =
            testPropertiesManager.getProperty(TestPropertyKey.MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS.getPropertyKey()).orElse("0");
        retryAttempts = Integer.parseInt(retryAttemptsProperty);

        String retryInitialIntervalProperty =
            testPropertiesManager.getProperty(TestPropertyKey.MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL.getPropertyKey()).orElse("0");
        retryInitialInterval = Long.parseLong(retryInitialIntervalProperty);

        IntLogger intLogger = new Slf4jIntLogger(logger);
        int timeoutInSeconds = 120;
        httpClient = new BlackDuckHttpClient(
            intLogger,
            new Gson(),
            timeoutInSeconds,
            true,
            ProxyInfo.NO_PROXY_INFO,
            blackduckUrl,
            blackduckApiToken
        );

        binaryScanRequestData = new BinaryScanRequestData(
            "project name",
            "project version",
            "code location name",
            "https://code-location-uri"
        );
    }

    private ThrowingFunction<Response, BinaryUploadStatus, IntegrationException> createBinaryUploadStatusFunction() {
        return response -> {
            int statusCode = response.getStatusCode();
            String statusMessage = response.getStatusMessage();

            Map<String, String> responseHeaders = response.getHeaders();
            String location = Optional.ofNullable(responseHeaders.get(HttpHeaders.LOCATION)).orElseThrow(() -> new IntegrationException("Could not find Location header."));
            String eTag = Optional.ofNullable(responseHeaders.get(HttpHeaders.ETAG)).orElseThrow(() -> new IntegrationException("Could not find Etag header."));
            BinaryFinishResponseContent binaryFinishResponseContent = new BinaryFinishResponseContent(location, eTag);

            return new BinaryUploadStatus(statusCode, statusMessage, null, binaryFinishResponseContent);
        };
    }

    private BiFunction<MutableResponseStatus, IntegrationException, BinaryUploadStatus> createUploadStatusErrorFunction() {
        return (response, exception) -> new BinaryUploadStatus(response.getStatusCode(), response.getStatusMessage(), exception, null);
    }

    // Upload tests
    @Test
    void testUpload() throws Exception {
        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        MultipartBodyContent bodyContent = new MultipartBodyContent(
            Map.of("fileupload", generatedSampleFilePath.toFile()),
            Map.of(
                "projectName", binaryScanRequestData.getProjectName(),
                "version", binaryScanRequestData.getVersion(),
                "codeLocationName", binaryScanRequestData.getCodeLocationName().orElse(""),
                "codeLocationUri", binaryScanRequestData.getCodeLocationUri().orElse("")
            )
        );

        BinaryUploadStatus uploadStatus = fileUploader.upload(bodyContent, createBinaryUploadStatusFunction(), createUploadStatusErrorFunction());
        assertFalse(uploadStatus.isError());
        assertTrue(uploadStatus.hasContent());
        BinaryFinishResponseContent binaryFinishResponseContent = uploadStatus.getResponseContent()
            .orElseThrow(() -> new AssertionError("Could not get response content when it was expected."));
        assertNotNull(binaryFinishResponseContent.getETag());
        assertNotNull(binaryFinishResponseContent.getLocation());
    }

    @Test
    void testMultipartUpload() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);

        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        Map<String, String> startRequestHeaders = Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(metaData.getFileSize(), metaData.getChecksum(), binaryScanRequestData);

        BinaryUploadStatus uploadStatus = fileUploader.multipartUpload(
            metaData,
            startRequestHeaders,
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest,
            createBinaryUploadStatusFunction(),
            createUploadStatusErrorFunction()
        );
        assertFalse(uploadStatus.isError());
        assertTrue(uploadStatus.hasContent());
        assertTrue(uploadStatus.getStatusCode() < 400);
        BinaryFinishResponseContent binaryFinishResponseContent = uploadStatus.getResponseContent()
            .orElseThrow(() -> new AssertionError("Could not get response content when it was expected."));
        assertNotNull(binaryFinishResponseContent.getETag());
        assertNotNull(binaryFinishResponseContent.getLocation());
    }

    @Test
    void testMultipartUploadFailure() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);

        //Modify the part list to create an invalid checksum on one of the parts.
        MultipartUploadFileMetadata invalidMetadata = new MultipartUploadFileMetadata(
            metaData.getFileName(),
            "badChecksum",
            metaData.getUploadId(),
            metaData.getFileSize(),
            metaData.getChunkSize(),
            metaData.getFileChunks()
        );

        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        Map<String, String> startRequestHeaders = Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(
            invalidMetadata.getFileSize(),
            invalidMetadata.getChecksum(),
            binaryScanRequestData
        );

        BinaryUploadStatus uploadStatus = fileUploader.multipartUpload(
            metaData,
            startRequestHeaders,
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest,
            createBinaryUploadStatusFunction(),
            createUploadStatusErrorFunction()
        );
        assertTrue(uploadStatus.isError());
        assertFalse(uploadStatus.hasContent());
        assertTrue(uploadStatus.getStatusCode() >= 400);
        uploadStatus.getException().orElseThrow(() -> new AssertionError("Could not get exception when it was expected."));
        IntegrationException exception = uploadStatus.getException()
            .orElseThrow(() -> new AssertionError("Could not get response content when it was expected."));
        assertNotNull(exception.getMessage());
    }

    // Multipart upload tests
    @Test
    void testMultipartUploadStart() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(metaData.getFileSize(), metaData.getChecksum(), binaryScanRequestData);
        String startUploadUrl = fileUploader.startMultipartUpload(
            mutableResponseStatus,
            Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1),
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest
        );
        assertDoesNotThrow(() -> new URL(startUploadUrl).toURI());
        assertTrue(mutableResponseStatus.getStatusCode() < 400);
    }

    @Test
    void testMultipartUploadParts() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(metaData.getFileSize(), metaData.getChecksum(), binaryScanRequestData);
        String startUploadUrl = fileUploader.startMultipartUpload(
            mutableResponseStatus,
            Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1),
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest
        );
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, startUploadUrl);

        assertEquals(20, partsMap.size());
        assertTrue(mutableResponseStatus.getStatusCode() < 400);
    }

    @Test
    void testMultipartUploadPartsFail() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        BlackDuckHttpClient incorrectHttpClient = new BlackDuckHttpClient(
            new Slf4jIntLogger(logger),
            new Gson(),
            120,
            true,
            ProxyInfo.NO_PROXY_INFO,
            new HttpUrl("https://invalid"),
            "incorrect token"
        );

        FileUploader fileUploader = new FileUploader(incorrectHttpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, "https://invalid");

        assertEquals(0, partsMap.size());
    }

    @Test
    void testMultipartUploadPartsFromInvalidPath() throws Exception {
        MultipartUploadFilePart multipartUploadFilePart = new MultipartUploadFilePart(UUID.randomUUID(), "checksum", 1, 0, 1, Path.of("/tmp/doesnt/exist"));
        MultipartUploadFileMetadata metaData = new MultipartUploadFileMetadata("fileName", "checksum", UUID.randomUUID(), 100L, 1, List.of(multipartUploadFilePart));
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(metaData.getFileSize(), metaData.getChecksum(), binaryScanRequestData);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        String startUploadUrl = fileUploader.startMultipartUpload(
            mutableResponseStatus,
            Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1),
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest
        );
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, startUploadUrl);

        assertEquals(0, partsMap.size());
    }

    @Test
    void testUploadCancel() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        //Modify the part list to create an invalid checksum on one of the parts.
        List<MultipartUploadFilePart> modifiedPartList = metaData.getFileChunks();
        MultipartUploadFilePart existingFilePart = modifiedPartList.get(0);
        MultipartUploadFilePart invalidFilePart = new MultipartUploadFilePart(
            existingFilePart.getTagId(),
            "invalidChecksum",
            existingFilePart.getIndex(),
            existingFilePart.getStartByteRange(),
            existingFilePart.getChunkSize(),
            existingFilePart.getFilePath()
        );
        modifiedPartList.set(0, invalidFilePart);

        MultipartUploadFileMetadata invalidMetadata = new MultipartUploadFileMetadata(
            metaData.getFileName(),
            metaData.getChecksum(),
            metaData.getUploadId(),
            metaData.getFileSize(),
            metaData.getChunkSize(),
            modifiedPartList
        );

        FileUploader multipartFileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(invalidMetadata.getFileSize(), metaData.getChecksum(), binaryScanRequestData);
        String startUploadUrl = multipartFileUploader.startMultipartUpload(
            mutableResponseStatus,
            Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1),
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest
        );
        Map<Integer, String> partsMap = multipartFileUploader.multipartUploadParts(mutableResponseStatus, invalidMetadata, startUploadUrl);
        // TODO: All fail when one fails due to single threaded nature
        assertEquals(0, partsMap.size());
        assertFalse(mutableResponseStatus.getStatusCode() < 400);
    }

    @Test
    void testMultipartUploadFinish() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(metaData.getFileSize(), metaData.getChecksum(), binaryScanRequestData);
        String startUploadUrl = fileUploader.startMultipartUpload(
            mutableResponseStatus,
            Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1),
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest
        );
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, metaData, startUploadUrl);

        assertEquals(20, partsMap.size());

        BinaryUploadStatus uploadStatus = fileUploader.finishMultipartUpload(mutableResponseStatus, startUploadUrl, createBinaryUploadStatusFunction());
        assertFalse(uploadStatus.isError());
        assertTrue(uploadStatus.hasContent());
        BinaryFinishResponseContent binaryFinishResponseContent = uploadStatus.getResponseContent()
            .orElseThrow(() -> new AssertionError("Could not get response content when it was expected."));
        assertNotNull(binaryFinishResponseContent.getETag());
        assertNotNull(binaryFinishResponseContent.getLocation());
    }

    @Test
    void testFinishBadChecksum() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);
        MutableResponseStatus mutableResponseStatus = new MutableResponseStatus(-1, "unknown status message");

        //Modify the part list to create an invalid checksum on one of the parts.
        MultipartUploadFileMetadata invalidMetadata = new MultipartUploadFileMetadata(
            metaData.getFileName(),
            "badChecksum",
            metaData.getUploadId(),
            metaData.getFileSize(),
            metaData.getChunkSize(),
            metaData.getFileChunks()
        );
        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, uploadTimeoutMinutes);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(
            invalidMetadata.getFileSize(),
            invalidMetadata.getChecksum(),
            binaryScanRequestData
        );
        String startUploadUrl = fileUploader.startMultipartUpload(
            mutableResponseStatus,
            Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1),
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest
        );
        Map<Integer, String> partsMap = fileUploader.multipartUploadParts(mutableResponseStatus, invalidMetadata, startUploadUrl);

        assertEquals(20, partsMap.size());

        assertThrows(IntegrationException.class, () -> fileUploader.finishMultipartUpload(mutableResponseStatus, startUploadUrl, createBinaryUploadStatusFunction()));
    }

    @Test
    void testMultipartUploadTimeOut() throws Exception {
        MultipartUploadFileMetadata metaData = fileSplitter.splitFile(generatedSampleFilePath, chunkSize);

        // Set timeout to 0 minutes to terminate executor service immediately
        FileUploader fileUploader = new FileUploader(httpClient, uploadRequestPaths, retryAttempts, retryInitialInterval, 0);
        Map<String, String> startRequestHeaders = Map.of(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1);
        BinaryMultipartUploadStartRequest uploadStartRequest = new BinaryMultipartUploadStartRequest(metaData.getFileSize(), metaData.getChecksum(), binaryScanRequestData);

        BinaryUploadStatus uploadStatus = fileUploader.multipartUpload(
            metaData,
            startRequestHeaders,
            ContentTypes.APPLICATION_BINARY_MULTIPART_UPLOAD_START_V1,
            uploadStartRequest,
            createBinaryUploadStatusFunction(),
            createUploadStatusErrorFunction()
        );
        assertTrue(uploadStatus.isError());
        IntegrationException ex = uploadStatus.getException().orElseThrow(() -> new IntegrationException("An exception was expected."));
        assertTrue(ex instanceof IntegrationTimeoutException);
    }
}
