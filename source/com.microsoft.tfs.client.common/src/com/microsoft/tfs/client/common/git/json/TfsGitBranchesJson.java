// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class TfsGitBranchesJson {
    private final List<TfsGitBranchJson> branches;

    @JsonCreator
    public TfsGitBranchesJson(
        @JsonProperty("value") final List<TfsGitBranchJson> branches,
        @JsonProperty("count") final int count) throws JsonProcessingException {
        this.branches = branches;
    }

    public List<TfsGitBranchJson> getBranches() {
        return branches;
    }
}
