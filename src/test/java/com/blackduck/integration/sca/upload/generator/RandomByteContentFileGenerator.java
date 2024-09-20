package com.blackduck.integration.sca.upload.generator;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomByteContentFileGenerator {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Random randomGenerator = new Random();

    public Optional<Path> generateFile(long generatedFileSize) throws IOException {
        return generateFile(generatedFileSize, ".tmp");
    }

    public Optional<Path> generateFile(long generatedFileSize, String fileExtension) throws IOException {
        Path result = null;
        if (generatedFileSize > 0) {
            Path generatedTempFilePath  = generatedTempFile(fileExtension);
            logger.info("====================================================");
            logger.info("Random Byte Content File Generation:");
            logger.info("  File size(bytes): {}", generatedFileSize);
            logger.info("  File path:        {}", generatedTempFilePath);
            try(RandomAccessFile generatedFile = new RandomAccessFile(generatedTempFilePath.toFile(),"rw");
                FileChannel fileChannel = generatedFile.getChannel()) {
                // set the capacity for the byte buffer
                int capacity = generatedFileSize < 1024 ? (int) generatedFileSize : 1024;
                ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
                int numberOfBytesWritten = 0;
                while(numberOfBytesWritten < generatedFileSize) {
                    // assign random bytes to the file.
                    randomGenerator.nextBytes(byteBuffer.array());
                    numberOfBytesWritten += fileChannel.write(byteBuffer);
                    byteBuffer.clear();
                }
                // truncate the file contents to the generated file length in case more bytes were written to the file.
                generatedFile.setLength(generatedFileSize);
                result = generatedTempFilePath;
            } finally {
                logger.info("====================================================");
            }
        }
        return Optional.ofNullable(result);
    }

    private Path generatedTempFile(String fileExtension) throws IOException {
        String fileName = String.format("generated_%s", UUID.randomUUID());
        Path generatedTempFilePath  = Files.createTempFile(fileName, fileExtension);
        // delete the file from the temp directory after the JVM shuts down.
        generatedTempFilePath.toFile().deleteOnExit();
        return generatedTempFilePath;
    }
}
