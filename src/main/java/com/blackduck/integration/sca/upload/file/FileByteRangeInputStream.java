/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.jetbrains.annotations.NotNull;

/**
 * Class that reads the {@link RandomAccessFile} upload file into an {@link InputStream}.
 */
public class FileByteRangeInputStream extends InputStream {
    private final RandomAccessFile randomAccessFile;
    private final long startOffset;
    private final long endOffset;
    private long position;

    /**
     * Constructor for the {@link InputStream}.
     *
     * @param randomAccessFile The file to upload as a {@link RandomAccessFile}.
     * @param startOffset The starting byte offset for which to read the upload file at.
     * @param contentLength The length in bytes of the file part.
     * @throws IOException If an I/O error occurs reading the upload file.
     */
    public FileByteRangeInputStream(RandomAccessFile randomAccessFile, long startOffset, long contentLength) throws IOException {
        this.randomAccessFile = randomAccessFile;
        this.startOffset = startOffset;
        this.endOffset = startOffset + contentLength;
        this.position = startOffset;
        randomAccessFile.seek(startOffset);
    }

    @Override
    public int read() throws IOException {
        // check if still within range to keep reading the contents.
        if(randomAccessFile.getFilePointer() < endOffset) {
            int bytesRead = randomAccessFile.read();
            if(bytesRead != -1) {
                position++;
            }
            return bytesRead;
        } else {
            return -1;
        }
    }

    @Override
    public int read(@NotNull final byte[] b, final int off, final int len) throws IOException {
        long remainingBytes = endOffset - position;
        // check if the end of the range has been read or not.
        if(remainingBytes <= 0) {
            return -1;
        }
        Long bytesToReadCount = Math.min(len, remainingBytes);
        int bytesRead = randomAccessFile.read(b, off, bytesToReadCount.intValue());

        // increment the current position by the number of bytes read.
        if(bytesRead > 0) {
            position += bytesRead;
        }

        return bytesRead;
    }

    @Override
    public synchronized void reset() throws IOException {
        this.position = startOffset;
        randomAccessFile.seek(startOffset);
    }

    @Override
    public long skip(long n) throws IOException {
        position += n;
        randomAccessFile.seek(position);
        return position;
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }
}
