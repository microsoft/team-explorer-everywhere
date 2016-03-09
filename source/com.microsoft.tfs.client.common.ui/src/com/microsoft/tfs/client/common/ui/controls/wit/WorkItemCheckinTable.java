// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.ComboBoxCellEditorHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.table.TableViewerUtils;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;

public class WorkItemCheckinTable extends TableControl {
    public static final int CHECKIN_ACTION = (1 << 24);

    public static final String[] EXTRA_FIELDS = {
        CoreFieldReferenceNames.TITLE,
        CoreFieldReferenceNames.STATE
    };

    public WorkItemCheckinTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public WorkItemCheckinTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, checkStyle(style), WorkItemCheckinInfo.class, viewDataKey);

        TableColumnData[] columnData;

        /*
         * Historic changeset details: note that SWT.READ_ONLY is set for
         * shelveset details, but we want the check-in action column
         */
        if ((style & SWT.READ_ONLY) == SWT.READ_ONLY && (style & CHECKIN_ACTION) == 0) {
            columnData = new TableColumnData[] {
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameType"), 100, 0.4F, "type"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameId"), 75, 0.02F, "id"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameTitle"), 100, 0.9F, "title"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameState"), 100, 0.04F, "state") //$NON-NLS-1$ //$NON-NLS-2$
            };
        }
        /* Pending checkins OR shelveset details */
        else {
            columnData = new TableColumnData[] {
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameType"), 100, 0.03F, "type"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameId"), 75, 0.01F, "id"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameTitle"), 100, 0.9F, "title"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameState"), 100, 0.03F, "state"), //$NON-NLS-1$ //$NON-NLS-2$
                new TableColumnData(Messages.getString("WorkItemCheckinTable.ColumnNameAction"), 100, 0.03F, "action") //$NON-NLS-1$ //$NON-NLS-2$
            };
        }

        setupTable(true, true, columnData);

        setUseViewerDefaults();
        setEnableTooltips(true);

        if ((style & SWT.READ_ONLY) == 0) {
            final CheckboxTableViewer viewer = (CheckboxTableViewer) getViewer();

            new WorkItemActionCellEditor(viewer);
            viewer.addCheckStateListener(new WorkItemCheckListener());
        }
    }

    private final static int checkStyle(final int style) {
        return (style & (SWT.MULTI | SWT.CHECK | SWT.FULL_SELECTION));
    }

    public void setWorkItems(final WorkItem[] workItems) {
        Check.notNull(workItems, "workItems"); //$NON-NLS-1$

        final WorkItemCheckinInfo[] info = new WorkItemCheckinInfo[workItems.length];

        /* Convert to WorkItemCheckinInfos with no association */
        for (int i = 0; i < workItems.length; i++) {
            info[i] = new WorkItemCheckinInfo(workItems[i]);
        }

        setWorkItems(info);
    }

    public void setWorkItems(final WorkItemCheckinInfo[] workItems) {
        setElements(workItems);
    }

    public WorkItemCheckinInfo[] getWorkItems() {
        return (WorkItemCheckinInfo[]) getElements();
    }

    public void setSelectedWorkItem(final WorkItemCheckinInfo item) {
        setSelectedElement(item);
    }

    public WorkItemCheckinInfo getSelectedWorkItem() {
        return (WorkItemCheckinInfo) getSelectedElement();
    }

    public void setSelectedWorkItems(final WorkItemCheckinInfo[] items) {
        setSelectedElements(items);
    }

    public WorkItemCheckinInfo[] getSelectedWorkItems() {
        return (WorkItemCheckinInfo[]) getSelectedElements();
    }

    public void setCheckedWorkItems(final WorkItemCheckinInfo[] items) {
        setCheckedElements(items);
    }

    public WorkItemCheckinInfo[] getCheckedWorkItems() {
        return (WorkItemCheckinInfo[]) getCheckedElements();
    }

    public void clearActions() {
        final WorkItemCheckinInfo[] workItems = getWorkItems();

        final List updatedList = new ArrayList();
        for (int i = 0; i < workItems.length; i++) {
            if (workItems[i].getAction() != null) {
                workItems[i].clearAction();
                updatedList.add(workItems[i]);
            }
        }

        final WorkItemCheckinInfo[] changed =
            (WorkItemCheckinInfo[]) updatedList.toArray(new WorkItemCheckinInfo[updatedList.size()]);

        getViewer().update(changed, new String[] {
            "action" //$NON-NLS-1$
        });
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final WorkItemCheckinInfo workItemInfo = (WorkItemCheckinInfo) element;

        if ("type".equals(columnPropertyName)) //$NON-NLS-1$
        {
            return workItemInfo.getWorkItem().getType().getName();
        } else if ("id".equals(columnPropertyName)) //$NON-NLS-1$
        {
            return String.valueOf(workItemInfo.getWorkItem().getFields().getID());
        } else if ("title".equals(columnPropertyName)) //$NON-NLS-1$
        {
            return (String) workItemInfo.getWorkItem().getFields().getField(CoreFieldReferenceNames.TITLE).getValue();
        } else if ("state".equals(columnPropertyName)) //$NON-NLS-1$
        {
            return (String) workItemInfo.getWorkItem().getFields().getField(CoreFieldReferenceNames.STATE).getValue();
        } else if ("action".equals(columnPropertyName)) //$NON-NLS-1$
        {
            return workItemInfo.getActionString();
        }

        return Messages.getString("WorkItemCheckinTable.UnknownActionString"); //$NON-NLS-1$
    }

    private class WorkItemActionCellEditor extends ComboBoxCellEditorHelper {
        public WorkItemActionCellEditor(final TableViewer viewer) {
            super(viewer, TableViewerUtils.columnPropertyNameToColumnIndex("action", true, viewer)); //$NON-NLS-1$
        }

        @Override
        protected boolean shouldAllowEdit(final Object element) {
            final WorkItemCheckinInfo[] selection = getCheckedWorkItems();

            for (int i = 0; i < selection.length; i++) {
                if (selection[i].equals(element)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected String[] getAvailableOptions(final Object element) {
            return ((WorkItemCheckinInfo) element).getAvailableActionStrings();
        }

        @Override
        protected String getSelectedOption(final Object element) {
            return ((WorkItemCheckinInfo) element).getActionString();
        }

        @Override
        protected void setSelectedOption(final Object element, final String option) {
            ((WorkItemCheckinInfo) element).setActionFromString(option);
        }
    }

    private class WorkItemCheckListener implements ICheckStateListener {
        @Override
        public void checkStateChanged(final CheckStateChangedEvent event) {
            /*
             * Don't rely on the event to tell us what item was checked - it
             * will only ever tell us one item, when in fact, you could check
             * multiple.
             */
            final WorkItemCheckinInfo[] items = getWorkItems();
            final WorkItemCheckinInfo[] checked = getCheckedWorkItems();
            final List changedItemList = new ArrayList();

            for (int i = 0; i < items.length; i++) {
                boolean isChecked = false;

                for (int j = 0; j < checked.length; j++) {
                    if (items[i] == checked[j]) {
                        isChecked = true;
                        break;
                    }
                }

                if (isChecked && items[i].getAction() == null) {
                    items[i].setActionToDefault();
                    changedItemList.add(items[i]);
                } else if (!isChecked && items[i].getAction() != null) {
                    items[i].clearAction();
                    changedItemList.add(items[i]);
                }
            }

            final WorkItemCheckinInfo[] changedItems =
                (WorkItemCheckinInfo[]) changedItemList.toArray(new WorkItemCheckinInfo[changedItemList.size()]);
            getViewer().update(changedItems, new String[] {
                "action" //$NON-NLS-1$
            });
        }
    }
}
