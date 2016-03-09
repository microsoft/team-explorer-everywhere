// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.TimeZone;

import com.microsoft.tfs.core.clients.versioncontrol.LocalPendingChangeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._LocalPendingChange;

/**
 * Contains information about a change the user wants to perform to a versioned
 * item (or possibly a newly added item).
 *
 * @since TEE-SDK-11.0
 */
public final class LocalPendingChange extends WebServiceObjectWrapper {
    public static final int LOCAL_PENDING_CHANGE_ID = -123; // 0xFFFFFF85

    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];
    private static final ChangeType BRANCH_AND_TARGET_RENAME = ChangeType.BRANCH.combine(ChangeType.TARGET_RENAME);

    private byte flags;

    public LocalPendingChange() {
        this(new _LocalPendingChange());
    }

    public LocalPendingChange(final _LocalPendingChange change) {
        super(change);
    }

    public LocalPendingChange(
        final WorkspaceLocalItem lvEntry,
        final String targetServerItem,
        final ChangeType change) {
        this();

        setTargetServerItem(targetServerItem);
        setCommittedServerItem(lvEntry.getServerItem());
        setVersion(lvEntry.getVersion());
        setItemType(lvEntry.isDirectory() ? ItemType.FOLDER : ItemType.FILE);
        setEncoding(lvEntry.getEncoding());
        setHashValue(lvEntry.isDirectory() ? ZERO_LENGTH_BYTE_ARRAY : lvEntry.getHashValue());
        setItemID(lvEntry.getItemID());
        setCreationDate(Calendar.getInstance(TimeZone.getTimeZone("UTC"))); //$NON-NLS-1$
        setChangeType(change);

        setPropertyValues(lvEntry.getPropertyValues());
    }

    /**
     * Used for a candidate add
     */
    public LocalPendingChange(
        final String targetServerItem,
        final String committedServerItem,
        final int version,
        final ItemType itemType,
        final int encoding,
        final byte[] hashValue,
        final int itemId,
        final ChangeType change) {
        this();

        setTargetServerItem(targetServerItem);
        setCommittedServerItem(committedServerItem);
        setVersion(version);
        setItemType(itemType);
        setEncoding(itemType == ItemType.FOLDER ? VersionControlConstants.ENCODING_FOLDER : encoding);
        setHashValue(itemType == ItemType.FOLDER ? ZERO_LENGTH_BYTE_ARRAY : hashValue);
        setItemID(itemId);
        setCreationDate(Calendar.getInstance(TimeZone.getTimeZone("UTC"))); //$NON-NLS-1$
        setChangeType(change);

        // properties null for adds
    }

    @Override
    public LocalPendingChange clone() {
        final LocalPendingChange clone = new LocalPendingChange(cloneWebServiceObject(getWebServiceObject()));

        // Flags carries property value data
        clone.flags = flags;

        return clone;
    }

    private static _LocalPendingChange cloneWebServiceObject(final _LocalPendingChange change) {
        Check.notNull(change, "change"); //$NON-NLS-1$

        /*
         * To make a full clone, mutable field types must be cloned (flag sets
         * and arrays).
         */
        return new _LocalPendingChange(
            change.getTsi(),
            change.getCsi(),
            change.getBfi(),
            change.getV(),
            change.getBfv(),
            change.getC(),
            change.getIt(),
            change.getE(),
            change.getL(),
            change.getIid(),
            change.getCd() != null ? (Calendar) change.getCd().clone() : null,
            change.getHv() != null ? change.getHv().clone() : null,
            change.getDi(),
            change.getFl());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LocalPendingChange getWebServiceObject() {
        return (_LocalPendingChange) webServiceObject;
    }

    public synchronized ItemType getItemType() {
        return ItemType.fromByteValue(getWebServiceObject().getIt());
    }

    public synchronized void setItemType(final ItemType value) {
        getWebServiceObject().setIt(value.getValue());
    }

    public synchronized String getBranchFromItem() {
        return getWebServiceObject().getBfi();
    }

    public synchronized void setBranchFromItem(final String value) {
        getWebServiceObject().setBfi(value);
    }

    public synchronized int getBranchFromVersion() {
        return getWebServiceObject().getBfv();
    }

    public synchronized void setBranchFromVersion(final int value) {
        getWebServiceObject().setBfv(value);
    }

    public synchronized Calendar getCreationDate() {
        return getWebServiceObject().getCd();
    }

    public synchronized void setCreationDate(final Calendar value) {
        getWebServiceObject().setCd(value);
    }

    public synchronized String getCommittedServerItem() {
        return getWebServiceObject().getCsi();
    }

    public synchronized void setCommittedServerItem(final String value) {
        getWebServiceObject().setCsi(value);
    }

    public synchronized int getDeletionID() {
        return getWebServiceObject().getDi();
    }

    public synchronized void setDeletionID(final int value) {
        getWebServiceObject().setDi(value);
    }

    public synchronized int getEncoding() {
        return getWebServiceObject().getE();
    }

    public synchronized void setEncoding(final int value) {
        getWebServiceObject().setE(value);
    }

    public synchronized byte[] getHashValue() {
        return getWebServiceObject().getHv() != null ? getWebServiceObject().getHv().clone() : null;
    }

    public synchronized void setHashValue(final byte[] value) {
        getWebServiceObject().setHv(value != null ? value.clone() : null);
    }

    public synchronized int getItemID() {
        return getWebServiceObject().getIid();
    }

    public synchronized void setItemID(final int value) {
        getWebServiceObject().setIid(value);
    }

    public synchronized byte getLockStatus() {
        return getWebServiceObject().getL();
    }

    public synchronized void setLockStatus(final byte value) {
        getWebServiceObject().setL(value);
    }

    public synchronized String getTargetServerItem() {
        return getWebServiceObject().getTsi();
    }

    public synchronized void setTargetServerItem(final String value) {
        getWebServiceObject().setTsi(value);
    }

    public synchronized String getServerItem() {
        String serverItem = getCommittedServerItem();
        if (serverItem == null) {
            serverItem = getTargetServerItem();
        }
        return serverItem;
    }

    public synchronized int getVersion() {
        return getWebServiceObject().getV();
    }

    public synchronized void setVersion(final int value) {
        getWebServiceObject().setV(value);
    }

    public synchronized LocalPendingChangeFlags getFlags() {
        return new LocalPendingChangeFlags(flags);
    }

    public synchronized void setFlags(final LocalPendingChangeFlags value) {
        flags = (byte) value.toIntFlags();
    }

    public synchronized ChangeType getChangeType() {
        // Client values are one bit to the left -- None is 1 instead of 0
        ChangeType changeType = ChangeType.fromIntFlags(getWebServiceObject().getC() << 1);

        // If the pending command is a rename (stored as Branch | TargetRename).
        if (changeType.containsAll(BRANCH_AND_TARGET_RENAME)) {
            // Remove the Branch|TargetRename and add in the Rename bit.
            changeType = changeType.remove(BRANCH_AND_TARGET_RENAME).combine(ChangeType.RENAME);
        }

        return changeType;
    }

    public synchronized void setChangeType(final ChangeType value) {
        // Server values are one bit to the right -- None is 0 instead of 1.
        // Strip the rename bit from the provided ChangeType if it is present.
        int flags = value.remove(ChangeType.RENAME).toIntFlags() >> 1;

        // If we stripped the rename bit, add it back as Branch | TargetRename.
        if (value.contains(ChangeType.RENAME)) {
            flags |= (BRANCH_AND_TARGET_RENAME.toIntFlags()) >> 1;
        }

        getWebServiceObject().setC(flags);
    }

    public boolean isCommitted() {
        return getVersion() != 0;
    }

    public boolean isAdd() {
        return getChangeType().contains(ChangeType.ADD);
    }

    public boolean isEdit() {
        return getChangeType().contains(ChangeType.EDIT);
    }

    public boolean isProperty() {
        return getChangeType().contains(ChangeType.PROPERTY);
    }

    public boolean isRename() {
        return getChangeType().contains(ChangeType.RENAME);
    }

    public boolean isEncoding() {
        return getChangeType().contains(ChangeType.ENCODING);
    }

    public boolean isDelete() {
        return getChangeType().contains(ChangeType.DELETE);
    }

    public boolean isUndelete() {
        return getChangeType().contains(ChangeType.UNDELETE);
    }

    public boolean isBranch() {
        return getChangeType().contains(ChangeType.BRANCH);
    }

    public boolean isMerge() {
        return getChangeType().contains(ChangeType.MERGE);
    }

    public boolean isRollback() {
        return getChangeType().contains(ChangeType.ROLLBACK);
    }

    public boolean isLock() {
        return getChangeType().contains(ChangeType.LOCK);
    }

    public boolean isRecursiveChange() {
        return getChangeType().containsAny(ChangeType.RENAME_OR_DELETE) && ItemType.FOLDER == getItemType();
    }

    public boolean hasMergeConflict() {
        return getFlags().contains(LocalPendingChangeFlags.HAS_MERGE_CONFLICT);
    }

    public synchronized void setMergeConflict(final boolean value) {
        // Must synchronize across the get/set calls for atomicity

        if (value) {
            setFlags(getFlags().combine(LocalPendingChangeFlags.HAS_MERGE_CONFLICT));
        } else {
            setFlags(getFlags().remove(LocalPendingChangeFlags.HAS_MERGE_CONFLICT));
        }
    }

    public boolean isCandidate() {
        return getFlags().contains(LocalPendingChangeFlags.IS_CANDIDATE);
    }

    public synchronized void setCandidate(final boolean value) {
        // Must synchronize across the get/set calls for atomicity

        if (value) {
            setFlags(getFlags().combine(LocalPendingChangeFlags.IS_CANDIDATE));
        } else {
            setFlags(getFlags().remove(LocalPendingChangeFlags.IS_CANDIDATE));
        }
    }

    /**
     * Gets the property values for this change. Because full property values
     * are not stored for local pending changes, only the properties that fit in
     * the {@link LocalPendingChangeFlags} flags field will be returned.
     *
     * @return the {@link PropertyValue}s associated with this change (may be
     *         <code>null</code>)
     */
    public PropertyValue[] getPropertyValues() {
        /*
         * Property values aren't stored locally, so construct from flags. Only
         * one of SYMLINK or EXECUTABLE property can be set in the flags.
         */

        final ArrayList<PropertyValue> values = new ArrayList<PropertyValue>();
        if (getFlags().contains(LocalPendingChangeFlags.SYMLINK)) {
            values.add(PropertyConstants.IS_SYMLINK);
        } else if (getFlags().contains(LocalPendingChangeFlags.NOT_SYMLINK)) {
            values.add(PropertyConstants.NOT_SYMLINK);
        } else if (getFlags().contains(LocalPendingChangeFlags.EXECUTABLE)) {
            values.add(PropertyConstants.EXECUTABLE_ENABLED_VALUE);
        } else if (getFlags().contains(LocalPendingChangeFlags.NOT_EXECUTABLE)) {
            values.add(PropertyConstants.EXECUTABLE_DISABLED_VALUE);
        }

        return values.size() > 0 ? values.toArray(new PropertyValue[values.size()]) : null;
    }

    /**
     * Sets the property values for this change. Because full property values
     * are not stored for local pending changes, only the properties that fit in
     * the {@link LocalPendingChangeFlags} flags field can be saved. Others will
     * be ignored.
     *
     * @param values
     *        the properties to set (may be <code>null</code>)
     */
    public void setPropertyValues(final PropertyValue[] values) {
        boolean setExecutable = false;
        boolean setNotExecutable = false;

        final PropertyValue x = PropertyUtils.selectMatching(values, PropertyConstants.EXECUTABLE_KEY);
        if (x != null) {
            if (PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(x)) {
                setExecutable = true;
            } else {
                // Could be PropertyConstants.EXECUTABLE_DISABLED_VALUE or null
                // value
                setNotExecutable = true;
            }
        }

        if (setExecutable) {
            setFlags(
                getFlags().remove(LocalPendingChangeFlags.NOT_EXECUTABLE).combine(LocalPendingChangeFlags.EXECUTABLE));
        } else if (setNotExecutable) {
            setFlags(
                getFlags().remove(LocalPendingChangeFlags.EXECUTABLE).combine(LocalPendingChangeFlags.NOT_EXECUTABLE));
        } else {
            setFlags(
                getFlags().remove(LocalPendingChangeFlags.EXECUTABLE).remove(LocalPendingChangeFlags.NOT_EXECUTABLE));
        }

        // Set property values for symbolic links
        boolean setSymlink = false;
        boolean setNotSymlink = false;

        final PropertyValue v = PropertyUtils.selectMatching(values, PropertyConstants.SYMBOLIC_KEY);
        if (v != null) {
            if (PropertyConstants.IS_SYMLINK.equals(v)) {
                setSymlink = true;
            } else {
                setNotSymlink = true;
            }
        }

        if (setSymlink) {
            setFlags(getFlags().remove(LocalPendingChangeFlags.NOT_SYMLINK).combine(LocalPendingChangeFlags.SYMLINK));
        } else if (setNotSymlink) {
            setFlags(getFlags().remove(LocalPendingChangeFlags.SYMLINK).combine(LocalPendingChangeFlags.NOT_SYMLINK));
        } else {
            setFlags(getFlags().remove(LocalPendingChangeFlags.SYMLINK).remove(LocalPendingChangeFlags.NOT_SYMLINK));
        }

    }

    /**
     * Materializes a LocalPendingChange object as a PendingChange to be
     * returned via the public client object model.
     */
    public PendingChange toPendingChange(final VersionControlClient sourceControl, final String targetLocalItem) {
        byte[] hashValue = getHashValue();

        if (null == getHashValue() || 16 != getHashValue().length) {
            hashValue = ZERO_LENGTH_BYTE_ARRAY;
        }

        String item = getBranchFromItem();
        if (item == null) {
            item = getCommittedServerItem();
        }

        int version = getBranchFromVersion();
        if (version <= 0) {
            version = getVersion();
        }

        final Calendar creationDate = getCreationDate();
        creationDate.setTimeZone(TimeZone.getDefault());

        return new PendingChange(
            getTargetServerItem(),
            item,
            version,
            0,
            getDeletionID(),
            LockLevel.fromByteValue(getLockStatus()),
            targetLocalItem,
            getItemType(),
            getItemID(),
            creationDate,
            getVersion(),
            getChangeType(),
            hashValue,
            hashValue, /* Return an identical hash code for UploadHashValue */
            getEncoding(),
            LOCAL_PENDING_CHANGE_ID,
            getPropertyValues(),
            isCandidate());
    }

    /**
     * Creates a LocalPendingChange object from a PendingChange that has come
     * down from the server.
     */
    public static LocalPendingChange fromPendingChange(final PendingChange pc) {
        final LocalPendingChange localPc = new LocalPendingChange();

        localPc.setTargetServerItem(pc.getServerItem());
        localPc.setCommittedServerItem(pc.getSourceServerItem());

        if (null == localPc.getCommittedServerItem() && !pc.isAdd() && !pc.isBranch()) {
            localPc.setCommittedServerItem(pc.getServerItem());
        }

        if (pc.isBranch()) {
            localPc.setBranchFromItem(pc.getSourceServerItem());
            localPc.setBranchFromVersion(pc.getSourceVersionFrom());
        }

        localPc.setVersion(pc.getVersion());
        localPc.setChangeType(pc.getChangeType());
        localPc.setItemType(pc.getItemType());
        localPc.setEncoding(pc.getEncoding());

        if (pc.getLockLevel() == LockLevel.CHECKOUT) {
            localPc.setLockStatus(LockLevel.CHECKIN.getValue());
        } else {
            localPc.setLockStatus(pc.getLockLevel().getValue());
        }

        // adds or branches have pending changes of 0.
        localPc.setItemID((pc.isAdd() || pc.isBranch()) ? 0 : pc.getItemID());

        final Calendar creationDate = pc.getCreationDate();
        creationDate.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        localPc.setCreationDate(creationDate);

        localPc.setDeletionID(pc.getDeletionID());

        if (ItemType.FILE == pc.getItemType()) {
            localPc.setHashValue(pc.getHashValue());

            if (null == localPc.getHashValue() || localPc.getHashValue().length != 16) {
                localPc.setHashValue(ZERO_LENGTH_BYTE_ARRAY);
            }
        }

        localPc.setMergeConflict(pc.getConflictType() == ConflictType.MERGE);

        localPc.setPropertyValues(pc.getPropertyValues());

        return localPc;
    }

    /**
     * Compares the target server paths of local pending changes.
     */
    public static final Comparator<LocalPendingChange> SERVER_ITEM_COMPARATOR = new Comparator<LocalPendingChange>() {
        @Override
        public int compare(final LocalPendingChange change1, final LocalPendingChange change2) {
            return ServerPath.compareTopDown(change1.getTargetServerItem(), change2.getTargetServerItem());
        };
    };

    @Override
    public String toString() {
        return MessageFormat.format(
            "LocalPendingChange [getServerItem()={11}, getItemType()={0}, getBranchFromItem()={1}, getBranchFromVersion()={2}, getCreationDate()={3}, getCommittedServerItem()={4}, getDeletionID()={5}, getEncoding()={6}, getHashValue()={7}, getItemID()={8}, getLockStatus()={9}, getTargetServerItem()={10}, getVersion()={12}, getFlags()={13}, getChangeType()={14}, isCommitted()={15}, getPropertyValues()={16}]", //$NON-NLS-1$
            getItemType(),
            getBranchFromItem(),
            getBranchFromVersion(),
            getCreationDate() != null ? getCreationDate().getTimeInMillis() : null,
            getCommittedServerItem(),
            getDeletionID(),
            getEncoding(),
            Arrays.toString(getHashValue()),
            getItemID(),
            getLockStatus(),
            getTargetServerItem(),
            getServerItem(),
            getVersion(),
            getFlags(),
            getChangeType(),
            isCommitted(),
            Arrays.toString(getPropertyValues()));
    }
}
