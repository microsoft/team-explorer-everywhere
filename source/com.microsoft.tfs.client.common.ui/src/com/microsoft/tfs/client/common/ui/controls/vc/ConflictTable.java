// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelManager;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelProvider;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.LocallyDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeSourceDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeTargetDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ServerDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;

/**
 * A table to hold conflicts. Suitable for use in ConflictView or
 * ConflictDialog.
 */
public class ConflictTable extends Composite implements ISelectionProvider {
    private final List<ConflictDescription> conflictDescriptions = new ArrayList<ConflictDescription>();

    private final Table table;
    private final TableViewer tableViewer;

    public ConflictTable(final Composite parent, final int style) {
        super(parent, SWT.NONE);

        setLayout(new FillLayout());

        tableViewer = new TableViewer(this, style);
        tableViewer.setLabelProvider(new ConflictLabelProvider());
        tableViewer.setContentProvider(new ConflictContentProvider());

        table = tableViewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        tableLayout.addColumnData(new ColumnWeightData(15, 100, true));
        final TableColumn nameTableColumn = new TableColumn(table, SWT.NONE);
        nameTableColumn.setText(Messages.getString("ConflictTable.ColumnHeaderName")); //$NON-NLS-1$

        tableLayout.addColumnData(new ColumnWeightData(40, true));
        final TableColumn pathTableColumn = new TableColumn(table, SWT.NONE);
        pathTableColumn.setText(Messages.getString("ConflictTable.ColumnHeaderPath")); //$NON-NLS-1$

        tableLayout.addColumnData(new ColumnWeightData(10, 60, true));
        final TableColumn typeTableColumn = new TableColumn(table, SWT.NONE);
        typeTableColumn.setText(Messages.getString("ConflictTable.ColumnHeaderType")); //$NON-NLS-1$

        tableLayout.addColumnData(new ColumnWeightData(35, true));
        final TableColumn descriptionTableColumn = new TableColumn(table, SWT.NONE);
        descriptionTableColumn.setText(Messages.getString("ConflictTable.ColumnHeaderDescription")); //$NON-NLS-1$

        tableViewer.setInput(conflictDescriptions);

        final TableViewerSorter sorter = new TableViewerSorter(tableViewer);
        tableViewer.setSorter(sorter);
        sorter.sort(1);

        // hook up tooltips
        final TableTooltipLabelManager tooltipManager =
            new TableTooltipLabelManager(table, new ConflictTableTooltipProvider(), false);

        tooltipManager.addTooltipManager();
    }

    public void setConflictDescriptions(final ConflictDescription[] descriptions) {
        conflictDescriptions.clear();
        conflictDescriptions.addAll(Arrays.asList(descriptions));
        refresh();
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions.toArray(new ConflictDescription[conflictDescriptions.size()]);
    }

    @Override
    public void setMenu(final Menu menu) {
        table.setMenu(menu);
    }

    @Override
    public ISelection getSelection() {
        final ConflictDescription[] selectedElements = getSelectedElements();

        if (selectedElements == null) {
            return StructuredSelection.EMPTY;
        }

        return new StructuredSelection(selectedElements.clone());
    }

    public ConflictDescription[] getSelectedElements() {
        return getConflictDescriptions(table.getSelection());
    }

    @Override
    public void setSelection(final ISelection selection) {
        tableViewer.setSelection(selection);
    }

    public void setSelection(final int index) {
        table.setSelection(index);
    }

    public void setSelection(final int[] indices) {
        table.setSelection(indices);
    }

    private ConflictDescription[] getConflictDescriptions(final TableItem[] tableItems) {
        final ConflictDescription[] descriptions = new ConflictDescription[tableItems.length];

        for (int i = 0; i < tableItems.length; i++) {
            descriptions[i] = (ConflictDescription) tableItems[i].getData();
        }

        return descriptions;
    }

    public void refresh() {
        if (!isDisposed()) {
            tableViewer.refresh();
        }
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        tableViewer.addSelectionChangedListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        tableViewer.removeSelectionChangedListener(listener);
    }

    public void addDoubleClickListener(final IDoubleClickListener listener) {
        tableViewer.addDoubleClickListener(listener);
    }

    public void removeDoubleClickListener(final IDoubleClickListener listener) {
        tableViewer.removeDoubleClickListener(listener);
    }

    public class ConflictTableTooltipProvider implements TableTooltipLabelProvider {
        @Override
        public String getTooltipText(final Object element, final int columnIndex) {
            if (!(element instanceof ConflictDescription)) {
                return null;
            }

            final ConflictDescription description = (ConflictDescription) element;
            final Conflict conflict = description.getConflict();

            String tooltip = ""; //$NON-NLS-1$

            if (conflict.getType() == ConflictType.MERGE) {
                if (conflict.getTheirServerItem() != null) {
                    tooltip += MessageFormat.format(
                        Messages.getString("ConflictTable.TooltipSourceServerPathFormat"), //$NON-NLS-1$
                        conflict.getTheirServerItem());
                }
                if (conflict.getYourServerItem() != null) {
                    tooltip += MessageFormat.format(
                        Messages.getString("ConflictTable.TooltipTargetServerPathFormat"), //$NON-NLS-1$
                        conflict.getYourServerItem());
                }

                if (conflict.getSourceLocalItem() != null) {
                    tooltip += MessageFormat.format(
                        Messages.getString("ConflictTable.TooltipSourceLocalPathFormat"), //$NON-NLS-1$
                        conflict.getSourceLocalItem());
                }
                if (conflict.getTargetLocalItem() != null) {
                    tooltip += MessageFormat.format(
                        Messages.getString("ConflictTable.TooltipTargetLocalPathFormat"), //$NON-NLS-1$
                        conflict.getTargetLocalItem());
                }
            } else {
                if (description.getLocalPath() != null) {
                    tooltip += MessageFormat.format(
                        Messages.getString("ConflictTable.TooltipLocalPathFormat"), //$NON-NLS-1$
                        description.getLocalPath());
                }
                if (description.getServerPath() != null) {
                    tooltip += MessageFormat.format(
                        Messages.getString("ConflictTable.TooltipServerPathFormat"), //$NON-NLS-1$
                        description.getServerPath());
                }
            }

            tooltip += MessageFormat.format(
                Messages.getString("ConflictTable.TooltipConflictTypeFormat"), //$NON-NLS-1$
                description.getName());

            if (conflict.getType() == ConflictType.MERGE) {
                tooltip += MessageFormat.format(
                    Messages.getString("ConflictTable.TooltipSourceVersionFormat"), //$NON-NLS-1$
                    Integer.toString(conflict.getTheirVersion()));
                if (description instanceof MergeSourceDeletedConflictDescription) {
                    tooltip += Messages.getString("ConflictTable.TooltipDeletedSuffix"); //$NON-NLS-1$
                }
                tooltip += "\n"; //$NON-NLS-1$

                tooltip += MessageFormat.format(
                    Messages.getString("ConflictTable.TooltipTargetVersionFormat"), //$NON-NLS-1$
                    Integer.toString(conflict.getYourVersion()));
                if (description instanceof MergeTargetDeletedConflictDescription) {
                    tooltip += Messages.getString("ConflictTable.TooltipDeletedSuffix"); //$NON-NLS-1$
                }
                tooltip += "\n"; //$NON-NLS-1$
            } else {
                tooltip += MessageFormat.format(
                    Messages.getString("ConflictTable.TooltipLocalVersionFormat"), //$NON-NLS-1$
                    Integer.toString(conflict.getYourVersion()));
                if (description instanceof LocallyDeletedConflictDescription) {
                    tooltip += Messages.getString("ConflictTable.TooltipDeletedSuffix"); //$NON-NLS-1$
                }
                tooltip += "\n"; //$NON-NLS-1$

                tooltip += MessageFormat.format(
                    Messages.getString("ConflictTable.TooltipServerVersionFormat"), //$NON-NLS-1$
                    Integer.toString(conflict.getTheirVersion()));
                if (description instanceof ServerDeletedConflictDescription) {
                    tooltip += Messages.getString("ConflictTable.TooltipDeletedSuffix"); //$NON-NLS-1$
                }
                tooltip += "\n"; //$NON-NLS-1$
            }

            tooltip += "\n"; //$NON-NLS-1$
            tooltip += description.getDescription();

            return tooltip;
        }
    }

    private class ConflictLabelProvider extends FolderFileLabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            // safety
            if (!(element instanceof ConflictDescription)) {
                return Messages.getString("ConflictTable.ConflictLabelPartUnknownText"); //$NON-NLS-1$
            }

            final ConflictDescription description = (ConflictDescription) element;
            final String serverPath = description.getServerPath();

            switch (columnIndex) {
                case 0:
                    return (serverPath != null) ? ServerPath.getFileName(serverPath)
                        : Messages.getString("ConflictTable.ConflictLabelPartUnknownText"); //$NON-NLS-1$
                case 1:
                    return (serverPath != null) ? ServerPath.getParent(description.getServerPath())
                        : Messages.getString("ConflictTable.ConflictLabelPartUnknownText"); //$NON-NLS-1$
                case 2:
                    return description.getName();
                case 3:
                    return description.getDescription();
            }

            return Messages.getString("ConflictTable.ConflictLabelPartUnknownText"); //$NON-NLS-1$
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (columnIndex != 0) {
                return null;
            }

            final ConflictDescription description = (ConflictDescription) element;
            final String serverPath = description.getServerPath();

            if (description.getConflict().getYourItemType() == ItemType.FOLDER) {
                return getImageForFolder();
            }

            if (serverPath != null) {
                return getImageForFile(ServerPath.getFileName(serverPath));
            }

            return getImageForFile(null);
        }
    }

    private class ConflictContentProvider implements IStructuredContentProvider {
        @Override
        public Object[] getElements(final Object inputElement) {
            return conflictDescriptions.toArray(new ConflictDescription[conflictDescriptions.size()]);
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        }
    }
}
