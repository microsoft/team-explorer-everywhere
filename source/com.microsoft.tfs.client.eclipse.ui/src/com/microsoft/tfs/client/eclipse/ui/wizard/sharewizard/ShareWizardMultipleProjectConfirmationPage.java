// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectAction;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectConfiguration;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.util.Check;

public class ShareWizardMultipleProjectConfirmationPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "ShareWizardConfirmationPage"; //$NON-NLS-1$

    public static final String CONFIRMATION_TABLE_ID = "ShareWizardMultipleProjectConfirmationPage.confirmationTable"; //$NON-NLS-1$

    private SizeConstrainedComposite container;

    private Label confirmationText;
    private ShareWizardConfigurationTable configurationTable;

    public ShareWizardMultipleProjectConfirmationPage() {
        super(
            PAGE_NAME,
            Messages.getString("ShareWizardMultipleProjectConfirmationPage.PageName"), //$NON-NLS-1$
            Messages.getString("ShareWizardMultipleProjectConfirmationPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        /* Build a size-constrained container for either confirmation setup */
        container = new SizeConstrainedComposite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        /*
         * Set the growing containers default size to the wizard size. This will
         * encourage the multiline text control to wrap (which it tends not to
         * do, it will only wrap at your display's width.)
         */
        container.setDefaultSize(getShell().getSize().x, SWT.DEFAULT);

        confirmationText = new Label(container, SWT.WRAP | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hGrab().applyTo(confirmationText);

        configurationTable = new ShareWizardConfigurationTable(container, SWT.NONE);
        configurationTable.setText(
            Messages.getString("ShareWizardMultipleProjectConfirmationPage.SelectedProjectsLabelText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(configurationTable.getTable(), CONFIRMATION_TABLE_ID);

        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().vIndent(10).applyTo(configurationTable);
    }

    @Override
    public void refresh() {
        final ShareProjectConfiguration[] configuration =
            (ShareProjectConfiguration[]) getExtendedWizard().getPageData(ShareProjectConfiguration.class);
        Check.notNullOrEmpty(configuration, "configuration"); //$NON-NLS-1$

        /* All actions must currently be identical */
        final ShareProjectAction action = configuration[0].getAction();

        String explanation;
        if (action == ShareProjectAction.CONNECT) {
            explanation = Messages.getString("ShareWizardMultipleProjectConfirmationPage.ConnectConfirmationText"); //$NON-NLS-1$
        } else if (action == ShareProjectAction.MAP_AND_UPLOAD || action == ShareProjectAction.UPLOAD) {
            explanation = Messages.getString("ShareWizardMultipleProjectConfirmationPage.UploadConfirmationText"); //$NON-NLS-1$
        } else {
            explanation = Messages.getString("ShareWizardMultipleProjectConfirmationPage.OtherConfirmationText"); //$NON-NLS-1$
        }

        confirmationText.setText(explanation);

        configurationTable.setConfiguration(configuration);

        container.layout(true);
    }

    private class ShareWizardConfigurationTable extends TableControl {
        protected ShareWizardConfigurationTable(final Composite parent, final int style) {
            super(parent, style | SWT.FULL_SELECTION, ShareProjectConfiguration.class, null);

            final TableColumnData[] columnData = new TableColumnData[] {
                new TableColumnData(
                    Messages.getString("ShareWizardMultipleProjectConfirmationPage.ProjectColumnName"), //$NON-NLS-1$
                    15,
                    0.15F,
                    "project"), //$NON-NLS-1$
                new TableColumnData(
                    Messages.getString("ShareWizardMultipleProjectConfirmationPage.LocalPathColumName"), //$NON-NLS-1$
                    40,
                    0.40F,
                    "localpath"), //$NON-NLS-1$
                new TableColumnData(
                    Messages.getString("ShareWizardMultipleProjectConfirmationPage.ServerPathColumnName"), //$NON-NLS-1$
                    45,
                    0.45F,
                    "serverpath") //$NON-NLS-1$
            };

            setupTable(true, true, columnData);
            setUseViewerDefaults();
            setEnableTooltips(true);
        }

        @Override
        protected String getColumnText(final Object element, final String propertyName) {
            final ShareProjectConfiguration shareProject = (ShareProjectConfiguration) element;

            if (propertyName.equals("project")) //$NON-NLS-1$
            {
                return shareProject.getProject().getName();
            } else if (propertyName.equals("localpath")) //$NON-NLS-1$
            {
                return shareProject.getProject().getLocation().toOSString();
            } else if (propertyName.equals("serverpath")) //$NON-NLS-1$
            {
                return shareProject.getServerPath();
            }

            return Messages.getString("ShareWizardMultipleProjectConfirmationPage.UnknownCellContents"); //$NON-NLS-1$
        }

        @Override
        public String getTooltipText(final Object element, final int columnIndex) {
            final ShareProjectConfiguration shareProject = (ShareProjectConfiguration) element;

            final String messageFormat =
                Messages.getString("ShareWizardMultipleProjectConfirmationPage.ToolTipTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                shareProject.getProject().getName(),
                shareProject.getProject().getLocation().toOSString(),
                shareProject.getServerPath());

            return message;
        }

        public void setConfiguration(final ShareProjectConfiguration[] configuration) {
            setElements(configuration);
        }
    }
}
