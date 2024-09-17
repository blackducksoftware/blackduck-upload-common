/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.blackduck.upload.file.model;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Class containing data associated with a multipart upload file part.
 *
 * @see MultipartUploadFileMetadata
 */
public class MultipartUploadFilePart {
    private final UUID tagId;
    private final String checksum;
    private final int index;
    private final long startByteRange;
    private final int chunkSize;
    private final Path filePath;

    /**
     * Constructor for the file part object.
     *
     * @param tagId The tag {@link UUID} associated with the file part.
     * @param checksum The MD5 checksum of the file part.
     * @param index The position of the file part relative to all parts of the file.
     * @param startByteRange The byte index in which this file part begins relative to the whole file byte size.
     * @param chunkSize The byte size of the file part.
     * @param filePath The path on this system where the file is stored.
     */
    public MultipartUploadFilePart(UUID tagId, String checksum, int index, long startByteRange, int chunkSize, Path filePath) {
        this.tagId = tagId;
        this.checksum = checksum;
        this.index = index;
        this.startByteRange = startByteRange;
        this.chunkSize = chunkSize;
        this.filePath = filePath;
    }

    /**
     * Retrieve the tag id of the file part.
     *
     * @return {@link UUID}.
     */
    public UUID getTagId() {
        return tagId;
    }

    /**
     * Retrieve MD5 checksum of the file part
     *
     * @return checksum.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Retrieve the index of the file part
     *
     * @return index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Retrieve the starting byte range of this file part.
     *
     * @return start byte range.
     */
    public long getStartByteRange() {
        return startByteRange;
    }

    /**
     * Retrieve the byte chunk size of this file part.
     *
     * @return chunk size.
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Retrieve the path to the file.
     *
     * @return file path.
     */
    public Path getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "MultipartUploadFilePart{" +
            "tagId=" + tagId +
            ", checksum='" + checksum + '\'' +
            ", index=" + index +
            ", startByteRange=" + startByteRange +
            ", chunkSize=" + chunkSize +
            ", filePath=" + filePath +
            '}';
    }
}

