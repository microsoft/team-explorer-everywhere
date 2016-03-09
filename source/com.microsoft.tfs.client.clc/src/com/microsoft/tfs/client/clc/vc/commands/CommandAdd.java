// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionNoIgnore;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionSilent;
import com.microsoft.tfs.client.clc.vc.options.OptionType;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

public final class CommandAdd extends Command {
    public CommandAdd() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        if (workspace.getLocation() != WorkspaceLocation.LOCAL && getFreeArguments().length < 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandAdd.AddRequiresAtLeastOneLocalPath")); //$NON-NLS-1$
        }

        LockLevel lockLevel = LockLevel.UNCHANGED;
        FileEncoding encoding = null;
        final boolean recursive = findOptionType(OptionRecursive.class) != null;
        PendChangesOptions pendOptions = PendChangesOptions.APPLY_LOCAL_ITEM_EXCLUSIONS;

        Option o = null;

        if ((o = findOptionType(OptionType.class)) != null) {
            encoding = ((OptionType) o).getValueAsEncoding();
        }

        if ((o = findOptionType(OptionLock.class)) != null) {
            lockLevel = ((OptionLock) o).getValueAsLockLevel();
        }

        if (findOptionType(OptionNoIgnore.class) != null) {
            pendOptions = pendOptions.remove(PendChangesOptions.APPLY_LOCAL_ITEM_EXCLUSIONS);
        }

        if (findOptionType(OptionSilent.class) != null) {
            pendOptions = pendOptions.combine(PendChangesOptions.SILENT);
        }

        if (getFreeArguments().length > 0) {
            final int changes =
                workspace.pendAdd(getFreeArguments(), recursive, encoding, lockLevel, GetOptions.NONE, pendOptions);

            if (changes == 0) {
                /*
                 * Nothing was added. Perhaps none of the arguments resolved to
                 * any files, or an error happened scanning the files.
                 */
                getDisplay().printLine(Messages.getString("CommandAdd.NoArgumentsMatchedAnyFiles")); //$NON-NLS-1$

                setExitCode(ExitCode.SUCCESS);
            }
        } else {
            final ItemSpec[] itemSpecs = new ItemSpec[1];
            itemSpecs[0] = new ItemSpec(ServerPath.ROOT, RecursionType.FULL);
            final AtomicReference<PendingChange[]> outCandidateChanges = new AtomicReference<PendingChange[]>();

            workspace.getPendingChangesWithCandidates(itemSpecs, false, outCandidateChanges);
            final PendingChange[] candidateChanges = outCandidateChanges.get();

            final List<String> itemsToAdd = new ArrayList<String>();
            for (final PendingChange change : candidateChanges) {
                if (change.getChangeType().contains(ChangeType.ADD)) {
                    itemsToAdd.add(change.getLocalItem());
                }
            }

            if (itemsToAdd.size() > 0) {
                // Force exclusion list usage in this case.
                pendOptions = pendOptions.combine(PendChangesOptions.APPLY_LOCAL_ITEM_EXCLUSIONS);

                workspace.pendAdd(
                    itemsToAdd.toArray(new String[itemsToAdd.size()]),
                    false /* recursive */,
                    encoding,
                    lockLevel,
                    GetOptions.NONE,
                    pendOptions);
            } else {
                getDisplay().printErrorLine(Messages.getString("CommandAdd.NoAddsDetected")); //$NON-NLS-1$
                setExitCode(ExitCode.PARTIAL_SUCCESS);
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionLock.class,
            OptionType.class,
            OptionRecursive.class,
            OptionSilent.class,
            OptionNoIgnore.class
        }, "<localItemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandAdd.HelpText1") //$NON-NLS-1$
        };
    }
}
