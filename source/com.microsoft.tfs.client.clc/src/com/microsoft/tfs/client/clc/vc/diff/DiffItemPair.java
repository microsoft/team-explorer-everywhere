// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff;

public class DiffItemPair {
    private final DiffItem sourceItem;
    private final DiffItem targetItem;

    public DiffItemPair(final DiffItem sourceItem, final DiffItem targetItem) {
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
    }

    public DiffItem getSourceItem() {
        return sourceItem;
    }

    public DiffItem getTargetItem() {
        return targetItem;
    }
}
