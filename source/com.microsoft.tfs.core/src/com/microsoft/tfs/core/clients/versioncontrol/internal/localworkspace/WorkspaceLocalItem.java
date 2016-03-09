// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.util.Arrays;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.ServerItemLocalVersionUpdate;
import com.microsoft.tfs.util.Platform;

public class WorkspaceLocalItem {
    private static final byte[] EMPTY_HASH = new byte[16];
    private static final byte[] ZERO_LENGTH_ARRAY_BYTES = new byte[0];

    public WorkspaceLocalItem() {
        this.checkinDate = -1;
        this.lastModifiedTime = -1;
        this.hashValue = ZERO_LENGTH_ARRAY_BYTES;
    }

    /**
     * Saves the current item to the specified BinaryWriter.
     *
     * @param writer
     *        The writer to save the current item to.
     * @throws IOException
     */
    public void saveToVersion2(final BinaryWriter writer) throws IOException {
        writer.write(flags);
        writer.write(serverItem);
        writer.write(localItem == null ? "" : localItem); //$NON-NLS-1$
        writer.write(version);
        writer.write(itemID);
        writer.write(lastModifiedTime);
        writer.write(encoding);
        writer.write(checkinDate);

        if (!isDirectory()) {
            writer.write(length);

            if (null == hashValue || hashValue.length != 16) {
                writer.write(EMPTY_HASH);
            } else {
                writer.write(hashValue);
            }

            if (baselineFileGUID == null || baselineFileGUID.length != 16) {
                // Indicates no baseline GUID is included with this local
                // version entry.
                writer.write(false);
            } else {
                writer.write(true);
                writer.write(baselineFileGUID);
            }
        }
    }

    /**
     * Deserializes a WorkspaceLocalItem instance from version 1 of the
     * WorkspaceVersionTable schema.
     *
     * @throws IOException
     */
    public static WorkspaceLocalItem fromVersion1(final BinaryReader reader) throws IOException {
        final WorkspaceLocalItem toReturn = new WorkspaceLocalItem();

        toReturn.flags = reader.readByte();
        toReturn.serverItem = reader.readString();
        toReturn.localItem = reader.readString();
        toReturn.version = reader.readInt32();
        toReturn.itemID = reader.readInt32();
        toReturn.lastModifiedTime = reader.readInt64();
        toReturn.encoding = reader.readInt32();

        if (toReturn.localItem == null || toReturn.localItem.length() == 0) {
            toReturn.localItem = null;
        }

        if (!toReturn.isDirectory()) {
            toReturn.length = reader.readInt64();
            toReturn.hashValue = reader.readBytes(16);

            if (Arrays.equals(toReturn.hashValue, EMPTY_HASH)) {
                toReturn.hashValue = ZERO_LENGTH_ARRAY_BYTES;
            }

            // A boolean is written to indicate whether a baseline file GUID is
            // packed with this item.
            if (reader.readBoolean()) {
                toReturn.baselineFileGUID = reader.readBytes(16);
            }
        } else {
            toReturn.length = 0;
            toReturn.hashValue = ZERO_LENGTH_ARRAY_BYTES;
        }

        return toReturn;
    }

    /**
     * Deserializes a WorkspaceLocalItem instance from version 2 of the
     * WorkspaceVersionTable schema. Version 2 adds the CheckinDate column.
     *
     * @throws IOException
     */
    public static WorkspaceLocalItem fromVersion2(final BinaryReader reader) throws IOException {
        final WorkspaceLocalItem toReturn = new WorkspaceLocalItem();

        toReturn.flags = reader.readByte();
        toReturn.serverItem = reader.readString();
        toReturn.localItem = reader.readString();
        toReturn.version = reader.readInt32();
        toReturn.itemID = reader.readInt32();
        toReturn.lastModifiedTime = reader.readInt64();
        toReturn.encoding = reader.readInt32();
        toReturn.checkinDate = reader.readInt64();

        if (toReturn.localItem == null || toReturn.localItem.length() == 0) {
            toReturn.localItem = null;
        }

        if (!toReturn.isDirectory()) {
            toReturn.length = reader.readInt64();
            toReturn.hashValue = reader.readBytes(16);

            if (Arrays.equals(toReturn.hashValue, EMPTY_HASH)) {
                toReturn.hashValue = ZERO_LENGTH_ARRAY_BYTES;
            }

            // A boolean is written to indicate whether a baseline file GUID is
            // packed with this item.
            if (reader.readBoolean()) {
                toReturn.baselineFileGUID = reader.readBytes(16);
            }
        } else {
            toReturn.length = 0;
            toReturn.hashValue = ZERO_LENGTH_ARRAY_BYTES;
        }

        return toReturn;
    }

    /**
     * Flags for this item.
     */
    private byte flags;

    /**
     * For a committed (Version != 0) item, the committed server item. For an
     * uncommitted (Version == 0) item, the target server item.
     */
    private String serverItem;

    /**
     * The current local path of the item. Should be non-null unless this
     * WorkspaceLocalItem is in the deleted state.
     */
    private String localItem;

    /**
     * The version of the item in the workspace. Also indicates whether or not
     * the item is committed.
     */
    private int version;

    /**
     * The item id corresponding to ServerItem (zero if unknown).
     */
    private int itemID;

    /**
     * The FILETIME, in UTC, that was recorded by the scanner on its last pass
     * over this item. If not present, this value is -1.
     */
    private long lastModifiedTime;

    /**
     * The committed encoding of the item
     */
    private int encoding;

    /**
     * The FILETIME, in UTC, when the changeset Version was committed to version
     * control. If not present, this value is -1. This value will not be
     * available unless WorkspaceOptions.SetFileTimeToCheckin is set on the
     * Workspace object.
     */
    private long checkinDate;

    /**
     * The size of the item's committed content.
     */
    private long length;

    /**
     * The 16-byte MD5 hash value of the item's committed content.
     */
    private byte[] hashValue;

    /**
     * The GUID, from the baseline file service, of the committed content
     */
    private byte[] baselineFileGUID;

    /**
     * Returns TRUE if the local item is a folder.
     */
    public boolean isDirectory() {
        return (VersionControlConstants.ENCODING_FOLDER == this.encoding);
    }

    /**
     * Returns type of this item as FOLDER or FILE
     */
    public ItemType getItemType() {
        if (VersionControlConstants.ENCODING_FOLDER == this.encoding) {
            return ItemType.FOLDER;
        }

        return ItemType.FILE;
    }

    /**
     * True if the item is committed on the server.
     */
    public boolean isCommitted() {
        return version != 0;
    }

    /**
     * Indicates if on the next reconcile operation, the data in this row should
     * be reconciled to the server because it changed offline.
     */
    public boolean isPendingReconcile() {
        return (flags & (byte) WorkspaceLocalItemStates.PENDING_RECONCILE.toIntFlags()) != 0;
    }

    public void setPendingReconcile(final boolean value) {
        if (value) {
            flags |= (byte) WorkspaceLocalItemStates.PENDING_RECONCILE.toIntFlags();

        } else {
            flags &= (byte) ~WorkspaceLocalItemStates.PENDING_RECONCILE.toIntFlags();
        }
    }

    public boolean isMissingOnDisk() {
        return (flags & (byte) WorkspaceLocalItemStates.LOCAL_ITEM_MISSING.toIntFlags()) != 0;
    }

    public void setMissingOnDisk(final boolean value) {
        if (value) {
            flags |= (byte) WorkspaceLocalItemStates.LOCAL_ITEM_MISSING.toIntFlags();
        } else {
            flags &= (byte) ~WorkspaceLocalItemStates.LOCAL_ITEM_MISSING.toIntFlags();
        }
    }

    /**
     * True if item was deleted from disk due to pending change operation
     */
    public boolean isDeleted() {
        return (flags & (byte) WorkspaceLocalItemStates.DELETED.toIntFlags()) != 0;
    }

    public void setDeleted(final boolean value) {
        if (value) {
            flags |= (byte) WorkspaceLocalItemStates.DELETED.toIntFlags();
        } else {
            flags &= (byte) ~WorkspaceLocalItemStates.DELETED.toIntFlags();
        }
    }

    /**
     * Ephemeral bit. Used as temporary state during the execution of the
     * scanner.
     *
     * Indicates whether the first pass (mapped local space) over the workspace
     * hit this item or not. If not, the second pass (local version table) needs
     * to pick it up.
     */
    public boolean isScanned() {
        return (flags & (byte) WorkspaceLocalItemStates.SCANNED.toIntFlags()) != 0;
    }

    public void setScanned(final boolean value) {
        if (value) {
            flags |= (byte) WorkspaceLocalItemStates.SCANNED.toIntFlags();
        } else {
            flags &= (byte) ~WorkspaceLocalItemStates.SCANNED.toIntFlags();
        }
    }

    /**
     * Gets the property values for this item. Because full property values are
     * not stored for local workspaces, only the properties that fit in the
     * {@link WorkspaceLocalItemStates} flags field will be returned.
     *
     * @return the {@link PropertyValue}s associated with this item (may be
     *         <code>null</code>)
     */
    public PropertyValue[] getPropertyValues() {
        /*
         * Property values aren't stored locally, so construct from flags. Note
         * here we don't return NOT_SYMLINK when symlinkFlag is false because
         * lack of a symlink property value has same effect of NOT_SYMLINK, but
         * if the returned property values have both NOT_SYMLINK and
         * EXECUTABLE_DISABLED_VALUE it could cause name confusions for
         * executable and symlink files; Previously we only have one property
         * value, we just need to simply return EXECUTABLE_ENABLED or
         * EXECUTABLE_DISABLED; Only returning EXECUTABLE_DISABLED for normal
         * files is consistent with previous code too.
         */
        final boolean executableFlag = (flags & (byte) WorkspaceLocalItemStates.EXECUTABLE.toIntFlags()) != 0;
        final boolean symlinkFlag = (flags & (byte) WorkspaceLocalItemStates.SYMLINK.toIntFlags()) != 0;
        final PropertyValue[] values = new PropertyValue[1];

        if (symlinkFlag) {
            values[0] = PropertyConstants.IS_SYMLINK;
        } else if (executableFlag) {
            values[0] = PropertyConstants.EXECUTABLE_ENABLED_VALUE;
        } else {
            values[0] = PropertyConstants.EXECUTABLE_DISABLED_VALUE;
        }

        return values;
    }

    /**
     * Sets the property values for this item. Because full property values are
     * not stored for local workspaces, only the properties that fit in the
     * {@link WorkspaceLocalItemStates} flags field can be saved. Others will be
     * ignored.
     *
     * @param values
     *        the {@link PropertyValue}s to set on this item (may be
     *        <code>null</code> or empty)
     */
    public void setPropertyValues(final PropertyValue[] values) {
        final PropertyValue v = PropertyUtils.selectMatching(values, PropertyConstants.SYMBOLIC_KEY);
        final PropertyValue x = PropertyUtils.selectMatching(values, PropertyConstants.EXECUTABLE_KEY);

        if (PropertyConstants.IS_SYMLINK.equals(v)) {
            flags |= (byte) WorkspaceLocalItemStates.SYMLINK.toIntFlags();
        } else {
            flags &= (byte) ~WorkspaceLocalItemStates.SYMLINK.toIntFlags();
        }

        if (PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(x)) {
            flags |= (byte) WorkspaceLocalItemStates.EXECUTABLE.toIntFlags();
        } else {
            // Could be PropertyConstants.EXECUTABLE_DISABLED_VALUE or null
            // value
            flags &= (byte) ~WorkspaceLocalItemStates.EXECUTABLE.toIntFlags();
        }
    }

    /**
     * Indicates whether or not there is a valid value for the hash value in the
     * HashValue field.
     *
     * @return
     */
    public boolean hasHashValue() {
        return null != hashValue && 16 == hashValue.length;
    }

    /**
     * Indicates whether or not there is a valid value for the baseline file
     * GUID in the BaselineFileGuid field.
     *
     * @return
     */
    public boolean hasBaselineFileGUID() {
        return null != baselineFileGUID && 16 == baselineFileGUID.length;
    }

    @Override
    public WorkspaceLocalItem clone() {
        final WorkspaceLocalItem clone = new WorkspaceLocalItem();

        clone.version = this.version;
        clone.flags = this.flags;
        clone.serverItem = this.serverItem;
        clone.localItem = this.localItem;
        clone.length = this.length;
        clone.lastModifiedTime = this.lastModifiedTime;
        clone.itemID = this.itemID;
        clone.encoding = this.encoding;
        clone.checkinDate = this.checkinDate;

        if (null == this.hashValue || this.hashValue.length != 16) {
            clone.hashValue = ZERO_LENGTH_ARRAY_BYTES;
        } else {
            clone.hashValue = this.hashValue.clone();
        }

        if (null != this.baselineFileGUID) {
            clone.baselineFileGUID = this.baselineFileGUID.clone();
        }

        return clone;
    }

    public ServerItemLocalVersionUpdate getLocalVersionUpdate() {
        return getLocalVersionUpdate(false, false);
    }

    public ServerItemLocalVersionUpdate getLocalVersionUpdate(final boolean reconcileMissingOnDisk) {
        return getLocalVersionUpdate(reconcileMissingOnDisk, false);
    }

    public ServerItemLocalVersionUpdate getLocalVersionUpdate(
        final boolean reconcileMissingOnDisk,
        final boolean force) {
        if (!isPendingReconcile() && !(reconcileMissingOnDisk && isMissingOnDisk()) && !force) {
            return null;
        }

        final ServerItemLocalVersionUpdate toReturn = new ServerItemLocalVersionUpdate();
        toReturn.setSourceServerItem(this.serverItem);
        toReturn.setLocalVersion(this.version);
        toReturn.setTargetLocalItem(this.localItem);

        if (isDeleted() || (reconcileMissingOnDisk && isMissingOnDisk())) {
            toReturn.setTargetLocalItem(null);
        }

        return toReturn;
    }

    public GetOperation toGetOperation(final String[] itemPropertyFilters) {
        final GetOperation getOp = new GetOperation();

        getOp.setItemType(this.isDirectory() ? ItemType.FOLDER : ItemType.FILE);
        getOp.setVersionLocal(this.getVersion());
        getOp.setVersionServer(this.getVersion());
        getOp.setDeletionID(0);
        getOp.setSourceLocalItem(this.getLocalItem());
        getOp.setTargetLocalItem(this.getLocalItem());
        getOp.setTargetServerItem(this.getServerItem());
        getOp.setItemID(this.getItemID());
        getOp.setHashValue(this.getHashValue().clone());
        getOp.setEncoding(this.getEncoding());
        getOp.setSourceServerItem(this.getServerItem());
        getOp.setLocalVersionEntry(this);
        getOp.setPropertyValues(PropertyUtils.selectMatching(getPropertyValues(), itemPropertyFilters));

        return getOp;
    }

    public GetOperation toGetOperation(final LocalPendingChange pcEntry, final String[] itemPropertyFilters) {
        final GetOperation getOp = toGetOperation(itemPropertyFilters);

        getOp.setTargetServerItem(pcEntry.getTargetServerItem());

        if (!pcEntry.isCommitted()) {
            getOp.setSourceServerItem(pcEntry.getTargetServerItem());
        }

        getOp.setBaselineFileGUID(this.getBaselineFileGUID());
        getOp.setPendingChangeID(LocalPendingChange.LOCAL_PENDING_CHANGE_ID);
        getOp.setChangeType(pcEntry.getChangeType());
        getOp.setLockLevel(LockLevel.fromByteValue(pcEntry.getLockStatus()));
        getOp.setHasConflict(false);
        getOp.setHashValue(pcEntry.getHashValue());
        getOp.setProcessType(ProcessType.PEND);
        getOp.setPropertyValues(pcEntry.getPropertyValues());

        return getOp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        final String newline = System.getProperty("line.separator"); //$NON-NLS-1$
        sb.append("WorkspaceLocalItem instance " + ((Object) this).hashCode()).append(newline); //$NON-NLS-1$
        sb.append("  ServerItem: " + this.serverItem).append(newline); //$NON-NLS-1$
        sb.append("  LocalItem: " + this.localItem).append(newline); //$NON-NLS-1$
        sb.append("  Version: " + this.version).append(newline); //$NON-NLS-1$
        sb.append("  Flags: " + this.flags).append(newline); //$NON-NLS-1$
        sb.append("  Length: " + this.length).append(newline); //$NON-NLS-1$
        sb.append("  HashValue: " + Arrays.toString(this.hashValue)).append(newline); //$NON-NLS-1$
        sb.append("  LastModifiedTime: " + this.lastModifiedTime).append(newline); //$NON-NLS-1$
        sb.append("  ItemId: " + this.itemID).append(newline); //$NON-NLS-1$
        sb.append("  Encoding: " + this.encoding).append(newline); //$NON-NLS-1$
        sb.append("  CheckinDate: " + this.checkinDate).append(newline); //$NON-NLS-1$
        sb.append("  BaselineFileGuid: " + Arrays.toString(this.baselineFileGUID)).append(newline); //$NON-NLS-1$

        return sb.toString();
    }

    public long getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(final long value) {
        checkinDate = value;
    }

    public int getEncoding() {
        return encoding;
    }

    public void setEncoding(final int value) {
        encoding = value;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(final int value) {
        itemID = value;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(final long value) {
        lastModifiedTime = value;
    }

    public long getLength() {
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) && isSymbolicLink()) {
            return 0;
        }
        return length;
    }

    public void setLength(final long value) {
        length = value;
    }

    public String getLocalItem() {
        return localItem;
    }

    public void setLocalItem(final String value) {
        localItem = value;
    }

    public String getServerItem() {
        return serverItem;
    }

    public void setServerItem(final String value) {
        serverItem = value;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int value) {
        version = value;
    }

    public byte[] getBaselineFileGUID() {
        return baselineFileGUID;
    }

    public void setBaselineFileGUID(final byte[] value) {
        baselineFileGUID = value;
    }

    public byte[] getHashValue() {
        return (hashValue != null) ? hashValue.clone() : null;
    }

    public void setHashValue(final byte[] hashValue) {
        this.hashValue = hashValue != null ? hashValue.clone() : null;
    }

    public boolean isSymbolicLink() {
        return (flags & (byte) WorkspaceLocalItemStates.SYMLINK.toIntFlags()) != 0;
    }
}
