// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

public class LabelItemTable extends TableControl {
    private static final Log log = LogFactory.getLog(LabelItemTable.class);

    private static final String ITEM_COLUMN_ID = "item"; //$NON-NLS-1$
    private static final String VERSION_COLUMN_ID = "version"; //$NON-NLS-1$

    public LabelItemTable(final Composite parent, final int style) {
        super(parent, style, LabelItem.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("LabelItemTable.ColumnHeaderItem"), 150, 0.75F, ITEM_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("LabelItemTable.ColumnHeaderVersion"), 50, 0.25F, VERSION_COLUMN_ID) //$NON-NLS-1$
        };

        setOptionPersistGeometry(false);
        setupTable(true, true, columnData);
        setUseViewerDefaults();
    }

    @Override
    protected String getColumnText(final Object element, final String propertyName) {
        final LabelItem item = (LabelItem) element;

        if (propertyName.equals(ITEM_COLUMN_ID)) {
            return VersionedFileSpec.formatPathWithDeletionIfNecessary(item.getServerItem(), item.getDeletionID());
        } else if (propertyName.equals(VERSION_COLUMN_ID)) {
            final VersionSpec versionSpec = item.getVersionSpec();

            if (versionSpec instanceof LatestVersionSpec) {
                return Messages.getString("LabelItemTable.ColumnTextLatest"); //$NON-NLS-1$
            } else if (versionSpec instanceof LabelVersionSpec) {
                final LabelVersionSpec labelSpec = (LabelVersionSpec) versionSpec;

                return labelSpec.getLabel() + "@" + labelSpec.getScope(); //$NON-NLS-1$
            } else if (versionSpec instanceof DateVersionSpec) {
                final Date date = ((DateVersionSpec) versionSpec).getDate().getTime();
                return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date);
            } else if (versionSpec instanceof WorkspaceVersionSpec) {
                final WorkspaceVersionSpec workspaceSpec = (WorkspaceVersionSpec) versionSpec;

                return workspaceSpec.getName() + ";" + workspaceSpec.getOwner(); //$NON-NLS-1$
            } else if (versionSpec instanceof ChangesetVersionSpec) {
                return Integer.toString(((ChangesetVersionSpec) versionSpec).getChangeset());
            } else {
                return versionSpec.toString();
            }
        }

        return "(Unknown)"; //$NON-NLS-1$
    }

    public void setItems(final LabelItem[] items) {
        setElements(items);
    }

    public LabelItem[] getItems() {
        return (LabelItem[]) getElements();
    }

    public void setSelectedItems(final LabelItem[] selectedItems) {
        setSelectedElements(selectedItems);
    }

    public LabelItem[] getSelectedItems() {
        return (LabelItem[]) getSelectedElements();
    }

    public void removeItems(final LabelItem[] items) {
        removeElements(items);
    }

    public static class LabelItemStatus extends TypesafeEnum {
        public static final LabelItemStatus NONE = new LabelItemStatus(0, "none"); //$NON-NLS-1$
        public static final LabelItemStatus EXISTS = new LabelItemStatus(1, "exists"); //$NON-NLS-1$
        public static final LabelItemStatus ADD = new LabelItemStatus(2, "add"); //$NON-NLS-1$
        public static final LabelItemStatus ADD_IMPLICIT = new LabelItemStatus(3, "add_implicit"); //$NON-NLS-1$
        public static final LabelItemStatus EXCLUDE = new LabelItemStatus(4, "exclude"); //$NON-NLS-1$
        public static final LabelItemStatus REMOVE = new LabelItemStatus(5, "remove"); //$NON-NLS-1$

        private final String name;

        private LabelItemStatus(final int value, final String name) {
            super(value);

            Check.notNull(name, "name"); //$NON-NLS-1$
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class LabelItem {
        private final String serverItem;
        private final RecursionType recursionType;
        private final VersionSpec versionSpec;
        private final int deletionID;

        private LabelItemStatus status;

        public LabelItem(
            final String serverItem,
            final RecursionType recursionType,
            final VersionSpec versionSpec,
            final int deletionID,
            final LabelItemStatus status) {
            Check.notNull(serverItem, "serverItem"); //$NON-NLS-1$
            Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
            Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$
            Check.notNull(status, "status"); //$NON-NLS-1$

            this.serverItem = serverItem;
            this.recursionType = recursionType;
            this.versionSpec = versionSpec;
            this.deletionID = deletionID;
            this.status = status;
        }

        public String getServerItem() {
            return serverItem;
        }

        public RecursionType getRecursionType() {
            return recursionType;
        }

        public VersionSpec getVersionSpec() {
            return versionSpec;
        }

        public int getDeletionID() {
            return deletionID;
        }

        public LabelItemStatus getItemStatus() {
            return status;
        }

        public void setItemStatus(final LabelItemStatus status) {
            Check.notNull(status, "status"); //$NON-NLS-1$

            this.status = status;
        }
    }
}