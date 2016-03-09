// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

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
import com.microsoft.tfs.client.clc.vc.options.OptionNewName;
import com.microsoft.tfs.client.clc.vc.options.OptionNoGet;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public final class CommandUndelete extends Command {
    public CommandUndelete() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length < 1) {
            final String messageFormat = Messages.getString("CommandUndelete.CommandRequiresAtLeastOneItemPathFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        LockLevel l = LockLevel.UNCHANGED;
        final boolean recursive = findOptionType(OptionRecursive.class) != null;
        final boolean noGet = findOptionType(OptionNoGet.class) != null;
        String newName = null;

        Option o = null;

        if ((o = findOptionType(OptionLock.class)) != null) {
            l = ((OptionLock) o).getValueAsLockLevel();
        }

        if ((o = findOptionType(OptionNewName.class)) != null) {
            newName = ((OptionNewName) o).getValue();
        }

        int changes = 0;

        GetOptions options = GetOptions.NONE;
        if (noGet == true) {
            options = options.combine(GetOptions.PREVIEW);
        }

        if (newName != null) {
            if (getFreeArguments().length > 1) {
                throw new InvalidFreeArgumentException(
                    Messages.getString("CommandUndelete.OnlyOneItemNameWithNewname")); //$NON-NLS-1$
            }
            reportBadOptionCombinationIfPresent(OptionNewName.class, OptionRecursive.class);

            final ItemSpec[] items = ItemSpec.fromStrings(getFreeArguments(), RecursionType.NONE);
            Check.isTrue(items.length == 1, "items.length == 1"); //$NON-NLS-1$

            changes = workspace.pendUndelete(items[0], newName, l, options, PendChangesOptions.NONE);
        } else {
            final ItemSpec[] items =
                ItemSpec.fromStrings(getFreeArguments(), (recursive) ? RecursionType.FULL : RecursionType.NONE);

            changes = workspace.pendUndelete(items, l, options, PendChangesOptions.NONE);
        }

        if (changes == 0) {
            /*
             * Nothing was deleted. Perhaps none of the arguments resolved to
             * any files, or an error happened scanning the files.
             */
            getDisplay().printErrorLine(Messages.getString("CommandUndelete.NoArgumentsMatchedAnyToUndeleted")); //$NON-NLS-1$

            setExitCode(ExitCode.FAILURE);
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[2];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionNoGet.class,
            OptionLock.class,
            OptionRecursive.class
        }, "<itemSpec>[;deletionID]..."); //$NON-NLS-1$
        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionNoGet.class,
            OptionLock.class,
            OptionNewName.class
        }, "<itemSpec>[;deletionID]..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandUndelete.HelpText1") //$NON-NLS-1$
        };
    }
}
