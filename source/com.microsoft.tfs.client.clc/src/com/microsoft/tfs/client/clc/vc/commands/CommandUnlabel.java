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
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.printers.LabelResultPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.Check;

public final class CommandUnlabel extends Command {
    public CommandUnlabel() {
        super();
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length < 1) {
            final String messageFormat = Messages.getString("CommandUnlabel.CommandRequiresOneLabelNameFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        RecursionType recursion = RecursionType.NONE;

        if (findOptionType(OptionRecursive.class) != null) {
            recursion = RecursionType.FULL;
        }

        final LabelSpec spec = LabelSpec.parse(getFreeArguments()[0], null, true);

        final QualifiedItem[] items = parseQualifiedItems(null, true, 1);

        /*
         * If there aren't items.
         */
        if (items.length == 0) {
            throw new InvalidFreeArgumentException(
                Messages.getString("CommandUnlabel.YouMustSpecifiyAnItemToRemoveLabelFrom")); //$NON-NLS-1$
        }

        /*
         * Prepare the subset of the free arguments that can be paths so
         * createConnection() searches the correct arguments.
         */
        final String[] pathFreeArguments = getLastFreeArguments(1);

        final TFSTeamProjectCollection connection = createConnection(pathFreeArguments);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(pathFreeArguments), client);
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        /*
         * Convert the qualified items into item specs with the correct
         * recursion type.
         */
        final ItemSpec[] itemSpecs = new ItemSpec[items.length];
        for (int i = 0; i < items.length; i++) {
            itemSpecs[i] = new ItemSpec(items[i].getPath(), recursion, items[i].getDeletionID());
        }

        final LabelResult[] results =
            workspace.unlabelItem(spec.getLabel(), spec.getScope(), itemSpecs, new LabelVersionSpec(spec));

        if (results == null || results.length == 0) {
            setExitCode(ExitCode.FAILURE);
            return;
        }

        LabelResultPrinter.printLabelResults(results, getDisplay());
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class
        }, "<labelName>[@<scope>] <itemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandUnlabel.HelpText1") //$NON-NLS-1$
        };
    }
}
