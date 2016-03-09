// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnection;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnectionType;

public class QEQueryConnectionImp implements QEQueryConnection {
    private final QEQueryConnectionType type;
    private final int startRow;
    private final int endRow;

    public QEQueryConnectionImp(final QEQueryConnectionType type) {
        this(type, -1, -1);
    }

    public QEQueryConnectionImp(final QEQueryConnectionType type, final int startRow, final int endRow) {
        this.type = type;
        this.startRow = startRow;
        this.endRow = endRow;
    }

    /*
     * ************************************************************************
     * START of implementation of QEQueryConnection interface
     * ***********************************************************************
     */

    @Override
    public int getEndRow() {
        return endRow;
    }

    @Override
    public int getStartRow() {
        return startRow;
    }

    @Override
    public QEQueryConnectionType getType() {
        return type;
    }

    /*
     * ************************************************************************
     * END of implementation of QEQueryConnection interface
     * ***********************************************************************
     */
}
