package com.synopsys.blackduck.upload.validation;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.exception.IntegrationException;

/**
 * Class containing helper methods to perform validation for uploader configurations and uploader I/O operations.
 */
public class UploadValidator {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // The minimum supported size of a file chunk is 5 MB.
    public static final int MINIMUM_UPLOAD_CHUNK_SIZE = 1024 * 1024 * 5;
    // The maximum supported size of a file chunk that can be uploaded is 2 GB.
    public static final int MAXIMUM_UPLOAD_CHUNK_SIZE = 1024 * 1024 * (1024 - 1) * 2;
    // The default chunk size, if no chunk size is provided, is 25 MB.
    public static final int DEFAULT_UPLOAD_CHUNK_SIZE = 1024 * 1024 * 25;
    // The default minimum file size for initiating a multipart upload is 5 GB.
    public static final long DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD = 1024L * 1024L * 1024L * 5L;
    // The maximum supported total file size for blackduck-upload-common is 100 GB.
    public static final long MAXIMUM_SUPPORTED_FILE_SIZE = 1024L * 1024L * 1024L * 100L;
    // The default number of retry attempts for uploading a part.
    public static final int DEFAULT_MULTIPART_UPLOAD_PART_RETRY_ATTEMPTS = 5;
    // The default initial interval to wait in between retry attempts for uploading a part.
    public static final long DEFAULT_MULTIPART_UPLOAD_PART_RETRY_INITIAL_INTERVAL = 1000L;
    // The default timeout value when performing part uploads.
    public static final int DEFAULT_MULTIPART_UPLOAD_TIMEOUT_MINUTES = 10;
    // The response status codes to perform a retry upload against.
    public static final Set<Integer> MULTIPART_UPLOAD_PART_RETRY_STATUS_CODES = new HashSet<>(
        Arrays.asList(
            HttpStatus.SC_GATEWAY_TIMEOUT,
            HttpStatus.SC_BAD_GATEWAY,
            HttpStatus.SC_SERVICE_UNAVAILABLE,
            HttpStatus.SC_REQUEST_TIMEOUT,
            HttpStatus.SC_TOO_MANY_REQUESTS
        )
    );

    private final UploadStateManager uploadStateManager;

    private final long multipartUploadThreshold;

    /**
     * Constructor for the upload validator.
     * @param uploadStateManager       Object maintaining state of errors as they occur.
     * @param multipartUploadThreshold The threshold (in bytes) for when a multipart upload should be invoked.
     */
    public UploadValidator(UploadStateManager uploadStateManager, long multipartUploadThreshold) {
        this.uploadStateManager = uploadStateManager;
        if (multipartUploadThreshold > DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD) {
            logger.warn(
                "The configured multipart upload threshold cannot be higher than the supported upload threshold. Defaulting to: " + DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD);
            this.multipartUploadThreshold = DEFAULT_MULTIPART_UPLOAD_FILE_SIZE_THRESHOLD;
        } else {
            this.multipartUploadThreshold = multipartUploadThreshold;
        }
    }

    /**
     * Determine if the target file Path is larger than the multipart threshold. If the file is smaller we return false to indicate file partitioning should not occur.
     * @param targetFilePath The {@link Path} to the file to be uploaded.
     * @return True if the target file is larger than the threshold, false otherwise.
     */
    public boolean isFileForPartitioning(Path targetFilePath) {
        long fileSize = targetFilePath.toFile().length();
        return fileSize >= multipartUploadThreshold;
    }

    /**
     * Retrieve the threshold at which a multipart splitting should occur.
     * @return the multipart threshold.
     */
    public long getMultipartUploadThreshold() {
        return multipartUploadThreshold;
    }

    /**
     * Retrieves a list of all errors occurring during validation.
     * @return {@link List} of errors that occurred during validation.
     */
    public List<UploadError> getUploadErrors() {
        return uploadStateManager.getUploadErrors();
    }

    /**
     * Validates the configuration of the uploader, ensuring file size and chunk size are both valid. If either are invalid an {@link IntegrationException} is thrown.
     * @param filePath  The {@link Path} to the file being validated.
     * @param chunkSize The size in bytes of chunks to be uploaded.
     * @throws IntegrationException containing exceptions that occurred during validations.
     */
    public void validateUploaderConfiguration(Path filePath, int chunkSize) throws IntegrationException {
        validateFileSize(filePath);
        validateChunkSize(chunkSize);
        if (uploadStateManager.hasErrors()) {
            throw uploadStateManager.createUploaderValidationException();
        }
    }

    private void validateFileSize(Path filePath) {
        if (filePath.toFile().length() > MAXIMUM_SUPPORTED_FILE_SIZE) {
            uploadStateManager.addError(ErrorCode.FILE_SIZE_ERROR, String.format("Target file %s cannot be scanned. Only files up to 100 GB are supported.", filePath));
        }
    }

    private void validateChunkSize(int chunkSize) {
        if (chunkSize < MINIMUM_UPLOAD_CHUNK_SIZE) {
            uploadStateManager.addError(ErrorCode.CHUNK_SIZE_ERROR, "File partition chunk size is set below the minimum 5 MB threshold.");
        }
    }

    /**
     * Validates the upload file to determine if the file exists, is not a directory, and has read permissions.
     * @param filePath The {@link Path} to the file being validated.
     * @throws UploaderValidationException containing exceptions that occurred during validations.
     */
    public void validateUploadFile(Path filePath) throws UploaderValidationException {
        validateFileExists(filePath);
        validateFileIsFile(filePath);
        validateFileHasReadPermissions(filePath);
        if (uploadStateManager.hasErrors()) {
            throw uploadStateManager.createUploaderValidationException();
        }
    }

    private void validateFileExists(Path filePath) throws UploaderValidationException {
        if (!filePath.toFile().exists()) {
            uploadStateManager.addErrorAndTerminate(ErrorCode.SOURCE_FILE_MISSING_ERROR, String.format("The target file does not exist: %s", filePath));
        }
    }

    private void validateFileIsFile(Path filePath) {
        if (!filePath.toFile().isFile()) {
            uploadStateManager.addError(ErrorCode.SOURCE_FILE_NOT_A_FILE_ERROR, String.format("The target is not a file: %s", filePath));
        }
    }

    private void validateFileHasReadPermissions(Path filePath) {
        if (!filePath.toFile().canRead()) {
            uploadStateManager.addError(ErrorCode.SOURCE_FILE_READ_PERMISSION_ERROR, String.format("The target scan file does not have read permission: %s", filePath));
        }
    }
}
