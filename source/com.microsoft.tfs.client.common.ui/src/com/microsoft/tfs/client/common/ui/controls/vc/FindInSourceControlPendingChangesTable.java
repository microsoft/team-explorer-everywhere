// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.table.TableViewerUtils;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;

public class FindInSourceControlPendingChangesTable extends TableControl {
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$
    private static final String USER_COLUMN_ID = "user"; //$NON-NLS-1$
    private static final String WORKSPACE_COLUMN_ID = "workspace"; //$NON-NLS-1$
    private static final String CHANGE_COLUMN_ID = "change"; //$NON-NLS-1$
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$

    private Map<String, List<PendingSet>> pendingChangesMap = null;
    private Map<String, Item> itemsMap = null;

    public FindInSourceControlPendingChangesTable(final Composite parent, final int style) {
        super(parent, style, String.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("FindInSourceControlPendingChangesTable.ColumnHeaderName"), //$NON-NLS-1$
                100,
                0.2F,
                NAME_COLUMN_ID),
            new TableColumnData(
                Messages.getString("FindInSourceControlPendingChangesTable.ColumnHeaderUser"), //$NON-NLS-1$
                75,
                0.1F,
                USER_COLUMN_ID),
            new TableColumnData(
                Messages.getString("FindInSourceControlPendingChangesTable.ColumnHeaderWorkspace"), //$NON-NLS-1$
                75,
                0.1F,
                WORKSPACE_COLUMN_ID),
            new TableColumnData(
                Messages.getString("FindInSourceControlPendingChangesTable.ColumnHeaderChange"), //$NON-NLS-1$
                75,
                0.1F,
                CHANGE_COLUMN_ID),
            new TableColumnData(
                Messages.getString("FindInSourceControlPendingChangesTable.ColumnHeaderFolder"), //$NON-NLS-1$
                400,
                0.5F,
                FOLDER_COLUMN_ID),
        };

        setupTable(true, true, columnData);

        setUseDefaultContentProvider();
        getViewer().setSorter(new TableViewerSorter(getViewer(), null, 4, SortDirection.ASCENDING));
        getViewer().setLabelProvider(new FindInSourceControlPendingChangeLabelProvider());
    }

    public void setRepository(final TFSRepository repository) {
    }

    public void setItems(final Map<String, Item> itemsMap) {
        this.itemsMap = itemsMap;

    }

    public void setPendingChangesMap(final Map<String, List<PendingSet>> pendingChangesMap, final boolean showStatus) {
        this.pendingChangesMap = pendingChangesMap;

        // Hide the pending changes columns in case the user unchecked the show
        // status option
        hideShowStatusColumns(showStatus);

        if (pendingChangesMap == null && (itemsMap == null || itemsMap.size() == 0)) {
            setElements(new String[0]);
        } else {
            final String[] serverItems;
            if ((itemsMap == null || itemsMap.size() == 0) && pendingChangesMap != null) {
                serverItems = pendingChangesMap.keySet().toArray(new String[pendingChangesMap.keySet().size()]);
            } else {
                serverItems = itemsMap.keySet().toArray(new String[itemsMap.keySet().size()]);
            }
            setElements(serverItems);

        }
    }

    public String[] getSelectedServerItems() {
        return (String[]) getSelectedElements();
    }

    public ItemType getSelectedItemType(final String serverItem) {
        if (serverItem != null) {
            final List<PendingSet> pendingSets = pendingChangesMap.get(serverItem);

            if (pendingSets != null) {
                for (final PendingSet pendingSet : pendingSets) {
                    for (final PendingChange pendingChange : pendingSet.getPendingChanges()) {
                        if (pendingChange.getServerItem().equals(serverItem)) {
                            return pendingChange.getItemType();
                        }
                    }
                }
            } else {
                // In case the user selected all items regardless of their state
                if (itemsMap != null) {
                    final Item item = itemsMap.get(serverItem);
                    if (item != null) {
                        return item.getItemType();
                    }
                }

            }
        }

        return null;
    }

    public Map<PendingChange, PendingSet> getSelectedPendingChanges() {
        final Map<PendingChange, PendingSet> pendingChangeMap = new HashMap<PendingChange, PendingSet>();
        final String[] selectedItems = getSelectedServerItems();
        if (selectedItems != null) {
            for (final String serverItem : selectedItems) {
                if (serverItem != null && pendingChangesMap != null) {
                    final List<PendingSet> pendingSets = pendingChangesMap.get(serverItem);

                    if (pendingSets != null) {
                        for (final PendingSet pendingSet : pendingSets) {
                            for (final PendingChange pendingChange : pendingSet.getPendingChanges()) {
                                if (serverItem.equals(pendingChange.getServerItem())) {
                                    pendingChangeMap.put(pendingChange, pendingSet);
                                }
                            }
                        }
                    }
                }
            }
        }

        return pendingChangeMap;
    }

    // shows/hides the status columns based on the use selection
    private void hideShowStatusColumns(final boolean showStatus) {
        // Hide the pending changes columns in case the user unchecked the
        // show
        // status option
        final int columnWidth = showStatus ? 75 : 0;
        final int userColumnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(USER_COLUMN_ID, getViewer());
        final int workspaceColumnIndex =
            TableViewerUtils.columnPropertyNameToColumnIndex(WORKSPACE_COLUMN_ID, getViewer());
        final int changeColumnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(CHANGE_COLUMN_ID, getViewer());
        getTable().getColumn(userColumnIndex).setWidth(columnWidth);
        getTable().getColumn(workspaceColumnIndex).setWidth(columnWidth);
        getTable().getColumn(changeColumnIndex).setWidth(columnWidth);
    }

    private class FindInSourceControlPendingChangeLabelProvider extends FolderFileLabelProvider
        implements ITableLabelProvider {
        private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (columnIndex != 0) {
                return null;
            }

            final String serverItem = (String) element;
            final List<PendingSet> pendingSetList =
                pendingChangesMap != null ? pendingChangesMap.get(serverItem) : null;
            final Item item = (itemsMap == null || itemsMap.size() == 0) ? null : itemsMap.get(serverItem);

            if ((pendingSetList == null
                || pendingSetList.size() == 0
                || pendingSetList.get(0).getPendingChanges().length == 0) && (item == null)) {
                return null;
            }

            if (ServerPath.equals(ServerPath.ROOT, serverItem)) {
                return imageHelper.getImage("images/common/team_foundation_server.gif"); //$NON-NLS-1$
            } else if (ServerPath.equals(serverItem, ServerPath.getTeamProject(serverItem))) {
                return imageHelper.getImage("images/common/team_project.gif"); //$NON-NLS-1$
            } else {
                if (pendingSetList != null) {
                    /* Note: this should match quickly. */
                    for (final PendingSet pendingSet : pendingSetList) {
                        boolean foundServerItem = false;

                        for (final PendingChange pendingChange : pendingSet.getPendingChanges()) {
                            if ((pendingChange.getServerItem() != null
                                && ServerPath.equals(pendingChange.getServerItem(), serverItem))
                                || (pendingChange.getSourceServerItem() != null
                                    && ServerPath.equals(pendingChange.getSourceServerItem(), serverItem))) {
                                foundServerItem = true;

                                if (pendingChange.getItemType().equals(ItemType.FOLDER)) {
                                    return getImageForFolder();
                                }
                            }
                        }

                        if (foundServerItem) {
                            break;
                        }
                    }
                } else {
                    if (item.getItemType().equals(ItemType.FOLDER) && item.isBranch()) {
                        return imageHelper.getImage("images/vc/folder_branch.gif"); //$NON-NLS-1$
                    } else if (item.getItemType().equals(ItemType.FOLDER)) {
                        return getImageForFolder();
                    }
                }
            }

            return getImageForFile(ServerPath.getFileName(serverItem));
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final String columnId = TableViewerUtils.columnIndexToColumnProperty(columnIndex, getViewer());

            final String serverItem = (String) element;
            final List<PendingSet> pendingSetList =
                pendingChangesMap != null ? pendingChangesMap.get(serverItem) : null;

            if (NAME_COLUMN_ID.equals(columnId)) {
                return ServerPath.getFileName(serverItem);
            } else if (USER_COLUMN_ID.equals(columnId) && pendingSetList != null) {
                final Set<String> users = new TreeSet<String>();
                final StringBuffer userBuffer = new StringBuffer();

                for (final PendingSet pendingSet : pendingSetList) {
                    users.add(pendingSet.getOwnerDisplayName());
                }

                for (final String user : users) {
                    if (userBuffer.length() > 0) {
                        userBuffer.append(", "); //$NON-NLS-1$
                    }

                    userBuffer.append(user);
                }

                return userBuffer.toString();
            } else if (WORKSPACE_COLUMN_ID.equals(columnId) && pendingSetList != null) {
                final Set<String> workspaces = new TreeSet<String>();
                final StringBuffer workspaceBuffer = new StringBuffer();

                for (final PendingSet pendingSet : pendingSetList) {
                    workspaces.add(MessageFormat.format("{0} [{1}]", pendingSet.getName(), pendingSet.getComputer())); //$NON-NLS-1$
                }

                for (final String workspace : workspaces) {
                    if (workspaceBuffer.length() > 0) {
                        workspaceBuffer.append(", "); //$NON-NLS-1$
                    }

                    workspaceBuffer.append(workspace);
                }

                return workspaceBuffer.toString();
            } else if (CHANGE_COLUMN_ID.equals(columnId) && pendingSetList != null) {
                // Combine all the changes for this item in all pending sets
                ChangeType changeType = ChangeType.NONE;
                final List<PropertyValue> allProperties = new ArrayList<PropertyValue>();

                for (final PendingSet pendingSet : pendingSetList) {
                    for (final PendingChange pendingChange : pendingSet.getPendingChanges()) {
                        if (ServerPath.equals(pendingChange.getServerItem(), serverItem)) {
                            changeType = changeType.combine(pendingChange.getChangeType());
                            if (pendingChange.getPropertyValues() != null) {
                                allProperties.addAll(Arrays.asList(pendingChange.getPropertyValues()));
                            }
                        }
                    }
                }

                return changeType.toUIString(true, allProperties.toArray(new PropertyValue[allProperties.size()]));
            } else if (FOLDER_COLUMN_ID.equals(columnId)) {
                return ServerPath.getParent(serverItem);
            } else {
                return ""; //$NON-NLS-1$
            }
        }

        /**
         * {@link FolderFileLabelProvider} overrides this {@link LabelProvider}
         * method to clean up the {@link ImageHelper} that it manages.
         * Subclasses must call super.dispose() if they also override this
         * method.
         */
        @Override
        public void dispose() {
            imageHelper.dispose();
            super.dispose();
        }
    }
}
