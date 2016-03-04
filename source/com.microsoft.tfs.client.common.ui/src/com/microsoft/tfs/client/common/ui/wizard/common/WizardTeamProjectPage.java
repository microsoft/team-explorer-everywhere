// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.common;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.ConnectionErrorControl;
import com.microsoft.tfs.client.common.ui.controls.connect.TeamProjectSelectControl;
import com.microsoft.tfs.client.common.ui.controls.connect.TeamProjectSelectControl.ProjectSelectionChangedEvent;
import com.microsoft.tfs.client.common.ui.controls.connect.TeamProjectSelectControl.ProjectSelectionChangedListener;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.httpclient.Credentials;

public class WizardTeamProjectPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "WizardTeamProjectPage"; //$NON-NLS-1$

    public static final String PROJECT_COLLECTION_ERROR_MESSAGE = "WizardTeamProjectPage.teamProjectErrorMessage"; //$NON-NLS-1$

    private ConnectionErrorControl errorControl;
    private TeamProjectSelectControl projectSelectControl;

    public WizardTeamProjectPage() {
        super(
            PAGE_NAME,
            Messages.getString("WizardTeamProjectPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("WizardTeamProjectPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        container.setLayout(layout);

        final boolean projectCollectionReadonly = false;

        if (getExtendedWizard().hasPageData(PROJECT_COLLECTION_ERROR_MESSAGE)) {
            final SizeConstrainedComposite errorComposite = new SizeConstrainedComposite(container, SWT.NONE);
            errorComposite.setDefaultSize(parent.getSize().x, SWT.DEFAULT);
            errorComposite.setLayout(new FillLayout());
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(errorComposite);

            errorControl = new ConnectionErrorControl(errorComposite, SWT.NONE);
            errorControl.setMessage((String) getExtendedWizard().getPageData(PROJECT_COLLECTION_ERROR_MESSAGE));

            final Label spacerLabel = new Label(container, SWT.NONE);
            spacerLabel.setText(""); //$NON-NLS-1$
        }

        final ICommandExecutor errorDialogCommandExecutor = getCommandExecutor();

        final ICommandExecutor noErrorDialogCommandExecutor = getCommandExecutor();
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
            UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final SourceControlCapabilityFlags sourceControlCapabilityFlags =
            ((ConnectWizard) getExtendedWizard()).getSourceControlCapabilityFlags();
        projectSelectControl = new TeamProjectSelectControl(container, SWT.NONE, sourceControlCapabilityFlags);
        projectSelectControl.setCommandExecutor(errorDialogCommandExecutor);
        projectSelectControl.setNoErrorDialogCommandExecutor(noErrorDialogCommandExecutor);
        projectSelectControl.setProjectCollectionReadonly(projectCollectionReadonly);
        projectSelectControl.addProjectSelectionChangedListener(new ProjectSelectionChangedListener() {
            @Override
            public void onProjectSelectionChanged(final ProjectSelectionChangedEvent event) {
                if (projectSelectControl.getSelectedProjects().length > 0) {
                    setPageData();
                } else {
                    removePageData();
                }

                setPageComplete(projectSelectControl.getTeamProjectCollection() != null);
            }
        });
        GridDataBuilder.newInstance().grab().fill().applyTo(projectSelectControl);

        setPageComplete(false);
    }

    @Override
    protected boolean onPageFinished() {
        setPageData();
        return true;
    }

    private void setPageData() {
        getExtendedWizard().setPageData(URI.class, projectSelectControl.getTeamProjectCollection().getBaseURI());
        getExtendedWizard().setPageData(
            TFSTeamProjectCollection.class,
            projectSelectControl.getTeamProjectCollection());
        getExtendedWizard().setPageData(ConnectWizard.SELECTED_TEAM_PROJECTS, projectSelectControl.getSelectedProjects());
    }

    private void removePageData() {
        getExtendedWizard().removePageData(URI.class);
        getExtendedWizard().removePageData(TFSTeamProjectCollection.class);
        getExtendedWizard().removePageData(ConnectWizard.SELECTED_TEAM_PROJECTS);
    }

    @Override
    protected void refresh() {
        getExtendedWizard().removePageData(TFSTeamProjectCollection.class);
        getExtendedWizard().removePageData(ConnectWizard.SELECTED_TEAM_PROJECTS);

        if (getExtendedWizard().hasPageData(TFSConnection.class)) {
            final TFSConnection connection = (TFSConnection) getExtendedWizard().getPageData(TFSConnection.class);
            projectSelectControl.setConnection(connection);
        } else if (getExtendedWizard().hasPageData(URI.class)) {
            final Credentials credentials = getExtendedWizard().hasPageData(Credentials.class)
                ? (Credentials) getExtendedWizard().getPageData(Credentials.class) : null;

            if (credentials != null) {
                projectSelectControl.setServer((URI) getExtendedWizard().getPageData(URI.class), credentials);
            }
        }
    }
}
