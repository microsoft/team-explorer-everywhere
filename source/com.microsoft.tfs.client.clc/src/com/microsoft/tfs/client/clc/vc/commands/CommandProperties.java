// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;

public final class CommandProperties extends Command {
    static class ItemProperties implements Comparable<ItemProperties> {
        /**
         * The default long format for the current locale.
         */
        private static DateFormat FORMATTER = SimpleDateFormat.getDateTimeInstance();

        /**
         * TODO implement ServerPath.comapreTopDownUI() and use that instead of
         * one of these; using this plain case-insensitive collator works for
         * these paths because they'll be well formed and normalized since they
         * came from the server.
         */
        private static Collator SERVER_PATH_UI_COMPARATOR = CollatorFactory.getCaseInsensitiveCollator();

        private final VersionControlClient client;
        private ItemType itemType;

        // Local properties
        private String localItem;
        private String serverItemLocal;
        private int versionLocal;
        private String pendingChangeString;
        private String lockStatusString;
        private String lockOwner;
        private String itemTypeLocalString;

        // Server properties
        private String serverItemServer;
        private int versionServer;
        private int deletionID;
        private String checkinDateString;
        private String itemTypeServerString;
        private String encodingString;
        private long fileLength;

        public ItemProperties(final VersionControlClient client) {
            this.client = client;
        }

        @Override
        public int compareTo(final ItemProperties other) {
            final ItemProperties leftProperties = this;
            final ItemProperties rightProperties = other;

            // First, compare the server path for what the user has.
            if (leftProperties.serverItemLocal != null && rightProperties.serverItemLocal != null) {
                final int order =
                    SERVER_PATH_UI_COMPARATOR.compare(leftProperties.serverItemLocal, rightProperties.serverItemLocal);

                if (order != 0) {
                    return order;
                }
            }

            // Next, compare the server path for what is in the repository.
            if (leftProperties.serverItemServer != null && rightProperties.serverItemServer != null) {
                final int order = SERVER_PATH_UI_COMPARATOR.compare(
                    leftProperties.serverItemServer,
                    rightProperties.serverItemServer);

                if (order != 0) {
                    return order;
                }
            }

            // Next, put the item with local info first.
            if (leftProperties.localItem != null && rightProperties.localItem == null) {
                return -1;
            } else if (leftProperties.localItem == null && rightProperties.localItem != null) {
                return 1;
            }

            // Otherwise, they are equal.
            return 0;
        }

        public void setItem(final Item item) {
            if (item != null) {
                itemType = item.getItemType();
                itemTypeServerString = item.getItemType().toUIString();
                serverItemServer = item.getServerItem();
                versionServer = item.getChangeSetID();
                deletionID = item.getDeletionID();
                /*
                 * TODO Get the server's time zone and convert from it to local
                 * time.
                 */
                checkinDateString = FORMATTER.format(item.getCheckinDate().getTime());

                if (item.getItemType() == ItemType.FILE
                    && item.getEncoding().equals(
                        new FileEncoding(VersionControlConstants.ENCODING_UNCHANGED)) == false) {
                    encodingString = item.getEncoding().getName();
                }
                fileLength = item.getContentLength();
            }
        }

        public void setExtendedItem(final ExtendedItem item) {
            if (item != null) {
                itemType = item.getItemType();

                /*
                 * Only show the server item when the user has the file or has a
                 * pending change on the file.
                 */
                if (item.getLocalItem() != null
                    || (item.getPendingChange() != null && item.getPendingChange().equals(ChangeType.NONE) == false)) {
                    localItem = item.getLocalItem();
                    itemTypeLocalString = item.getItemType().toUIString();
                    serverItemLocal = item.getTargetServerItem();
                    versionLocal = item.getLocalVersion();
                }

                final ChangeType change = item.getPendingChange();

                pendingChangeString = change.equals(ChangeType.NONE)
                    ? Messages.getString("CommandProperties.ChangeTypeNone") : change.toUIString(false, item); //$NON-NLS-1$
                lockStatusString = item.getLockLevel().toUIString();
                lockOwner = item.getLockOwnerDisplayName();
            }
        }

        public String getCheckinDateString() {
            return checkinDateString;
        }

        public VersionControlClient getClient() {
            return client;
        }

        public int getDeletionID() {
            return deletionID;
        }

        public String getEncodingString() {
            return encodingString;
        }

        public long getFileLength() {
            return fileLength;
        }

        public ItemType getItemType() {
            return itemType;
        }

        public String getItemTypeLocalString() {
            return itemTypeLocalString;
        }

        public String getItemTypeServerString() {
            return itemTypeServerString;
        }

        public String getLocalItem() {
            return localItem;
        }

        public String getLockOwner() {
            return lockOwner;
        }

        public String getLockStatusString() {
            return lockStatusString;
        }

        public String getPendingChangeString() {
            return pendingChangeString;
        }

        public String getServerItemLocal() {
            return serverItemLocal;
        }

        public String getServerItemServer() {
            return serverItemServer;
        }

        public int getVersionLocal() {
            return versionLocal;
        }

        public int getVersionServer() {
            return versionServer;
        }
    }

    public CommandProperties() {
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
         * Spit out obsoleted message if they used "prop" or "properties" and
         * set the exit code to something other than 0.
         */
        if (getMatchedAlias().equalsIgnoreCase("properties") || getMatchedAlias().equalsIgnoreCase("prop")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            getDisplay().printErrorLine(
                MessageFormat.format(
                    Messages.getString("CommandProperties.PropertiesCommandObsoleteFormat"), //$NON-NLS-1$
                    getMatchedAlias()));
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }

        String workspaceName = null;
        boolean recursive = false;
        VersionSpec[] versions = null;

        Option o = null;

        if ((o = findOptionType(OptionWorkspace.class)) != null) {
            workspaceName = ((OptionWorkspace) o).getValue();
        }

        if ((o = findOptionType(OptionRecursive.class)) != null) {
            recursive = true;
        }

        if ((o = findOptionType(OptionVersion.class)) != null) {
            versions = ((OptionVersion) o).getParsedVersionSpecs();
        }

        if (getFreeArguments().length == 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandProperties.NoFilesSpecified")); //$NON-NLS-1$
        }

        if (workspaceName != null && workspaceName.equalsIgnoreCase("*")) //$NON-NLS-1$
        {
            throw new InvalidOptionValueException(
                Messages.getString("CommandProperties.WildcardWorkspaceNamesNotAllowed")); //$NON-NLS-1$
        }

        VersionSpec version = null;
        if (versions != null) {
            if (versions.length != 1) {
                throw new InvalidOptionValueException(
                    Messages.getString("CommandProperties.VersionSpecRangeNotPermitted")); //$NON-NLS-1$
            }

            version = versions[0];
        } else {
            version = LatestVersionSpec.INSTANCE;
        }

        final QualifiedItem[] qualifiedItems = parseQualifiedItems(version, false, 0);

        if (qualifiedItems.length == 0) {
            return;
        }

        /*
         * Ignore any workspace detection failures, because the free argument
         * path given for status may be a server path and we can get by without
         * one. We'll determine a cached workspace manually later so we can deal
         * with local paths (if that's what the free argument uses).
         */
        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * If the user provided a workspace option, query for that workspace. If
         * not, use the default one.
         */
        Workspace workspace = null;
        if (workspaceName != null) {
            final Workspace[] all = client.queryWorkspaces(
                workspaceName,
                VersionControlConstants.AUTHENTICATED_USER,
                LocalHost.getShortName());

            if (all == null || all.length == 0) {
                final String messageFormat = Messages.getString("CommandProperties.WorkspaceCouldNotBeFoundFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, workspaceName);
                throw new CLCException(message);
            }

            workspace = all[0];
        } else {
            /*
             * See if we have a cached one for our paths, current directory.
             */
            workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);
        }
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        for (int i = 0; i < qualifiedItems.length; i++) {
            final QualifiedItem qi = qualifiedItems[i];
            Check.notNull(qi, "qi"); //$NON-NLS-1$

            DeletedState deletedState = DeletedState.ANY;
            if (qi.getDeletionID() != 0) {
                deletedState = DeletedState.DELETED;
            }

            Item[] items = null;
            ExtendedItem[] extendedItems;
            try {
                final ItemSpec itemSpec = new ItemSpec(
                    qi.getPath(),
                    (recursive) ? RecursionType.FULL : RecursionType.NONE,
                    qi.getDeletionID());

                final ItemSet itemSet = client.getItems(new ItemSpec[] {
                    itemSpec
                }, qi.getVersions()[0], deletedState, ItemType.ANY, false)[0];

                Check.notNull(itemSet, "itemSet"); //$NON-NLS-1$
                items = itemSet.getItems();

                /*
                 * If the workspace name was specified, we need to use it to
                 * present different information to the user. If no workspace
                 * was specified we can use the default workspace's method
                 * directly.
                 */
                if (workspaceName != null) {
                    final Workspace[] returnedWorkspaces =
                        client.queryWorkspaces(workspaceName, VersionControlConstants.AUTHENTICATED_USER, null);

                    if (returnedWorkspaces.length == 0) {
                        final String messageFormat =
                            Messages.getString("CommandProperties.WorkspaceDidNotMatchAnyWorkspaceFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, workspaceName);
                        throw new CLCException(message);
                    }

                    if (returnedWorkspaces.length > 1) {
                        final String messageFormat =
                            Messages.getString("CommandProperties.WorkspaceMatchedMoreThanOneWorkspaceFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, workspaceName);

                        throw new CLCException(message);
                    }

                    final Workspace specificWorkspace = returnedWorkspaces[0];

                    extendedItems = specificWorkspace.getExtendedItems(new ItemSpec[] {
                        itemSpec
                    }, deletedState, ItemType.ANY)[0];
                } else {
                    extendedItems = workspace.getExtendedItems(new ItemSpec[] {
                        itemSpec
                    }, deletedState, ItemType.ANY)[0];
                }
            } catch (final Exception e) {
                log.warn(e);
                getDisplay().printErrorLine(e.getMessage());
                setExitCode(ExitCode.PARTIAL_SUCCESS);
                continue;
            }

            /*
             * Map the item and extended item info together.
             */
            final Map<Integer, ItemProperties> itemIDToItemPropertiesMap = new HashMap<Integer, ItemProperties>();

            for (int j = 0; j < items.length; j++) {
                final ItemProperties itemProperties = new ItemProperties(client);

                itemProperties.setItem(items[j]);
                itemIDToItemPropertiesMap.put(new Integer(items[j].getItemID()), itemProperties);
            }

            for (int j = 0; j < extendedItems.length; j++) {
                final ExtendedItem extendedItem = extendedItems[j];

                if (itemIDToItemPropertiesMap.containsKey(new Integer(extendedItem.getItemID()))) {
                    itemIDToItemPropertiesMap.get(new Integer(extendedItem.getItemID())).setExtendedItem(extendedItem);
                } else {
                    /*
                     * Microsoft's comments:
                     *
                     * Skip any item where it doesn't match a versioned item and
                     * there's not a pending change on the extended item. This
                     * occurs when the user requests information on item at a
                     * version where it doesn't exist, but the
                     * GetExtendedItems() call will return info about the
                     * latest, which isn't relevant.
                     *
                     * This check also weeds out historical items that may come
                     * back in the extended item list, but not the normal item
                     * list, and without the normal item info we can't even tell
                     * the user the paths for the file.
                     */
                    if (extendedItem.getPendingChange().isEmpty()
                        || extendedItem.getPendingChange().equals(ChangeType.NONE)) {
                        continue;
                    }

                    final ItemProperties itemProperties = new ItemProperties(client);

                    itemProperties.setExtendedItem(extendedItem);
                    itemIDToItemPropertiesMap.put(new Integer(extendedItem.getItemID()), itemProperties);
                }
            }

            /*
             * Get the items from the map and sort them.
             */
            final ItemProperties[] sorted = itemIDToItemPropertiesMap.values().toArray(new ItemProperties[0]);
            Arrays.sort(sorted);

            if (sorted.length == 0) {
                final String messageFormat = Messages.getString("CommandProperties.NoItemsMatchFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, qi.getPath());

                getDisplay().printLine(message);
                continue;
            }

            for (int j = 0; j < sorted.length; j++) {
                final ItemProperties item = sorted[j];

                getDisplay().printLine(Messages.getString("CommandProperties.LocalInformationHeader")); //$NON-NLS-1$

                getDisplay().printLine(Messages.getString("CommandProperties.LocalInformationLocalPath") //$NON-NLS-1$
                    + emptyForNull(item.getLocalItem()));
                getDisplay().printLine(Messages.getString("CommandProperties.LocalInformationServerPath") //$NON-NLS-1$
                    + emptyForNull(item.getServerItemLocal()));
                getDisplay().printLine(Messages.getString("CommandProperties.LocalInformationChangeset") //$NON-NLS-1$
                    + Integer.toString(item.getVersionLocal()));
                getDisplay().printLine(
                    Messages.getString("CommandProperties.LocalInformationChange") + item.getPendingChangeString()); //$NON-NLS-1$
                getDisplay().printLine(Messages.getString("CommandProperties.LocalInformationType") //$NON-NLS-1$
                    + emptyForNull(item.getItemTypeLocalString()));

                getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationHeader")); //$NON-NLS-1$

                getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationServerPath") //$NON-NLS-1$
                    + emptyForNull(item.getServerItemServer()));
                getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationChangeset") //$NON-NLS-1$
                    + Integer.toString(item.getVersionServer()));
                getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationDeletionID") //$NON-NLS-1$
                    + Integer.toString(item.getDeletionID()));
                getDisplay().printLine(
                    Messages.getString("CommandProperties.ServerInformationLock") + item.getLockStatusString()); //$NON-NLS-1$
                getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationLockOwner") //$NON-NLS-1$
                    + emptyForNull(item.getLockOwner()));
                getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationLastModified") //$NON-NLS-1$
                    + emptyForNull(item.getCheckinDateString()));
                getDisplay().printLine(
                    Messages.getString("CommandProperties.ServerInformationType") + item.getItemTypeServerString()); //$NON-NLS-1$

                if (item.getItemType() == ItemType.FILE) {
                    getDisplay().printLine(
                        Messages.getString("CommandProperties.ServerInformationFileType") + item.getEncodingString()); //$NON-NLS-1$
                    getDisplay().printLine(Messages.getString("CommandProperties.ServerInformationSize") //$NON-NLS-1$
                        + Long.toString(item.getFileLength()));
                }

                getDisplay().printLine(""); //$NON-NLS-1$
            }
        }
    }

    private String emptyForNull(final String string) {
        if (string == null) {
            return ""; //$NON-NLS-1$
        }

        return string;
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
            OptionVersion.class
        }, "<itemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandProperties.HelpText1") //$NON-NLS-1$
        };
    }
}
