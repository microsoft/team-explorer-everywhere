// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.vc.options.OptionOwner;
import com.microsoft.tfs.client.clc.vc.printers.ShelvesetPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ShelvesetComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.Check;

public final class CommandShelvesets extends Command {
    private static final String SHELVESETS_ELEMENT_NAME = "shelvesets"; //$NON-NLS-1$

    /**
     * The default long format for the current locale.
     */
    private final DateFormat defaultFormat = SimpleDateFormat.getDateTimeInstance();

    public CommandShelvesets() {
        super();
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length > 1) {
            final String messageFormat =
                Messages.getString("CommandShelvesets.CommandRequiresZeroOrOneShelvesetNamesFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        String ownerArg = null;
        String format = OptionFormat.BRIEF;

        Option o = null;

        if ((o = findOptionType(OptionFormat.class)) != null) {
            format = ((OptionFormat) o).getValue();
        }

        if ((o = findOptionType(OptionOwner.class)) != null) {
            ownerArg = ((OptionOwner) o).getValue();
        }

        /*
         * Pass an empty array of local paths because none of the free arguments
         * is a local path for this command.
         */
        final TFSTeamProjectCollection connection = createConnection(new String[0]);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(new String[0]), client);
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        String shelvesetName = null;
        String shelvesetOwner = null;

        if (getFreeArguments().length == 1) {
            final WorkspaceSpec spec =
                WorkspaceSpec.parse(getFreeArguments()[0], VersionControlConstants.AUTHENTICATED_USER);

            shelvesetName = spec.getName();
            shelvesetOwner = spec.getOwner();

            if (shelvesetName.equalsIgnoreCase("*")) //$NON-NLS-1$
            {
                shelvesetName = null;
            }
        }

        if (ownerArg != null) {
            shelvesetOwner = ownerArg;
        }

        if (shelvesetOwner == null) {
            shelvesetOwner = VersionControlConstants.AUTHENTICATED_USER;
        } else if (shelvesetOwner.equalsIgnoreCase("*")) //$NON-NLS-1$
        {
            shelvesetOwner = null;
        }

        final Shelveset[] shelvesets = client.queryShelvesets(shelvesetName, shelvesetOwner, null);

        if (shelvesets.length == 0) {
            final String displayOwner =
                (shelvesetOwner != null && shelvesetOwner.equals(VersionControlConstants.AUTHENTICATED_USER))
                    ? connection.getAuthorizedIdentity().getDisplayName()
                    : ((shelvesetOwner == null) ? "*" : shelvesetOwner); //$NON-NLS-1$

            final String displayShelvesetName = (shelvesetName == null) ? "*" : shelvesetName; //$NON-NLS-1$

            final String shelvesetPattern = new WorkspaceSpec(displayShelvesetName, displayOwner).toString();

            final String messageFormat = Messages.getString("CommandShelvesets.NoShelvesetsFoundMatchingFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, shelvesetPattern);

            if (shelvesetName == null) {
                getDisplay().printLine(message);
                setExitCode(ExitCode.PARTIAL_SUCCESS);
                return;
            }

            throw new InvalidFreeArgumentException(message);
        }

        Arrays.sort(shelvesets, ShelvesetComparator.INSTANCE);

        if (OptionFormat.DETAILED.equalsIgnoreCase(format)) {
            ShelvesetPrinter.printDetailedShelvesets(
                shelvesets,
                defaultFormat,
                getDisplay(),
                connection.getWorkItemClient());
        } else if (OptionFormat.BRIEF.equalsIgnoreCase(format)) {
            ShelvesetPrinter.printBriefShelvesets(shelvesets, getDisplay());
        } else if (OptionFormat.XML.equalsIgnoreCase(format)) {
            ShelvesetPrinter.printXMLShelvesets(shelvesets, SHELVESETS_ELEMENT_NAME, getDisplay());
        } else {
            final String messageFormat = Messages.getString("CommandShelvesets.UnsupportedOutputFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, format);

            throw new RuntimeException(message);
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionOwner.class,
            OptionFormat.class
        }, "[<shelvesetName>]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandShelvesets.HelpText1") //$NON-NLS-1$
        };
    }
}
