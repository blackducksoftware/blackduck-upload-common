package com.synopsys.blackduck.upload.client.model;

import java.io.Serializable;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

/**
 * Data object needed to initiate a binary scan request.
 */
public class BinaryScanRequestData implements Serializable {
    private static final long serialVersionUID = 3828560274385707393L;
    private final String projectName;
    private final String version;
    private final String codeLocationName;
    private final String codeLocationUri;

    /**
     * Constructor for the binary scan request data.
     *
     * @param projectName The name of the Black Duck project.
     * @param version The version of the Black Duck project.
     * @param codeLocationName The Black Duck code location name.
     * @param codeLocationUri The Black Duck code location URI.
     */
    public BinaryScanRequestData(
        String projectName,
        String version,
        @Nullable String codeLocationName,
        @Nullable String codeLocationUri
    ) {
        this.projectName = projectName;
        this.version = version;
        this.codeLocationName = codeLocationName;
        this.codeLocationUri = codeLocationUri;
    }

    /**
     * Retrieve the name of the Black Duck Project.
     *
     * @return project name.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Retrieve the version of the Black Duck Project.
     *
     * @return version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Retrieve Black Duck code location name.
     *
     * @return {@link Optional} code location name.
     */
    public Optional<String> getCodeLocationName() {
        return Optional.ofNullable(codeLocationName);
    }

    /**
     * Retrieve Black Duck code location URI.
     *
     * @return {@link Optional} code location URI.
     */
    public Optional<String> getCodeLocationUri() {
        return Optional.ofNullable(codeLocationUri);
    }

}
