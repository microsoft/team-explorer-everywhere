// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.conflicts;

import com.microsoft.tfs.util.Check;

public class ConflictComparisonDescription {
    private final String pathDescription;
    private final String pathAndVersionLink;
    private final String compareActionDescription;
    private final Object modifiedNode;
    private final Object originalNode;

    public ConflictComparisonDescription(
        final String pathDescription,
        final String pathAndVersionLink,
        final String compareActionDescription,
        final Object modifiedNode,
        final Object originalNode) {
        Check.notNull(pathDescription, "pathDescription"); //$NON-NLS-1$
        Check.notNull(pathAndVersionLink, "pathAndVersionLink"); //$NON-NLS-1$
        Check.notNull(modifiedNode, "modifiedNode"); //$NON-NLS-1$
        Check.notNull(originalNode, "originalNode"); //$NON-NLS-1$

        this.pathDescription = pathDescription;
        this.pathAndVersionLink = pathAndVersionLink;
        this.compareActionDescription = compareActionDescription;
        this.modifiedNode = modifiedNode;
        this.originalNode = originalNode;
    }

    public String getPathDescription() {
        return pathDescription;
    }

    public String getPathAndVersionLink() {
        return pathAndVersionLink;
    }

    public String getCompareActionDescription() {
        return compareActionDescription;
    }

    public Object getModifiedNode() {
        return modifiedNode;
    }

    public Object getOriginalNode() {
        return originalNode;
    }
}
