// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

/**
 * <p>
 * {@link TFSItemTable} is a control that displays a collection of
 * {@link TFSItem}s in a table.
 * </p>
 *
 * <p>
 * The supported style bits that can be used with {@link TFSItemTable} are
 * defined by the base class {@link TableControl}.
 * </p>
 *
 * @see TFSItem
 * @see TableControl
 */
public class TFSItemTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    public TFSItemTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public TFSItemTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, TFSItem.class, viewDataKey);

        setupTable();
        setUseViewerDefaults();
    }

    public void setItems(final TFSItem[] items) {
        setElements(items);
    }

    public TFSItem[] getItems() {
        return (TFSItem[]) getElements();
    }

    public void setSelectedItems(final TFSItem[] items) {
        setSelectedElements(items);
    }

    public void setSelectedItem(final TFSItem item) {
        setSelectedElement(item);
    }

    public TFSItem[] getSelectedItems() {
        return (TFSItem[]) getSelectedElements();
    }

    public TFSItem[] getSelectedItem() {
        return (TFSItem[]) getSelectedElement();
    }

    public void setCheckedItems(final TFSItem[] resources) {
        setCheckedElements(resources);
    }

    public TFSItem[] getCheckedItems() {
        return (TFSItem[]) getCheckedElements();
    }

    private void setupTable() {
        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("TFSItemTable.ColumnHeaderName"), 100, 0.4F, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("TFSItemTable.ColumnHeaderFolder"), 100, 0.6F, FOLDER_COLUMN_ID) //$NON-NLS-1$
        };

        setupTable(true, true, columnData);
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final TFSItem item = (TFSItem) element;

        switch (columnIndex) {
            case 0:
                return item.getName();
            case 1:
                return LocalPath.getDirectory(item.getFullPath());
            default:
                return super.getColumnText(element, columnIndex);
        }
    }
}
