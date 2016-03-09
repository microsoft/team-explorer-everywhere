// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff;

import java.io.File;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Holds CLC diff item information.
 */
public class DiffItem {
    private String serverPath;
    private String localPath;
    private String tempFile;
    private int codePage;
    private DiffItem rootItem;
    private String relativeLocalPath;
    private final ItemType itemType;
    private final long lastModified;
    private final boolean inRepository;
    private final VersionSpec version;

    private Item item;
    private int changesetVersion;

    private boolean isPendingChange;
    private boolean isWritable;

    public DiffItem(
        final Item item,
        final String localPath,
        final String tempFile,
        final DiffItem rootItem,
        final VersionSpec version) {
        this(
            item.getServerItem(),
            localPath,
            tempFile,
            item.getEncoding().getCodePage(),
            rootItem,
            item.getItemType(),
            item.getCheckinDate().getTimeInMillis(),
            true,
            version);

        Check.notNull(item, "item"); //$NON-NLS-1$

        this.item = item;
        changesetVersion = item.getChangeSetID();
    }

    public DiffItem(
        final String serverPath,
        final String localPath,
        final String tempFile,
        final int codePage,
        final DiffItem rootItem,
        final ItemType itemType,
        final long lastModified,
        final boolean inRepository,
        final VersionSpec version) {
        super();

        item = null;
        this.serverPath = serverPath;
        this.localPath = localPath;
        this.tempFile = tempFile;
        this.codePage = codePage;
        this.rootItem = rootItem;
        this.itemType = itemType;
        this.inRepository = inRepository;
        this.lastModified = lastModified;
        this.version = version;

        boolean isRoot = false;
        if (rootItem == null) {
            this.rootItem = this;
            isRoot = true;
        }

        /*
         * Calculate this item's paths relative to the root item (which will be
         * this item if it's the root).
         */
        relativeLocalPath = null;

        final String rootLocalPath = this.rootItem.getLocalPath();
        final String rootServerPath = this.rootItem.getServerPath();

        if (rootLocalPath != null && this.localPath != null && LocalPath.isChild(rootLocalPath, this.localPath)) {
            relativeLocalPath = this.localPath.substring(rootLocalPath.length());

            /*
             * If we're a file at the root, just use the file name part.
             */
            if (relativeLocalPath.length() == 0 && isRoot && isFolderItem() == false) {
                relativeLocalPath = LocalPath.getFileName(rootLocalPath);
            }

            /*
             * Strip any leading sepearator from the substring operation above
             * if we have at least one more character.
             */
            if (relativeLocalPath.length() > 1 && relativeLocalPath.startsWith(File.separator)) {
                relativeLocalPath = relativeLocalPath.substring(1);
            }
        } else if (rootServerPath != null
            && this.serverPath != null
            && ServerPath.isChild(rootServerPath, this.serverPath)) {
            /*
             * We were given a server path, but we'll make it relative like we
             * did for local paths and just flip the separators at the end.
             */

            relativeLocalPath = this.serverPath.substring(rootServerPath.length());

            /*
             * If we're a file at the root, just use the file name part.
             */
            if (relativeLocalPath.length() == 0 && isRoot && isFolderItem() == false) {
                relativeLocalPath = ServerPath.getFileName(rootServerPath);
            }

            /*
             * Strip any leading sepearator from the substring operation above
             * if we have at least one more character.
             */
            if (relativeLocalPath.length() > 1
                && relativeLocalPath.startsWith("" + ServerPath.PREFERRED_SEPARATOR_CHARACTER)) //$NON-NLS-1$
            {
                relativeLocalPath = relativeLocalPath.substring(1);
            }

            /*
             * Convert server path to local path separators.
             */
            relativeLocalPath = relativeLocalPath.replace(ServerPath.PREFERRED_SEPARATOR_CHARACTER, File.separatorChar);
        }

        /*
         * Make the local item a full path if required.
         */
        if (this.localPath == null && rootLocalPath != null && relativeLocalPath != null) {
            this.localPath = LocalPath.combine(rootLocalPath, relativeLocalPath);
        }

        /*
         * Make the server item a full path if required, converting any
         * filesystem separators in the relative part.
         */
        if (this.serverPath == null && rootServerPath != null && relativeLocalPath != null) {
            this.serverPath = ServerPath.combine(
                rootServerPath,
                relativeLocalPath.replace(File.separatorChar, ServerPath.PREFERRED_SEPARATOR_CHARACTER));
        }

        /*
         * Just use the local item for the temp path if one wasn't given.
         */
        this.tempFile = (tempFile != null) ? tempFile : this.localPath;
    }

    public void setCodePage(final int codePage) {
        this.codePage = codePage;
    }

    public int getCodePage() {
        return codePage;
    }

    public void setIsPendingChange(final boolean isPendingChange) {
        this.isPendingChange = isPendingChange;
    }

    public void setWritable(final boolean isWritable) {
        this.isWritable = isWritable;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof DiffItem == false) {
            return false;
        }

        final DiffItem other = (DiffItem) o;

        if (serverPath != null && other.serverPath != null && ServerPath.equals(serverPath, other.serverPath)) {
            return true;
        }

        if (localPath != null && other.localPath != null && LocalPath.equals(localPath, other.localPath)) {
            return true;
        }

        if (relativeLocalPath != null && other.relativeLocalPath != null) {
            if (relativeLocalPath.length() == 0 || other.relativeLocalPath.length() == 0) {
                return (relativeLocalPath.length() == other.relativeLocalPath.length());
            }

            final String thisFullPath = LocalPath.canonicalize(relativeLocalPath);
            final String otherFullPath = LocalPath.canonicalize(other.relativeLocalPath);
            if (LocalPath.equals(thisFullPath, otherFullPath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + ((serverPath != null) ? serverPath.hashCode() : 0);
        result = result * 37 + ((localPath != null) ? LocalPath.hashCode(localPath) : 0);

        return result;
    }

    public boolean isFolderItem() {
        return itemType == ItemType.FOLDER;
    }

    public String getServerPath() {
        return serverPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getTempFile() {
        return tempFile;
    }

    public DiffItem getRootItem() {
        return rootItem;
    }

    public String getRelativeLocalPath() {
        return relativeLocalPath;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isInRepository() {
        return inRepository;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public boolean isPendingChange() {
        return isPendingChange;
    }

    public VersionSpec getVersion() {
        return version;
    }

    /**
     * Always 0 unless
     * {@link #DiffItem(Item, String, File, DiffItem, VersionSpec)} was called.
     */
    public int getChangesetVersion() {
        return changesetVersion;
    }

    /**
     * Always null unless
     * {@link #DiffItem(Item, String, File, DiffItem, VersionSpec)} was called.
     */
    public Item getItem() {
        return item;
    }
}
