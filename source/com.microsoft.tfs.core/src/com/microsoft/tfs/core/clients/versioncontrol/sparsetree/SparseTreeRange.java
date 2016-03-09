// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

public class SparseTreeRange {
    private int start;
    private int end;

    public SparseTreeRange() {
    }

    public SparseTreeRange(final int start, final int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(final int value) {
        start = value;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(final int value) {
        end = value;
    }
}
