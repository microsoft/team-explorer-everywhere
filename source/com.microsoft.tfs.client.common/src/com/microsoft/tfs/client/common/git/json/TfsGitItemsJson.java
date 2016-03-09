// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class TfsGitItemsJson {
    private final List<TfsGitItemJson> items;

    @JsonCreator
    public TfsGitItemsJson(
        @JsonProperty("value") final List<TfsGitItemJson> items,
        @JsonProperty("count") final int count) throws JsonProcessingException {
        this.items = items;
    }

    public List<TfsGitItemJson> getItems() {
        return items;
    }
}
