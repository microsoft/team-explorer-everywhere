// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.options.shared.OptionUser;
import com.microsoft.tfs.client.clc.vc.options.OptionItemMode;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionSlotMode;
import com.microsoft.tfs.client.clc.vc.options.OptionStopAfter;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.printers.ChangesetPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public final class CommandHistory extends Command {
    private final static String HISTORY_ELEMENT_NAME = "history"; //$NON-NLS-1$

    /**
     * The default long format for the current locale used in brief and detailed
     * displays.
     */
    private final DateFormat defaultFormat = SimpleDateFormat.getDateTimeInstance();

    public CommandHistory() {
        super();
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length != 1) {
            final String messageFormat = Messages.getString("CommandHistory.CommandRequiresExactlyOneArgumentFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        String filterUser = null;
        String format = OptionFormat.BRIEF;
        boolean itemMode = false;
        VersionSpec[] optionVersions = null;
        int stopAfter = VersionControlConstants.MAX_HISTORY_RESULTS;

        Option o = null;

        if ((o = findOptionType(OptionFormat.class)) != null) {
            format = ((OptionFormat) o).getValue();
        }

        if ((o = findOptionType(OptionVersion.class)) != null) {
            optionVersions = ((OptionVersion) o).getParsedVersionSpecs();
        }

        if ((o = findOptionType(OptionStopAfter.class)) != null) {
            stopAfter = ((OptionStopAfter) o).getNumber();
        }

        if ((o = findOptionType(OptionUser.class)) != null) {
            filterUser = ((OptionUser) o).getValue();
        }

        final boolean recursive = findOptionType(OptionRecursive.class) != null;

        if ((o = findOptionType(OptionItemMode.class)) != null) {
            if ((o = findOptionType(OptionSlotMode.class)) != null) {
                /* Can't specify item mode and slot mode both. */
                throw new InvalidOptionException(
                    Messages.getString("CommandHistory.SlotmodeCannotBeCombinedWithItemmode")); //$NON-NLS-1$
            }

            if (recursive) {
                /* Can't specify itemmode with recursive */
                getDisplay().printErrorLine(Messages.getString("CommandHistory.IgnoringItemmode")); //$NON-NLS-1$
            } else {
                itemMode = true;
            }
        }

        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        Changeset[] changesets = null;

        // TODO : Call parseWithDeletions?

        final VersionedFileSpec vfs =
            VersionedFileSpec.parse(getFreeArguments()[0], connection.getAuthorizedTFSUser().toString(), false);

        /*
         * Local items require a workspace, so verify that there is one.
         */
        WorkspaceInfo cachedWorkspace = null;
        try {
            cachedWorkspace = determineCachedWorkspace(new String[] {
                vfs.getItem()
            });
        } catch (final CannotFindWorkspaceException e) {
            throwIfContainsUnmappedLocalPath(new String[] {
                vfs.getItem()
            });
        }

        /*
         * Determine the range for the query based on how many versions were
         * supplied for the version option.
         */

        VersionSpec cutoffVersion = null;
        VersionSpec startVersion = null;
        VersionSpec stopVersion = null;

        if (optionVersions == null) {
            /*
             * No option was specified, so we want to pass a single onlyVersion
             * to the server, but the onlyVersion should be appropriate for
             * whether the file is local (use workspace version) or server (use
             * latest version).
             */
            startVersion = null;
            stopVersion = null;

            if (ServerPath.isServerPath(vfs.getItem()) == false && new File(vfs.getItem()).exists()) {
                /*
                 * The path was to a local item, and it does exist.
                 */
                cutoffVersion = new WorkspaceVersionSpec(
                    cachedWorkspace.getName(),
                    cachedWorkspace.getOwnerName(),
                    cachedWorkspace.getOwnerDisplayName());
            } else {
                /*
                 * The path was to a server item, or the local item didn't exist
                 * (so we don't care to use its version).
                 */
                cutoffVersion = LatestVersionSpec.INSTANCE;
            }
        } else if (optionVersions.length == 1) {
            /*
             * Only one version was specified, so we use that as the stopVersion
             * and the onlyVersion.
             */
            startVersion = null;
            stopVersion = optionVersions[0];
            cutoffVersion = optionVersions[0];
        } else {
            /*
             * At least two were supplied, so use those two.
             */
            startVersion = optionVersions[0];
            stopVersion = optionVersions[1];

            // TODO : Re-order version specs to be compatible with
            // Microsoft.

            cutoffVersion = optionVersions[1];
        }

        /*
         * We need to canonicalize the item path if it's a local path.
         */
        final String canonicalItemPath = ItemPath.canonicalize(vfs.getItem());

        /*
         * We include changes if we're looking at a single item (non-recursive)
         * or if the output format is detailed or XML.
         */
        final boolean includeChanges = !(recursive && OptionFormat.BRIEF.equalsIgnoreCase(format));

        changesets = client.queryHistory(
            ItemPath.smartNativeToTFS(canonicalItemPath),
            cutoffVersion,
            0,
            (recursive) ? RecursionType.FULL : RecursionType.NONE,
            filterUser,
            startVersion,
            stopVersion,
            stopAfter,
            includeChanges ? true : false,
            (!itemMode),
            false,
            false);

        if (OptionFormat.XML.equalsIgnoreCase(format)) {
            ChangesetPrinter.printXMLChangesets(changesets, HISTORY_ELEMENT_NAME, getDisplay());
        } else {
            if (changesets != null && changesets.length > 0) {
                if (OptionFormat.DETAILED.equalsIgnoreCase(format)) {
                    ChangesetPrinter.printDetailedChangesets(
                        changesets,
                        defaultFormat,
                        getDisplay(),
                        connection.getWorkItemClient());
                } else if (OptionFormat.BRIEF.equalsIgnoreCase(format)) {
                    ChangesetPrinter.printBriefChangesets(changesets, includeChanges, defaultFormat, getDisplay());
                } else {
                    final String messageFormat = Messages.getString("CommandHistory.UnsupportedOutputFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, format);

                    throw new RuntimeException(message);
                }
            } else {
                // No items were returned.
                getDisplay().printLine(Messages.getString("CommandHistory.NoHistoryFound")); //$NON-NLS-1$
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class,
            OptionStopAfter.class,
            OptionRecursive.class,
            OptionUser.class,
            OptionFormat.class,
            OptionSlotMode.class,
            OptionItemMode.class
        }, "<itemSpec>"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandHistory.HelpText1") //$NON-NLS-1$
        };
    }
}
