// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.eclipse.ui.Messages;

public class StatusItemTable extends TableControl {
    public StatusItemTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public StatusItemTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, StatusItem.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("StatusItemTable.ColumnNameUser"), 100, "user"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("StatusItemTable.ColumnNameChangeType"), 100, "changetype"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("StatusItemTable.ColumnNameWorkspace"), 100, "workspace") //$NON-NLS-1$ //$NON-NLS-2$
        };

        setupTable(true, false, columnData);

        setUseViewerDefaults();
    }

    public void setStatusItems(final StatusItem[] statusItems) {
        setElements(statusItems);
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final StatusItem statusItem = (StatusItem) element;

        switch (columnIndex) {
            case 0:
                return statusItem.getUserName();
            case 1:
                return statusItem.getChangeType().toUIString(true, statusItem.getPropertyValues());
            case 2:
                return statusItem.getWorkspaceName();
            default:
                return ""; //$NON-NLS-1$
        }
    }
}
