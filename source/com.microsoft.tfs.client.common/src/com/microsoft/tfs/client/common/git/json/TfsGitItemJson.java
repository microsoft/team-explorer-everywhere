// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TfsGitItemJson {

    private final String objectId;
    private final String gitObjectType;
    private final String commitId;
    private final String path;
    private final boolean isFolder;

    @JsonCreator
    public TfsGitItemJson(
        @JsonProperty("objectId") final String objectId,
        @JsonProperty("gitObjectType") final String gitObjectType,
        @JsonProperty("commitId") final String commitId,
        @JsonProperty("path") final String path,
        @JsonProperty("isFoloder") final String isFolderValue) {
        this.objectId = objectId;
        this.gitObjectType = gitObjectType;
        this.commitId = commitId;
        this.path = path;
        this.isFolder = Boolean.valueOf(isFolderValue);
    }

    public String getObjectId() {
        return objectId;
    }

    public String getGitObjectType() {
        return gitObjectType;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getPath() {
        return path;
    }

    public boolean isFolder() {
        return isFolder;
    }
}
