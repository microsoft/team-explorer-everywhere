// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.commands.helpers.ItemSpecHelper;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.vc.VersionSpecHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class GetCommand extends TFSConnectedCommand {
    private final TFSRepository repository;

    private final GetRequest[] getRequests;
    private final GetOptions getOptions;
    private final boolean queryConflicts = true;

    private final NonFatalCommandHelper nonFatalHelper;

    private GetStatus getStatus;
    private boolean hasConflicts;
    private ConflictDescription[] conflictDescriptions;

    public GetCommand(final TFSRepository repository, final GetRequest[] getRequests, final GetOptions getOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(getRequests, "getRequests"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.getRequests = getRequests;
        this.getOptions = getOptions;

        setCancellable(true);
        setConnection(repository.getConnection());

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    @Override
    public String getName() {
        return (Messages.getString("GetCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (getRequests.length == 1) {
            final ItemSpec itemSpec = getRequests[0].getItemSpec();
            return (MessageFormat.format(
                "Getting {0} ({1}{2, choice, 0#|1#, recursive}{3, choice, 0#|1#, force}{4, choice, 0#|1#, overwrite}{5, choice, 0#|1#, preview}{6, choice, 0#|1#, remap})", //$NON-NLS-1$
                (itemSpec == null) ? "ALL" : itemSpec.getItem(), //$NON-NLS-1$
                VersionSpecHelper.getVersionSpecDescription(getRequests[0]),
                (itemSpec == null) ? 1 : itemSpec.getRecursionType().equals(RecursionType.NONE) ? 0 : 1,
                getOptions.contains(GetOptions.GET_ALL) ? 1 : 0,
                getOptions.contains(GetOptions.OVERWRITE) ? 1 : 0,
                getOptions.contains(GetOptions.PREVIEW) ? 1 : 0,
                getOptions.contains(GetOptions.REMAP) ? 1 : 0));
        } else {
            return (MessageFormat.format(
                "Getting {0} items ({1}{2, choice, 0#|1#, force}{3, choice, 0#|1#, overwrite}{4, choice, 0#|1#, preview}{5, choice, 0#|1#, remap})", //$NON-NLS-1$
                getRequests.length,
                VersionSpecHelper.getVersionSpecDescription(getRequests[0]),
                getOptions.contains(GetOptions.GET_ALL) ? 1 : 0,
                getOptions.contains(GetOptions.OVERWRITE) ? 1 : 0,
                getOptions.contains(GetOptions.PREVIEW) ? 1 : 0,
                getOptions.contains(GetOptions.REMAP) ? 1 : 0));
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            getStatus = repository.getWorkspace().get(getRequests, getOptions);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (progressMonitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        if (getStatus.getNumConflicts() > 0) {
            hasConflicts = true;

            if (queryConflicts) {
                final SubProgressMonitor conflictsMonitor = new SubProgressMonitor(progressMonitor, 1);

                ItemSpec[] itemSpecs;
                if (getRequests.length == 1 && getRequests[0].getItemSpec() == null) {
                    // The get operation was for the entire workspace. The
                    // conflicts query should be against the whole workspace
                    // too. Null itemSpecs is used to indicate the query applies
                    // to the whole workspace.
                    itemSpecs = null;
                } else {
                    itemSpecs = ItemSpecHelper.getItemSpecs(getRequests);
                }

                final QueryConflictsCommand conflictsCommand = new QueryConflictsCommand(repository, itemSpecs);
                final IStatus conflictsStatus = conflictsCommand.run(conflictsMonitor);

                if (!conflictsStatus.isOK()) {
                    return conflictsStatus;
                }

                conflictDescriptions = conflictsCommand.getConflictDescriptions();

                /*
                 * The server can decline to add local conflicts that we've
                 * notified them of.
                 */
                if (conflictDescriptions.length == 0) {
                    hasConflicts = false;
                }
            }
        }

        if (progressMonitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        if (nonFatalHelper.hasNonFatals()) {
            final int severity = getStatus.getNumOperations() == 0 ? IStatus.ERROR : IStatus.WARNING;
            return nonFatalHelper.getMultiStatus(severity, Messages.getString("GetCommand.SomeFilesNotUpdated")); //$NON-NLS-1$
        } else if (hasConflicts) {
            return new Status(
                IStatus.WARNING,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("GetCommand.ConflictsOccurredGettingFiles"), //$NON-NLS-1$
                null);
        }

        return Status.OK_STATUS;
    }

    public GetStatus getGetStatus() {
        return getStatus;
    }

    public boolean hasConflicts() {
        return hasConflicts;
    }

    public ConflictDescription[] getConflictDescriptions() {
        return conflictDescriptions;
    }
}
