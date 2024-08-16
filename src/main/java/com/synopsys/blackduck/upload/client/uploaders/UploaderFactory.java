package com.synopsys.blackduck.upload.client.uploaders;

import com.google.gson.Gson;
import com.synopsys.blackduck.upload.client.UploaderConfig;
import com.synopsys.blackduck.upload.client.model.BinaryScanRequestData;
import com.synopsys.blackduck.upload.file.FileUploader;
import com.synopsys.blackduck.upload.file.UploadRequestPaths;
import com.synopsys.blackduck.upload.rest.BlackDuckHttpClient;
import com.synopsys.blackduck.upload.validation.UploadStateManager;
import com.synopsys.blackduck.upload.validation.UploadValidator;
import com.synopsys.integration.log.IntLogger;

/**
 * Factory class to create needed uploader.
 *
 * @see UploaderConfig
 * @see ArtifactsUploader
 * @see BinaryUploader
 * @see ContainerUploader
 * @see ReversingLabUploader
 * @see ToolsUploader
 */
public class UploaderFactory {
    private final UploaderConfig uploaderConfig;
    private final IntLogger intLogger;
    private final Gson gson;

    /**
     * Constructor for creating a specified uploader.
     *
     * @param uploaderConfig The configuration needed for multipart uploads.
     * @param intLogger The {@link IntLogger} to log messages from the HTTP requests.
     * @param gson The object to serialize/deserialize data to and from JSON.
     */
    public UploaderFactory(UploaderConfig uploaderConfig, IntLogger intLogger, Gson gson) {
        this.uploaderConfig = uploaderConfig;
        this.intLogger = intLogger;
        this.gson = gson;
    }

    // TODO: Make public along with uncommenting test when ready
    private ArtifactsUploader createArtifactsUploader(String urlPrefix) {
        return new ArtifactsUploader(uploaderConfig.getUploadChunkSize(), createFileUploader(urlPrefix), createUploadValidator());
    }

    /**
     * Construct the uploader for Binary uploads.
     *
     * @param urlPrefix Used to create {@link UploadRequestPaths}.
     * @param binaryScanRequestData Object needed to initiate a binary scan request.
     * @return the {@link BinaryUploader} created.
     */
    public BinaryUploader createBinaryUploader(String urlPrefix, BinaryScanRequestData binaryScanRequestData) {
        return new BinaryUploader(uploaderConfig.getUploadChunkSize(), createFileUploader(urlPrefix), createUploadValidator(), binaryScanRequestData);
    }

    // TODO: Make public along with uncommenting test when ready
    private ContainerUploader createContainerUploader(String urlPrefix) {
        return new ContainerUploader(uploaderConfig.getUploadChunkSize(), createFileUploader(urlPrefix), createUploadValidator());
    }

    // TODO: Make public along with uncommenting test when ready
    private ReversingLabUploader createReversingLabUploader(String urlPrefix) {
        return new ReversingLabUploader(uploaderConfig.getUploadChunkSize(), createFileUploader(urlPrefix), createUploadValidator());
    }

    // TODO: Make public along with uncommenting test when ready
    private ToolsUploader createToolsUploader(String urlPrefix) {
        return new ToolsUploader(uploaderConfig.getUploadChunkSize(), createFileUploader(urlPrefix), createUploadValidator());
    }

    private FileUploader createFileUploader(String urlPrefix) {
        return new FileUploader(
            createHttpClient(),
            createUploadRequestPaths(urlPrefix),
            uploaderConfig.getMultipartUploadPartRetryAttempts(),
            uploaderConfig.getMultipartUploadPartRetryInitialInterval(),
            uploaderConfig.getMultipartUploadTimeoutInMinutes()
        );
    }

    private BlackDuckHttpClient createHttpClient() {
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

    private UploadRequestPaths createUploadRequestPaths(String urlPrefix) {
        return new UploadRequestPaths(urlPrefix);
    }

    private UploadValidator createUploadValidator() {
        return new UploadValidator(new UploadStateManager(), uploaderConfig.getMultipartUploadThreshold());
    }

}
