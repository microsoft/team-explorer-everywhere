// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.GenericElementsContentProvider;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Tab used to display server status of item from the context menu, properties,
 * Team Explorer Everywhere.
 */
public class StatusPropertiesTab implements PropertiesTab {
    private Composite noPendingChangesComposite;
    private Composite pendingChangesComposite;
    private TableViewer tableViewer;
    private StackLayout stackLayout;
    private StatusPropertiesControl statusPropertiesControl;

    private final static Log log = LogFactory.getLog(StatusPropertiesTab.class);

    private class StatusPropertiesControl extends BaseControl {
        public StatusPropertiesControl(final Composite parent, final int style) {
            super(parent, style);
            stackLayout = new StackLayout();
            setLayout(stackLayout);

            noPendingChangesComposite = new Composite(this, SWT.NONE);
            final FormLayout formLayout_1 = new FormLayout();
            formLayout_1.spacing = getSpacing();
            formLayout_1.marginWidth = 0;
            formLayout_1.marginHeight = 0;
            noPendingChangesComposite.setLayout(formLayout_1);

            final Label thereAreNoLabel = new Label(noPendingChangesComposite, SWT.NONE);
            final FormData formData_8 = new FormData();
            formData_8.left = new FormAttachment(0, 0);
            formData_8.top = new FormAttachment(0, 0);
            thereAreNoLabel.setLayoutData(formData_8);
            thereAreNoLabel.setText(Messages.getString("StatusPropertiesTab.NoChangesLabelText")); //$NON-NLS-1$

            pendingChangesComposite = new Composite(this, SWT.NONE);
            final FormLayout formLayout = new FormLayout();
            formLayout.spacing = getSpacing();
            formLayout.marginWidth = 0;
            formLayout.marginHeight = 0;
            pendingChangesComposite.setLayout(formLayout);

            Label statusLabel;
            statusLabel = new Label(pendingChangesComposite, SWT.NONE);
            final FormData formData_6 = new FormData();
            formData_6.top = new FormAttachment(0, 0);
            formData_6.left = new FormAttachment(0, 0);
            statusLabel.setLayoutData(formData_6);
            statusLabel.setText(Messages.getString("StatusPropertiesTab.StatusLabelText")); //$NON-NLS-1$

            final Table table = new Table(pendingChangesComposite, SWT.BORDER | SWT.FULL_SELECTION);
            table.setLinesVisible(true);
            table.setHeaderVisible(true);
            createTableColumns(table);
            final FormData formData_7 = new FormData();
            formData_7.bottom = new FormAttachment(100, 0);
            formData_7.top = new FormAttachment(statusLabel, 0, SWT.BOTTOM);
            formData_7.right = new FormAttachment(100, 0);
            formData_7.left = new FormAttachment(0, 0);
            table.setLayoutData(formData_7);

            tableViewer = new TableViewer(table);
            tableViewer.setContentProvider(new GenericElementsContentProvider());
            tableViewer.setLabelProvider(new TableLabelProvider());
            tableViewer.setInput(new Object());

            stackLayout.topControl = noPendingChangesComposite;
        }
    }

    private void createTableColumns(final Table table) {
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        column1.setText(Messages.getString("StatusPropertiesTab.ColumNameUser")); //$NON-NLS-1$
        column1.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        column2.setText(Messages.getString("StatusPropertiesTab.ChangeNameChangeType")); //$NON-NLS-1$
        column2.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column3 = new TableColumn(table, SWT.NONE);
        column3.setText(Messages.getString("StatusPropertiesTab.ColumnNameWorkspace")); //$NON-NLS-1$
        column3.setResizable(true);
    }

    private static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final PendingSet pendingSet = (PendingSet) element;
            switch (columnIndex) {
                case 0: // user
                    return pendingSet.getOwnerName();

                case 1: // change type
                    final PendingChange[] changes = pendingSet.getPendingChanges();
                    if (changes.length > 0) {
                        return changes[0].getChangeType().toUIString(false, changes[0]);
                    } else {
                        return null;
                    }

                case 2: // workspace
                    return pendingSet.getName();
            }
            return null;
        }
    }

    @Override
    public void populate(final TFSRepository repository, final TFSItem item) {
        if (item != null) {
            populateStatus(repository, item.getPath());
        }
    }

    @Override
    public void populate(final TFSRepository repository, final ItemIdentifier itemId) {
        if (itemId != null) {
            populateStatus(repository, itemId.getItem());
        }
    }

    private void populateStatus(final TFSRepository repository, final String itemPath) {
        try {
            final PendingSet[] pendingSets = getAllPendingSetsForItem(repository, itemPath);

            if (pendingSets == null || pendingSets.length == 0) {
                stackLayout.topControl = noPendingChangesComposite;
            } else {
                tableViewer.setInput(pendingSets);
                stackLayout.topControl = pendingChangesComposite;
            }
            statusPropertiesControl.layout();
        } catch (final Exception ex) {
            final String messageFormat = "Error populating the status for {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, itemPath);
            log.error(message, ex);
        }
    }

    /**
     * returns all pending changes for that item (local+server)
     *
     *
     * @param repository
     * @param itemPath
     * @return
     */
    private PendingSet[] getAllPendingSetsForItem(final TFSRepository repository, final String itemPath) {
        final ArrayList<PendingSet> localAndServerPendingSets = new ArrayList<PendingSet>();

        final Workspace workspace = repository.getWorkspace();

        // Get local pending changes
        if (workspace.isLocalWorkspace()) {
            final PendingSet[] localPendingSets = getLocalPendingSetsForItem(workspace, itemPath);

            if (localPendingSets != null) {
                localAndServerPendingSets.addAll(Arrays.asList(localPendingSets));
            }

        }

        // Get server pending changes
        final PendingSet[] serverPendingSets = getServerPendingSetsForItem(workspace, itemPath);

        if (serverPendingSets != null) {
            localAndServerPendingSets.addAll(Arrays.asList(serverPendingSets));
        }

        return localAndServerPendingSets.toArray(new PendingSet[localAndServerPendingSets.size()]);
    }

    /**
     * returns pending changes for the current workspace
     *
     *
     * @param repository
     * @param itemPath
     * @return
     */
    private PendingSet[] getLocalPendingSetsForItem(final Workspace workspace, final String itemPath) {

        return workspace.queryPendingSets(new String[] {
            itemPath
        }, RecursionType.NONE, workspace.getName(), workspace.getOwnerName(), false);
    }

    /**
     * returns server pending changes
     *
     *
     * @param repository
     * @param itemPath
     * @return
     */
    private PendingSet[] getServerPendingSetsForItem(final Workspace workspace, final String itemPath) {

        return workspace.queryPendingSets(new String[] {
            itemPath
        }, RecursionType.NONE, null, null, false);
    }

    @Override
    public String getTabItemText() {
        return Messages.getString("StatusPropertiesTab.TabItemText"); //$NON-NLS-1$
    }

    @Override
    public Control setupTabItemControl(final Composite parent) {
        statusPropertiesControl = new StatusPropertiesControl(parent, SWT.NONE);
        return statusPropertiesControl;
    }

    @Override
    public boolean okPressed() {
        return true;
    }
}
