/*
 * blackduck-upload-common
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance
 * Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.sca.upload.client.uploaders;

import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.sca.upload.client.UploaderConfig;
import com.blackduck.integration.sca.upload.client.model.BinaryScanRequestData;
import com.blackduck.integration.sca.upload.file.BlackDuckFileUploader;
import com.blackduck.integration.sca.upload.file.UploadRequestPaths;
import com.blackduck.integration.sca.upload.file.XmlApiFileUploader;
import com.blackduck.integration.sca.upload.rest.BlackDuckHttpClient;
import com.blackduck.integration.sca.upload.validation.UploadStateManager;
import com.blackduck.integration.sca.upload.validation.UploadValidator;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Factory class to create needed uploader.
 * @see UploaderConfig
 * @see ArtifactsUploader
 * @see BdbaUploader
 * @see BinaryUploader
 * @see ContainerUploader
 * @see ReversingLabUploader
 * @see ScassUploader
 * @see ToolsUploader
 */
public class UploaderFactory {
    private final UploaderConfig uploaderConfig;

    private final IntLogger intLogger;

    private final Gson gson;

    /**
     * Constructor for creating a specified uploader.
     * @param uploaderConfig The configuration needed for multipart uploads.
     * @param intLogger      The {@link IntLogger} to log messages from the HTTP requests.
     * @param gson           The object to serialize/deserialize data to and from JSON.
     */
    public UploaderFactory(UploaderConfig uploaderConfig, IntLogger intLogger, Gson gson) {
        this.uploaderConfig = uploaderConfig;
        this.intLogger = intLogger;
        this.gson = gson;
    }

    // TODO: Make public along with uncommenting test when ready
    private ArtifactsUploader createArtifactsUploader(String urlPrefix) {
        return new ArtifactsUploader(uploaderConfig.getUploadChunkSize(), createBlackDuckFileUploader(urlPrefix), createUploadValidator());
    }

    /**
     * Construct the uploader for BDBA uploads.
     * @param urlPrefix Used to create {@link UploadRequestPaths}.
     * @return the {@link BdbaUploader} created.
     */
    public BdbaUploader createBdbaUploader(String urlPrefix) {
        return new BdbaUploader(uploaderConfig.getUploadChunkSize(), createBlackDuckFileUploader(urlPrefix), createUploadValidator());
    }

    /**
     * Construct the uploader for Binary uploads.
     * @param urlPrefix             Used to create {@link UploadRequestPaths}.
     * @param binaryScanRequestData Object needed to initiate a binary scan request.
     * @return the {@link BinaryUploader} created.
     */
    public BinaryUploader createBinaryUploader(String urlPrefix, BinaryScanRequestData binaryScanRequestData) {
        return new BinaryUploader(uploaderConfig.getUploadChunkSize(), createBlackDuckFileUploader(urlPrefix), createUploadValidator(), binaryScanRequestData);
    }

    /**
     * Construct the uploader for Container uploads.
     * @param urlPrefix Used to create {@link UploadRequestPaths}.
     * @return the {@link ContainerUploader} created.
     */
    public ContainerUploader createContainerUploader(String urlPrefix) {
        return new ContainerUploader(uploaderConfig.getUploadChunkSize(), createBlackDuckFileUploader(urlPrefix), createUploadValidator());
    }

    /**
     * Construct the uploader for SCASS uploads.
     * @return the {@link ScassUploader} created.
     */
    public ScassUploader createScassUploader() {
        return new ScassUploader(createScassHttpClient(), createUploadValidator(),
            uploaderConfig.getUploadChunkSize(),
            uploaderConfig.getMultipartUploadPartRetryInitialInterval(),
            uploaderConfig.getMultipartUploadPartRetryAttempts()
        );
    }

    // TODO: Make public along with uncommenting test when ready
    private ReversingLabUploader createReversingLabUploader(String urlPrefix) {
        return new ReversingLabUploader(uploaderConfig.getUploadChunkSize(), createBlackDuckFileUploader(urlPrefix), createUploadValidator());
    }

    // TODO: Make public along with uncommenting test when ready
    private ToolsUploader createToolsUploader(String urlPrefix) {
        return new ToolsUploader(uploaderConfig.getUploadChunkSize(), createBlackDuckFileUploader(urlPrefix), createUploadValidator());
    }

    public ScassMultipartUploader createScassMultipartUploader() {
        return new ScassMultipartUploader(uploaderConfig.getUploadChunkSize(), createXmlApiFileUploader(), createUploadValidator());
    }

    private BlackDuckFileUploader createBlackDuckFileUploader(String urlPrefix) {
        // Black Duck SCA can only support single threaded executor right now.  Use a single threaded executor.
        return new BlackDuckFileUploader(createBlackduckHttpClient(),
                uploaderConfig.getMultipartUploadPartRetryAttempts(),
                uploaderConfig.getMultipartUploadPartRetryInitialInterval(),
                uploaderConfig.getMultipartUploadTimeoutInMinutes(),
                Executors.newSingleThreadExecutor(),
                createUploadRequestPaths(urlPrefix));
    }

    private XmlApiFileUploader createXmlApiFileUploader() {
        return new XmlApiFileUploader(createHttpClient(),
                uploaderConfig.getMultipartUploadPartRetryAttempts(),
                uploaderConfig.getMultipartUploadPartRetryInitialInterval(),
                uploaderConfig.getMultipartUploadTimeoutInMinutes(),
                uploaderConfig.getExecutorService());
    }

    private IntHttpClient createHttpClient() {
        return new IntHttpClient(
                intLogger,
                gson,
                uploaderConfig.getBlackDuckTimeoutInSeconds(),
                uploaderConfig.isAlwaysTrustServerCertificate(),
                uploaderConfig.getProxyInfo()
        );
    }

    private BlackDuckHttpClient createBlackduckHttpClient() {
        return new BlackDuckHttpClient(
            intLogger,
            gson,
            uploaderConfig.getBlackDuckTimeoutInSeconds(),
            uploaderConfig.isAlwaysTrustServerCertificate(),
            uploaderConfig.getProxyInfo(),
            uploaderConfig.getBlackDuckUrl(),
            uploaderConfig.getApiToken()
        );
    }

    private IntHttpClient createScassHttpClient() {
        return new IntHttpClient(
            intLogger,
            gson,
            uploaderConfig.getBlackDuckTimeoutInSeconds(),
            uploaderConfig.isAlwaysTrustServerCertificate(),
            ProxyInfo.NO_PROXY_INFO
        );
    }

    private UploadRequestPaths createUploadRequestPaths(String urlPrefix) {
        return new UploadRequestPaths(urlPrefix);
    }

    private UploadValidator createUploadValidator() {
        return new UploadValidator(new UploadStateManager(), uploaderConfig.getMultipartUploadThreshold());
    }

}
