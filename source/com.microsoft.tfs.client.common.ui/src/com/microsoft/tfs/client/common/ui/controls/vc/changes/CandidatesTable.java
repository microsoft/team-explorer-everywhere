// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;

public class CandidatesTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String CHANGE_COLUMN_ID = "change"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    public CandidatesTable(final Composite parent, final int style, final ChangeItem[] candidates) {
        this(parent, style, candidates, null);
    }

    public CandidatesTable(
        final Composite parent,
        final int style,
        final ChangeItem[] candidates,
        final String viewDataKey) {
        super(parent, style, ChangeItem.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("ChangesTable.ColumnHeaderName"), 100, 0.2F, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("ChangesTable.ColumnHeaderChange"), 100, 0.1F, CHANGE_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("ChangesTable.ColumnHeaderFolder"), 200, 0.7F, FOLDER_COLUMN_ID) //$NON-NLS-1$
        };

        setupTable(true, true, columnData);

        setUseDefaultContentProvider();
        getViewer().setLabelProvider(new CandidateLabelProvider());
        setEnableTooltips(true);

        setInput(candidates);
        setCheckedElements(candidates);

        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        getViewer().setSorter(sorter);

        /* Sort by folder, then by filename */
        sorter.sort(NAME_COLUMN_ID, SortDirection.ASCENDING);
        sorter.sort(FOLDER_COLUMN_ID, SortDirection.ASCENDING);
    }

    public void setChangeItems(final ChangeItem[] elements) {
        setElements(elements);
    }

    public ChangeItem[] getCheckedChangeItems() {
        return (ChangeItem[]) getCheckedElements();
    }

    private class CandidateLabelProvider extends FolderFileLabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (columnIndex != 0) {
                return null;
            }

            final ChangeItem changeItem = (ChangeItem) element;

            if (ItemType.FOLDER == changeItem.getItemType()) {
                return getImageForFolder();
            }

            return getImageForFile(changeItem.getName());
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final ChangeItem changeItem = (ChangeItem) element;

            String text = null;

            switch (columnIndex) {
                case 0:
                    text = changeItem.getName();
                    break;
                case 1:
                    text = changeItem.getChangeType().toUIString(true, changeItem.getPropertyValues());
                    break;
                case 2:
                    text = changeItem.getFolder();
                    break;
            }

            if (text == null) {
                text = ""; //$NON-NLS-1$
            }

            return text;
        }
    }
}
