// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportTask;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class TfsImportWizardConfirmationPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "TfsImportWizardConfirmationPage"; //$NON-NLS-1$

    public static final String CONFIRMATION_TABLE_ID = "ImportWizardConfirmationPage.confirmationTable"; //$NON-NLS-1$

    private Label confirmationLabel;
    private ImportWizardConfirmationTable confirmationTable;

    public TfsImportWizardConfirmationPage() {
        super(PAGE_NAME, Messages.getString("ImportWizardConfirmationPage.PageName"), ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        confirmationLabel = new Label(container, SWT.NONE);

        GridDataBuilder.newInstance().hFill().hGrab().applyTo(confirmationLabel);

        confirmationTable = new ImportWizardConfirmationTable(container, SWT.FULL_SELECTION);
        AutomationIDHelper.setWidgetID(confirmationTable.getTable(), CONFIRMATION_TABLE_ID);

        GridDataBuilder.newInstance().fill().grab().applyTo(confirmationTable);
    }

    @Override
    protected void refresh() {
        final ImportFolderCollection folderCollection =
            (ImportFolderCollection) getExtendedWizard().getPageData(ImportItemCollectionBase.class);

        final ImportTask[] importTasks = folderCollection.makeImportTasks();

        if (folderCollection.getFolders().length == 1) {
            confirmationLabel.setText(Messages.getString("ImportWizardConfirmationPage.SingleProjectImportLabelText")); //$NON-NLS-1$
        } else {
            confirmationLabel.setText(Messages.getString("ImportWizardConfirmationPage.MultiProjectImportLabelText")); //$NON-NLS-1$
        }

        confirmationTable.setImportTasks(importTasks);
    }

    private class ImportWizardConfirmationTable extends TableControl {
        protected ImportWizardConfirmationTable(final Composite parent, final int style) {
            super(parent, style, ImportTask.class, null);

            final TableColumnData[] columnData = new TableColumnData[] {
                new TableColumnData(
                    Messages.getString("ImportWizardConfirmationPage.ColumnNameProject"), //$NON-NLS-1$
                    100,
                    0.2F,
                    "project"), //$NON-NLS-1$
                new TableColumnData(
                    Messages.getString("ImportWizardConfirmationPage.ColumnNameServerPath"), //$NON-NLS-1$
                    100,
                    0.8F,
                    "path") //$NON-NLS-1$
            };

            setupTable(true, true, columnData);
            setUseViewerDefaults();
            setEnableTooltips(true);
        }

        public void setImportTasks(final ImportTask[] tasks) {
            setElements(tasks);
        }

        @Override
        public String getColumnText(final Object element, final String identifier) {
            final ImportTask task = (ImportTask) element;
            final String path = task.getServerPath();

            if ("project".equals(identifier)) //$NON-NLS-1$
            {
                return ServerPath.getFileName(path);
            } else if ("path".equals(identifier)) //$NON-NLS-1$
            {
                return path;
            }

            return ""; //$NON-NLS-1$
        }

        @Override
        public String getTooltipText(final Object element, final int columnIndex) {
            final ImportTask task = (ImportTask) element;

            return task.getLocalizedDescription();
        }
    }
}