// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

/**
 * A collection of QEQueryRows. This interface exists mostly for testing
 * purposes. Real clients should use a QEQUery instance (QEQuery extends
 * QEQueryRowCollection).
 *
 * @since TEE-SDK-10.1
 */
public interface QEQueryRowCollection {
    public QEQuery getQuery();

    public QEQueryRow[] getRows();

    public int getRowCount();

    public int indexOf(QEQueryRow row);

    public QEQueryRow addRow();

    public QEQueryRow addRow(int index);

    public void addNewRow(int index);

    public void deleteRow(QEQueryRow row);

    public QEQueryRow getRow(int index);

    public QEQueryGrouping getGrouping();
}
