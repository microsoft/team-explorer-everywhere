// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListEntry;

public class ServerListTable extends TableControl {
    public ServerListTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public ServerListTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, ServerListConfigurationEntry.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("ServerListTable.ColumnNameName"), 200, "name"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("ServerListTable.ColumnNameServer"), 200, "server"), //$NON-NLS-1$ //$NON-NLS-2$
        };

        setupTable(true, false, columnData);
        setUseViewerDefaults();
    }

    public void setServerListEntries(final ServerListConfigurationEntry[] serverListEntries) {
        setElements(serverListEntries);
    }

    public ServerListConfigurationEntry[] getServerListEntries() {
        return (ServerListConfigurationEntry[]) getElements();
    }

    public void setSelectedServerURIs(final ServerListConfigurationEntry[] serverListEntries) {
        setSelectedElements(serverListEntries);
    }

    public void setSelectedServerURI(final ServerListConfigurationEntry serverListEntry) {
        setSelectedElement(serverListEntry);
    }

    public ServerListConfigurationEntry[] getSelectedServerListEntries() {
        return (ServerListConfigurationEntry[]) getSelectedElements();
    }

    public ServerListConfigurationEntry getSelectedServerListEntry() {
        return (ServerListConfigurationEntry) getSelectedElement();
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        return ControlSize.computeCharSize(wHint, hHint, this, 100, 15);
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final ServerListEntry serverListEntry = (ServerListEntry) element;

        String result = null;

        switch (columnIndex) {
            case 0:
                result = serverListEntry.getName();
                break;

            case 1:
                result = ServerURIUtils.normalizeURI(serverListEntry.getURI()).toString();
                break;
        }

        if (result == null) {
            result = ""; //$NON-NLS-1$
        }

        return result;
    }
}
