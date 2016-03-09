// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionDeleted;
import com.microsoft.tfs.client.clc.vc.options.OptionFolders;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;

public final class CommandDir extends Command {
    /**
     * Used instead of {@link Command#setExitCode(int)} for just this command
     * because {@link CommandDir} wants non-escalating combination of multiple
     * codes.
     */
    private int dirExitCode = ExitCode.UNKNOWN;

    public CommandDir() {
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
         * Build a list of paths to search.
         */
        final List<String> pathsToList = new ArrayList<String>();

        if (getFreeArguments().length == 0) {
            /*
             * If the current directory is mapped, use it, otherwise do the root
             * of the server specified (possibly via an option).
             */
            if (!Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).isMapped(LocalPath.getCurrentWorkingDirectory())) {
                pathsToList.add(LocalPath.getCurrentWorkingDirectory());
            } else {
                pathsToList.add(ServerPath.ROOT);
            }
        } else {
            /*
             * Use all the free arguments as paths.
             */
            pathsToList.addAll(Arrays.asList(getFreeArguments()));
        }

        final RecursionType recursionType =
            (findOptionType(OptionRecursive.class) != null) ? RecursionType.FULL : RecursionType.ONE_LEVEL;

        final DeletedState deletedState =
            (findOptionType(OptionDeleted.class) != null) ? DeletedState.DELETED : DeletedState.NON_DELETED;

        final ItemType itemType = (findOptionType(OptionFolders.class) != null) ? ItemType.FOLDER : ItemType.ANY;

        /*
         * Parse the paths as qualified items. We do this early (before we load
         * a cached workspace) to print errors (about disallowed ranges, etc.)
         * early.
         */
        final QualifiedItem[] qualifiedItems = parseQualifiedItems(
            pathsToList.toArray(new String[pathsToList.size()]),
            LatestVersionSpec.INSTANCE,
            false,
            0);

        if (qualifiedItems.length > 0) {
            /*
             * Use the list of paths we constructed, not the free arguments for
             * the workspace search. We don't need a workspace, so ignore
             * detection failure.
             */
            final TFSTeamProjectCollection connection =
                createConnection(pathsToList.toArray(new String[pathsToList.size()]), true);
            final VersionControlClient client = connection.getVersionControlClient();
            initializeClient(client);

            int itemsPrinted = 0;

            for (int i = 0; i < qualifiedItems.length; i++) {
                if (i > 0) {
                    getDisplay().printLine(""); //$NON-NLS-1$
                }

                final String path = qualifiedItems[i].getPath();

                if (!ServerPath.isServerPath(path)
                    && !Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).isMapped(path)) {
                    final String messageFormat = Messages.getString("CommandDir.NoWorkingFolderMappingFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, path);

                    getDisplay().printErrorLine(message);
                    setDirExitCode(ExitCode.FAILURE);
                } else {
                    itemsPrinted += dir(client, qualifiedItems[i], recursionType, deletedState, itemType);
                }
            }

            if (itemsPrinted > 0) {
                getDisplay().printLine(""); //$NON-NLS-1$

                final String messageFormat = Messages.getString("CommandDir.ItemsFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, itemsPrinted);

                getDisplay().printLine(message);
            }
        }

        setExitCode(dirExitCode);
    }

    private int dir(
        final VersionControlClient client,
        final QualifiedItem qualifiedItem,
        final RecursionType recursionType,
        final DeletedState deletedState,
        final ItemType itemType) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(qualifiedItem, "qualifiedItem"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(deletedState, "deletedState"); //$NON-NLS-1$
        Check.notNull(itemType, "itemType"); //$NON-NLS-1$

        /*
         * Ensure local paths are mapped before letting VersionControlClient
         * query on them. We can display a better error here.
         */
        if (!ServerPath.isServerPath(qualifiedItem.getPath())
            && !Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).isMapped(qualifiedItem.getPath())) {
            final String messageFormat = Messages.getString("CommandDir.LocalPathIsNotMappedOnThisComputerFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, qualifiedItem.getPath());

            getDisplay().printErrorLine(message);
            setDirExitCode(ExitCode.FAILURE);
            return 0;
        }

        final ItemSet itemSet = client.getItems(
            qualifiedItem.getPath(),
            qualifiedItem.getVersions()[0],
            recursionType,
            deletedState,
            itemType,
            false);

        if (itemSet == null || itemSet.getQueryPath() == null) {
            final String messageFormat = Messages.getString("CommandDir.PathNotFoundOrNotSupportedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, qualifiedItem.getPath());

            getDisplay().printErrorLine(message);
            setDirExitCode(ExitCode.FAILURE);
            return 0;
        }

        final Item[] items = itemSet.getItems();

        if (items == null || items.length == 0) {
            final String messageFormat = Messages.getString("CommandDir.NoItemsMatchFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, qualifiedItem.getPath());

            getDisplay().printErrorLine(message);
            setDirExitCode(ExitCode.PARTIAL_SUCCESS);
            return 0;
        }

        int firstIndex = 0;
        int count = 0;

        /*
         * If we specified a directory by server path, and without a deletion
         * ID, it will be the first item in the results and we can skip listing
         * it.
         */
        if (itemSet.getPattern() == null
            && ServerPath.equals(items[0].getServerItem(), itemSet.getQueryPath())
            && items[0].getDeletionID() == 0) {
            firstIndex = 1;
        }

        /*
         * Iterate over the directories in the returned items, list all
         * subdirectories first, then files.
         */
        int index = firstIndex;
        boolean doneAtLeastOne = false;

        while (index < items.length) {
            final String folderName = ServerPath.getParent(items[index].getServerItem());
            int folderNameLength = folderName.length();

            /*
             * Omit the separator if the path is "$/" so the subdirectory check
             * can work.
             */
            if (folderNameLength == 2) {
                folderNameLength = 1;
            }

            boolean displayedFolderHeader = false;

            /*
             * Starting at the current index, print each item that is a
             * subdirectory, then proceed to the next item if it's a direct
             * child of this item, otherwise stop and we'll print the files
             * using the boundaries we found in this pass.
             */
            final int start = index;
            for (; index < items.length; index++) {
                final String itemPath = items[index].getServerItem();

                /*
                 * If the next item is not a direct child of the current item
                 * (not a child at all or has an additional separator so it's a
                 * grandchild or more) break out of the loop.
                 */
                if (ServerPath.isChild(folderName, itemPath) == false
                    || itemPath.indexOf(ServerPath.PREFERRED_SEPARATOR_CHARACTER, folderNameLength + 1) >= 0) {
                    break;
                }

                /*
                 * Skip non-folder items.
                 */
                if (items[index].getItemType() != ItemType.FOLDER) {
                    continue;
                }

                /*
                 * We might need to print the header (which is just the folder
                 * name).
                 */
                if (displayedFolderHeader == false) {
                    if (doneAtLeastOne == false) {
                        doneAtLeastOne = true;
                    } else {
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    getDisplay().printLine(folderName + ":"); //$NON-NLS-1$
                    displayedFolderHeader = true;
                }

                /*
                 * Print the subfolder item.
                 */
                getDisplay().printLine(formatDirectoryItem(items[index]));
                count++;
            }

            /*
             * Print the files starting from the original starting point (before
             * the directory printing) to where the directory printing stopped
             * (because it detected a non-direct child).
             */
            for (int i = start; i < index; i++) {
                if (items[i].getItemType() == ItemType.FOLDER) {
                    continue;
                }

                if (displayedFolderHeader == false) {
                    if (doneAtLeastOne == false) {
                        doneAtLeastOne = true;
                    } else {
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    getDisplay().printLine(folderName + ":"); //$NON-NLS-1$
                    displayedFolderHeader = true;
                }

                getDisplay().printLine(formatDirectoryItem(items[i]));
                count++;
            }
        }

        /*
         * If the first index was 1, it was a folder we could skip, and if there
         * were no more items, there was nothing to print. We can return early
         * in this case.
         */
        if (firstIndex == 1 && items.length == firstIndex) {
            final String messageFormat = Messages.getString("CommandDir.NoItemsFoundUnderFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, qualifiedItem.getPath());

            getDisplay().printLine(message);
            setDirExitCode(ExitCode.SUCCESS);
            return count;
        } else if (count == 0) {
            /*
             * There were no items that matched, return early.
             */
            final String messageFormat = Messages.getString("CommandDir.NoItemsMatchFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, qualifiedItem.getPath());

            getDisplay().printLine(message);
            setDirExitCode(ExitCode.PARTIAL_SUCCESS);
            return count;
        } else {
            /*
             * Keep moving through the items.
             */
            setDirExitCode(ExitCode.SUCCESS);
        }

        return count;
    }

    private String formatDirectoryItem(final Item item) {
        final StringBuffer sb = new StringBuffer();

        /*
         * This looks dumb to me, but Microsoft's client puts a $ (not "$/")
         * before each folder item, instead of something like "/" after it.
         */
        if (item.getItemType() == ItemType.FOLDER) {
            sb.append(ServerPath.ROOT_NAME_ONLY);
        }

        final String itemName = VersionedFileSpec.formatPathWithDeletionIfNecessary(
            ServerPath.getFileName(item.getServerItem()),
            item.getDeletionID());

        sb.append(itemName);

        return sb.toString();
    }

    /**
     * Unlike the base {@link Command#setExitCode(int)}, this method doesn't
     * escalate to higher codes, it just assigns
     * {@link ExitCode#PARTIAL_SUCCESS} if a second, different known value
     * arrives.
     */
    public void setDirExitCode(final int exitCode) {
        if (this.dirExitCode == ExitCode.UNKNOWN) {
            this.dirExitCode = exitCode;
        } else if (exitCode != this.dirExitCode) {
            this.dirExitCode = ExitCode.PARTIAL_SUCCESS;
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class,
            OptionRecursive.class,
            OptionFolders.class,
            OptionDeleted.class
        }, "<itemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandDir.HelpText1") //$NON-NLS-1$
        };
    }
}
