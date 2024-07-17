package com.synopsys.blackduck.upload.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;

class UploadValidatorTest {
    private static final Long TEST_MULTIPART_UPLOAD_THRESHOLD = 1024 * 1024 * 5L; //5 MB
    private final Path uploadFilePath = Path.of("src/test/resources/sample_file_100MB.txt");
    private final String outputDirectory = "build/resources/test/output/";

    private UploadValidator uploadValidator;

    @BeforeEach
    void init() {
        if (!Path.of(outputDirectory).toAbsolutePath().toFile().exists()) {
            assertDoesNotThrow(() -> Files.createDirectories(Path.of(outputDirectory)));
        }
        uploadValidator = new UploadValidator(new UploadStateManager(), TEST_MULTIPART_UPLOAD_THRESHOLD);
    }

    @AfterEach
    void cleanup() {
        assertDoesNotThrow(() -> cleanUpDirectory(Path.of(outputDirectory)), String.format("An error has occurred while cleaning up the output directory: %s", outputDirectory));
    }

    @Test
    void isFileForPartitioningTest() {
        Path partitionPath = Mockito.mock(Path.class);
        File pathFile = Mockito.mock(File.class);
        Mockito.when(partitionPath.toFile()).thenReturn(pathFile);
        Mockito.when(pathFile.length()).thenReturn(UploadValidator.DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD + 1L); // 5GB file
        assertTrue(uploadValidator.isFileForPartitioning(partitionPath));

        Path smallFilePath = Mockito.mock(Path.class);
        File smallFile = Mockito.mock(File.class);
        Mockito.when(smallFilePath.toFile()).thenReturn(smallFile);
        Mockito.when(smallFile.length()).thenReturn(1024L * 1024L); // 1 MB file
        assertFalse(uploadValidator.isFileForPartitioning(smallFilePath));
    }

    @Test
    void smallMultipartUploadThresholdTest() {
        long smallMultipartUploadThreshold = 1024 * 1024 * 5L; // 5 MB
        UploadValidator uploadValidatorTest = new UploadValidator(new UploadStateManager(), smallMultipartUploadThreshold);
        assertEquals(smallMultipartUploadThreshold, uploadValidatorTest.getMultipartUploadThreshold());
    }

    @Test
    void largeMultipartUploadThresholdTest() {
        long smallMultipartUploadThreshold = 1024 * 1024 * 1024 * 10L; // 10 GB
        UploadValidator uploadValidatorTest = new UploadValidator(new UploadStateManager(), smallMultipartUploadThreshold);
        assertEquals(UploadValidator.DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD, uploadValidatorTest.getMultipartUploadThreshold());
    }

    @Test
    void validateUploaderConfigurationTest() throws IntegrationException {
        uploadValidator.validateUploaderConfiguration(uploadFilePath, UploadValidator.MINIMUM_UPLOAD_CHUNK_SIZE);
        assertEquals(0, uploadValidator.getUploadErrors().size());
    }

    @Test
    void validateUploadConfigurationTestFileSizeTooLargeTest() {
        Path largeFilePath = Mockito.mock(Path.class);
        File largeFile = Mockito.mock(File.class);
        Mockito.when(largeFilePath.toFile()).thenReturn(largeFile);
        Mockito.when(largeFile.length()).thenReturn(UploadValidator.MAXIMUM_SUPPORTED_FILE_SIZE + 1L); // Slightly over 100 GB file

        assertThrows(UploaderValidationException.class, () -> uploadValidator.validateUploaderConfiguration(largeFilePath, UploadValidator.MINIMUM_UPLOAD_CHUNK_SIZE));
        List<UploadError> errors = uploadValidator.getUploadErrors();
        assertEquals(1, errors.size());
        assertEquals(ErrorCode.FILE_SIZE_ERROR, errors.stream()
            .findFirst().orElseThrow(() -> new AssertionError("Failed to find error code")).getErrorCode());
    }

    @Test
    void validateUploadConfigurationMultipleFailuresTest() {
        Path largeFilePath = Mockito.mock(Path.class);
        File largeFile = Mockito.mock(File.class);
        Mockito.when(largeFilePath.toFile()).thenReturn(largeFile);
        Mockito.when(largeFile.length()).thenReturn(UploadValidator.MAXIMUM_SUPPORTED_FILE_SIZE + 1L); // Slightly over 100 GB file

        assertThrows(UploaderValidationException.class, () -> uploadValidator.validateUploaderConfiguration(largeFilePath, 1024));
        assertErrorCodes(List.of(ErrorCode.FILE_SIZE_ERROR, ErrorCode.CHUNK_SIZE_ERROR));
    }

    @Test
    void validateUploadFileTest() {
        assertDoesNotThrow(() -> uploadValidator.validateUploadFile(uploadFilePath));
        assertEquals(0, uploadValidator.getUploadErrors().size());
    }

    @Test
    void validateUploadFileNotExistsTest() {
        assertThrows(UploaderValidationException.class, () -> uploadValidator.validateUploadFile(Path.of("/this/path/does/not/exist")));
        assertErrorCodes(List.of(ErrorCode.SOURCE_FILE_MISSING_ERROR));
    }

    @Test
    void validateFileIsFileIsDirectoryTest() {
        assertThrows(UploaderValidationException.class, () -> uploadValidator.validateUploadFile(Path.of(outputDirectory)));
        assertErrorCodes(List.of(ErrorCode.SOURCE_FILE_NOT_A_FILE_ERROR));
    }

    @Test
    void validateUploadFileMissingReadPermissions() throws IOException {
        Path testParentDirectory = Path.of(outputDirectory, "unit-testing").toAbsolutePath();
        try {
            if (!testParentDirectory.toFile().exists()) {
                assertDoesNotThrow(() -> Files.createDirectories(testParentDirectory));
            }
            File testFile = assertDoesNotThrow(() -> File.createTempFile(RandomStringUtils.randomAlphanumeric(10), ".txt", testParentDirectory.toFile()));
            assertDoesNotThrow(testFile::createNewFile);
            assertTrue(testFile.setReadable(false));
            assertFalse(testFile.canRead());
            testFile.deleteOnExit();

            assertThrows(UploaderValidationException.class, () -> uploadValidator.validateUploadFile(testFile.toPath()));
            assertErrorCodes(List.of(ErrorCode.SOURCE_FILE_READ_PERMISSION_ERROR));
        } finally {
            cleanUpDirectory(testParentDirectory);
        }

    }

    private void assertErrorCodes(List<ErrorCode> expectedErrorCodes) {
        List<UploadError> errors = uploadValidator.getUploadErrors();
        assertEquals(expectedErrorCodes.size(), errors.size());
        assertTrue(errors
            .stream()
            .map(UploadError::getErrorCode)
            .collect(Collectors.toList())
            .containsAll(expectedErrorCodes));
    }

    private void cleanUpDirectory(Path directoryToClean) throws IOException {
        FileUtils.deleteDirectory(directoryToClean.toFile());
    }
}
