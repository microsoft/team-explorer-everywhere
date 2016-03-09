// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
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
import com.microsoft.tfs.client.clc.vc.options.OptionDetect;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public final class CommandDelete extends Command {
    public CommandDelete() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        LockLevel l = LockLevel.UNCHANGED;
        final boolean recursive = findOptionType(OptionRecursive.class) != null;
        final boolean detect = findOptionType(OptionDetect.class) != null;

        Option o = null;
        if ((o = findOptionType(OptionLock.class)) != null) {
            l = ((OptionLock) o).getValueAsLockLevel();
        }

        if (!detect && getFreeArguments().length < 1) {
            final String messageFormat = Messages.getString("CommandDelete.DeleteRequiresAtLeastOnePathFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());
            throw new InvalidFreeArgumentException(message);
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        if (detect) {
            String[] items = getFreeArguments();
            RecursionType recursion = recursive ? RecursionType.FULL : RecursionType.NONE;

            // if no item was passed in, we
            if (items.length == 0) {
                items = new String[1];
                items[0] = ServerPath.ROOT;
                recursion = RecursionType.FULL;
            }

            final AtomicReference<PendingChange[]> outCandidateChanges = new AtomicReference<PendingChange[]>();
            workspace.getPendingChangesWithCandidates(
                ItemSpec.fromStrings(items, recursion),
                false,
                outCandidateChanges);

            final PendingChange[] changes = outCandidateChanges.get();
            final List<String> deletes = new ArrayList<String>();

            for (final PendingChange pc : changes) {
                if (pc.getChangeType().contains(ChangeType.DELETE)) {
                    deletes.add(pc.getServerItem());
                }
            }

            if (deletes.size() > 0) {
                workspace.pendDelete(
                    deletes.toArray(new String[deletes.size()]),
                    RecursionType.NONE,
                    l,
                    GetOptions.NONE, // TODO: Option should cause items to be
                                     // deleted.
                    PendChangesOptions.NONE);
            } else {
                getDisplay().printErrorLine(Messages.getString("CommandDelete.NoDeletesDetected")); //$NON-NLS-1$
                setExitCode(ExitCode.PARTIAL_SUCCESS);
                return;
            }
        } else {
            final int changes = workspace.pendDelete(
                getFreeArguments(),
                (recursive) ? RecursionType.FULL : RecursionType.NONE,
                l,
                GetOptions.NONE,
                PendChangesOptions.NONE);

            if (changes == 0) {
                /*
                 * Nothing was deleted. Perhaps none of the arguments resolved
                 * to any files, or an error happened scanning the files.
                 */
                getDisplay().printErrorLine(Messages.getString("CommandDelete.NoArgumentsMatchedForDelete")); //$NON-NLS-1$

                setExitCode(ExitCode.FAILURE);
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[2];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionLock.class,
            OptionRecursive.class
        }, "<itemSpec>..."); //$NON-NLS-1$
        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionLock.class,
            OptionRecursive.class
        }, null, new Class[] {
            OptionDetect.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandDelete.HelpText1") //$NON-NLS-1$
        };
    }
}
