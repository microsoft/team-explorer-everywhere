// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;

public class ServerItemSelectionTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    public ServerItemSelectionTable(final Composite parent, final int style) {
        super(parent, style, TypedServerItem.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("ServerItemSelectionTable.ColumnHeaderName"), //$NON-NLS-1$
                100,
                0.2F,
                NAME_COLUMN_ID),
            new TableColumnData(
                Messages.getString("ServerItemSelectionTable.ColumnHeaderFolder"), //$NON-NLS-1$
                400,
                0.8F,
                FOLDER_COLUMN_ID)
        };
        setupTable(true, true, columnData);

        setUseViewerDefaults();
    }

    public void setServerItems(final TypedServerItem[] serverPaths) {
        setElements(serverPaths);
    }

    public void setSelectedServerItems(final TypedServerItem[] serverPaths) {
        setSelectedElements(serverPaths);
    }

    public void setCheckedServerItems(final TypedServerItem[] serverPaths) {
        setCheckedElements(serverPaths);
    }

    public TypedServerItem[] getServerItems() {
        return (TypedServerItem[]) getElements();
    }

    public TypedServerItem[] getSelectedServerItems() {
        return (TypedServerItem[]) getSelectedElements();
    }

    public TypedServerItem[] getCheckedServerItems() {
        return (TypedServerItem[]) getCheckedElements();
    }

    @Override
    protected String getColumnText(final Object element, final String columnName) {
        final TypedServerItem serverItem = (TypedServerItem) element;

        if (columnName.equals(NAME_COLUMN_ID)) {
            return serverItem.getName();
        } else if (columnName.equals(FOLDER_COLUMN_ID)) {
            return serverItem.getParent().getServerPath();
        }

        return Messages.getString("ServerItemSelectionTable.UnknownColumnText"); //$NON-NLS-1$
    }

    @Override
    protected Image getColumnImage(final Object element, final String columnName) {
        if (!columnName.equals(NAME_COLUMN_ID)) {
            return null;
        }

        final TypedServerItem serverItem = (TypedServerItem) element;

        if (serverItem.getType().equals(ServerItemType.FILE)) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
        }

        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    }
}
