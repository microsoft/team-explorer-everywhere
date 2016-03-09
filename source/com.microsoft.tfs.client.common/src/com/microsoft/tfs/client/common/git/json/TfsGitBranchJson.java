// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TfsGitBranchJson {

    private final static String REFS_HEADS = "refs/heads/"; //$NON-NLS-1$

    private final String objectId;
    private final String fullName;

    @JsonCreator
    public TfsGitBranchJson(
        @JsonProperty("objectId") final String objectId,
        @JsonProperty("name") final String fullName) {
        this.objectId = objectId;
        this.fullName = fullName;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        if (fullName.startsWith(REFS_HEADS)) {
            return fullName.substring(REFS_HEADS.length());
        } else {
            return fullName;
        }
    }

    public String getFullName() {
        return fullName;
    }
}
