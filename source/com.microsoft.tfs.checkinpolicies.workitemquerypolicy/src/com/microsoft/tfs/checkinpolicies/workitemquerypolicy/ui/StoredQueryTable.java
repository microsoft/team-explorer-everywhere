// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.workitemquerypolicy.ui;

import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.checkinpolicies.workitemquerypolicy.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;

public class StoredQueryTable extends TableControl {
    private static final String QUERY_COLUMN_NAME = "query"; //$NON-NLS-1$

    public StoredQueryTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public StoredQueryTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style | SWT.SINGLE, StoredQuery.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("StoredQueryTable.ColumnNameQueryName"), 300, QUERY_COLUMN_NAME), //$NON-NLS-1$
        };

        setupTable(false, false, columnData);

        setUseViewerDefaults();
        setEnableTooltips(false);

        final TableViewerSorter sorter = (TableViewerSorter) getViewer().getSorter();

        sorter.setComparator(QUERY_COLUMN_NAME, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(
                    ((StoredQuery) o1).getName(),
                    ((StoredQuery) o2).getName());
            }
        });
    }

    public void setStoredQueries(final StoredQuery[] storedQueries) {
        setElements(storedQueries);
    }

    public StoredQuery[] getStoredQueries() {
        return (StoredQuery[]) getElements();
    }

    public void setSelectedStoredQuery(final StoredQuery storedQuery) {
        setSelectedElement(storedQuery);
    }

    public StoredQuery getSelectedStoredQuery() {
        return (StoredQuery) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final StoredQuery query = (StoredQuery) element;

        if (QUERY_COLUMN_NAME.equals(columnPropertyName)) {
            return query.getName();
        }

        return null;
    }
}
