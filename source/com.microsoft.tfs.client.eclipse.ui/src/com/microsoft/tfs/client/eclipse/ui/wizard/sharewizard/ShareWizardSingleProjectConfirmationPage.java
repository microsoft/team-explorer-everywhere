// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.controls.generic.AccessibilityLabel;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectAction;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectConfiguration;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class ShareWizardSingleProjectConfirmationPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "ShareWizardSingleProjectConfirmationPage"; //$NON-NLS-1$

    public static final String PROJECT_NAME_TEXT_ID = "ShareWizardSingleProjectConfirmationPage.projectNameText"; //$NON-NLS-1$
    public static final String LOCAL_PATH_TEXT_ID = "ShareWizardSingleProjectConfirmationPage.localPathText"; //$NON-NLS-1$
    public static final String SERVER_PATH_TEXT_ID = "ShareWizardSingleProjectConfirmationPage.serverPathText"; //$NON-NLS-1$
    public static final String WORKSPACE_TEXT_ID = "ShareWizardSingleProjectConfirmationPage.workspaceText"; //$NON-NLS-1$

    private SizeConstrainedComposite container;

    private Label confirmationText;
    private Label workspaceLabel;

    private AccessibilityLabel projectNameText;
    private AccessibilityLabel localPathText;
    private AccessibilityLabel serverPathText;
    private AccessibilityLabel workspaceText;

    public ShareWizardSingleProjectConfirmationPage() {
        super(
            PAGE_NAME,
            Messages.getString("ShareWizardSingleProjectConfirmationPage.PageName"), //$NON-NLS-1$
            Messages.getString("ShareWizardSingleProjectConfirmationPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final ShareProjectConfiguration[] configuration =
            (ShareProjectConfiguration[]) getExtendedWizard().getPageData(ShareProjectConfiguration.class);
        Check.notNullOrEmpty(configuration, "configuration"); //$NON-NLS-1$

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
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(confirmationText);

        final Composite dataComposite = new Composite(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().vIndent(15).applyTo(dataComposite);

        final GridLayout dataLayout = new GridLayout(2, false);
        dataLayout.marginWidth = 0;
        dataLayout.marginHeight = 0;
        dataLayout.verticalSpacing = getVerticalSpacing();
        dataLayout.horizontalSpacing = getHorizontalSpacing();
        dataComposite.setLayout(dataLayout);

        final Label projectNameLabel = new Label(dataComposite, SWT.NONE);
        projectNameLabel.setText(Messages.getString("ShareWizardSingleProjectConfirmationPage.ProjectNameLabelText")); //$NON-NLS-1$

        projectNameText = new AccessibilityLabel(dataComposite, SWT.WRAP);
        projectNameText.setAutomationID(PROJECT_NAME_TEXT_ID);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(projectNameText);

        final Label localPathLabel = new Label(dataComposite, SWT.NONE);
        localPathLabel.setText(Messages.getString("ShareWizardSingleProjectConfirmationPage.LocalPathLabelText")); //$NON-NLS-1$

        localPathText = new AccessibilityLabel(dataComposite, SWT.WRAP);
        localPathText.setAutomationID(LOCAL_PATH_TEXT_ID);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(localPathText);

        final Label serverPathLabel = new Label(dataComposite, SWT.NONE);
        serverPathLabel.setText(Messages.getString("ShareWizardSingleProjectConfirmationPage.ServerPathLabelText")); //$NON-NLS-1$

        serverPathText = new AccessibilityLabel(dataComposite, SWT.WRAP);
        serverPathText.setAutomationID(SERVER_PATH_TEXT_ID);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(serverPathText);

        workspaceLabel = new Label(dataComposite, SWT.NONE);
        workspaceLabel.setText(Messages.getString("ShareWizardSingleProjectConfirmationPage.WorkspaceLabelText")); //$NON-NLS-1$

        workspaceText = new AccessibilityLabel(dataComposite, SWT.WRAP);
        workspaceText.setAutomationID(WORKSPACE_TEXT_ID);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceText);
    }

    @Override
    public void refresh() {
        final ShareProjectConfiguration[] configuration =
            (ShareProjectConfiguration[]) getExtendedWizard().getPageData(ShareProjectConfiguration.class);
        Check.notNullOrEmpty(configuration, "configuration"); //$NON-NLS-1$
        Check.isTrue(configuration.length == 1, "configuration.length == 1"); //$NON-NLS-1$

        final ShareProjectAction action = configuration[0].getAction();

        String messageFormat;
        String message;

        if (action == ShareProjectAction.CONNECT) {
            messageFormat = Messages.getString("ShareWizardSingleProjectConfirmationPage.ConnectExplainationFormat"); //$NON-NLS-1$
        } else {
            messageFormat = Messages.getString("ShareWizardSingleProjectConfirmationPage.AddExplanationFormat"); //$NON-NLS-1$

        }

        message = MessageFormat.format(messageFormat, configuration[0].getProject().getName());

        if (action == ShareProjectAction.MAP_AND_UPLOAD || action == ShareProjectAction.UPLOAD) {
            message = message + Messages.getString("ShareWizardSingleProjectConfirmationPage.NeedToCheckin"); //$NON-NLS-1$
        }
        confirmationText.setText(message);

        projectNameText.setText(configuration[0].getProject().getName());
        localPathText.setText(configuration[0].getProject().getLocation().toOSString());
        serverPathText.setText(configuration[0].getServerPath());
        final Workspace workspace = (Workspace) getExtendedWizard().getPageData(Workspace.class);
        if (workspace != null) {
            workspaceText.setText(workspace.getName());
        } else {
            workspaceLabel.setText(""); //$NON-NLS-1$
        }
        container.layout(true);
    }
}