// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc;

import java.io.File;
import java.text.MessageFormat;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Information about an item at a specific version to be used in a compare
 * operation. Use
 * {@link #newResult(String, VersionSpec, VersionControlClient, Workspace)} for
 * proper validation.
 */
public class ItemAndVersionResult {
    public static class ValidationException extends Exception {
        private static final long serialVersionUID = -6973509776633131115L;

        public ValidationException(final String message) {
            super(message);
        }
    }

    /**
     * Creates a new {@link ItemAndVersionResult} for the specified path and
     * version. If the path is a local path, the local item is validated (must
     * exist and be readable). If the path is a server item, the server is
     * contacted for information about the item.
     *
     * @param path
     *        the local or server path to create an item for (must not be
     *        <code>null</code>)
     * @param version
     *        the version to create the item at (must not be <code>null</code>)
     * @param vcClient
     *        the {@link VersionControlClient} to use to validate server paths
     *        (must not be <code>null</code>)
     * @param workspace
     *        the workspace to use to validate server paths (must not be
     *        <code>null</code>)
     * @return the {@link ItemAndVersionResult} (never <code>null</code>)
     * @throws ValidationException
     *         if the local item or server item does not exist
     */
    public static ItemAndVersionResult newResult(
        final String path,
        final VersionSpec version,
        final VersionControlClient vcClient) throws ValidationException {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        if (ServerPath.isServerPath(path)) {
            return newServerItemResult(path, version, vcClient);
        } else {
            return newLocalItemResult(path);
        }
    }

    /**
     * Validates a local path and creates an {@link ItemAndVersionResult} if it
     * is valid.
     *
     * @param localPath
     *        the local path for the result (must not be <code>null</code>)
     * @return the new {@link ItemAndVersionResult} (never <code>null</code>)
     * @throws ValidationException
     *         if the local item did not exist or could not be read
     */
    private static ItemAndVersionResult newLocalItemResult(final String localPath) throws ValidationException {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        final File file = new File(localPath);

        if (!file.exists()) {
            throw new ValidationException(
                MessageFormat.format(
                    Messages.getString("ItemAndVersionResult.PathDoesNotExistFormat"), //$NON-NLS-1$
                    localPath));
        }

        if (!file.canRead()) {
            throw new ValidationException(
                MessageFormat.format(
                    Messages.getString("ItemAndVersionResult.PathNotReadableFormat"), //$NON-NLS-1$
                    localPath));
        }

        return new ItemAndVersionResult(file);
    }

    /**
     * Validates a server path and creates an {@link ItemAndVersionResult} if it
     * is valid. The server path must be a valid (latest) path to an item, and
     * the historic version is found using an <b>item mode</b> search (so
     * versions before renames can be found).
     *
     * @param serverPath
     *        the server path for the result (must not be <code>null</code>)
     * @param targetVersion
     *        the version of the server item to use in the result (must not be
     *        <code>null</code>)
     * @param vcClient
     *        the client to validate with (must not be <code>null</code>)
     * @return the new {@link ItemAndVersionResult} (never <code>null</code>)
     * @throws ValidationException
     *         if the server item did not exist at the specified version
     */
    private static ItemAndVersionResult newServerItemResult(
        final String serverPath,
        final VersionSpec targetVersion,
        final VersionControlClient vcClient) throws ValidationException {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(targetVersion, "targetVersion"); //$NON-NLS-1$
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        /*
         * Query history to determine the proper item/version. More expensive
         * than getItem() in the case where the target version is latest, but
         * handles renames and branches correctly.
         */

        // non-deleted items (did = 0)
        // no user filter
        // no versionFrom
        // versionTo = targetVersion
        // just one result
        // includeFileDetails = true
        // slotMode = false
        // downloadURLs = false

        final Changeset[] changesets = vcClient.queryHistory(
            serverPath,
            LatestVersionSpec.INSTANCE,
            0,
            RecursionType.NONE,
            null,
            null,
            targetVersion,
            1,
            true,
            false,
            false,
            false);

        if (changesets != null && changesets.length == 1) {
            final Change[] changes = changesets[0].getChanges();
            if (changes != null && changes.length == 1) {
                return new ItemAndVersionResult(changes[0].getItem(), targetVersion);
            }
        }

        throw new ValidationException(
            MessageFormat.format(
                Messages.getString("ItemAndVersionResult.SpecifiedVersionNotFoundFormat"), //$NON-NLS-1$
                serverPath));
    }

    /**
     * The server item, if this is a server item result (otherwise
     * <code>null</code>).
     */
    private final Item item;

    /**
     * The target version, if this is a server item result (otherwise
     * <code>null</code>).
     */
    private final VersionSpec targetVersion;

    /**
     * The local file, if this is a local item result (otherwise
     * <code>null</code>).
     */
    private final File file;

    private ItemAndVersionResult(final Item item, final VersionSpec targetVersion) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(targetVersion, "targetVersion"); //$NON-NLS-1$

        this.item = item;
        this.targetVersion = targetVersion;
        file = null;
    }

    private ItemAndVersionResult(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        item = null;
        targetVersion = null;
        this.file = file;
    }

    /**
     * @return true if the item is a server item, false if it is a local item
     */
    public boolean isServerItem() {
        return item != null;
    }

    /**
     * @return true if the item is a local item, false if the item is a server
     *         item
     */
    public boolean isLocalItem() {
        return file != null;
    }

    /**
     * @return the server {@link Item} when this is a server item (
     *         <code>null</code> when {@link #isServerItem()} is false)
     *
     */
    public Item getItem() {
        return item;
    }

    /**
     * @return the target version (never <code>null</code>)
     */
    public VersionSpec getTargetVersion() {
        return targetVersion;
    }

    /**
     * @return the local {@link File} when this is a local item (
     *         <code>null</code> when {@link #isLocalItem()} is false)
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the {@link ItemType} of the server or local item (never
     *         <code>null</code>)
     */
    public ItemType getItemType() {
        if (item != null) {
            return item.getItemType();
        } else {
            return file.isDirectory() ? ItemType.FOLDER : ItemType.FILE;
        }
    }
}
