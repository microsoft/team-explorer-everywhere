// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.repository.ResourceRepositoryMap;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Remaps one server path to another, in a different branch, efficiently.
 */
public class SwitchToBranchCommand extends TFSCommand {

    private final Workspace workspace;
    private final String fromServerPath;
    private final String toServerPath;
    private final String localPath;
    private final IResource localResource;

    public SwitchToBranchCommand(
        final Workspace workspace,
        final IResource localResource,
        final String fromServerPath,
        final String toServerPath) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(localResource, "localResource"); //$NON-NLS-1$
        Check.notNullOrEmpty(fromServerPath, "fromServerPath"); //$NON-NLS-1$
        Check.notNullOrEmpty(toServerPath, "toServerPath"); //$NON-NLS-1$
        this.workspace = workspace;
        this.localResource = localResource;
        localPath = localResource.getLocation().toOSString();
        this.fromServerPath = fromServerPath;
        this.toServerPath = toServerPath;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("SwitchToBranchCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localResource.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("SwitchToBranchCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("SwitchToBranchCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, localResource.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {

        final WorkingFolder[] workingFolders = workspace.getFolders();
        boolean switchFolderFound = false;

        // First check that server path exists.
        final Item item = workspace.getClient().getItem(toServerPath, LatestVersionSpec.INSTANCE);
        if (item == null) {
            final String messageFormat = Messages.getString("SwitchToBranchCommand.ServerPathDoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, toServerPath);
            return new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, message, null);
        }

        // Check to see if you have the files in that server path already
        // downloaded in your workspace.
        if (workspace.isServerPathMapped(toServerPath)) {
            final ItemSet[] itemSets = workspace.getClient().getItems(new ItemSpec[] {
                new ItemSpec(toServerPath, RecursionType.ONE_LEVEL)
            }, new WorkspaceVersionSpec(workspace), DeletedState.NON_DELETED, ItemType.FILE, false);

            if (itemSets != null && itemSets[0].getItems().length > 0) {
                // We have files in our workspace - throw error.
                final String messageFormat =
                    Messages.getString("SwitchToBranchCommand.ServerPathAlreadyDownloadedFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, toServerPath);
                return new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, message, null);
            }
        }

        for (int i = 0; i < workingFolders.length; i++) {
            if (workingFolders[i].getServerItem().equals(fromServerPath)
                && workingFolders[i].getLocalItem().equals(localPath)) {
                workingFolders[i].setServerItem(toServerPath);
                switchFolderFound = true;
                break;
            }
        }

        if (!switchFolderFound) {
            return Status.CANCEL_STATUS;
        }

        workspace.update(null, null, workingFolders);

        workspace.get(new GetRequest[] {
            new GetRequest(new ItemSpec(localPath, RecursionType.FULL), LatestVersionSpec.INSTANCE)
        }, GetOptions.REMAP);

        /*
         * Refresh the cached server information (used for label decoration,
         * etc).
         */

        // Calculate the TFSRepository used for this resource
        final ResourceRepositoryMap map = PluginResourceHelpers.mapResources(new IResource[] {
            localResource
        });

        final TFSRepository repository = map.getRepository(localResource);
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        TFSEclipseClientPlugin.getDefault().getResourceDataManager().refreshAsync(repository, new IProject[] {
            localResource.getProject()
        });

        return Status.OK_STATUS;
    }
}
