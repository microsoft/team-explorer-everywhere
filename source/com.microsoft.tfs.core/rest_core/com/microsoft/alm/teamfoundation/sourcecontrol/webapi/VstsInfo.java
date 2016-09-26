// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.alm.teamfoundation.sourcecontrol.webapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.alm.teamfoundation.core.webapi.TeamProjectCollectionReference;
import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.GitRepository;

public class VstsInfo {
    private String serverUrl;
    private TeamProjectCollectionReference collection;
    private GitRepository repository;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @JsonProperty("collection")
    public TeamProjectCollectionReference getCollectionReference() {
        return collection;
    }

    @JsonProperty("collection")
    public void setCollectionReference(TeamProjectCollectionReference collection) {
        this.collection = collection;
    }

    @JsonProperty("repository")
    public GitRepository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(GitRepository repository) {
        this.repository = repository;
    }
}
