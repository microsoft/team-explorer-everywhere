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
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 *         This class is not final so CommandMove can extend it.
 */
public class CommandRename extends Command {
    public CommandRename() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length != 2) {
            final String messageFormat = Messages.getString("CommandRename.CommandRequiresExactlyTwoArgumentsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        Option o = null;
        LockLevel l = LockLevel.UNCHANGED;

        if ((o = findOptionType(OptionLock.class)) != null) {
            l = ((OptionLock) o).getValueAsLockLevel();
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        /*
         * Canonicalize the paths, if they are local paths.
         */
        String sourcePath = getFreeArguments()[0];
        String targetPath = getFreeArguments()[1];

        if (ServerPath.isServerPath(sourcePath) == false) {
            sourcePath = LocalPath.canonicalize(sourcePath);
        }

        if (ServerPath.isServerPath(targetPath) == false) {
            targetPath = LocalPath.canonicalize(targetPath);
        }

        if (workspace.pendRename(sourcePath, targetPath, l, GetOptions.NONE, true, PendChangesOptions.NONE) == 0) {
            setExitCode(ExitCode.FAILURE);
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionLock.class
        }, "<oldItem> <newItem>"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandRename.HelpText1") //$NON-NLS-1$
        };
    }
}
