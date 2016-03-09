// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalPendingChangesTable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.util.FileEncoding;

import ms.tfs.versioncontrol.clientservices._03._ExtendedItem;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Describes an extended item.
 *
 * Similar to {@link Item}, but contains more information about working folder
 * versions (so users of it can tell whether their disk items are synced to the
 * latest server versions of these files).
 *
 * @since TEE-SDK-10.1
 */
public class ExtendedItem extends WebServiceObjectWrapper implements Comparable {
    public ExtendedItem() {
        super(new _ExtendedItem());
    }

    public ExtendedItem(final _ExtendedItem item) {
        super(item);

        final String displayName = item.getLownerdisp();
        if (displayName == null || displayName.length() == 0) {
            item.setLownerdisp(item.getLowner());
        }
    }

    public ExtendedItem(
        final LocalPendingChangesTable pc,
        final WorkspaceLocalItem localItem,
        final LocalPendingChange pendingChange) {
        this(new _ExtendedItem());

        getWebServiceObject().setEnc(localItem.getEncoding());
        getWebServiceObject().setItemid(localItem.getItemID());

        if (localItem.isDirectory()) {
            getWebServiceObject().setType(ItemType.FOLDER.getWebServiceObject());
        } else {
            getWebServiceObject().setType(ItemType.FILE.getWebServiceObject());
        }

        getWebServiceObject().setLocal(localItem.isDeleted() ? "" : localItem.getLocalItem()); //$NON-NLS-1$
        getWebServiceObject().setSitem(localItem.getServerItem());
        getWebServiceObject().setLatest(localItem.getVersion());
        getWebServiceObject().setLver(localItem.isDeleted() ? 0 : localItem.getVersion());

        if (pendingChange == null) {
            if (localItem.isCommitted()) {
                getWebServiceObject().setTitem(pc.getTargetServerItemForCommittedServerItem(localItem.getServerItem()));
            } else {
                getWebServiceObject().setTitem(localItem.getServerItem());
            }

            getWebServiceObject().setPropertyValues(
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, localItem.getPropertyValues()));
        } else {
            getWebServiceObject().setTitem(pendingChange.getTargetServerItem());
            getWebServiceObject().setChg(pendingChange.getChangeType().getWebServiceObject());
            getWebServiceObject().setDid(pendingChange.getDeletionID());

            getWebServiceObject().setPropertyValues(
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, pendingChange.getPropertyValues()));
        }
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ExtendedItem getWebServiceObject() {
        return (_ExtendedItem) webServiceObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Object otherItem) {
        final ExtendedItem other = (ExtendedItem) otherItem;

        // First we sort by path.
        final int res = ServerPath.compareTopDown(getTargetServerItem(), other.getTargetServerItem());
        if (res != 0) {
            return res;
        }

        /*
         * Now we compare by deletion ID. Visual Studio's code sorts this way to
         * give consistent order to matching paths. I'm not sure if this field
         * is special in any way, or if they just arbitrarily chose it for the
         * secondary sort.
         */
        if (getDeletionID() < other.getDeletionID()) {
            return -1;
        } else if (getDeletionID() > other.getDeletionID()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getLocalVersion() {
        return getWebServiceObject().getLver();
    }

    public void setLocalVersion(final int version) {
        getWebServiceObject().setLver(version);
    }

    public int getDeletionID() {
        return getWebServiceObject().getDid();
    }

    public void setDeletionID(final int did) {
        getWebServiceObject().setDid(did);
    }

    public int getLatestVersion() {
        return getWebServiceObject().getLatest();
    }

    public void setLatestVersion(final int version) {
        getWebServiceObject().setLatest(version);
    }

    public ItemType getItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getType());
    }

    public void setItemType(final ItemType type) {
        getWebServiceObject().setType(type.getWebServiceObject());
    }

    public FileEncoding getEncoding() {
        return new FileEncoding(getWebServiceObject().getEnc());
    }

    public void setEncoding(final FileEncoding encoding) {
        getWebServiceObject().setEnc(encoding.getCodePage());
    }

    public int getItemID() {
        return getWebServiceObject().getItemid();
    }

    public void setItemID(final int itemid) {
        getWebServiceObject().setItemid(itemid);
    }

    public String getLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getLocal());
    }

    public void setLocalItem(final String item) {
        getWebServiceObject().setLocal(LocalPath.nativeToTFS(item));
    }

    public String getTargetServerItem() {
        return getWebServiceObject().getTitem();
    }

    public void setTargetServerItem(final String item) {
        getWebServiceObject().setTitem(item);
    }

    public String getSourceServerItem() {
        return getWebServiceObject().getSitem();
    }

    public void setSourceServerItem(final String item) {
        getWebServiceObject().setSitem(item);
    }

    /**
     * Gets the types of changes currently pending for this item.
     *
     * @return the types of the changes currently pending for this item.
     */
    public ChangeType getPendingChange() {
        return new ChangeType(getWebServiceObject().getChg(), getWebServiceObject().getChgEx());
    }

    /**
     * Sets the types of the changes currently pending for this item.
     *
     * @param changeType
     *        the types of the changes currently pending for this item.
     */
    public void setPendingChange(final ChangeType changeType) {
        getWebServiceObject().setChg(changeType.getWebServiceObject());
    }

    public boolean hasOtherPendingChange() {
        return getWebServiceObject().isOchg();
    }

    public void setHasOtherPendingChange(final boolean oChange) {
        getWebServiceObject().setOchg(oChange);
    }

    public LockLevel getLockLevel() {
        return LockLevel.fromWebServiceObject(getWebServiceObject().getLock());
    }

    public void setLockLevel(final LockLevel lock) {
        getWebServiceObject().setLock(lock.getWebServiceObject());
    }

    public String getLockOwner() {
        return getWebServiceObject().getLowner();
    }

    public void setLockOwner(final String owner) {
        getWebServiceObject().setLowner(owner);
    }

    public String getLockOwnerDisplayName() {
        return getWebServiceObject().getLownerdisp();
    }

    public boolean hasLocalChange() {
        final ChangeType change = new ChangeType(getWebServiceObject().getChg(), getWebServiceObject().getChgEx());
        return !change.isEmpty();
    }

    public Calendar getCheckinDate() {
        return getWebServiceObject().getDate();
    }

    public void setCheckinDate(final Calendar date) {
        getWebServiceObject().setDate(date);
    }

    /**
     * This method always returns false for items in a TFS 2005 or TFS 2008
     * server.
     *
     * @return true if the item is a branch, false if it is not a branch
     * @since TFS2010
     */
    public boolean isBranch() {
        return getWebServiceObject().isIsBranch();
    }

    public PropertyValue[] getPropertyValues() {
        // TODO remove the selectUnique
        return PropertyUtils.selectUnique(
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getPropertyValues()));
    }
}
