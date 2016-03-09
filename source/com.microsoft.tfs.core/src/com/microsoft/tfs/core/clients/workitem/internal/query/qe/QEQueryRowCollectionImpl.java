// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.query.qe.QEQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryGrouping;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRow;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRowCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;

public class QEQueryRowCollectionImpl implements QEQueryRowCollection {
    private final QEQueryImpl query;
    private final List<QEQueryRow> rows;
    private final QEQueryGroupingImp grouping;
    private final PropertyChangeListener rowPropertyChangeListener = new RowPropertyChangeListener();

    public QEQueryRowCollectionImpl(final QEQueryImpl query) {
        this.query = query;
        rows = new ArrayList<QEQueryRow>();
        grouping = new QEQueryGroupingImp(query);
    }

    @Override
    public QEQuery getQuery() {
        return query;
    }

    @Override
    public QEQueryRow addRow() {
        return addRow(rows.size());
    }

    @Override
    public QEQueryRow addRow(final int index) {
        final QEQueryRowImp row = new QEQueryRowImp();
        row.addPropertyChangeListener(rowPropertyChangeListener);
        rows.add(index, row);
        grouping.addRow(index);

        query.notifyModifiedListeners();
        return row;
    }

    @Override
    public void addNewRow(final int index) {
        /*
         * See:Microsoft.TeamFoundation.WorkItemTracking.Controls.FilterGrid.
         * InsertClause()
         * Microsoft.TeamFoundation.WorkItemTracking.Controls.FilterGrid
         * .AssureLogicalOperatorSet()
         */

        final QEQueryRow row = addRow(index);

        if (index == 0) {
            /*
             * If adding a new row at the top, ensure that the row has the empty
             * string for the logical operator. Also ensure that if there's a
             * second row, we fill in a default logical operator if the second
             * row has the empty string for the logical operator.
             *
             * The intent is to ensure that the first row always has the empty
             * string as the logical operator, and every other row has a logical
             * operator set.
             */
            row.setLogicalOperator(""); //$NON-NLS-1$
            if (getRowCount() > 1) {
                final QEQueryRow secondRow = getRow(1);
                if (secondRow.getLogicalOperator() == null || secondRow.getLogicalOperator().trim().length() == 0) {
                    secondRow.setLogicalOperator(WIQLOperators.getLocalizedOperator(WIQLOperators.AND));
                }
            }
        } else {
            row.setLogicalOperator(WIQLOperators.getLocalizedOperator(WIQLOperators.AND));
        }

        row.setOperator(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
    }

    @Override
    public void deleteRow(final QEQueryRow row) {
        final int index = rows.indexOf(row);

        if (index == -1) {
            return;
        }

        grouping.removeRow(index);

        rows.remove(index);

        ((QEQueryRowImp) row).removePropertyChangeListener(rowPropertyChangeListener);

        query.notifyModifiedListeners();
    }

    @Override
    public QEQueryGrouping getGrouping() {
        return grouping;
    }

    @Override
    public QEQueryRow getRow(final int index) {
        return rows.get(index);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public QEQueryRow[] getRows() {
        return rows.toArray(new QEQueryRow[rows.size()]);
    }

    @Override
    public int indexOf(final QEQueryRow row) {
        return rows.indexOf(row);
    }

    private class RowPropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            query.notifyModifiedListeners();
        }
    }
}
