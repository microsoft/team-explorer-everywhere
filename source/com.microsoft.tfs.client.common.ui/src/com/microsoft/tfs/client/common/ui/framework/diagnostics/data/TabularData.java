// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TabularData {
    private final String[] columns;
    private final List rows = new ArrayList();
    private boolean sorted = false;

    public TabularData(final TabularData otherTable, final Row[] otherRows) {
        columns = otherTable.columns;
        rows.addAll(Arrays.asList(otherRows));
    }

    public void setSorted(final boolean sorted) {
        this.sorted = sorted;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void sortByFirstColumn() {
        Collections.sort(rows, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                final Row r1 = (Row) o1;
                final Row r2 = (Row) o2;

                final Object[] values1 = r1.getValues();
                final Object[] values2 = r2.getValues();

                if (values1.length > 0 && values1[0] instanceof String && values2[0] instanceof String) {
                    return ((String) values1[0]).compareToIgnoreCase((String) values2[0]);
                }

                return 0;
            }
        });
    }

    public TabularData(final String[] columns) {
        if (columns == null) {
            throw new IllegalArgumentException();
        }
        this.columns = columns;
    }

    public String[] getColumns() {
        return columns;
    }

    public Row[] getRows() {
        return (Row[]) rows.toArray(new Row[rows.size()]);
    }

    public void addRow(final Row row) {
        if (row.getValues() == null || row.getValues().length != columns.length) {
            throw new IllegalArgumentException();
        }
        rows.add(row);
    }
}
