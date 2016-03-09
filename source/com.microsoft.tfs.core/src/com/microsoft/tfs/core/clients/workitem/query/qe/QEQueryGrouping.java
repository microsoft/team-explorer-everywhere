// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

/**
 * @since TEE-SDK-10.1
 */
public interface QEQueryGrouping {
    public void addGrouping(int row1, int row2);

    public boolean canGroup(int row1, int row2);

    public boolean canUngroup(int row1, int row2);

    public QEQueryConnection getConnection(int depth, int row);

    public boolean hasGroup(int row1, int row2);

    public boolean hasGroupings();

    public int getMaxDepth();

    public boolean removeGrouping(int row1, int row2);

    public boolean rowInGroup(int row);

    public boolean rowIsGrouped(int row);
}
