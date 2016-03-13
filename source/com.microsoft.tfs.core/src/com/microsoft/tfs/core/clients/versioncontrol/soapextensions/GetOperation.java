// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;
import java.util.Comparator;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesFile;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.DownloadURL;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.datetime.CalendarUtils;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.versioncontrol.clientservices._03._GetOperation;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Describes the work required to be done by the client to complete a "get".
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class GetOperation extends WebServiceObjectWrapper implements Comparable<GetOperation> {
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(GetOperation.class);

    /**
     * Signifies an emptysource server item. Added in TFS 2012.
     */
    private static final String EMPTY_SOURCE_SERVER_ITEM = new String(new char[] {
        ServerPath.PREFERRED_SEPARATOR_CHARACTER
    });

    /**
     * Set by {@link #setMergeDetails(Conflict)}, contains any conflict
     * encountered as a result of this operation.
     */
    private Conflict mergeDetails = null;

    /**
     * The way this option will be processed. Can be set with
     * {@link #setProcessType(ProcessType)}.
     */
    private ProcessType processType = ProcessType.NONE;

    /**
     * Tracks whether the source local item has been cleared because this
     * operation was reused. Set via {@link #clearLocalItem()}.
     */
    private boolean isSourceLocalCleared = false;

    /**
     * Tracks whether the download for this operation has completed. Set via
     * {@link #setDownloadCompleted(boolean)}.
     */
    private boolean isDownloadCompleted = false;

    /**
     * Byte representation of the guid that is used to locate original file
     * content in the baseline folder. This is populated only when new content
     * was / will be downloaded using this GetOperation object.
     */
    private byte[] baselineFileGUID;

    /**
     * @see #getLocalVersionEntry()
     */
    private WorkspaceLocalItem localVersionEntry;

    /**
     * @see #isOkayToOverwriteExistingLocal()
     */
    private boolean isOkayToOverwriteExistingLocal;

    /**
     * @see #isIgnore()
     */
    private boolean ignore;

    /**
     * Parses the download URL to provide extra information.
     */
    private DownloadURL downloadURLObject;

    public GetOperation() {
        this(new _GetOperation());
    }

    public GetOperation(final _GetOperation op) {
        super(op);

        /*
         * New for TFS 2012: The server returns a special character for
         * sourceserver item, if it has the same value as target server item.
         */
        if (EMPTY_SOURCE_SERVER_ITEM.equals(getSourceServerItem())) {
            setSourceServerItem(getTargetServerItem());
        }

        /*
         * The default 'time zero' value defined in WISDL does not include a
         * time zone. Use the standard instance so future comparisons work.
         */
        if (CalendarUtils.equalsIgnoreTimeZone(DotNETDate.MIN_CALENDAR, getVersionServerDate())) {
            setVersionServerDate(DotNETDate.MIN_CALENDAR);
        }

        // Update the wrapper and wrapped objects
        setDownloadURL(op.getDurl());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _GetOperation getWebServiceObject() {
        return (_GetOperation) webServiceObject;
    }

    /**
     * @return the merge details (will be null if no merge conflict has
     *         occurred).
     */
    public synchronized Conflict getMergeDetails() {
        return mergeDetails;
    }

    /**
     * Sets the merge details for this operation. This method is only called
     * when a conflict arises during a merge.
     *
     * @param mergeDetails
     *        the details to set (null to remove any details).
     */
    public synchronized void setMergeDetails(final Conflict mergeDetails) {
        this.mergeDetails = mergeDetails;
    }

    public synchronized boolean isNamespaceConflict() {
        // back compat old servers do not send back this info
        if (getWebServiceObject().getNmscnflct() == 0) {
            // Check for m_conflictingItemId != 0 for Dogfood compatibility.
            // (10/13/2005)
            return hasConflict() && getConflictingItemID() != 0 && getConflictingItemID() != getItemID();
        } else {
            return getWebServiceObject().getNmscnflct() == 1;
        }
    }

    /**
     * Gets the hash code for the target item affected by this operation.
     *
     * @return the hash code for the target item.
     */
    public synchronized byte[] getHashValue() {
        return getWebServiceObject().getHashValue() != null ? getWebServiceObject().getHashValue().clone() : null;
    }

    /**
     * Sets the hash code for the target item affected by this operation.
     *
     * @param hashValue
     *        the new hash code value for the target item.
     */
    public synchronized void setHashValue(final byte[] hashValue) {
        getWebServiceObject().setHashValue(hashValue != null ? hashValue.clone() : null);
    }

    /**
     * Gets the type of the item affected by this operation.
     *
     * @return the type of the item.
     */
    public synchronized ItemType getItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getType());
    }

    /**
     * Sets the type of the item affected by this operation.
     *
     * @param type
     *        the type of the item.
     */
    public synchronized void setItemType(final ItemType type) {
        getWebServiceObject().setType(type.getWebServiceObject());
    }

    /**
     * Gets the numeric ID of the item affected by this operation.
     *
     * @return the numeric ID of the item.
     */
    public synchronized int getItemID() {
        return getWebServiceObject().getItemid();
    }

    /**
     * Sets the numeric ID of the item affected by this operation.
     *
     * @param id
     *        the numeric ID of the item.
     */
    public synchronized void setItemID(final int id) {
        getWebServiceObject().setItemid(id);
    }

    /**
     * Gets the URL at which this item can be downloaded.
     *
     * @return the url where this item can be downloaded.
     */
    public synchronized String getDownloadURL() {
        return getWebServiceObject().getDurl();
    }

    /**
     * @return the name of the file or folder in the server's repository path
     *         space. If null this means that this is a pre-Dev11 server which
     *         doesn't support sending it back
     */
    public synchronized String getSourceServerItem() {
        return getWebServiceObject().getSitem();
    }

    public synchronized void setSourceServerItem(final String item) {
        getWebServiceObject().setSitem(item);
    }

    /**
     * @return the file's encoding, always
     *         {@link VersionControlConstants#ENCODING_UNCHANGED} for pre-Dev 11
     *         servers.
     */
    public synchronized int getEncoding() {
        return getWebServiceObject().getEnc();
    }

    public synchronized void setEncoding(final int encoding) {
        getWebServiceObject().setEnc(encoding);
    }

    /**
     * @return the current local item is the source local item, unless
     *         {@link #clearLocalItem()} has been called, in which case it is
     *         <code>null</code>.
     */
    public synchronized String getCurrentLocalItem() {
        if (isSourceLocalCleared == true) {
            return null;
        }

        return LocalPath.tfsToNative(getWebServiceObject().getSlocal());
    }

    /**
     * Gets the local item that is the source of this operation, as defined by
     * the web service object. The source item is the name of the item before
     * the operation (i.e. rename.) This will always return the underlying web
     * service object, even if the get operation has been modified by the
     * GetEngine and the source local item has been cleared.
     *
     * @return the name of the source local item
     *
     * @see #getCurrentLocalItem()
     */
    public synchronized String getSourceLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getSlocal());
    }

    /**
     * Sets the local item that is the source of this operation. The source item
     * is the name of the item before the operation (i.e. rename).
     *
     * @param item
     *        the path to the source local item.
     */
    public synchronized void setSourceLocalItem(final String item) {
        getWebServiceObject().setSlocal(LocalPath.nativeToTFS(item));
    }

    /**
     * Gets the local item that is the target of this operation. The target item
     * is the name of the item after the operation (i.e. rename).
     *
     * @return the path to the target local item.
     */
    public synchronized String getTargetLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getTlocal());
    }

    /**
     * Sets the local item that is the target of this operation. The target item
     * is the name of the item after the operation (i.e. rename).
     *
     * @param item
     *        the path to the target local item.
     */
    public synchronized void setTargetLocalItem(final String item) {
        getWebServiceObject().setTlocal(LocalPath.nativeToTFS(item));
    }

    /**
     * Gets the server item that is the target of this operation. The target
     * item is the name of the item after the operation (i.e. rename).
     *
     * @return the path to the target server item.
     */
    public synchronized String getTargetServerItem() {
        return getWebServiceObject().getTitem();
    }

    /**
     * Sets the server item that is the target of this operation. The target
     * item is the name of the item after the operation (i.e. rename).
     *
     * @param item
     *        the path to the target server item.
     */
    public synchronized void setTargetServerItem(final String item) {
        getWebServiceObject().setTitem(item);
    }

    /**
     * Gets the version of the item on the server.
     *
     * @return the version of the item on the server.
     */
    public synchronized int getVersionServer() {
        return getWebServiceObject().getSver();
    }

    /**
     * Sets the version of the item on the server.
     *
     * @param version
     *        the version of the item on the server.
     */
    public synchronized void setVersionServer(final int version) {
        getWebServiceObject().setSver(version);
    }

    /**
     * Gets the version of the item locally.
     *
     * @return the local version of the item.
     */
    public synchronized int getVersionLocal() {
        return getWebServiceObject().getLver();
    }

    /**
     * Sets the version of the item locally.
     *
     * @param version
     *        the local version of the item.
     */
    public synchronized void setVersionLocal(final int version) {
        getWebServiceObject().setLver(version);
    }

    /**
     * Get the date / time the server version was created.
     *
     *
     * @return
     */
    public synchronized Calendar getVersionServerDate() {
        return getWebServiceObject().getVsd();
    }

    /**
     * Set the date / time the server version was created.
     *
     *
     * @param value
     */
    public synchronized void setVersionServerDate(final Calendar value) {
        getWebServiceObject().setVsd(value);
    }

    /**
     * Gets the deletion ID of the item.
     *
     * @return the deletion ID.
     */
    public synchronized int getDeletionID() {
        return getWebServiceObject().getDid();
    }

    /**
     * Sets the deletion ID of the item.
     *
     * @param did
     *        the deletion ID of the item.
     */
    public synchronized void setDeletionID(final int did) {
        getWebServiceObject().setDid(did);
    }

    /**
     * Gets the types of the pending change described by this item.
     *
     * @return the types of the pending change described by this item.
     */
    public synchronized ChangeType getChangeType() {
        return new ChangeType(getWebServiceObject().getChg(), getWebServiceObject().getChgEx());
    }

    /**
     * Sets the types of the pending change described by this item.
     *
     * @param changeType
     *        the types of the pending change described by this item.
     */
    public synchronized void setChangeType(final ChangeType changeType) {
        getWebServiceObject().setChg(changeType.getWebServiceObject());
        getWebServiceObject().setChgEx(changeType.getWebServiceObjectExtendedFlags());
    }

    public synchronized ChangeType getEffectiveChangeType() {
        final ChangeType changeType = getChangeType();

        if (isUndo()) {
            // For an add, we want to keep the add/edit bits so that the file
            // doesn't ever get overwritten.
            if (changeType.contains(ChangeType.ADD)) {
                return changeType;
            }

            // To preserve the assertions (they're very useful) in
            // ProcessOperationsInternal(), preserve
            // the branch bit when undoing a branch (do not preserve any
            // others).
            if (changeType.contains(ChangeType.BRANCH)) {
                return ChangeType.BRANCH;
            }

            return ChangeType.NONE;
        }

        return changeType;
    }

    /**
     * Gets the lock status for this operation.
     *
     * @return the lock status for this operation.
     */
    public synchronized LockLevel getLockLevel() {
        return LockLevel.fromWebServiceObject(getWebServiceObject().getLock());
    }

    /**
     * Sets the lock status for this operation.
     *
     * @param lock
     *        the lock status for this operation.
     */
    public synchronized void setLockLevel(final LockLevel lock) {
        getWebServiceObject().setLock(lock != null ? lock.getWebServiceObject() : null);
    }

    /**
     * Gets the ID of the pending change.
     *
     * @return the ID of the pending change.
     */
    public synchronized int getPendingChangeID() {
        return getWebServiceObject().getPcid();
    }

    /**
     * Sets the ID of the pending change.
     *
     * @param pcid
     *        the ID of the pending change.
     */
    public synchronized void setPendingChangeID(final int pcid) {
        getWebServiceObject().setPcid(pcid);
    }

    /**
     * Sets the URL at which this item can be downloaded.
     *
     * @param url
     *        the url where this item can be downloaded.
     */
    public synchronized void setDownloadURL(final String url) {
        getWebServiceObject().setDurl(url);
        this.downloadURLObject = new DownloadURL(url);
    }

    /**
     * Gets whether this operation is for the latest version of a file.
     *
     * @return true if this operation is the latest, false if not.
     */
    public synchronized boolean isLatest() {
        return getWebServiceObject().isIl();
    }

    /**
     * Gets whether this operation has a pending change.
     *
     * @return <code>true</code> if associated pending change is not caused just
     *         by pending change on the parent.
     */
    public boolean hasPendingChange() {
        return getPendingChangeID() != 0;
    }

    /**
     * Gets whether this operation conflicts with another.
     *
     * @return true if this operation conflicts with another, false if not.
     */
    public synchronized boolean hasConflict() {
        return getWebServiceObject().isCnflct();
    }

    /**
     * Sets whether this operation conflicts with another.
     *
     * @param conflict
     *        true if this operation conflicts with another, false if not.
     */
    public synchronized void setHasConflict(final boolean conflict) {
        getWebServiceObject().setCnflct(conflict);
    }

    /**
     * Gets the type of change that conflicts with this item.
     *
     * @return the type of change that conflicts with this item.
     */
    public synchronized ChangeType getConflictingChangeType() {
        return new ChangeType(getWebServiceObject().getCnflctchg(), getWebServiceObject().getCnflctchgEx());
    }

    /**
     * Sets the type of change that conflicts with this item.
     *
     * @param changeType
     *        the type of change that conflicts with this item.
     */
    public synchronized void setConflictingChangeType(final ChangeType changeType) {
        getWebServiceObject().setCnflctchg(changeType.getWebServiceObject());
    }

    /**
     * @return the item ID of the conflict item
     */
    public synchronized int getConflictingItemID() {
        return getWebServiceObject().getCnflctitemid();
    }

    /*
     * Extended knowledge about a GetOperation. The information these methods
     * return is not stored in the GetOperation this object has, but can be
     * inferred from it, or is otherwise tracked by an AGetOperation.
     */

    public synchronized ProcessType getType() {
        return processType;
    }

    public synchronized void setProcessType(final ProcessType processType) {
        this.processType = processType;
    }

    /**
     * @return true if the type of this operation is Undo, false if not.
     */
    public synchronized boolean isUndo() {
        return getType() == ProcessType.UNDO;
    }

    /**
     * @return true if this operation results in the ultimate deletion of a
     *         file, false if it will create a local item.
     */
    public synchronized boolean isDelete() {
        return (getWebServiceObject().getTlocal() == null);
    }

    public synchronized boolean isNewContentNeeded() {
        /*
         * If no download URL was provided, there is nothing to download, unless
         * this is an undo and we have a baseline GUID (to restore a file in a
         * local workspace from a baseline folder).
         */
        if (getDownloadURL() == null && !(isUndo() && null != getBaselineFileGUID())) {
            return false;
        }

        boolean newContentNeeded;

        // If this is an undo for an edit or delete, but NOT an add or branch,
        // we always download.
        final ChangeType pendingChange = getChangeType();

        if (isUndo()
            && (pendingChange.contains(ChangeType.EDIT) || pendingChange.contains(ChangeType.DELETE))
            && (pendingChange.contains(ChangeType.ADD) == false
                && pendingChange.contains(ChangeType.BRANCH) == false)) {
            newContentNeeded = true;
        } else {
            newContentNeeded = getVersionLocal() != getVersionServer()
                || (getCurrentLocalItem() == null && getTargetLocalItem() != null);
        }

        /*
         * If we're downloading new content, then we need to either have a
         * download URL or a baseline file GUID during an offline undo.
         */
        Check.isTrue(
            !newContentNeeded || null != this.getDownloadURL() || (isUndo() && null != getBaselineFileGUID()),
            "!newContentNeeded || null != this.getDownloadURL() || (isUndo() && null != getBaselineFileGUID())"); //$NON-NLS-1$

        return newContentNeeded;
    }

    /**
     * Tests whether this operation describes a rename that is simply changing
     * the character case of the file in this operation. If a part of the path
     * that is not the file is changing case, but the file is not changing case,
     * then the method returns false.
     *
     * @return true if the the operation is a rename changing the case of the
     *         file.
     */
    public synchronized boolean isCaseChangingRename() {
        final String sourceLocalItem = getCurrentLocalItem();
        final String targetLocalItem = getTargetLocalItem();

        if (sourceLocalItem != null && targetLocalItem != null) {
            /*
             * Compare directory parts using the case comparison rules of the
             * underlying filesystem.
             */
            if (LocalPath.equals(LocalPath.getDirectory(sourceLocalItem), LocalPath.getDirectory(targetLocalItem))) {
                final String sourceFile = LocalPath.getFileName(sourceLocalItem);
                final String targetFile = LocalPath.getFileName(targetLocalItem);

                /*
                 * Compare the file parts case insensitively (to ensure that
                 * this isn't a different filename entirely) and then case
                 * sensitively.
                 */
                if (sourceFile.equalsIgnoreCase(targetFile) == true && sourceFile.equals(targetFile) == false) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Called by code during get when multiple operations affect a single local
     * item.
     */
    public synchronized void clearLocalItem() {
        isSourceLocalCleared = true;
    }

    /**
     * @return true if the download for this item has been completed already
     *         (used in the conflict resolution code to signal that we don't
     *         need to download the item any more).
     */
    public synchronized boolean isDownloadCompleted() {
        return isDownloadCompleted;
    }

    /**
     * @param completed
     *        whether the download for this item should be marked completed.
     */
    public synchronized void setDownloadCompleted(final boolean completed) {
        isDownloadCompleted = completed;
    }

    /**
     * Byte representation of the guid that is used to locate original file
     * content in the baseline folder. This is populated only when new content
     * was / will be downloaded using this GetOperation object.
     */
    public synchronized byte[] getBaselineFileGUID() {
        return baselineFileGUID;
    }

    /**
     * @see #getBaselineFileGUID()
     */
    public synchronized void setBaselineFileGUID(final byte[] guid) {
        this.baselineFileGUID = guid;
    }

    /**
     * Before looping through the GetOperations and calling ProcessOperation on
     * each one, a pass is made to tag GetOperations meeting certain criteria
     * with their local version entries. This data is needed at ProcessOperation
     * time to determine whether or not to file a writable file conflict.
     */
    public synchronized WorkspaceLocalItem getLocalVersionEntry() {
        return localVersionEntry;
    }

    /**
     * @see #getLocalVersionEntry()
     */
    public synchronized void setLocalVersionEntry(final WorkspaceLocalItem entry) {
        localVersionEntry = entry;
    }

    /**
     * This flag indicates that writable file conflicts should be suppressed for
     * this GetOperation, if the target local item is equal to the source local
     * item. This flag is set when undoing an edit and when performing certain
     * types of resolves (AcceptTheirs on an edit, for example).
     */
    public synchronized boolean isOkayToOverwriteExistingLocal() {
        return isOkayToOverwriteExistingLocal;
    }

    public synchronized void setOkayToOverwriteExistingLocal(final boolean value) {
        isOkayToOverwriteExistingLocal = value;
    }

    /**
     * Creates an download spec instance for this operation. The spec instance
     * can be passed to FileDownloader to create a connection to the server to
     * download this file's data.
     *
     * @return a new {@link DownloadSpec} instance.
     */
    public synchronized DownloadSpec createDownloadSpec() {
        return new DownloadSpec(getDownloadURL());
    }

    public PropertyValue[] getPropertyValues() {
        // TODO remove the selectUnique
        return PropertyUtils.selectUnique(
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getPropertyValues()));
    }

    public void setPropertyValues(final PropertyValue[] propertyValues) {
        getWebServiceObject().setPropertyValues(
            (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, propertyValues));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public synchronized int compareTo(final GetOperation other) {
        if (this == other) {
            return 0;
        }

        /*
         * WARNING This implementation primarily affects the order in which get
         * operations are processed (items downloaded from the server). Any
         * changes you make here must not cause ordering problems during the
         * download/local version update process.
         */

        // The target server item is the primary sort target.
        final String thisTargetServerItem = getTargetServerItem();
        final String otherTargetServerItem = other.getTargetServerItem();

        if (thisTargetServerItem != null && otherTargetServerItem != null) {
            /*
             * We make a special exception for .tpattributes files because we
             * want them to sort first in their directory, so they are fetched
             * first, and can be read for all other files nested in the same
             * folder.
             */

            final String thisFileName = ServerPath.getFileName(thisTargetServerItem);
            final String thisParent = ServerPath.getParent(thisTargetServerItem);

            // If this item is a .tpattribute file, it has to precede all its
            // siblings
            // and siblings' children.
            if (thisFileName.equals(FileAttributesFile.DEFAULT_FILENAME)
                && ServerPath.isChild(thisParent, otherTargetServerItem)) {
                return -1;
            }

            final String otherFileName = ServerPath.getFileName(otherTargetServerItem);
            final String otherParent = ServerPath.getParent(otherTargetServerItem);

            // If the other item is a .tpattribute file, it has to precede all
            // its siblings
            // and siblings' children.

            if (otherFileName.equals(FileAttributesFile.DEFAULT_FILENAME)
                && ServerPath.isChild(otherParent, thisTargetServerItem)) {
                return 1;
            }

            // Two items are either the same or unrelated

            final int res = ServerPath.compareTopDown(thisTargetServerItem, otherTargetServerItem);
            if (res != 0) {
                // The items are unrelated. Return their native order in the
                // file system tree.
                return res;
            }
        }

        /*
         * If any of target server paths is null or both are the same, we order
         * items by their IDs, which probably represents their appearance in the
         * changes history on the server.
         */

        return getItemID() - other.getItemID();
    }

    @Override
    public synchronized boolean equals(final Object o) {
        if (o == null || !(o instanceof GetOperation)) {
            return false;
        }

        return compareTo((GetOperation) o) == 0;
    }

    @Override
    public synchronized int hashCode() {
        int c = getItemID();

        final String targetServerItem = getTargetServerItem();
        if (!StringUtil.isNullOrEmpty(targetServerItem)) {
            c = c << 7 | targetServerItem.hashCode();
        }

        return c;
    }

    public static final Comparator<GetOperation> GET_OPERATION_COMPARATOR = new Comparator<GetOperation>() {
        @Override
        public int compare(final GetOperation op1, final GetOperation op2) {
            return op1.compareTo(op2);
        };
    };

    public void setIgnore(final boolean value) {
        ignore = value;
    }

    /**
     * True if this GetOperation should be ignored by the client-side Get logic.
     * When a filtered Get is performed, the provided callback may set Ignore to
     * true on the ILocalUpdateOperation objects it receives. That is this
     * property.
     */
    public boolean isIgnore() {
        return ignore;
    }

    public boolean isContentDestroyed() {
        return downloadURLObject.isContentDestroyed();
    }
}
