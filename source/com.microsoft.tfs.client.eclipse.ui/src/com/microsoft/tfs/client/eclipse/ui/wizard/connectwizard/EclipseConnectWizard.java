// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.connectwizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.commands.QueryLocalWorkspacesCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.commands.GetDefaultWorkspaceCommand;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Extends the default connect wizard to add offline repository and ongoing
 * connection detection.
 */
public abstract class EclipseConnectWizard extends ConnectWizard {
    private final String actionText;

    protected EclipseConnectWizard(
        final String windowTitle,
        final String actionText,
        final ImageDescriptor defaultPageImageDescriptor,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags,
        final int selectionType) {
        this(windowTitle, actionText, defaultPageImageDescriptor, null, sourceControlCapabilityFlags, selectionType);
    }

    protected EclipseConnectWizard(
        final String windowTitle,
        final String actionText,
        final ImageDescriptor defaultPageImageDescriptor,
        final String dialogSettingsKey,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags,
        final int selectionType) {
        super(windowTitle, defaultPageImageDescriptor, dialogSettingsKey, sourceControlCapabilityFlags, selectionType);

        this.actionText = actionText;
    }

    public String getActionText() {
        return actionText;
    }

    @Override
    protected void addConnectionPages() {
        addPage(new WizardConnectionBusyPage());
        addPage(new WizardConnectionOfflinePage());

        super.addConnectionPages();
    }

    @Override
    protected TFSRepository initConnectionPages() {
        /* Make sure that we're not in the process of connecting to a server. */
        if (TFSEclipseClientPlugin.getDefault().getProjectManager().isConnecting() == false
            && TFSEclipseClientUIPlugin.getDefault().getAutoConnector().isConnecting() == false) {
            setPageData(WizardConnectionBusyPage.CONNECTION_AVAILABLE, Boolean.TRUE);
        }

        /*
         * Always set that a connection is online -- we never veto connections
         * when we have offline projects anymore.
         */
        setPageData(WizardConnectionOfflinePage.CONNECTION_ONLINE, Boolean.TRUE);

        return super.initConnectionPages();
    }

    @Override
    protected boolean enableNextConnectionPage(final IWizardPage currentPage) {
        if (WizardConnectionBusyPage.PAGE_NAME.equals(currentPage.getName())) {
            return false;
        }

        if (WizardConnectionOfflinePage.PAGE_NAME.equals(currentPage.getName())) {
            return false;
        }

        return super.enableNextConnectionPage(currentPage);
    }

    @Override
    protected IWizardPage getNextConnectionPage() {
        /*
         * Front-load license pages, even though our base class takes care of
         * this.
         */
        final IWizardPage nextLicensePage = getNextEULAPage();

        if (nextLicensePage != null) {
            return nextLicensePage;
        }

        if (!hasPageData(WizardConnectionBusyPage.CONNECTION_AVAILABLE)) {
            return getPage(WizardConnectionBusyPage.PAGE_NAME);
        }

        if (!hasPageData(WizardConnectionOfflinePage.CONNECTION_ONLINE)) {
            return getPage(WizardConnectionOfflinePage.PAGE_NAME);
        }

        return super.getNextConnectionPage();
    }

    protected Workspace[] getCurrentWorkspaces(final TFSTeamProjectCollection connection) {
        if (connection == null) {
            return null;
        }

        final QueryLocalWorkspacesCommand queryCommand = new QueryLocalWorkspacesCommand(connection);
        if (UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
            queryCommand).getSeverity() != IStatus.OK) {
            return null;
        }
        return queryCommand.getWorkspaces();
    }

    protected Workspace getDefaultWorkspace(final TFSTeamProjectCollection connection) {
        if (connection == null) {
            return null;
        }

        /* get the default workspace */
        final GetDefaultWorkspaceCommand workspaceCommand = new GetDefaultWorkspaceCommand(connection);
        final IStatus workspaceStatus = getCommandExecutor().execute(workspaceCommand);

        if (!workspaceStatus.isOK()) {
            return null;
        }

        return workspaceCommand.getWorkspace();
    }
}
