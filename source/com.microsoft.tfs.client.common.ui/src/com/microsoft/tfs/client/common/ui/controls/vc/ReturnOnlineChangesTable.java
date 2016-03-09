// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChange;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public class ReturnOnlineChangesTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String CHANGE_COLUMN_ID = "change"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    public ReturnOnlineChangesTable(final Composite parent, final int style) {
        super(parent, style | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK, OfflineChange.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("ReturnOnlineChangesTable.ColumnHeaderName"), //$NON-NLS-1$
                75,
                0.20F,
                NAME_COLUMN_ID),
            new TableColumnData(
                Messages.getString("ReturnOnlineChangesTable.ColumnHeaderChange"), //$NON-NLS-1$
                40,
                0.15F,
                CHANGE_COLUMN_ID),
            new TableColumnData(
                Messages.getString("ReturnOnlineChangesTable.ColumnHeaderFolder"), //$NON-NLS-1$
                150,
                0.65F,
                FOLDER_COLUMN_ID)
        };

        setupTable(true, true, columnData);
        setUseViewerDefaults();
        setEnableTooltips(true);
    }

    @Override
    protected String getColumnText(final Object element, final String propertyName) {
        final OfflineChange change = (OfflineChange) element;

        if (propertyName.equals(NAME_COLUMN_ID)) {
            return LocalPath.getFileName(change.getLocalPath());
        } else if (propertyName.equals(CHANGE_COLUMN_ID)) {
            return getChangeType(change.getChangeTypes());
        } else if (propertyName.equals(FOLDER_COLUMN_ID)) {
            final String folder = new File(change.getLocalPath()).getParent();
            return (folder != null) ? folder : ""; //$NON-NLS-1$
        }

        return Messages.getString("ReturnOnlineChangesTable.UnknownColumnText"); //$NON-NLS-1$
    }

    public String getTooltip(final Object element) {
        final OfflineChange change = (OfflineChange) element;

        return MessageFormat.format(
            Messages.getString("ReturnOnlineChangesTable.TooltipNameFolderChangesFormat"), //$NON-NLS-1$
            LocalPath.getFileName(change.getLocalPath()),
            LocalPath.getDirectory(change.getLocalPath()),
            getChangeType(change.getChangeTypes()));
    }

    public void setChanges(final OfflineChange[] changes) {
        setElements(changes);
    }

    public OfflineChange[] getChanges() {
        return (OfflineChange[]) getElements();
    }

    public void setCheckedChanges(final OfflineChange[] changes) {
        setCheckedElements(changes);
    }

    public OfflineChange[] getCheckedChanges() {
        return (OfflineChange[]) getCheckedElements();
    }

    private String getChangeType(final OfflineChangeType[] changes) {
        final StringBuffer changeType = new StringBuffer();

        for (int i = 0; i < changes.length; i++) {
            if (i > 0) {
                changeType.append(", "); //$NON-NLS-1$
            }

            changeType.append(changes[i].toString().toLowerCase());
        }

        return changeType.toString();
    }
}
