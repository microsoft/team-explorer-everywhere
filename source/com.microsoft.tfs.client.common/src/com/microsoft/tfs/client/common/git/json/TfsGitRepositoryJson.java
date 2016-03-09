// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TfsGitRepositoryJson {
    private final String id;
    private final String name;
    private final TfsGitTeamProjectJson project;
    private final String defaultBranch;
    private final String remoteUrl;

    @JsonCreator
    public TfsGitRepositoryJson(
        @JsonProperty("id") final String id,
        @JsonProperty("name") final String name,
        @JsonProperty("project") final TfsGitTeamProjectJson project,
        @JsonProperty("defaultBranch") final String defaultBranch,
        @JsonProperty("remoteUrl") final String remoteUrl) {
        this.id = id;
        this.name = name;
        this.project = project;
        this.defaultBranch = defaultBranch;
        this.remoteUrl = remoteUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TfsGitTeamProjectJson getTeamProject() {
        return project;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }
}
