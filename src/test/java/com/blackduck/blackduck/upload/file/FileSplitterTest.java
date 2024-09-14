package com.blackduck.blackduck.upload.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.blackduck.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.blackduck.upload.file.model.MultipartUploadFilePart;

class FileSplitterTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path uploadFilePath = Path.of("src/test/resources/sample_file_100MB.txt");
    private final int chunkSize = 1024 * 1024 * 5; //5 MB
    private final String outputDirectory = "build/resources/test/output/";
    private Path uploadCacheDirectory;

    @BeforeEach
    void init() {
        uploadCacheDirectory = Path.of(outputDirectory + FileSplitter.UPLOAD_CACHE);
        if (!uploadCacheDirectory.toFile().exists()) {
            assertDoesNotThrow(() -> Files.createDirectories(uploadCacheDirectory));
        }
    }

    @AfterEach
    void cleanUploadCache() throws IOException {
        File directoryToClean = uploadCacheDirectory.toAbsolutePath().toFile();
        if (directoryToClean.getParentFile() == null) {
            logger.error("Cannot delete a root directory.");
            return;
        }
        if (directoryToClean.exists() && directoryToClean.isDirectory()) {
            FileUtils.deleteDirectory(directoryToClean);
        } else {
            logger.info("The upload-cache directory could not be found or does not exist.");
        }
    }

    @Test
    void splitFileTest() throws IOException {
        FileSplitter fileSplitter = new FileSplitter();

        MultipartUploadFileMetadata multipartUploadFileMetadata = fileSplitter.splitFile(uploadFilePath, chunkSize);
        //length returns the size of the file in bytes. 100 MB for this test case
        long size = uploadFilePath.toAbsolutePath().toFile().length();
        int expectedNumberOfChunks = (int) (size / chunkSize);

        assertEquals(uploadFilePath.getFileName().toString(), multipartUploadFileMetadata.getFileName());
        assertEquals(expectedNumberOfChunks, multipartUploadFileMetadata.getFileChunks().size());
        validateChunks(multipartUploadFileMetadata, chunkSize);
    }

    @Test
    void splitFileDoesNotExistTest() {
        FileSplitter fileSplitter = new FileSplitter();

        Path testPath = Path.of("./this/path/should/not/exist/");
        assertFalse(testPath.toFile().exists());
        assertThrows(FileNotFoundException.class, () -> fileSplitter.splitFile(testPath, chunkSize));
    }

    @Test
    void splitFileInvalidReadPermissions() throws IOException {
        FileSplitter fileSplitter = new FileSplitter();

        Path testPath = Path.of(outputDirectory, FileSplitter.UPLOAD_CACHE, "unit-testing").toAbsolutePath();
        try {
            if (!testPath.toFile().exists()) {
                Files.createDirectories(testPath);
            }
            File testFile = Path.of(testPath.toString(), "permission-test-target.txt").toAbsolutePath().toFile();
            try (FileOutputStream fileOutputStream = new FileOutputStream(testFile)) {
                fileOutputStream.write("test message".getBytes());
                testFile.setReadable(false);
            }

            assertTrue(testPath.toFile().exists());
            assertThrows(IOException.class, () -> fileSplitter.splitFile(testFile.toPath(), chunkSize));
        } finally {
            cleanUpDirectory(testPath);
        }
    }

    @Test
    void splitFileSuccessWithInvalidFileWritePermission() throws IOException {
        FileSplitter fileSplitter = new FileSplitter();

        Path testPath = Path.of(outputDirectory, FileSplitter.UPLOAD_CACHE, "unit-testing").toAbsolutePath();
        try {
            if (!testPath.toFile().exists()) {
                Files.createDirectories(testPath);
            }
            File testFile = Path.of(testPath.toString(), "permission-test-target.txt").toAbsolutePath().toFile();
            try (FileOutputStream fileOutputStream = new FileOutputStream(testFile)) {
                fileOutputStream.write("test message".getBytes());
                testFile.setWritable(false);
            }

            MultipartUploadFileMetadata multipartUploadFileMetadata = fileSplitter.splitFile(testFile.toPath(), chunkSize);

            assertTrue(testPath.toFile().exists());
            assertEquals(1, multipartUploadFileMetadata.getFileChunks().size());
            validateChunks(multipartUploadFileMetadata, chunkSize);
        } finally {
            cleanUpDirectory(testPath);
        }
    }

    private void validateChunks(MultipartUploadFileMetadata multipartUploadFileMetadata, int chunkSize) throws IOException {
        File reassembledFile = assertDoesNotThrow(() -> File.createTempFile(
            RandomStringUtils.randomAlphanumeric(10),
            ".txt",
            Path.of(outputDirectory, FileSplitter.UPLOAD_CACHE).toFile()
        ));
        try (FileOutputStream outputStream = new FileOutputStream(reassembledFile)) {
            for (MultipartUploadFilePart multipartUploadFilePart : multipartUploadFileMetadata.getFileChunks()) {
                long startingByteOffset = multipartUploadFilePart.getStartByteRange();
                try (RandomAccessFile uploadFile = new RandomAccessFile(multipartUploadFilePart.getFilePath().toFile(), "r")) {
                    byte[] buffer = new byte[chunkSize];
                    uploadFile.seek(startingByteOffset);
                    int bytesRead = uploadFile.read(buffer);
                    MessageDigest messageDigest = assertDoesNotThrow(() -> MessageDigest.getInstance("md5"));
                    messageDigest.update(buffer, 0, bytesRead);
                    byte[] md5DigestChecksum = messageDigest.digest();
                    String checksum = Base64.getEncoder().encodeToString(md5DigestChecksum);

                    assertEquals(multipartUploadFilePart.getChecksum(), checksum);

                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            try (InputStream is = Files.newInputStream(reassembledFile.toPath())) {
                assertEquals(multipartUploadFileMetadata.getChecksum(), Base64.getEncoder().encodeToString(DigestUtils.md5(is)));
            }
        }
    }

    private void cleanUpDirectory(Path directoryToClean) throws IOException {
        FileUtils.deleteDirectory(directoryToClean.toFile());
    }
}
