// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.exceptions.MissingRequiredOptionException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionChangeset;
import com.microsoft.tfs.client.clc.vc.options.OptionLatest;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public final class CommandGetChangeset extends Command implements GetListener {
    public CommandGetChangeset() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run()
        throws ArgumentException,

            MalformedURLException,
            CLCException,
            LicenseException {
        final Option o = findOptionType(OptionChangeset.class);
        if (o == null) {
            throw new MissingRequiredOptionException(
                Messages.getString("CommandGetChangeset.ChangesetOptionMustBeSupplied")); //$NON-NLS-1$
        }

        final VersionSpec[] versions = ((OptionChangeset) o).getParsedVersionSpecs();
        if (versions.length != 1) {
            throw new InvalidOptionValueException(
                Messages.getString("CommandGetChangeset.ExactlyOneChangesetNumberMustBeSupplied")); //$NON-NLS-1$
        } else if (versions[0] instanceof ChangesetVersionSpec == false) {
            throw new InvalidOptionValueException(
                Messages.getString("CommandGetChangeset.OnlyChangesetVersionSpecsAllowed")); //$NON-NLS-1$
        }

        final int changesetNumber = ((ChangesetVersionSpec) versions[0]).getChangeset();

        VersionSpec versionSpec = new ChangesetVersionSpec(changesetNumber);

        if (findOptionType(OptionLatest.class) != null) {
            versionSpec = LatestVersionSpec.INSTANCE;
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        if (versionSpec instanceof LatestVersionSpec) {
            final String messageFormat =
                Messages.getString("CommandGetChangeset.GettingLatestVersionInChangesetFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(changesetNumber));

            getDisplay().printLine(message);
        } else {
            final String messageFormat = Messages.getString("CommandGetChangeset.GettingChangesInChangesetFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(changesetNumber));

            getDisplay().printLine(message);
        }

        /*
         * Only select changes that are mapped.
         */
        final Changeset changeset = client.getChangeset(changesetNumber);
        final Change[] changes = changeset.getChanges();
        final ArrayList getRequests = new ArrayList();
        if (changes != null) {
            for (int i = 0; i < changes.length; i++) {
                final Change thisChange = changes[i];
                if (workspace.isServerPathMapped(thisChange.getItem().getServerItem())) {
                    final ChangeType changeType = thisChange.getChangeType();

                    final String messageFormat =
                        Messages.getString("CommandGetChangeset.ChangeTypeColonServerItemFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        changeType.toUIString(false, thisChange),
                        thisChange.getItem().getServerItem());

                    getDisplay().printLine(message);

                    final GetRequest req = new GetRequest(
                        new ItemSpec(thisChange.getItem().getServerItem(), RecursionType.NONE, 0),
                        versionSpec);

                    getRequests.add(req);
                }
            }
        }

        if (getRequests.size() == 0) {
            getDisplay().printErrorLine(Messages.getString("CommandGetChangeset.ThereAreNoChangesToGet")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        getDisplay().printLine(Messages.getString("CommandGetChangeset.GettingTheChanges")); //$NON-NLS-1$

        final GetStatus status =
            workspace.get((GetRequest[]) getRequests.toArray(new GetRequest[getRequests.size()]), GetOptions.NONE);

        if (status.isNoActionNeeded()) {
            final String messageFormat = Messages.getString("CommandGetChangeset.GotChangesetFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(changesetNumber));
            getDisplay().printLine(message);
        } else {
            if (status.getNumConflicts() > 0) {
                final String messageFormat = Messages.getString("CommandGetChangeset.ConflictsFoundFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, status.getNumConflicts());

                getDisplay().printErrorLine(message);
            }
            if (status.haveResolvableWarnings()) {
                getDisplay().printErrorLine(Messages.getString("CommandGetChangeset.ResolvableWarningsEncountered")); //$NON-NLS-1$
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionLatest.class
        }, "", new Class[] //$NON-NLS-1$
        {
            OptionChangeset.class
        });
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandGetChangeset.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandGetChangeset.HelpText2") //$NON-NLS-1$
        };
    }
}
