// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties.branches;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;

public class BranchesPropertiesControl extends BaseControl {
    private final Table table;
    private final TableTree tableTree;

    private final TableTreeViewer tableTreeViewer;
    private final StackLayout stackLayout;
    private final Composite branchHistoryComposite;
    private final Composite noBranchHistoryComposite;

    private final Composite errorComposite;
    private final Label errorLabel;

    /**
     * Create the composite
     *
     * @param parent
     * @param style
     */
    public BranchesPropertiesControl(final Composite parent, final int style) {
        super(parent, style);
        stackLayout = new StackLayout();
        setLayout(stackLayout);

        /* No branch page */
        noBranchHistoryComposite = new Composite(this, SWT.NONE);

        final GridLayout noBranchHistoryCompositeLayout = new GridLayout();
        noBranchHistoryCompositeLayout.marginWidth = 0;
        noBranchHistoryCompositeLayout.marginHeight = 0;
        noBranchHistoryCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        noBranchHistoryCompositeLayout.verticalSpacing = getVerticalSpacing();
        noBranchHistoryComposite.setLayout(noBranchHistoryCompositeLayout);

        final Label noBranchesLabel = new Label(noBranchHistoryComposite, SWT.NONE);
        noBranchesLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        noBranchesLabel.setText(Messages.getString("BranchesPropertiesControl.NoBranchesLabelText")); //$NON-NLS-1$

        /* Error page */
        errorComposite = new Composite(this, SWT.NONE);

        final GridLayout errorCompositeLayout = new GridLayout();
        errorCompositeLayout.marginWidth = 0;
        errorCompositeLayout.marginHeight = 0;
        errorCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        errorCompositeLayout.verticalSpacing = getVerticalSpacing();
        errorComposite.setLayout(errorCompositeLayout);

        errorLabel = new Label(errorComposite, SWT.NONE);
        errorLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        errorLabel.setText(Messages.getString("BranchesPropertiesControl.ErrorLabelText")); //$NON-NLS-1$

        /* Message page */
        final Composite messageComposite = new Composite(this, SWT.NONE);

        final GridLayout messageCompositeLayout = new GridLayout();
        messageCompositeLayout.marginWidth = 0;
        messageCompositeLayout.marginHeight = 0;
        messageCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        messageCompositeLayout.verticalSpacing = getVerticalSpacing();
        messageComposite.setLayout(messageCompositeLayout);

        final Label messagesLabel = new Label(messageComposite, SWT.NONE);
        messagesLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        messagesLabel.setText(Messages.getString("BranchesPropertiesControl.MessagesLabelText")); //$NON-NLS-1$

        /* Branch history page */
        branchHistoryComposite = new Composite(this, SWT.NONE);

        final GridLayout branchHistoryCompositeLayout = new GridLayout();
        branchHistoryCompositeLayout.marginWidth = 0;
        branchHistoryCompositeLayout.marginHeight = 0;
        branchHistoryCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        branchHistoryCompositeLayout.verticalSpacing = getVerticalSpacing();
        branchHistoryComposite.setLayout(branchHistoryCompositeLayout);

        final Label branchesLabel = new Label(branchHistoryComposite, SWT.NONE);
        branchesLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        branchesLabel.setText(Messages.getString("BranchesPropertiesControl.BranchesLabelText")); //$NON-NLS-1$

        tableTreeViewer = new TableTreeViewer(branchHistoryComposite, SWT.BORDER);
        tableTree = tableTreeViewer.getTableTree();
        table = tableTree.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        createTableColumns(table);

        tableTree.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        tableTreeViewer.setContentProvider(new BranchesPropertiesContentProvider());
        tableTreeViewer.setLabelProvider(new BranchesPropertiesLabelProvider());

        final BranchHistoryTreeItem dummyItem = new BranchHistoryTreeItem();
        final Item item = new Item();
        item.setChangeSetID(0);
        item.setServerItem(""); //$NON-NLS-1$
        dummyItem.setItem(item);
        tableTreeViewer.setInput(dummyItem);

        stackLayout.topControl = messageComposite;
    }

    public void setError(final IStatus error) {
        if (error != null && error.getMessage() != null) {
            errorLabel.setText(error.getMessage());
        }

        stackLayout.topControl = errorComposite;

        this.layout();
    }

    public void setInput(final BranchHistory branchHistory) {
        if (branchHistory == null || !branchHistory.hasChildren()) {
            stackLayout.topControl = noBranchHistoryComposite;
        } else {
            tableTreeViewer.setInput(branchHistory);
            tableTreeViewer.expandAll();
            // TODO - select current ?
            stackLayout.topControl = branchHistoryComposite;
        }

        this.layout();
    }

    private void createTableColumns(final Table table) {
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        tableLayout.addColumnData(new ColumnWeightData(5, true));
        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        column1.setText(Messages.getString("BranchesPropertiesControl.ColumNameFileName")); //$NON-NLS-1$
        column1.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(3, true));
        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        column2.setText(Messages.getString("BranchesPropertiesControl.ColumnNameChange")); //$NON-NLS-1$
        column2.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column3 = new TableColumn(table, SWT.NONE);
        column3.setText(Messages.getString("BranchesPropertiesControl.ColumnNameBranchedFrom")); //$NON-NLS-1$
        column3.setResizable(true);

    }

}
