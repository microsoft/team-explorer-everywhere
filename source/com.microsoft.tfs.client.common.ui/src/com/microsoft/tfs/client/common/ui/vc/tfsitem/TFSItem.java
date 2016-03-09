// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import java.text.MessageFormat;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCache;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

/**
 * Class representing an item in the Team Foundation Server. Note, depending on
 * how this class has been populated, all the fields may not be present.
 */
public class TFSItem implements PathElement {
    private int remoteVersion;
    private final ServerItemPath path;
    private FileEncoding encoding;
    private ExtendedItem extendedItem;
    private boolean mappedLocalPathCalculated = false;
    private String mappedLocalPath;

    /*
     * OPTIONAL field - if present, this workspace will be used when computing
     * children. If absent, the "global" workspace from UiPlugin will be used
     */
    private final TFSRepository repository;

    public TFSItem(final ServerItemPath path) {
        this(path, null);
    }

    public TFSItem(final ServerItemPath path, final TFSRepository repository) {
        this.path = path;
        this.repository = repository;
    }

    public TFSItem(final ExtendedItem extendedItem) {
        this(extendedItem, null);
    }

    public TFSItem(final ExtendedItem extendedItem, final TFSRepository repository) {
        remoteVersion = extendedItem.getLatestVersion();
        path = new ServerItemPath(extendedItem.getTargetServerItem());
        encoding = extendedItem.getEncoding();

        this.extendedItem = extendedItem;

        this.repository = repository;
    }

    protected TFSRepository getRepository() {
        if (repository != null) {
            return repository;
        }

        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
    }

    public int getDeletionID() {
        return extendedItem != null ? extendedItem.getDeletionID() : 0;
    }

    public ItemSpec getItemSpec() {
        return new ItemSpec(getFullPath(), RecursionType.FULL, getDeletionID());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TFSItem)) {
            return false;
        }
        final TFSItem other = (TFSItem) obj;
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        final String messageFormat = "TFSItem [{0}] ({1})"; //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            path.getFullPath(),
            Integer.toHexString(System.identityHashCode(this)));
    }

    public boolean isLocal() {
        /*
         * Extended item is only null when the item does not yet exist on the
         * server (ie, a pending add will implicitly create this folder.) Thus,
         * it is by definition local.
         */
        if (getExtendedItem() == null) {
            return true;
        }

        final int localVersion = getLocalVersion();

        if (localVersion > 0) {
            return true;
        }

        /*
         * Local version may be zero when there's a pending add or branch. This
         * file should still be considered local.
         */
        final PendingChange[] pendingChanges = getPendingChanges(false);

        for (int i = 0; i < pendingChanges.length; i++) {
            final ChangeType changeType = pendingChanges[i].getChangeType();

            if (changeType.contains(ChangeType.ADD) || changeType.contains(ChangeType.BRANCH)) {
                return true;
            }
        }

        return false;
    }

    public boolean isLatest() {
        return getLocalVersion() == remoteVersion;
    }

    public int getLocalVersion() {
        if (extendedItem == null || extendedItem.getLocalItem() == null) {
            return 0;
        }
        return extendedItem.getLocalVersion();
    }

    /**
     * @see ItemPath.getName()
     */
    public String getName() {
        return path.getName();
    }

    public int getRemoteVersion() {
        return remoteVersion;
    }

    /**
     * Obtain the full (server) path of this ItemPath associated with this
     * TFSItem. This full path never includes a trailing forward slash, unless
     * this ItemPath represents root, in which case the full path always
     * includes a trailing forward slash.
     *
     * @return full path, never null
     * @see ItemPath.getFullPath()
     */
    public String getFullPath() {
        return path.getFullPath();
    }

    /**
     * Get the local path on the file system of this TFSItem. Note that null may
     * be returned if the item is not yet in the local file system.
     *
     * @return
     */
    public String getLocalPath() {
        if (extendedItem == null) {
            return getRepository().getWorkspace().getMappedLocalPath(path.getFullPath());
        }

        return extendedItem.getLocalItem();
    }

    /**
     * Gets a path from the TFSItem. If the file has a local path then this is
     * returned, otherwise the full server path is used.
     */
    public String getPath() {
        if (getLocalPath() == null || !getLocalPath().equals(getMappedLocalPath())) {
            // Not quite sure how it gets in the state that the server things
            // your local path
            // is different to that mapped, however this does happen and this is
            // how
            // Microsoft's client handles the situation.
            return path.getFullPath();
        }
        return getLocalPath();
    }

    /**
     * Return the source server path for this item. In the case of file with
     * pending renames or moves, this is the original server path to be used in
     * comparisons etc.
     */
    public String getSourceServerPath() {
        if (getExtendedItem() == null || getExtendedItem().getSourceServerItem() == null) {
            return path.getFullPath();
        }

        return getExtendedItem().getSourceServerItem();
    }

    public String getParentFullPath() {
        if (path.getParent() != null) {
            return path.getParent().getFullPath();
        } else {
            return null;
        }
    }

    @Override
    public ServerItemPath getItemPath() {
        return path;
    }

    public FileEncoding getEncoding() {
        return encoding;
    }

    public ExtendedItem getExtendedItem() {
        return extendedItem;
    }

    public boolean changeApplies(final PendingChange change) {
        final ServerItemPath serverItem = new ServerItemPath(change.getServerItem());

        if (serverItem.equals(getItemPath())) {
            return true;
        }

        if (change.getSourceServerItem() != null) {
            final ServerItemPath sourceServerItem = new ServerItemPath(change.getSourceServerItem());

            if (sourceServerItem.equals(getItemPath())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPendingChanges() {
        return hasPendingChanges(true);
    }

    public boolean hasPendingChanges(final boolean recursive) {
        final PendingChange[] changes = getPendingChanges(recursive);

        return (changes != null && changes.length > 0);
    }

    public PendingChange[] getPendingChanges() {
        return getPendingChanges(true);
    }

    public PendingChange[] getPendingChanges(final boolean recursive) {
        final String serverPath = getFullPath();

        if (serverPath == null) {
            return new PendingChange[0];
        }

        final PendingChangeCache pendingChangeCache = getRepository().getPendingChangeCache();
        PendingChange[] changes;

        if (recursive) {
            changes = pendingChangeCache.getPendingChangesByServerPathRecursive(serverPath);
        } else {
            final PendingChange pendingChange = pendingChangeCache.getPendingChangeByServerPath(serverPath);

            changes = (pendingChange != null) ? new PendingChange[] {
                pendingChange
            } : new PendingChange[0];
        }

        return changes;
    }

    public boolean isDeleted() {
        return getItemSpec().getDeletionID() > 0;
    }

    public String getMappedLocalPath() {
        if (!mappedLocalPathCalculated) {
            try {
                mappedLocalPath = getRepository().getWorkspace().getMappedLocalPath(path.getFullPath());
            } catch (final ServerPathFormatException e) {
                // ignore
            }
            mappedLocalPathCalculated = true;
        }
        return mappedLocalPath;
    }

}