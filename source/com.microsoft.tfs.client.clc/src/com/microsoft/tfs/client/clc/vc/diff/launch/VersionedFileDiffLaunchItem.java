// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class VersionedFileDiffLaunchItem extends AbstractDiffLaunchItem {
    private Item item;
    private VersionSpec version;
    private int changeset;
    private String displayPath;
    private int itemID;

    /**
     * A client to use to query the server for an {@link Item} and to download
     * file contents. Only initialized to non-null by constructors that don't
     * require {@link Item}s.
     */
    private VersionControlClient client;

    private String filePath;
    private String label;

    /**
     * Creates a {@link VersionedFileDiffLaunchItem} with the default label text
     * (generated from the given arguments).
     *
     * @param client
     *        a {@link VersionControlClient} (not null)
     * @param fileItem
     *        the file item (folders are not supported). Not null.
     * @param version
     *        the version of the item (may be null)
     * @throws CLCException
     *         if a folder was given instead of a file for the fileItem
     *         parameter
     */
    public VersionedFileDiffLaunchItem(final VersionControlClient client, final Item item, final VersionSpec version)
        throws CLCException {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(item, "item"); //$NON-NLS-1$

        if (item.getItemType() == ItemType.FOLDER) {
            throw new CLCException(Messages.getString("VersionedFileDiffLaunchItem.CannotCompareFileWithFolder")); //$NON-NLS-1$
        }

        this.client = client;
        this.item = item;
        this.version = version;
    }

    /**
     * Creates a {@link VersionedFileDiffLaunchItem} with the default label text
     * (generated from the given arguments).
     *
     * @param client
     *        a {@link VersionControlClient} (not null)
     * @param localOrServerPath
     *        a local or server path (not null or empty).
     * @param version
     *        the version of the item (may be null)
     * @throws CLCException
     *         if a folder was given instead of a file for the fileItem
     *         parameter
     */
    public VersionedFileDiffLaunchItem(
        final VersionControlClient client,
        final String localOrServerPath,
        final VersionSpec version) throws CLCException {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNullOrEmpty(localOrServerPath, "localOrServerPath"); //$NON-NLS-1$

        final Item item = client.getItem(localOrServerPath, version);

        if (item.getItemType() == ItemType.FOLDER) {
            throw new CLCException(Messages.getString("VersionedFileDiffLaunchItem.CannotCompareFileWithFolder")); //$NON-NLS-1$
        }

        this.item = item;
        this.version = version;
    }

    /**
     * Creates a {@link VersionedFileDiffLaunchItem} with the default label text
     * (generated from the given arguments).
     *
     * @param client
     *        a {@link VersionControlClient} (not null)
     * @param itemID
     *        the item's ID
     * @param changeset
     *        the changeset of the item to use
     * @param displayPath
     *        the path of the item to use in the label
     * @throws CLCException
     *         if a folder was given instead of a file for the fileItem
     *         parameter
     */
    public VersionedFileDiffLaunchItem(
        final VersionControlClient client,
        final int itemID,
        final int changeset,
        final String displayPath) throws CLCException {
        Check.notNull(client, "client"); //$NON-NLS-1$

        this.client = client;
        this.itemID = itemID;
        this.changeset = changeset;
        version = new ChangesetVersionSpec(changeset);
        this.displayPath = displayPath;
    }

    public Item getItem() throws CLCException {
        if (item == null) {
            item = client.getItem(itemID, changeset, true);

            if (item == null) {
                if (changeset == -1) {
                    final String messageFormat =
                        Messages.getString("VersionedFileDiffLaunchItem.ItemNotFoundInSourceControlFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, displayPath);

                    throw new CLCException(message);
                } else {
                    final String messageFormat =
                        Messages.getString("VersionedFileDiffLaunchItem.ItemNotFoundInSourceControlAtVersionFormat"); //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, displayPath, Integer.toString(changeset));

                    throw new CLCException(message);
                }
            }

            if (item.getItemType() == ItemType.FOLDER) {
                throw new CLCException(Messages.getString("VersionedFileDiffLaunchItem.CannotCompareFileWithFolder")); //$NON-NLS-1$
            }

            /*
             * Changeset -1 and latest are the same.
             */
            if ((version instanceof ChangesetVersionSpec && ((ChangesetVersionSpec) version).getChangeset() == -1)
                || (version instanceof LatestVersionSpec)) {
                version = new ChangesetVersionSpec(item.getChangeSetID());
            }
        }

        return item;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.clc.vc.diff.launch.DiffLaunchItem#getEncoding()
     */
    @Override
    public int getEncoding() throws CLCException {
        return getItem().getEncoding().getCodePage();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.clc.vc.diff.launch.DiffLaunchItem#getFilePath()
     */
    @Override
    public String getFilePath() throws CLCException {
        if (filePath == null) {
            // Make sure we have an item.
            final Item item = getItem();
            Check.notNull(item, "item"); //$NON-NLS-1$

            filePath = getVersionedTempFileFullPath(item.getServerItem(), item.getItemType(), version);

            item.downloadFile(client, filePath);
        }

        return filePath;
    }

    @Override
    public boolean isTemporary() {
        /*
         * Always true for this kind of file.
         */
        return true;
    }

    @Override
    public String getLabel() throws CLCException {
        if (label == null || label.length() == 0) {
            final String messageFormat = Messages.getString("VersionedFileDiffLaunchItem.VersionedItemLabelFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                getItem().getServerItem(),
                (version == null ? "" : version.toString()), //$NON-NLS-1$
                SHORT_DATE_TIME_FORMATTER.format(getItem().getCheckinDate().getTime()));

            label = message;
        }

        return label;
    }

    @Override
    public void setLabel(final String label) {
        this.label = label;
    }
}
