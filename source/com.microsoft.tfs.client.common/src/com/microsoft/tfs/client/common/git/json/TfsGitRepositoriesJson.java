// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class TfsGitRepositoriesJson {
    private final List<TfsGitRepositoryJson> repositories;

    @JsonCreator
    public TfsGitRepositoriesJson(
        @JsonProperty("value") final List<TfsGitRepositoryJson> repositories,
        @JsonProperty("count") final int count) throws JsonProcessingException {
        this.repositories = repositories;
    }

    public List<TfsGitRepositoryJson> getRepositories() {
        return repositories;
    }
}
