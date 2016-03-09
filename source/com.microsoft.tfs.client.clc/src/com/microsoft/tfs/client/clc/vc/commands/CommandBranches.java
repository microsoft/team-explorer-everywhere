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
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;

public final class CommandBranches extends Command {
    /**
     * Microsoft's client uses the tab mechanism in its console stream writer,
     * so we will too.
     */
    private static final String INDENT = "\t"; //$NON-NLS-1$

    public CommandBranches() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length == 0) {
            final String messageFormat = Messages.getString("CommandBranches.BranchRequiresAtLeastOneItemFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());
            throw new InvalidFreeArgumentException(message);
        }

        Option o = null;
        final VersionSpec version = LatestVersionSpec.INSTANCE;

        if ((o = findOptionType(OptionVersion.class)) != null) {
            final VersionSpec[] optionVersions = ((OptionVersion) o).getParsedVersionSpecs();

            if (optionVersions == null || optionVersions.length != 1) {
                throw new InvalidOptionValueException(Messages.getString("CommandBranches.VersionRangesNotSupported")); //$NON-NLS-1$
            }
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        final WorkspaceInfo cw = determineCachedWorkspace(getFreeArguments());
        final Workspace workspace = realizeCachedWorkspace(cw, client);

        final ItemSpec[] itemSpecs = ItemSpec.fromStrings(getFreeArguments(), RecursionType.NONE);

        /*
         * TODO: Remove unmapped item specs from the array. Microsoft does this,
         * but the worst case for not doing it is simply printing an error
         * below.
         */

        if (itemSpecs.length > 0) {
            final BranchHistory[] allBranchHistories = workspace.getBranchHistory(itemSpecs, version);
            boolean doneOne = false;

            for (int i = 0; i < allBranchHistories.length; i++) {
                final String itemPath = itemSpecs[i].getItem();
                final BranchHistory branchHistory = allBranchHistories[i];

                /*
                 * A null history means no match from the query. It doesn't mean
                 * no branch history.
                 */
                if (branchHistory == null) {
                    final String messageFormat = Messages.getString("CommandBranches.NoItemsMatchFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, itemPath);

                    getDisplay().printErrorLine(message);
                    setExitCode(ExitCode.PARTIAL_SUCCESS);
                    /*
                     * Don't exit, let others print.
                     */
                } else {
                    if (doneOne == false) {
                        doneOne = true;
                    } else {
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    /*
                     * Test if the item has no children. In that case skip
                     * printing it and print a nice message instead.
                     */
                    if (branchHistory.getChildren().length == 0) {
                        /*
                         * Deal with unmapped items which didn't come back from
                         * the server.
                         */

                        final String itemPathForMessage = branchHistory.getRequestedItem() != null
                            ? branchHistory.getRequestedItem().getServerItem() : itemPath;

                        final String messageFormat =
                            Messages.getString("CommandBranches.ItemNotUsedInAnyBranchesFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, itemPathForMessage);

                        getDisplay().printLine(message);
                    } else {
                        /*
                         * Print all the children.
                         */
                        final Object[] children = branchHistory.getChildren();
                        Check.notNull(children, "children"); //$NON-NLS-1$

                        for (int j = 0; j < children.length; j++) {
                            printBranchHistoryItem((BranchHistoryTreeItem) children[j]);
                        }
                    }

                }
            }

            if (doneOne == false) {
                setExitCode(ExitCode.FAILURE);
            }
        }
    }

    /**
     * Prints a branch history item, recursing into its children.
     *
     * @param branchHistoryTreeItem
     *        the branch history item to print (not null).
     */
    private void printBranchHistoryItem(final BranchHistoryTreeItem branchHistoryTreeItem) {
        Check.notNull(branchHistoryTreeItem, "branchHistoryTreeItem"); //$NON-NLS-1$

        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < branchHistoryTreeItem.getLevel(); i++) {
            sb.append(INDENT);
        }

        /*
         * A null "to" item (target) means no permission.
         */
        final Item targetItem = branchHistoryTreeItem.getItem();
        if (targetItem == null) {
            sb.append(Messages.getString("CommandBranches.NoPermissionToReadBranchItem")); //$NON-NLS-1$
        } else {
            sb.append(
                VersionedFileSpec.formatPathWithDeletionIfNecessary(
                    targetItem.getServerItem(),
                    targetItem.getDeletionID()));
        }

        sb.append(INDENT);

        /*
         * Unlike the "to" item (target), the "from" item (source) can be null
         * to indicate the root of the branch history.
         */
        if (branchHistoryTreeItem.getFromItemChangesetID() != 0) {
            final Item sourceItem = branchHistoryTreeItem.getFromItem();
            if (sourceItem == null) {
                sb.append(Messages.getString("CommandBranches.NoPermissionToReadBranchItem")); //$NON-NLS-1$
            } else {
                final String messageFormat = Messages.getString("CommandBranches.BranchedFromVersionFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, Integer.toString(sourceItem.getChangeSetID()));
                sb.append(message);
            }
        }

        /*
         * The requested item was the one the query was originally performed on,
         * so it should get a highlight.
         */
        if (branchHistoryTreeItem.isRequested()) {
            sb.insert(0, ">> "); //$NON-NLS-1$
            sb.append(" <<"); //$NON-NLS-1$
        }

        getDisplay().printLine(sb.toString());

        if (branchHistoryTreeItem.getChildren() != null) {
            for (int i = 0; i < branchHistoryTreeItem.getChildren().length; i++) {

                printBranchHistoryItem((BranchHistoryTreeItem) branchHistoryTreeItem.getChildren()[i]);
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class,
        }, "<itemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandBranches.HelpText1") //$NON-NLS-1$
        };
    }
}
