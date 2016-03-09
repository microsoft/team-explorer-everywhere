// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class ChangesTable extends TableControl {
    private static final String FOLDER_COLUMN_ID = "folder"; //$NON-NLS-1$
    private static final String CHANGE_COLUMN_ID = "change"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * When true, {@link #setChangeItems(ChangeItem[], ChangeItemType)} will set
     * the check state to "true" for new change items, and preserve the check
     * state of old items.
     *
     * When false, every time
     * {@link #setChangeItems(ChangeItem[], ChangeItemType)} is called, the
     * check states of the items goes to the {@link TableControl} default
     * (probably the SWT control's default).
     */
    private final boolean autoCheckNewItems = true;

    public ChangesTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public ChangesTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, ChangeItem.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("ChangesTable.ColumnHeaderName"), 100, 0.2F, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("ChangesTable.ColumnHeaderChange"), 100, 0.1F, CHANGE_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("ChangesTable.ColumnHeaderFolder"), 200, 0.7F, FOLDER_COLUMN_ID) //$NON-NLS-1$
        };

        setupTable(true, true, columnData);

        setUseDefaultContentProvider();
        getViewer().setLabelProvider(new LabelProvider());
        setEnableTooltips(true);

        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        getViewer().setSorter(sorter);

        /* Sort by folder, then by filename */
        sorter.sort(NAME_COLUMN_ID, SortDirection.ASCENDING);
        sorter.sort(FOLDER_COLUMN_ID, SortDirection.ASCENDING);
    }

    public void setChangeItems(final ChangeItem[] changeItems, final ChangeItemType type) {
        /*
         * Change items can be large enough (tens of thousands of items) that
         * adding items to native controls gets slow.
         */
        BusyIndicator.showWhile(getDisplay(), new Runnable() {
            @Override
            public void run() {
                if (type == ChangeItemType.CHANGESET) {
                    setClipboardTransferTypes(new Transfer[] {
                        TextTransfer.getInstance()
                    });
                    setDragTransferTypes(null);
                } else if (type == ChangeItemType.PENDING || type == ChangeItemType.SHELVESET) {
                    setClipboardTransferTypes(new Transfer[] {
                        FileTransfer.getInstance(),
                        TextTransfer.getInstance()
                    });
                    setDragTransferTypes(new Transfer[] {
                        FileTransfer.getInstance()
                    });
                } else {
                    setClipboardTransferTypes(null);
                    setDragTransferTypes(null);
                }

                /*
                 * New items are made "checked" by making a list of existing
                 * "unchecked" items, then adding all the new ones, checking
                 * them all, the unchecking the correct ones.
                 */
                if (autoCheckNewItems && isCheckboxTable()) {
                    final ChangeItem[] uncheckedItems = getUncheckedChangeItems();

                    setElements(changeItems);

                    checkAll();
                    uncheckItems(uncheckedItems);
                } else {
                    setElements(changeItems);
                }
            }
        });
    }

    public ChangeItem[] getChangeItems() {
        return (ChangeItem[]) getElements();
    }

    public void setSelectedChangeItems(final ChangeItem[] items) {
        setSelectedElements(items);
    }

    public void setSelectedChangeItem(final ChangeItem item) {
        setSelectedElement(item);
    }

    public ChangeItem[] getSelectedChangeItems() {
        return (ChangeItem[]) getSelectedElements();
    }

    public ChangeItem getSelectedChangeItem() {
        return (ChangeItem) getSelectedElement();
    }

    public void setCheckedChangeItems(final ChangeItem[] items) {
        setCheckedElements(items);
    }

    public ChangeItem[] getCheckedChangeItems() {
        return (ChangeItem[]) getCheckedElements();
    }

    /**
     * @return the items that are not checked
     */
    private ChangeItem[] getUncheckedChangeItems() {
        /*
         * This is not very efficient because the base class doesn't keep track
         * of the unchecked items, just the checked ones. So query the
         * underlying table viewer and hope it is efficient.
         */
        final ChangeItem[] allChangeItems = getChangeItems();

        final List uncheckedChangeItems = new ArrayList();

        for (int i = 0; i < allChangeItems.length; i++) {
            if (((CheckboxTableViewer) getViewer()).getChecked(allChangeItems[i]) == false) {
                uncheckedChangeItems.add(allChangeItems[i]);
            }
        }

        return (ChangeItem[]) uncheckedChangeItems.toArray(new ChangeItem[uncheckedChangeItems.size()]);
    }

    /**
     * Unchecks the given items.
     *
     * @param changeItems
     *        the items to uncheck (must not be <code>null</code>)
     */
    private void uncheckItems(final ChangeItem[] changeItems) {
        Check.notNull(changeItems, "changeItems"); //$NON-NLS-1$

        for (int i = 0; i < changeItems.length; i++) {
            ((CheckboxTableViewer) getViewer()).setChecked(changeItems[i], false);
        }

        computeCheckedElements(true);
    }

    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        final ChangeItem changeItem = (ChangeItem) element;

        final StringBuffer sb = new StringBuffer();

        sb.append(MessageFormat.format(
            Messages.getString("ChangesTable.TooltipNameFolderChangesFormat"), //$NON-NLS-1$
            changeItem.getName(),
            changeItem.getFolder(),
            changeItem.getChangeType().toUIString(true, changeItem.getPropertyValues())));

        if (changeItem.getVersion() != 0) {
            sb.append(MessageFormat.format(
                Messages.getString("ChangesTable.TooltipAdditionalVersionLineFormat"), //$NON-NLS-1$
                String.valueOf(changeItem.getVersion())));
        }

        return sb.toString();
    }

    @Override
    protected Object getTransferData(final Transfer transferType, final Object[] selectedElements) {
        final ChangeItem[] changeItems = (ChangeItem[]) selectedElements;

        if (transferType == FileTransfer.getInstance()) {
            final List paths = new ArrayList();
            for (int i = 0; i < changeItems.length; i++) {
                final PendingChange pendingChange = changeItems[i].getPendingChange();

                if (pendingChange.getLocalItem() != null) {
                    paths.add(pendingChange.getLocalItem());
                }
            }
            return paths.toArray(new String[paths.size()]);
        }

        if (transferType == TextTransfer.getInstance()) {
            final StringBuffer sb = new StringBuffer();
            for (int i = 0; i < changeItems.length; i++) {
                String path;

                if (changeItems[i].getType() == ChangeItemType.CHANGESET) {
                    final Change change = changeItems[i].getChange();
                    path = change.getItem().getServerItem();
                } else {
                    final PendingChange change = changeItems[i].getPendingChange();
                    if (change.getLocalItem() != null) {
                        path = change.getLocalItem();
                    } else {
                        path = change.getServerItem();
                    }
                }

                if (sb.length() > 0) {
                    sb.append(NEWLINE);
                }

                sb.append(path);
            }
            return sb.toString();
        }

        throw new IllegalArgumentException(MessageFormat.format("unsupported transfer type: {0}", transferType)); //$NON-NLS-1$
    }

    private static class LabelProvider extends FolderFileLabelProvider implements ITableLabelProvider {
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
