/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import com.blackduck.integration.sca.upload.file.model.MultipartUploadFileMetadata;
import com.blackduck.integration.sca.upload.file.model.MultipartUploadFilePart;

/**
 * Class use to split a file into parts for multipart upload.
 *
 * @see MultipartUploadFileMetadata
 * @see MultipartUploadFilePart
 */
public class FileSplitter {
    public static final String UPLOAD_CACHE = "/upload-cache";
    public static final int DIGEST_MAX_CHUNK_SIZE = 1024 * 1024 * 256;

    /**
     * Splits the file and creates the {@link MultipartUploadFileMetadata} needed to perform a multipart upload.
     *
     * @param uploadFilePath The file path of the file to upload.
     * @param chunkSize The byte size of each file part.
     * @return {@link MultipartUploadFileMetadata}
     * @throws IOException if the file does not exist in the given path.
     */
    public MultipartUploadFileMetadata splitFile(Path uploadFilePath, int chunkSize) throws IOException {
        if (!uploadFilePath.toFile().exists()) {
            throw new FileNotFoundException(String.format("Invalid file path, could not find file to split: %s", uploadFilePath.getFileName()));
        }
        String uploadedFileName = uploadFilePath.toFile().getName();
        long fileSize = Files.size(uploadFilePath);
        String checksum = toMD5Checksum(uploadFilePath);
        UUID uploadId = UUID.randomUUID();
        List<MultipartUploadFilePart> chunkList = createParts(uploadFilePath, chunkSize);
        return new MultipartUploadFileMetadata(uploadedFileName, checksum, uploadId, fileSize, chunkSize, chunkList);
    }

    private List<MultipartUploadFilePart> createParts(Path uploadFilePath, int chunkSize) throws IOException {
        List<MultipartUploadFilePart> partList = new LinkedList<>();
        int index = 0;
        long fileSize = Files.size(uploadFilePath);
        int numberOfChunks = (int) Math.ceil((double) fileSize / chunkSize);
        long startOffset = 0;
        while (index < numberOfChunks) {
            UUID tagId = UUID.randomUUID();
            String encodedChecksum = computePartMD5Checksum(uploadFilePath, startOffset, chunkSize);
            MultipartUploadFilePart multipartUploadFilePart;
            if(index == numberOfChunks - 1) {
                Long remainingSize = fileSize - startOffset;
                multipartUploadFilePart = new MultipartUploadFilePart(tagId, encodedChecksum, index, startOffset, remainingSize.intValue(), uploadFilePath);
            } else {
                multipartUploadFilePart = new MultipartUploadFilePart(tagId, encodedChecksum, index, startOffset, chunkSize, uploadFilePath);
            }
            partList.add(multipartUploadFilePart);
            startOffset += chunkSize;
            index++;
        }
        return partList;
    }

    private String computePartMD5Checksum(Path uploadFilePath, long startOffset, int chunkSize) throws IOException {
        String partChecksum = "";
        long position = startOffset;
        try (RandomAccessFile uploadFile = new RandomAccessFile(uploadFilePath.toFile(), "r")) {
            uploadFile.seek(startOffset);
            FileChannel fileChannel = uploadFile.getChannel();
            // if the chunk size is less than 256MB use it, otherwise use a chunk size that is smaller than 256MB.
            // This will allow the max chunk six of 2GB to be handled appropriately.
            // Compute the chunk size used for calculating the checksum.  Divide the chunksize by the maximum chunk size allowed to determine the quotient.
            // Then divide the chunksize by the quotient to ensure a byte buffer allocation will not cause bytes to be read beyond the endOffset.
            // This will ensure the correct checksum because bytes read will remain between the startOffset and the endOffset when computing the checksum.
            int digestChunkSize = chunkSize;
            if(chunkSize > DIGEST_MAX_CHUNK_SIZE) {
                int result = (int) Math.ceil((double) chunkSize / DIGEST_MAX_CHUNK_SIZE);
                digestChunkSize = chunkSize / result;
            }
            ByteBuffer buff = ByteBuffer.allocate(digestChunkSize);
            long endOffset = startOffset + chunkSize;
            int numberOfBytesRead = fileChannel.read(buff);
            long remainingBytes = endOffset - position;
            MessageDigest messageDigest = MessageDigest.getInstance("md5");
            while (numberOfBytesRead > -1 && remainingBytes > 0) {
                messageDigest.update(buff.array(), 0, numberOfBytesRead);
                buff.clear();
                // check if the end of the range has been read or not.
                position += numberOfBytesRead;
                remainingBytes = endOffset - position;
                numberOfBytesRead = fileChannel.read(buff);
            }
            partChecksum = Base64.getEncoder().encodeToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Cannot validate checksum of the part: ", e);
        }

        return partChecksum;
    }

    private String toMD5Checksum(Path uploadFilePath) throws IOException {
        try (InputStream is = Files.newInputStream(uploadFilePath)) {
            return Base64.getEncoder().encodeToString(DigestUtils.md5(is));
        }
    }
}
