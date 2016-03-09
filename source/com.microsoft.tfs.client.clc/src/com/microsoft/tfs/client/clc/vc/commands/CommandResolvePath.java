// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedListener;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;

import java.net.MalformedURLException;
import java.text.MessageFormat;

public final class CommandResolvePath
        extends Command
        implements ConflictResolvedListener {

    public CommandResolvePath() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run()
            throws ArgumentException, MalformedURLException, CLCException, LicenseException {

        if (getFreeArguments().length != 1) {
            final String messageFormat = Messages.getString("CommandResolvePath.CommandRequiresOnePathFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        final WorkspaceInfo cachedWorkspace = determineCachedWorkspace();

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final String serverPath = getFreeArguments()[0];
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        if (ServerPath.isServerPath(serverPath) == false) {
            throw new InvalidFreeArgumentException(
                    Messages.getString("CommandResolvePath.FirstFreeArgumentMustBeServerPath")); //$NON-NLS-1$
        }

        final PathTranslation pathTranslation = workspace.translateServerPathToLocalPath(serverPath);

        getDisplay().printLine(pathTranslation.getTranslatedPath());

    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {

        final AcceptedOptionSet withCollectionAndWorkspace = new AcceptedOptionSet(new Class[]
                {
                        OptionCollection.class, OptionWorkspace.class
                }, "<serverPath>"); //$NON-NLS-1$

        final AcceptedOptionSet withDefaults = new AcceptedOptionSet(new Class[]{}, "<serverPath>"); //$NON-NLS-1$

        return new AcceptedOptionSet[]{withCollectionAndWorkspace, withDefaults};
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
                Messages.getString("CommandResolvePath.HelpText"), //$NON-NLS-1$
        };
    }
}
