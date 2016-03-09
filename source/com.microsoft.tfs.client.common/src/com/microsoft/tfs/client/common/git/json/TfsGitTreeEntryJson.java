// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TfsGitTreeEntryJson {

    private final String objectId;
    private final String relativePath;
    private final int mode;
    private final String gitObjectType;
    private final long size;

    @JsonCreator
    public TfsGitTreeEntryJson(
        @JsonProperty("objectId") final String objectId,
        @JsonProperty("relativePath") final String relativePath,
        @JsonProperty("mode") final String modeValue,
        @JsonProperty("gitObjectType") final String gitObjectType,
        @JsonProperty("size") final String sizeValue) {
        this.objectId = objectId;
        this.relativePath = relativePath;
        this.gitObjectType = gitObjectType;
        this.mode = Integer.parseInt(modeValue, 8);
        this.size = Long.parseLong(sizeValue);
    }

    public String getObjectId() {
        return objectId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public int getMode() {
        return mode;
    }

    public String getGitObjectType() {
        return gitObjectType;
    }

    public long getSize() {
        return size;
    }
}
