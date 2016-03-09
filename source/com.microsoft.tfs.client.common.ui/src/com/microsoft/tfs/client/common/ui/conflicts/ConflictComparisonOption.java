// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.conflicts;

public class ConflictComparisonOption {
    private final String modifiedTitle;
    private final Object modifiedNode;

    private final String originalTitle;
    private final Object originalNode;

    public ConflictComparisonOption(
        final String modifiedTitle,
        final Object modifiedNode,
        final String originalTitle,
        final Object originalNode) {
        this.modifiedTitle = modifiedTitle;
        this.modifiedNode = modifiedNode;
        this.originalTitle = originalTitle;
        this.originalNode = originalNode;
    }

    public String getModifiedTitle() {
        return modifiedTitle;
    }

    public Object getModifiedNode() {
        return modifiedNode;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public Object getOriginalNode() {
        return originalNode;
    }
}
