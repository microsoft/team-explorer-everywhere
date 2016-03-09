// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnection;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnectionType;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryGrouping;

/**
 * Very similar to:
 * Microsoft.TeamFoundation.WorkItemTracking.Controls.FilterGrouping
 *
 */
public class QEQueryGroupingImp implements QEQueryGrouping {
    private final QEQueryImpl query;
    private final List<GroupNode> list = new ArrayList<GroupNode>();

    public QEQueryGroupingImp(final QEQueryImpl query) {
        this.query = query;
    }

    /*
     * ************************************************************************
     * START of implementation of QEQueryGrouping interface
     * ***********************************************************************
     */

    @Override
    public void addGrouping(final int row1, final int row2) {
        if ((row1 >= row2) || (row1 < 0)) {
            throw new IllegalArgumentException(MessageFormat.format(
                "the args row1={0} and row2={1} are invalid", //$NON-NLS-1$
                Integer.toString(row1),
                Integer.toString(row2)));
        }
        for (int i = 0; i < list.size(); i++) {
            if ((getItem(i).getRow1() == row1) && (getItem(i).getRow2() == row2)) {
                return;
            }
            if (getItem(i).getRow2() == row1) {
                throw new RuntimeException("cannot group"); //$NON-NLS-1$
            }
            if (getItem(i).getRow1() == row2) {
                throw new RuntimeException("cannot group"); //$NON-NLS-1$
            }

            /*
             * probably a check for overlapping groups
             */
            if (((getItem(i).getRow1() < row1) && (getItem(i).getRow2() > row1) && (getItem(i).getRow2() < row2))
                || ((getItem(i).getRow1() < row2) && (getItem(i).getRow2() > row2) && (getItem(i).getRow1() > row1))) {
                throw new RuntimeException("cannot group"); //$NON-NLS-1$
            }
        }

        final GroupNode node1 = new GroupNode(row1, row2);
        list.add(node1);

        query.notifyModifiedListeners();
    }

    @Override
    public boolean canGroup(final int row1, final int row2) {
        if ((row1 >= row2) || (row1 < 0)) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            if ((getItem(i).getRow1() == row1) && getItem(i).getRow2() == row2) {
                return false;
            }
            if (getItem(i).getRow2() == row1) {
                return false;
            }
            if (getItem(i).getRow1() == row2) {
                return false;
            }
            /*
             * probably an overlap test
             */
            if ((((getItem(i).getRow1() < row1) && (getItem(i).getRow2() > row1)) && (getItem(i).getRow2() < row2))
                || (((getItem(i).getRow1() < row2) && (getItem(i).getRow2() > row2))
                    && (getItem(i).getRow1() > row1))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean canUngroup(final int row1, final int row2) {
        if ((row1 < row2) && (row1 >= 0)) {
            for (int i = 0; i < list.size(); i++) {
                if ((getItem(i).getRow1() == row1) && (getItem(i).getRow2() == row2)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public QEQueryConnection getConnection(final int depth, final int row) {
        for (int i = 0; i < list.size(); i++) {
            if (depth(getItem(i)) == depth) {
                if (getItem(i).getRow1() == row) {
                    return new QEQueryConnectionImp(QEQueryConnectionType.DOWN, row, getItem(i).getRow2());
                }
                if (getItem(i).getRow2() == row) {
                    return new QEQueryConnectionImp(QEQueryConnectionType.UP, getItem(i).getRow1(), row);
                }
                if ((getItem(i).getRow1() < row) && (getItem(i).getRow2() > row)) {
                    return new QEQueryConnectionImp(QEQueryConnectionType.ACROSS);
                }
            }
        }
        return new QEQueryConnectionImp(QEQueryConnectionType.NONE);
    }

    @Override
    public boolean hasGroup(final int row1, final int row2) {
        return canUngroup(row1, row2);
    }

    @Override
    public boolean hasGroupings() {
        return list.size() > 0;
    }

    @Override
    public int getMaxDepth() {
        int num1 = 0;
        for (int i = 0; i < list.size(); i++) {
            final int num3 = depth(getItem(i));
            if (num1 < num3) {
                num1 = num3;
            }
        }
        return num1;
    }

    @Override
    public boolean removeGrouping(final int row1, final int row2) {
        for (int i = 0; i < list.size(); i++) {
            if ((getItem(i).getRow1() == row1) && (getItem(i).getRow2() == row2)) {
                list.remove(i);
                query.notifyModifiedListeners();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean rowInGroup(final int row) {
        for (int i = 0; i < list.size(); i++) {
            if ((getItem(i).getRow1() == row) || (getItem(i).getRow2() == row)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean rowIsGrouped(final int row) {
        for (int i = 0; i < list.size(); i++) {
            if ((getItem(i).getRow1() <= row) && (getItem(i).getRow2() >= row)) {
                return true;
            }
        }
        return false;
    }

    /*
     * ************************************************************************
     * END of implementation of QEQueryGrouping interface
     * ***********************************************************************
     */

    private GroupNode getItem(final int index) {
        return list.get(index);
    }

    private static class GroupNode {
        private int row1;
        private int row2;

        public GroupNode(final int row1, final int row2) {
            this.row1 = row1;
            this.row2 = row2;
        }

        public int getRow1() {
            return row1;
        }

        public void setRow1(final int row1) {
            this.row1 = row1;
        }

        public int getRow2() {
            return row2;
        }

        public void setRow2(final int row2) {
            this.row2 = row2;
        }
    }

    public void addRow(final int row) {
        if (row < 0) {
            throw new IllegalArgumentException("row is out of range"); //$NON-NLS-1$
        }
        for (int i = 0; i < list.size(); i++) {
            if (getItem(i).getRow1() >= row) {
                getItem(i).setRow1(getItem(i).getRow1() + 1);
            }
            if (getItem(i).getRow2() >= row) {
                getItem(i).setRow2(getItem(i).getRow2() + 1);
            }
        }
    }

    public void clear() {
        list.clear();
        query.notifyModifiedListeners();
    }

    private int depth(final GroupNode node) {
        return nestedGroupCount(node) + 1;
    }

    private boolean isDuplicate(final GroupNode node) {
        for (int i = 0; i < list.size(); i++) {
            if (((node != getItem(i)) && (getItem(i).getRow1() == node.getRow1()))
                && (getItem(i).getRow2() == node.getRow2())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNested(final GroupNode parent, final GroupNode node) {
        if (parent.getRow1() <= node.getRow1()) {
            return parent.getRow2() >= node.getRow2();
        }
        return false;
    }

    private int nestedGroupCount(final GroupNode parent) {
        int num1 = 0;
        for (int i = 0; i < list.size(); i++) {
            if ((parent != getItem(i)) && isNested(parent, getItem(i))) {
                num1++;
            }
        }
        return num1;
    }

    public void removeRow(final int row) {
        if (row < 0) {
            throw new IllegalArgumentException("row is out of range"); //$NON-NLS-1$
        }

        int num1 = 0;
        while (num1 < list.size()) {
            final GroupNode node1 = getItem(num1);
            if (node1.getRow1() == row) {
                if (node1.getRow2() == (row + 1)) {
                    list.remove(num1);
                } else {
                    node1.setRow2(node1.getRow2() - 1);
                }
            } else if (node1.getRow2() == row) {
                if (node1.getRow1() == (row - 1)) {
                    list.remove(num1);
                } else {
                    node1.setRow2(row - 1);
                }
            } else {
                if (node1.getRow1() > row) {
                    node1.setRow1(node1.getRow1() - 1);
                }
                if (node1.getRow2() > row) {
                    node1.setRow2(node1.getRow2() - 1);
                }
            }
            if (num1 < list.size() && node1 == getItem(num1)) {
                num1++;
            }
        }
        int num2 = 0;
        while (num2 < list.size()) {
            if (isDuplicate(getItem(num2))) {
                list.remove(num2);
            } else {
                num2++;
            }
        }
    }
}
