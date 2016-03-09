// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Like "get" but only for a single file and prints its contents on the default
 * display's output stream.
 */
public final class CommandPrint extends Command {
    public CommandPrint() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        Option o = null;
        VersionSpec optionVersion = null;
        if ((o = findOptionType(OptionVersion.class)) != null) {
            final VersionSpec[] versions = ((OptionVersion) o).getParsedVersionSpecs();

            if (versions == null || versions.length == 0) {
                throw new InvalidOptionValueException(Messages.getString("CommandPrint.ExactlyOneVersionRequired")); //$NON-NLS-1$
            }

            if (versions.length > 1) {
                throw new InvalidOptionValueException(Messages.getString("CommandPrint.VersionRangeNotPermitted")); //$NON-NLS-1$
            }

            optionVersion = versions[0];
        }

        /*
         * If no version option was supplied, default to latest.
         */
        if (optionVersion == null) {
            optionVersion = LatestVersionSpec.INSTANCE;
        }

        // Use the default version they specified in the option.
        final QualifiedItem[] qualifiedItems = parseQualifiedItems(optionVersion, false, 0);

        if (qualifiedItems.length != 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandPrint.ExactlyOneFileItemRequired")); //$NON-NLS-1$
        }

        final QualifiedItem qi = qualifiedItems[0];

        if (Wildcard.isWildcard(qi.getPath())) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandPrint.WildcardsNotAllowed")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Query the server to get a download URL for the spec.
         */
        final ItemSet itemSet = client.getItems(
            qi.getPath(),
            qi.getVersions()[0],
            RecursionType.NONE,
            DeletedState.NON_DELETED,
            ItemType.FILE,
            true);

        final Item[] items = itemSet.getItems();

        if (items == null) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandPrint.ExactlyOneFileItemRequired")); //$NON-NLS-1$
        }

        if (items.length != 1) {
            throw new InvalidFreeArgumentException(
                Messages.getString("CommandPrint.SpecifiedFileDoesNotExistAtVersion")); //$NON-NLS-1$
        }

        final Item item = items[0];

        Check.isTrue(
            item.getDownloadURL() != null && item.getDownloadURL().length() > 0,
            "item.getDownloadURL() != null && item.getDownloadURL().length() > 0"); //$NON-NLS-1$

        /*
         * Download the item to a stream.
         */
        client.downloadFileToStream(
            new DownloadSpec(item.getDownloadURL()),
            getDisplay().getPrintStream(),
            true,
            null,
            null);
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class
        }, "<itemSpec>"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandPrint.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandPrint.HelpText2") //$NON-NLS-1$
        };
    }
}
