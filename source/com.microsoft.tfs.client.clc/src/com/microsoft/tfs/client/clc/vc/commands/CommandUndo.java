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
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;

public final class CommandUndo extends Command {
    public CommandUndo() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        /*
         * Only allow one item spec as a free argument.
         */
        if (getFreeArguments().length < 1) {
            final String messageFormat =
                Messages.getString("CommandUndo.CommandRequiresAtLeastOneLocalPathArgumentFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        RecursionType recursion = RecursionType.NONE;

        if (findOptionType(OptionRecursive.class) != null) {
            recursion = RecursionType.FULL;
        }

        /*
         * Disable the workspace option value from being used during connection.
         * This is currently the only way to allow for workspace on other
         * computers to have changes undone. Unfortunately, this breaks the use
         * case: "tf undo $/server/path -workspace:myLocalWorkspace"
         */
        final TFSTeamProjectCollection connection = createConnection(false, true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * If there was a workspace option specified, use that workspace for the
         * undo (it may be on a different computer). Otherwise, determine a
         * local cached workspace from the free arguments.
         */
        final OptionWorkspace option = (OptionWorkspace) findOptionType(OptionWorkspace.class);
        Workspace workspace = null;
        if (option != null) {
            final WorkspaceSpec spec =
                WorkspaceSpec.parse(option.getValue(), VersionControlConstants.AUTHENTICATED_USER);
            workspace = client.queryWorkspace(spec.getName(), spec.getOwner());
            if (workspace == null) {
                final String messageFormat = Messages.getString("CommandUndo.WorkspaceNotFoundOnServerFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, spec);

                throw new CannotFindWorkspaceException(message);
            }
        } else {
            workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);
        }

        final String[] args = getFreeArguments();
        final ItemSpec[] specs = new ItemSpec[args.length];

        for (int i = 0; i < args.length; i++) {
            String localOrServerPath = args[i];

            if (ServerPath.isServerPath(args[i]) == false) {
                localOrServerPath = LocalPath.canonicalize(args[i]);
            }

            specs[i] = new ItemSpec(localOrServerPath, recursion);
        }

        if (workspace.undo(specs) == 0) {
            setExitCode(ExitCode.FAILURE);
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class
        }, "<itemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandUndo.HelpText1") //$NON-NLS-1$
        };
    }
}
