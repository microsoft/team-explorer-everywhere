// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.printers.MergePrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMergeDetails;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public final class CommandMerges extends Command {
    /**
     * The default long format for the current locale used in brief and detailed
     * displays.
     */
    private final DateFormat DEFAULT_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();

    public CommandMerges() {
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

        String outputFormat = OptionFormat.BRIEF;
        if ((o = findOptionType(OptionFormat.class)) != null) {
            outputFormat = ((OptionFormat) o).getValue();
        }

        final boolean recursive = (findOptionType(OptionRecursive.class) != null);

        if (getFreeArguments().length <= 0 || getFreeArguments().length > 2) {
            final String messageFormat = Messages.getString("CommandMerges.CommandRequiresOneOrTwoItemsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        /*
         * We don't require a workspace, but detect it later if we need to
         * qualify local paths.
         */
        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Determine the source and target specs.
         */

        String sourceItemSpec = null;
        String targetItemSpec = null;

        if (getFreeArguments().length == 1) {
            targetItemSpec = getFreeArguments()[0];
        } else {
            sourceItemSpec = getFreeArguments()[0];
            targetItemSpec = getFreeArguments()[1];
        }

        /*
         * Parse the source spec into components.
         */

        String sourcePath = null;
        int sourceDeletionID = 0;
        VersionSpec sourceVersion = null;

        if (sourceItemSpec != null) {
            final VersionedFileSpec sourceSpec =
                VersionedFileSpec.parse(sourceItemSpec, VersionControlConstants.AUTHENTICATED_USER, true);

            sourcePath = sourceSpec.getItem();

            /*
             * If detailed mode is enabled, we need deletion specifiers.
             */
            if (outputFormat.equalsIgnoreCase(OptionFormat.DETAILED) && sourceSpec.getDeletionVersionSpec() != null) {
                sourceDeletionID = sourceSpec.getDeletionVersionSpec().getDeletionID();
            }

            if (sourceSpec.getVersions().length == 0) {
                sourceVersion = LatestVersionSpec.INSTANCE;
            } else {
                sourceVersion = sourceSpec.getVersions()[0];
            }
        }

        /*
         * Parse the target spec into components.
         */

        String targetPath = null;
        int targetDeletionID = 0;
        VersionSpec targetVersion = null;

        final VersionedFileSpec targetSpec =
            VersionedFileSpec.parse(targetItemSpec, VersionControlConstants.AUTHENTICATED_USER, true);

        targetPath = targetSpec.getItem();

        /*
         * If detailed mode is enabled, we need deletion specifiers.
         */
        if (outputFormat.equalsIgnoreCase(OptionFormat.DETAILED) && targetSpec.getDeletionVersionSpec() != null) {
            targetDeletionID = targetSpec.getDeletionVersionSpec().getDeletionID();
        }

        if (targetSpec.getVersions().length == 0) {
            targetVersion = LatestVersionSpec.INSTANCE;
        } else {
            targetVersion = targetSpec.getVersions()[0];
        }

        /*
         * Make sure wildcards aren't used for either source or target.
         */
        if (LocalPath.isWildcard(targetPath) || (sourcePath != null && LocalPath.isWildcard(sourcePath))) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandMerges.WildcardsNotAllowed")); //$NON-NLS-1$
        }

        /*
         * Ensure all arguments which are local paths are mapped.
         */
        throwIfContainsUnmappedLocalPath(new String[] {
            targetPath
        });

        boolean shownAtLeastOne = false;

        if (outputFormat.equalsIgnoreCase(OptionFormat.BRIEF)) {
            final ChangesetMerge[] merges = client.queryMerges(
                sourcePath,
                sourceVersion,
                targetPath,
                targetVersion,
                null,
                null,
                recursive ? RecursionType.FULL : RecursionType.NONE);

            if (merges != null && merges.length > 0) {
                shownAtLeastOne = MergePrinter.printBriefMerges(merges, DEFAULT_DATE_FORMAT, getDisplay()) > 0;
            }
        } else if (outputFormat.equalsIgnoreCase(OptionFormat.DETAILED)) {
            final ChangesetMergeDetails merges = client.queryMergesWithDetails(
                sourcePath,
                sourceVersion,
                sourceDeletionID,
                targetPath,
                targetVersion,
                targetDeletionID,
                null,
                null,
                recursive ? RecursionType.FULL : RecursionType.NONE);

            if (merges != null) {
                shownAtLeastOne = MergePrinter.printDetailedMerges(merges, DEFAULT_DATE_FORMAT, getDisplay()) > 0;
            }
        } else {
            final String messageFormat = Messages.getString("CommandMerges.UnsupportedOutputFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, outputFormat);

            throw new RuntimeException(message);
        }

        if (shownAtLeastOne == false) {
            getDisplay().printErrorLine(Messages.getString("CommandMerges.NoMergeHistoryForTarget")); //$NON-NLS-1$
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
            OptionFormat.class,
        }, "[<sourceItem>] <destinationItem>"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandMerges.HelpText1") //$NON-NLS-1$
        };
    }
}
