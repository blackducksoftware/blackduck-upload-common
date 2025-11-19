/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.file.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.blackduck.integration.sca.upload.file.FileSplitter;

/**
 * Class containing metadata associated with a multipart upload.
 *
 * @see MultipartUploadFilePart
 */
public class MultipartUploadFileMetadata {
    private final String fileName;
    private final String checksum;
    private UUID uploadId;
    private final long fileSize;
    private final int chunkSize;
    private MultipartUrlData initalUploadUrl = null;
    private MultipartUrlData completeUploadUrl = null;
    private MultipartUrlData abortUploadUrl = null;
    private String scassUploadId = null;
    private final List<MultipartUploadFilePart> fileChunks;

    /**
     * Constructor for the metadata object.
     *
     * @see FileSplitter
     * @param fileName The name of the file to upload.
     * @param checksum The MD5 checksum of the file to upload.
     * @param uploadId The {@link UUID} of the metadata object to use within Black Duck.
     * @param fileSize The size of the file to upload in bytes.
     * @param chunkSize The chunk size in bytes of the file parts.
     * @param fileChunks A list of {@link MultipartUploadFilePart} for the file parts.
     */
    public MultipartUploadFileMetadata(String fileName, String checksum, UUID uploadId, long fileSize, int chunkSize, List<MultipartUploadFilePart> fileChunks) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.uploadId = uploadId;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.fileChunks = fileChunks;
    }

    public MultipartUploadFileMetadata(String fileName, String scassUploadId, String checksum, long fileSize, int chunkSize, List<MultipartUploadFilePart> fileChunks, MultipartUrlData initalUploadUrl, MultipartUrlData completeUploadUrl, MultipartUrlData abortUploadUrl) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.scassUploadId = scassUploadId;
        this.chunkSize = chunkSize;
        this.fileChunks = fileChunks;
        this.initalUploadUrl = initalUploadUrl;
        this.completeUploadUrl = completeUploadUrl;
        this.abortUploadUrl = abortUploadUrl;
    }

    /**
     * Retrieve the name of the file.
     *
     * @return file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Retrieve the MD5/CRC32C checksum of the file.
     *
     * @return checksum.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Retrieve the id of the metadata object.
     *
     * @return {@link UUID} id.
     */
    public UUID getUploadId() {
        return uploadId;
    }

    /**
     * Retrieve the size of the file in bytes.
     *
     * @return file size.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Retrieve the chunk size in bytes of the file parts.
     *
     * @return chunk size.
     */
    public int getChunkSize() {
        return chunkSize;
    }

    public MultipartUrlData getInitalUploadUrl() {
        return initalUploadUrl;
    }

    public MultipartUrlData getCompleteUploadUrl() {
        return completeUploadUrl;
    }

    public MultipartUrlData getAbortUploadUrl() {
        return abortUploadUrl;
    }

    public String getScassUploadId() {
        return scassUploadId;
    }

    /**
     * Retrieve a list of file parts for the file.
     *
     * @return {@link List} of {@link MultipartUploadFilePart}.
     */
    public List<MultipartUploadFilePart> getFileChunks() {
        return fileChunks;
    }

    @Override
    public String toString() {
        return "MultipartUploadFileMetadata{" +
            "fileName='" + fileName + '\'' +
            ", checksum='" + checksum + '\'' +
            ", uploadId='" + uploadId.toString() + '\'' +
            ", fileSize=" + fileSize +
            ", chunkSize=" + chunkSize +
            ", fileChunks=" + fileChunks.size() +
            '}';
    }
}