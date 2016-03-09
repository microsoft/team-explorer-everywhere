// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.EditCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

/**
 * Base task for checkout (pend edit) tasks.
 */
public abstract class AbstractCheckoutTask extends BaseTask {
    private final TFSRepository repository;

    /**
     * Creates a {@link AbstractCheckoutTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     */
    public AbstractCheckoutTask(final Shell shell, final TFSRepository repository) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
    }

    protected TFSRepository getRepository() {
        return repository;
    }

    public IStatus checkout(final ItemSpec[] itemSpecs, final LockLevel lockLevel, final FileEncoding fileEncoding) {
        /*
         * Query get latest on checkout preference (and ensure server supports
         * the feature)
         */
        PendChangesOptions pendChangesOptions = PendChangesOptions.NONE;

        if (TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.GET_LATEST_ON_CHECKOUT)
            && repository.getWorkspace().getClient().getServerSupportedFeatures().contains(
                SupportedFeatures.GET_LATEST_ON_CHECKOUT)) {
            if (WorkspaceLocation.LOCAL.equals(repository.getWorkspace().getLocation())) {
                TFSCommonUIClientPlugin.getDefault().getConsole().printWarning(
                    Messages.getString("AbstractCheckoutTask.GetLatestIncompatibleWithLocalWorkspace")); //$NON-NLS-1$
            } else {
                pendChangesOptions = PendChangesOptions.GET_LATEST_ON_CHECKOUT;
            }
        }

        /*
         * Query the server's get latest on checkout annotation so we know if we
         * need to query conflicts after pending the edit.
         */
        final boolean queryConflicts = (pendChangesOptions.contains(PendChangesOptions.GET_LATEST_ON_CHECKOUT)
            || hasGetLatestAnnotation(itemSpecs));

        final EditCommand checkoutCommand = new EditCommand(
            repository,
            itemSpecs,
            lockLevel,
            fileEncoding,
            GetOptions.NONE,
            pendChangesOptions,
            queryConflicts);

        /*
         * Use a command executor that does not raise error dialogs so that we
         * don't throw an error dialog on conflicts.
         */
        final ICommandExecutor commandExecutor = getCommandExecutor();
        commandExecutor.setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final IStatus checkoutStatus = commandExecutor.execute(new ResourceChangingCommand(checkoutCommand));

        if (!checkoutStatus.isOK() && checkoutCommand.hasConflicts()) {
            final ConflictDescription[] conflicts = checkoutCommand.getConflictDescriptions();

            final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, conflicts);
            final IStatus conflictStatus = conflictTask.run();

            if (conflictStatus.isOK()) {
                return Status.OK_STATUS;
            }
        } else if (!checkoutStatus.isOK()) {
            ErrorDialog.openError(
                getShell(),
                Messages.getString("AbstractCheckoutTask.ErrorDialogTitle"), //$NON-NLS-1$
                null,
                checkoutStatus);
        }

        return checkoutStatus;
    }

    private boolean hasGetLatestAnnotation(final ItemSpec[] itemSpecs) {
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        final Set<String> projectSet = new HashSet<String>();

        /*
         * Query the server's default get latest on checkout setting
         */
        for (int i = 0; i < itemSpecs.length; i++) {
            final String project = ServerPath.getTeamProject(itemSpecs[i].getItem());

            if (project == null || projectSet.contains(project)) {
                continue;
            }

            projectSet.add(project);

            final String getLatestAnnotation = repository.getAnnotationCache().getAnnotationValue(
                VersionControlConstants.GET_LATEST_ON_CHECKOUT_ANNOTATION,
                project,
                0);

            /* Server get latest on checkout forces us to work synchronously */
            if ("true".equalsIgnoreCase(getLatestAnnotation)) //$NON-NLS-1$
            {
                return true;
            }
        }

        return false;
    }
}
