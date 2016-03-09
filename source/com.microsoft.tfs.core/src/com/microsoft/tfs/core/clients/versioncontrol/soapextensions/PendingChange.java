// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.CheckinEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspacePropertiesLocalVersionTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.datetime.DotNETDate;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.temp.TempStorageService;

import ms.tfs.versioncontrol.clientservices._03._ChangeType;
import ms.tfs.versioncontrol.clientservices._03._MergeSource;
import ms.tfs.versioncontrol.clientservices._03._PendingChange;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Contains information about a change the user wants to perform to a versioned
 * item (or possibly a newly added item).
 *
 * @since TEE-SDK-10.1
 */
public final class PendingChange extends WebServiceObjectWrapper implements Comparable<PendingChange>, Cloneable {
    private static final Log log = LogFactory.getLog(PendingChange.class);

    /*
     * These additional values are derived from state in the GetOperation, if
     * that constructor is used.
     */
    private boolean undone;
    private boolean localItemDelete;
    private boolean isCandidate;
    private String pendingSetName;
    private String pendingSetOwner;
    private String pendingSetOwnerDisplay;
    private boolean inShelveset;

    /**
     * The hash code is somewhat expensive to compute, so it's cached.
     * {@link #hashCodeValid} is flipped false by the setters that change the
     * hashed data.
     */
    private volatile boolean hashCodeValid = false;
    private volatile int hashCode;

    public PendingChange(final _PendingChange change) {
        super(change);
    }

    /**
     * Creates a clone of the given {@link PendingChange}.
     *
     * @param change
     *        the change to clone (must not be <code>null</code>)
     */
    public PendingChange(final PendingChange change) {
        this(cloneWebServiceObject(change.getWebServiceObject()));
    }

    private static _PendingChange cloneWebServiceObject(final _PendingChange change) {
        Check.notNull(change, "change"); //$NON-NLS-1$

        /*
         * To make a full clone, mutable field types must be cloned (flag sets
         * and arrays).
         */

        _MergeSource[] mergeSources = null;
        if (change.getMergeSources() != null) {
            mergeSources = new _MergeSource[change.getMergeSources().length];
            for (int i = 0; i < mergeSources.length; i++) {
                mergeSources[i] = new _MergeSource(
                    change.getMergeSources()[i].getS(),
                    change.getMergeSources()[i].getVf(),
                    change.getMergeSources()[i].getVt(),
                    change.getMergeSources()[i].isR());
            }
        }

        return new _PendingChange(
            change.getChgEx(),
            change.getChg() != null ? (_ChangeType) change.getChg().clone() : null,
            change.getDate() != null ? (Calendar) change.getDate().clone() : null,
            change.getDid(),
            change.getType(),
            change.getEnc(),
            change.getItemid(),
            change.getLocal(),
            change.getLock(),
            change.getItem(),
            change.getSrclocal(),
            change.getSrcitem(),
            change.getSvrfm(),
            change.getSdi(),
            change.getVer(),
            change.getHash() != null ? change.getHash().clone() : null,
            change.getLen(),
            change.getUhash() != null ? change.getUhash().clone() : null,
            change.getPcid(),
            change.getDurl(),
            change.getShelvedurl(),
            change.getCt(),
            mergeSources,
            change.getPropertyValues() != null ? change.getPropertyValues().clone() : null);
    }

    /**
     * Construct a {@link PendingChange} from a {@link GetOperation}. This is
     * just a convenience method for {@link GetEngine} to use. Do not use this
     * outside of com.microsoft.tfs.core.
     *
     * @param workspace
     *        the workspace where the get operation happened (must not be
     *        <code>null</code>)
     * @param operation
     *        the operation to initialize the pending change from (must not be
     *        <code>null</code>)
     * @param processType
     *        the process type of the operation (must not be <code>null</code>)
     */
    public PendingChange(final Workspace workspace, final GetOperation operation, final ProcessType processType) {
        /*
         * Note that we get the original source local item from the get
         * operation, as the GetEngine may have modified the Get Operation to
         * remove the source local item. (Occurs normally when processing
         * getops.) We do this to recreate a pending change object that caused
         * the get operation as opposed to the get operation as transformed by
         * local filesystem state.
         */
        super(
            new _PendingChange(
                operation.getChangeType().getWebServiceObjectExtendedFlags(),
                operation.getChangeType().getWebServiceObject(),
                Calendar.getInstance(),
                operation.getDeletionID(),
                operation.getItemType().getWebServiceObject(),
                VersionControlConstants.ENCODING_UNCHANGED,
                operation.getItemID(),
                LocalPath.nativeToTFS(operation.getTargetLocalItem()),
                operation.getLockLevel().getWebServiceObject(),
                operation.getTargetServerItem(),
                LocalPath.nativeToTFS(operation.getSourceLocalItem()),
                null,
                0,
                0,
                operation.getVersionServer(),
                operation.getHashValue(),
                -1,
                null,
                operation.getPendingChangeID(),
                null,
                null,
                ConflictType.NONE.getValue(),
                new _MergeSource[0],
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, operation.getPropertyValues())));

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.undone = operation.isUndo();
        this.localItemDelete = operation.isDelete();

        this.pendingSetName = workspace.getName();
        this.pendingSetOwner = workspace.getOwnerName();
        this.pendingSetOwnerDisplay = workspace.getOwnerDisplayName();
        this.inShelveset = false;

        // This logic is to make the PendingChange object constructed from the
        // GetOp
        // match the PendingChange object retrieved by querying for
        // PendingChanges directly.
        // If the TargetLocalItem property of the get operation is not
        // populated,
        // attempt to populate m_localITem here so that the pendingChange object
        // is populated with the same data as during QueryPendingSets() -
        // which sets the m_localItem for pending deletes.
        if (getLocalItem() == null) {
            if (getSourceLocalItem() != null) {
                setLocalItem(getSourceLocalItem());
            } else {
                setLocalItem(workspace.getMappedLocalPath(getServerItem()));
            }

            // For known delete operations, clear out the sourceLocalItem
            // An undo of an undelete or branch is a delete. The reason for this
            // is that
            // QueryPendingSets() sets the target for pending deletes.
            if ((!operation.isUndo() && isDelete()) || (operation.isUndo() && (isUndelete() || isBranch()))) {
                setSourceLocalItem(null);
            }
        }

        // Do NOT set the download URL here. It may be shelved content or it may
        // be versioned
        // content, depending on the state of the item when we ran get. It's
        // safer not to set
        // either URL here and fetch the URL on demand.
    }

    public PendingChange(
        final String serverItem,
        final String sourceServerItem,
        final int sourceVersionFrom,
        final int sourceDeletionID,
        final int deletionID,
        final LockLevel lockLevel,
        final String localItem,
        final ItemType itemType,
        final int itemID,
        final Calendar creationDate,
        final int version,
        final ChangeType changeType,
        final byte[] hashValue,
        final byte[] uploadHashValue,
        final int encoding,
        final int pendingChangeID,
        final PropertyValue[] properties,
        final boolean isCandidate) {
        this(
            new _PendingChange(
                changeType.getWebServiceObjectExtendedFlags(),
                changeType.getWebServiceObject(),
                creationDate,
                deletionID,
                itemType.getWebServiceObject(),
                encoding,
                itemID,
                LocalPath.nativeToTFS(localItem),
                lockLevel.getWebServiceObject(),
                serverItem,
                null, // sourceLocalItem,
                sourceServerItem,
                sourceVersionFrom,
                sourceDeletionID,
                version,
                hashValue,
                -1, // length,
                uploadHashValue,
                pendingChangeID,
                null, // downloadUrl,
                null, // shelvedDownloadUrl,
                0, // conflictType,
                null, // mergeSources
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, properties)));

        this.isCandidate = isCandidate;
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PendingChange getWebServiceObject() {
        return (_PendingChange) webServiceObject;
    }

    /**
     * Downloads the shelved content for this pending change to a temp file in a
     * new temp directory allocated with {@link TempStorageService}. Throws
     * {@link VersionControlException} this is not a shelved pending change.
     *
     * @param fileName
     *        the file name (not full path) to give the temporary file (must not
     *        be <code>null</code> or empty)
     * @return the temporary file created
     */
    public synchronized File downloadShelvedFileToTempLocation(
        final VersionControlClient client,
        final String fileName) {
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        try {
            final File file = new File(TempStorageService.getInstance().createTempDirectory(), fileName);
            downloadShelvedFile(client, file.getAbsolutePath());
            return file;
        } catch (final IOException e) {
            throw new VersionControlException(e);
        }
    }

    /**
     * Downloads the shelved content for this pending change. Throws
     * {@link VersionControlException} this is not a shelved pending change.
     *
     * @param client
     *        the {@link VersionControlClient} to use to download the file (must
     *        not be <code>null</code>)
     * @param localFileName
     *        where to save the downloaded file contents (must not be
     *        <code>null</code>)
     */
    public synchronized void downloadShelvedFile(final VersionControlClient client, final String localFileName) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(localFileName, "localFileName"); //$NON-NLS-1$

        // The pending change must be a shelved change of a file.
        if (getItemType() != ItemType.FILE) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("PendingChange.PendingChangeIsNotAShelvedFileFormat"), //$NON-NLS-1$
                    getLocalOrServerItem()));
        }

        // If we don't have the download url for the shelved change, get it.
        if (getShelvedDownloadURL() == null || getShelvedDownloadURL().length() == 0) {
            updateMissingProperties(client);
        }

        // There is no shelved file when the shelved url is empty.
        if (getShelvedDownloadURL() == null || getShelvedDownloadURL().length() == 0) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("PendingChange.PendingChangeIsNotAShelvedFileFormat"), //$NON-NLS-1$
                    getLocalOrServerItem()));
        }

        client.downloadFile(new DownloadSpec(getShelvedDownloadURL()), new File(localFileName), true);
    }

    /**
     * Downloads the content of the version of the file against which the change
     * was pended to a temp file in a new temp directory allocated with
     * {@link TempStorageService}.
     *
     * @param fileName
     *        the file name (not full path) to give the temporary file (must not
     *        be <code>null</code> or empty)
     *
     * @return the temporary file created
     */
    public synchronized File downloadBaseFileToTempLocation(final VersionControlClient client, final String fileName) {
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        try {
            final File file = new File(TempStorageService.getInstance().createTempDirectory(), fileName);
            downloadBaseFile(client, file.getAbsolutePath());
            return file;
        } catch (final IOException e) {
            throw new VersionControlException(e);
        }
    }

    /**
     * Downloads the content of the version of the file against which the change
     * was pended.
     *
     * @param localFileName
     *        where to save the downloaded file contents (must not be
     *        <code>null</code>)
     */
    public synchronized void downloadBaseFile(final VersionControlClient client, final String localFileName) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(localFileName, "localFileName"); //$NON-NLS-1$

        if (getItemType() != ItemType.FILE) {
            throw new VersionControlException(
                MessageFormat.format(Messages.getString("PendingChange.PathIsNotAFileFormat"), getLocalOrServerItem())); //$NON-NLS-1$
        }

        if (copyLocalBaseline(client, localFileName)) {
            return;
        }

        // If we don't have the download url for the base, get it.
        if (getDownloadURL() == null || getDownloadURL().length() == 0) {
            updateMissingProperties(client);
        }

        // There is no shelved file when the shelved url is empty. Note that we
        // can't check version == 0 since branch has a download url (branch
        // source)
        if (getDownloadURL() == null || getDownloadURL().length() == 0) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("PendingChange.NoBaseFileForPendingChangeFormat"), //$NON-NLS-1$
                    getLocalOrServerItem()));
        }

        client.downloadFile(new DownloadSpec(getDownloadURL()), new File(localFileName), true);
    }

    /**
     * Queries server for full PendingChange object (including download URL) and
     * populates downloadUrl, shelvedDownloadUrl, SourceServerItem and
     * SourceLocalItem of this object. SourceLocalItem is null most of the time
     * for real PendingChange objects. After this method finishes download URLS
     * can be still empty, if the workspace or shelveset were deleted or we
     * didn't serialize properly all required properties (PendingSetName,
     * PendingSetOwner, IsInShelveset).
     */
    public void updateMissingProperties(final VersionControlClient client) {
        if (pendingSetName != null
            && pendingSetName.length() > 0
            && pendingSetOwner != null
            && pendingSetOwner.length() > 0
            && client != null) {
            PendingSet[] pss = null;
            if (inShelveset) {
                pss = client.queryShelvedChanges(pendingSetName, pendingSetOwner, new ItemSpec[] {
                    new ItemSpec(this)
                }, true);
            } else {
                pss = client.queryPendingSets(new ItemSpec[] {
                    new ItemSpec(this)
                }, true, pendingSetName, pendingSetOwner, true);
            }

            Check.isTrue(
                pss.length == 1,
                MessageFormat.format(
                    "Received more than one PendingSet in UpdateMissingProperties. ServerItem: {0}", //$NON-NLS-1$
                    getServerItem()));

            for (final PendingSet ps : pss) {
                for (final PendingChange pc : ps.getPendingChanges()) {
                    final boolean matchingVersion = getVersion() == pc.getVersion();
                    if (matchingVersion) {
                        setShelvedDownloadURL(pc.getShelvedDownloadURL());
                        setDownloadURL(pc.getDownloadURL());
                        setSourceServerItem(pc.getSourceServerItem());
                        setSourceLocalItem(pc.getSourceLocalItem());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Acquires base content if the pending change is in local workspace. If
     * file is in the baseline folder, copies it from there. If not, downloads
     * it from the server.
     *
     * @param client
     *        the {@link VersionControlClient} to use (must not be
     *        <code>null</code>)
     * @param localFileName
     *        the local file name to copy the baseline to (must not be
     *        <code>null</code>)
     * @return true if this pending change is in local workspace, false if this
     *         is server workspace.
     * @throws VersionControlException
     *         if this is local workspace but we failed to acquire content (e.g.
     *         baseline is deleted and connection to the server failed).
     */
    private boolean copyLocalBaseline(final VersionControlClient client, final String localFileName) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(localFileName, "localFileName"); //$NON-NLS-1$

        final AtomicBoolean handled = new AtomicBoolean(false);

        Check.isTrue(
            pendingSetName != null
                && pendingSetName.length() > 0
                && pendingSetOwner != null
                && pendingSetOwner.length() > 0,
            MessageFormat.format(
                "PendingSetName or PendingSetOwner were not populated for pending change {0}", //$NON-NLS-1$
                toString()));

        if (inShelveset
            || pendingSetName == null
            || pendingSetName.length() == 0
            || pendingSetOwner == null
            || pendingSetOwner.length() == 0) {
            return handled.get();
        }

        final Workspace workspace = client.getRuntimeWorkspaceCache().tryGetWorkspace(pendingSetName, pendingSetOwner);
        if (workspace != null && workspace.getLocation() == WorkspaceLocation.LOCAL) {
            if (isAdd()) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("PendingChange.NoBaseFileForPendingChangeFormat"), //$NON-NLS-1$
                        getLocalOrServerItem()));
            }

            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
            try {
                transaction.execute(new WorkspacePropertiesLocalVersionTransaction() {
                    @Override
                    public void invoke(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
                        final WorkspaceLocalItem lvEntry = lv.getByPendingChange(PendingChange.this);

                        if (null != lvEntry && null != lvEntry.getBaselineFileGUID()) {
                            try {
                                final boolean symlink = PropertyConstants.IS_SYMLINK.equals(
                                    PropertyUtils.selectMatching(
                                        lvEntry.getPropertyValues(),
                                        PropertyConstants.SYMBOLIC_KEY));

                                wp.copyBaselineToTarget(
                                    lvEntry.getBaselineFileGUID(),
                                    localFileName,
                                    lvEntry.getLength(),
                                    lvEntry.getHashValue(),
                                    symlink);
                                handled.set(true);
                            } catch (final Exception e) {
                                /* Could not copy the local baseline */
                            }
                        }
                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }

            if (!handled.get()) {
                // we don't have baseline which we should have, let's hit the
                // server to get it

                // TODO we can compress and store it as a baseline
                final String serverItem = (getSourceServerItem() == null || getSourceServerItem().length() == 0)
                    ? getServerItem() : getSourceServerItem();
                final int versionToDownload = isBranch() ? getSourceVersionFrom() : getVersion();

                // for pending branch we use SourceServerItem (source of
                // branch), but SourceVersionFrom (version of the source)
                // instead of Version since it's not committed

                final Item item =
                    client.getItem(serverItem, new ChangesetVersionSpec(versionToDownload), getDeletionID(), true);

                client.downloadFile(new DownloadSpec(item.getDownloadURL()), new File(localFileName), true);

                handled.set(true);
            }
        }

        return handled.get();
    }

    public synchronized ChangeType getChangeType() {
        return new ChangeType(getWebServiceObject().getChg(), getWebServiceObject().getChgEx());
    }

    public synchronized void setChangeType(final ChangeType changeType) {
        hashCodeValid = false;
        getWebServiceObject().setChg(changeType.getWebServiceObject());
        getWebServiceObject().setChgEx(changeType.getWebServiceObjectExtendedFlags());
    }

    public synchronized ConflictType getConflictType() {
        return ConflictType.fromInteger(getWebServiceObject().getCt());
    }

    public synchronized void setConflictType(final ConflictType conflictType) {
        getWebServiceObject().setCt(conflictType.getValue());
    }

    public synchronized Calendar getCreationDate() {
        return getWebServiceObject().getDate();
    }

    public synchronized void setCreationDate(final Calendar date) {
        getWebServiceObject().setDate(date);
    }

    public synchronized int getDeletionID() {
        return getWebServiceObject().getDid();
    }

    public synchronized void setDeletionID(final int did) {
        hashCodeValid = false;
        getWebServiceObject().setDid(did);
    }

    public synchronized int getEncoding() {
        return getWebServiceObject().getEnc();
    }

    public synchronized void setEncoding(final int enc) {
        getWebServiceObject().setEnc(enc);
    }

    /**
     * @return the MD5 HashValue for the contents of the version of the file the
     *         change is pended against.
     */
    public synchronized byte[] getHashValue() {
        return getWebServiceObject().getHash();
    }

    /**
     * @param hash
     *        the MD5 HashValue for the contents of the version of the file the
     *        change is pended against.
     */
    public synchronized void setHashValue(final byte[] hash) {
        getWebServiceObject().setHash(hash);
    }

    public synchronized String getServerItem() {
        return getWebServiceObject().getItem();
    }

    public synchronized void setServerItem(final String item) {
        hashCodeValid = false;
        getWebServiceObject().setItem(item);
    }

    public synchronized int getItemID() {
        return getWebServiceObject().getItemid();
    }

    public synchronized void setItemID(final int itemid) {
        hashCodeValid = false;
        getWebServiceObject().setItemid(itemid);
    }

    /**
     * Tests whether the local item is <code>null</code>. Skips path conversion,
     * making it faster than calling {@link #getLocalItem()}.
     *
     * @return true if the local item is not <code>null</code>,
     *         <code>false</code> if the local item is <code>null</code>
     */
    public synchronized boolean hasLocalItem() {
        return getWebServiceObject().getLocal() != null;
    }

    /**
     * @return the local item associated with this change. May be null (e.g. if
     *         a rename is pended from a mapped folder to an unmapped server
     *         path).
     */
    public synchronized String getLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getLocal());
    }

    public synchronized void setLocalItem(final String item) {
        hashCodeValid = false;
        getWebServiceObject().setLocal(LocalPath.nativeToTFS(item));
    }

    public synchronized LockLevel getLockLevel() {
        return LockLevel.fromWebServiceObject(getWebServiceObject().getLock());
    }

    public synchronized void setLockLevel(final LockLevel lock) {
        getWebServiceObject().setLock(lock.getWebServiceObject());
    }

    public synchronized int getSourceVersionFrom() {
        return getWebServiceObject().getSvrfm();
    }

    /**
     * @deprecated since TFS 2010
     */
    @Deprecated
    public synchronized int getPendingChangeID() {
        return getWebServiceObject().getPcid();
    }

    /**
     * @deprecated since TFS 2010
     */
    @Deprecated
    public synchronized void setPendingChangeID(final int pcid) {
        getWebServiceObject().setPcid(pcid);
    }

    public synchronized String getSourceServerItem() {
        return getWebServiceObject().getSrcitem();
    }

    public synchronized void setSourceServerItem(final String item) {
        hashCodeValid = false;
        getWebServiceObject().setSrcitem(item);
    }

    public synchronized String getSourceLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getSrclocal());
    }

    public synchronized void setSourceLocalItem(final String item) {
        getWebServiceObject().setSrclocal(LocalPath.nativeToTFS(item));
    }

    public synchronized ItemType getItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getType());
    }

    public synchronized void setItemType(final ItemType type) {
        hashCodeValid = false;
        getWebServiceObject().setType(type.getWebServiceObject());
    }

    /**
     * @return the MD5 HashValue for the file that is currently associated with
     *         this pending change.
     */
    public synchronized byte[] getUploadContentHashValue() {
        return getWebServiceObject().getUhash();
    }

    /**
     * @param hash
     *        the MD5 HashValue for the file that is currently associated with
     *        this pending change.
     */
    public synchronized void setUploadContentHashValue(final byte[] hash) {
        getWebServiceObject().setUhash(hash);
    }

    public synchronized int getVersion() {
        return getWebServiceObject().getVer();
    }

    public synchronized void setVersion(final int version) {
        hashCodeValid = false;
        getWebServiceObject().setVer(version);
    }

    /**
     * If false, the change is actually pended otherwise this is a candidate
     * change, which we have detected which the user might choose to accept or
     * discard.
     */
    public synchronized boolean isCandidate() {
        return isCandidate;
    }

    public synchronized void setCandidate(final boolean value) {
        isCandidate = value;
    }

    /**
     * Name of the PendingSet that this PendingChange is part of. This
     * denormalization is needed to download content of the base or shelved
     * file. The setter should be used only by the code that creates object.
     */
    public synchronized String getPendingSetName() {
        return pendingSetName;
    }

    public synchronized void setPendingSetName(final String name) {
        pendingSetName = name;
    }

    /**
     * Owner of the PendingSet that this PendingChange is part of. This
     * denormalization is needed to download content of the base or shelved
     * file. The setter should be used only by the code that creates object.
     */
    public synchronized String getPendingSetOwner() {
        return pendingSetOwner;
    }

    public synchronized void setPendingSetOwner(final String owner) {
        pendingSetOwner = owner;
    }

    /**
     * Unique Owner of the PendingSet that this PendingChange is part of. This
     * denormalization is needed to download content of the base or shelved
     * file. The setter should be used only by the code that creates object.
     */
    public synchronized String getPendingSetOwnerDisplay() {
        return pendingSetOwnerDisplay;
    }

    public synchronized void setPendingSetOwnerDisplay(final String owner) {
        pendingSetOwnerDisplay = owner;
    }

    /**
     * Flag indicating if this pending change is part of shelveset or workspace.
     * This denormalization is needed to download content of the base or shelved
     * file. The setter should be used only by the code that creates object.
     */
    public synchronized boolean isInShelveset() {
        return inShelveset;
    }

    public synchronized void setInShelveset(final boolean value) {
        inShelveset = value;
    }

    @Override
    public int compareTo(final PendingChange other) {
        // First we sort by path.
        final int result = ServerPath.compareTopDown(getServerItem(), other.getServerItem());
        if (result != 0) {
            return result;
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
        }

        return 0;
    }

    /**
     * Extracts all the non-null server items from an array of pending changes
     * and returns a new array containing them.
     *
     * @param changes
     *        the changes to examine for server items.
     * @return a new array of server item strings.
     */
    public static String[] toServerItems(final PendingChange[] changes) {
        if (changes == null) {
            return new String[0];
        }

        final List<String> items = new ArrayList<String>(changes.length);

        for (int i = 0; i < changes.length; i++) {
            if (changes[i].getServerItem() != null) {
                items.add(changes[i].getServerItem());
            }
        }

        return items.toArray(new String[items.size()]);
    }

    /**
     * Extracts all the non-null local items from an array of pending changes
     * and returns a new array containing them.
     *
     * @param changes
     *        the changes to examine for local items.
     * @return a new array of local item strings.
     */
    public static String[] toLocalItems(final PendingChange[] changes) {
        if (changes == null) {
            return new String[0];
        }

        final List<String> items = new ArrayList<String>(changes.length);

        for (int i = 0; i < changes.length; i++) {
            if (changes[i].getLocalItem() != null) {
                items.add(changes[i].getLocalItem());
            }
        }

        return items.toArray(new String[items.size()]);
    }

    /**
     * Like {@link #equals(Object)}, but does not test the lock level and
     * version. This method might be useful for some UI classes that need to
     * compare changes between pending change sets (e.g. during a refresh).
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equalsIgnoringLockLevelAndVersion(final Object obj) {
        return equals(obj, true, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        return equals(obj, false, false);
    }

    /**
     * Tests for equality, optionally ignoring some fields.
     *
     * @param obj
     *        the other object to test this object against. If null, return is
     *        always false.
     * @param ignoreLockLevel
     *        if true, the lock level fields in both objects are not tested. If
     *        false, the lock level fields are tested for equality.
     * @param ignoreVersion
     *        if true, the version fields in both objects are not tested. If
     *        false, the version fields are tested for equality.
     * @return true if the objects are equal according to the parameters given,
     *         false if they are not.
     *
     */
    private boolean equals(final Object obj, final boolean ignoreLockLevel, final boolean ignoreVersion) {
        /*
         * This method gets called in some tight loops for UI code, so make sure
         * to keep it efficient.
         */

        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ((obj instanceof PendingChange) == false) {
            return false;
        }

        final PendingChange other = (PendingChange) obj;

        /*
         * The local items are equal if either are null or that both have equal
         * value. In the case that one is null and the other is not then
         * equality of the server path will correctly pick if they are equal or
         * not. This is needed because in the case when undoing a branch of file
         * that has not been copied locally. The cached pending change will have
         * a local path but the server returned one will not. Bit of a hack
         * really.
         *
         * Because large lists of PendingChange objects are often compared (for
         * sorting in controls, etc.), the raw local path (always Windows style)
         * is compared. This saves the TFS to native conversion.
         */
        final String thisLocalRawItem = getWebServiceObject().getLocal();
        final String otherLocalRawItem = other.getWebServiceObject().getLocal();

        final boolean localItemsEqual =
            thisLocalRawItem == null || otherLocalRawItem == null || otherLocalRawItem.equals(thisLocalRawItem);

        if (localItemsEqual == false) {
            return false;
        }

        /*
         * If the file is not yet added to the server than the source server
         * item will be null.
         */
        final String thisSourceServerItem = getSourceServerItem();
        final String otherSourceServerItem = other.getSourceServerItem();

        final boolean sourceServerItemsEqual = (otherSourceServerItem == null || thisSourceServerItem == null)
            ? otherSourceServerItem == thisSourceServerItem : otherSourceServerItem.equals(thisSourceServerItem);

        if (sourceServerItemsEqual == false) {
            return false;
        }

        /*
         * Allow for the ignoreLockLevel override and null lock level objects.
         */

        boolean lockLevelsEqual = true;
        if (ignoreLockLevel == false) {
            final LockLevel thisLockLevel = getLockLevel();
            final LockLevel otherLockLevel = other.getLockLevel();

            lockLevelsEqual = (otherLockLevel == null || thisLockLevel == null) ? otherLockLevel == thisLockLevel
                : otherLockLevel == thisLockLevel;
        }

        /*
         * Allow for the ignoreVersion override and null lock level objects.
         */

        final boolean versionsEqual = (ignoreVersion) ? true : other.getVersion() == getVersion();

        // check for propertyValues
        if (other.getChangeType().equals(ChangeType.PROPERTY)) {
            final PropertyValue[] otherPropertyValues = other.getPropertyValues();
            if (otherPropertyValues != null && !otherPropertyValues.equals(getPropertyValues())) {
                return false;
            }

        }

        /*
         * We skip a few fields that will change during the lifetime of a
         * pending change.
         */
        return (other.getChangeType().equals(getChangeType())
            && other.getDeletionID() == getDeletionID()
            && other.getItemType() == getItemType()
            && other.getItemID() == getItemID()
            && localItemsEqual
            && lockLevelsEqual
            && other.getServerItem().equals(getServerItem())
            && sourceServerItemsEqual
            && versionsEqual);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (!hashCodeValid) {
            int result = 17;

            /*
             * Lock level is excluded because equals can be told to ignore it,
             * and we would violate the equals contract if two APendingChange
             * objects were equal but did not have the same hash code.
             */

            // Fetch fields once for speed
            final String sourceServerItem = getSourceServerItem();
            final ChangeType changeType = getChangeType();
            final ItemType itemType = getItemType();
            final String localItem = getLocalItem();
            final String serverItem = getServerItem();

            result = 37 * result + ((sourceServerItem == null) ? 0 : sourceServerItem.hashCode());
            result = 37 * result + ((changeType == null) ? 0 : changeType.hashCode());
            result = 37 * result + getDeletionID();
            result = 37 * result + ((itemType == null) ? 0 : itemType.hashCode());
            result = 37 * result + getItemID();
            result = 37 * result + ((localItem == null) ? 0 : localItem.hashCode());
            result = 37 * result + ((serverItem == null) ? 0 : serverItem.hashCode());
            result = 37 * result + getVersion();

            hashCode = result;
            hashCodeValid = true;
        }

        return hashCode;
    }

    /**
     * The localized string appropriate for normal text lock level display
     * (command-line client "status /format:detailed" command, for example).
     *
     * @return the localized short string that describes the lock level. If the
     *         lock member is unchanged, "none" is returned.
     */
    public String getLockLevelName() {
        return getLockLevel().toUIString();
    }

    /**
     * The localized string appropriate for very space-constrained lock level
     * display (command-line client "status /format:brief" command, for
     * example). The strings returned are one character long.
     *
     * @return the localized (very) short string that describes the lock level
     *         for this pending change.
     */
    public String getLockLevelShortName() {
        return getLockLevel().toShortUIString();
    }

    @Override
    public String toString() {
        return getWebServiceObject().getItem()
            + ":" //$NON-NLS-1$
            + (getWebServiceObject().getLock() != null ? getWebServiceObject().getLock().getName() : "null"); //$NON-NLS-1$
    }

    public synchronized boolean isUndone() {
        return undone;
    }

    public synchronized boolean isLocalItemDelete() {
        return localItemDelete;
    }

    public synchronized long getLength() {
        return getWebServiceObject().getLen();
    }

    public synchronized void setLength(final long value) {
        getWebServiceObject().setLen(value);
    }

    public synchronized String getDownloadURL() {
        return getWebServiceObject().getDurl();
    }

    public synchronized void setDownloadURL(final String url) {
        getWebServiceObject().setDurl(url);
    }

    public synchronized String getShelvedDownloadURL() {
        return getWebServiceObject().getShelvedurl();
    }

    public synchronized void setShelvedDownloadURL(final String url) {
        getWebServiceObject().setShelvedurl(url);
    }

    public static boolean isSourceRename(final ChangeType changeType) {
        Check.notNull(changeType, "changeType"); //$NON-NLS-1$

        /*
         * It is a source rename if it has the SOURCE_RENAME flag set, but not
         * BRANCH, RENAME, or ADD.
         */
        return changeType.contains(ChangeType.SOURCE_RENAME)
            && changeType.contains(ChangeType.BRANCH) == false
            && changeType.contains(ChangeType.RENAME) == false
            && changeType.contains(ChangeType.ADD) == false;
    }

    public synchronized MergeSource[] getMergeSources() {
        return (MergeSource[]) WrapperUtils.wrap(MergeSource.class, getWebServiceObject().getMergeSources());
    }

    /**
     * Tests whether this pending change has local content which differs from
     * the server item. Useful for finding users' changes during "return online"
     * and post-build reconcile operations.
     *
     * @return <code>true</code> if the pending change is a
     *         {@link ChangeType#EDIT} and the local item's contents are
     *         different than the server's contents, <code>false</code>
     *         otherwise
     * @throws CanceledException
     *         if the hash operation was cancelled via the default
     *         {@link TaskMonitor}
     */
    public synchronized boolean hasContentChange() throws CanceledException {
        final ChangeType changeType = getChangeType();
        final String localItem = getLocalItem();

        if (changeType.contains(ChangeType.EDIT) == false || changeType.contains(ChangeType.ADD) || localItem == null) {
            return false;
        }

        /*
         * Branches will always differ.
         */
        if (changeType.contains(ChangeType.BRANCH)) {
            return true;
        }

        if (new File(localItem).exists() == false) {
            return false;
        }

        try {
            return Arrays.equals(
                CheckinEngine.computeMD5Hash(localItem, TaskMonitorService.getTaskMonitor()),
                getHashValue()) == false;
        } catch (final CoreCancelException e) {
            throw new CanceledException();
        }
    }

    /**
     * Determine whether this pending change is unchanged using either local
     * workspace or server workspace.
     *
     * @param workspace
     * @return
     */
    public boolean isUnchanged(final Workspace workspace) {
        final ChangeType changeType = getChangeType();

        if (!changeType.remove(ChangeType.LOCK).equals(ChangeType.EDIT)) {
            return false;
        }

        if (workspace.isLocalWorkspace()) {
            return isUnchangedInLocalWorkspace(workspace);
        } else {
            return isUnchangedInServerWorkspace();
        }
    }

    /**
     * Determine whether this pending change is unchanged pending change. Return
     * true only if the pending change is only in type EDIT and no content
     * changes.
     *
     * @return <code>true</code> if the pending change is
     *         {@link ChangeType#EDIT}, and the local item's has no content
     *         changes, <code>false</code> otherwise
     * @throws CanceledException
     *         if the core operation was cancelled via the default
     *         {@link TaskMonitor}
     */
    public boolean isUnchangedInServerWorkspace() throws CanceledException {
        return !hasContentChange();
    }

    /**
     * Tests whether this pending change has local content which differs from
     * local baseline. Useful for determine pending change is unchanged pending
     * change when using local workspace.
     *
     * @return <code>true</code> if the pending change is a
     *         {@link ChangeType#EDIT} and the local item's contents are same as
     *         baseline, <code>false</code> otherwise
     * @throws CanceledException
     *         if the hash operation was cancelled via the default
     *         {@link TaskMonitor}
     */
    public synchronized boolean isUnchangedInLocalWorkspace(final Workspace workspace) throws CanceledException {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.isTrue(workspace.isLocalWorkspace());

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        final byte[][] hashValues = new byte[2][];

        try {
            transaction.execute(new WorkspacePropertiesLocalVersionTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
                    final WorkspaceLocalItem lvEntry = lv.getByPendingChange(PendingChange.this);
                    final String localItem = getLocalItem();
                    if (localItem != null
                        && lvEntry != null
                        && lvEntry.hasBaselineFileGUID()
                        && lvEntry.hasHashValue()) {
                        try {
                            hashValues[0] = lvEntry.getHashValue();
                            hashValues[1] =
                                CheckinEngine.computeMD5Hash(localItem, TaskMonitorService.getTaskMonitor());
                        } catch (final CoreCancelException e) {
                            throw new CanceledException();
                        }
                    }
                }
            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }
        }

        return Arrays.equals(hashValues[0], hashValues[1]);
    }

    public boolean isAdd() {
        return getChangeType().contains(ChangeType.ADD);
    }

    public boolean isEdit() {
        return getChangeType().contains(ChangeType.EDIT);
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

    /**
     * Gets the local path if it's available, server path otherwise.
     */
    private String getLocalOrServerItem() {
        final String localItem = getLocalItem();

        if (localItem != null && localItem.length() > 0) {
            return localItem;
        }

        return getServerItem();
    }

    public PropertyValue[] getPropertyValues() {
        // TODO remove the selectUnique
        return PropertyUtils.selectUnique(
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getPropertyValues()));
    }

    /**
     * Restores a candidate delete on disk, if it's a folder, walks children
     * recursively and restores them as well.
     *
     * @param workspace
     *        the workspace to restore changes in (must not be <code>null</code>
     *        )
     */
    public void restoreCandidateDelete(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        if (!isCandidate || !getChangeType().contains(ChangeType.DELETE)) {
            throw new VersionControlException(Messages.getString("PendingChange.ChangeNotACandidateDelete")); //$NON-NLS-1$
        }

        if (workspace.getLocation() == WorkspaceLocation.LOCAL) {
            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
            try {
                transaction.execute(new WorkspacePropertiesLocalVersionTransaction() {
                    @Override
                    public void invoke(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
                        for (final WorkspaceLocalItem lvEntry : lv.queryByLocalItem(
                            getLocalItem(),
                            getItemType() == ItemType.FILE ? RecursionType.NONE : RecursionType.FULL,
                            null)) {
                            if (null != lvEntry.getBaselineFileGUID()) {
                                try {
                                    final boolean symlink = PropertyConstants.IS_SYMLINK.equals(
                                        PropertyUtils.selectMatching(
                                            lvEntry.getPropertyValues(),
                                            PropertyConstants.SYMBOLIC_KEY));

                                    wp.copyBaselineToTarget(
                                        lvEntry.getBaselineFileGUID(),
                                        lvEntry.getLocalItem(),
                                        lvEntry.getLength(),
                                        lvEntry.getHashValue(),
                                        symlink);

                                    if (workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                                        && lvEntry.getCheckinDate() != -1) {
                                        final FileSystemAttributes attrs =
                                            FileSystemUtils.getInstance().getAttributes(lvEntry.getLocalItem());
                                        boolean restoreReadOnly = false;

                                        if (attrs != null && attrs.isReadOnly()) {
                                            attrs.setReadOnly(false);
                                            FileSystemUtils.getInstance().setAttributes(lvEntry.getLocalItem(), attrs);
                                            restoreReadOnly = true;
                                        }

                                        new File(lvEntry.getLocalItem()).setLastModified(
                                            DotNETDate.fromWindowsFileTimeUTC(
                                                lvEntry.getCheckinDate()).getTimeInMillis());

                                        if (restoreReadOnly) {
                                            attrs.setReadOnly(true);
                                            FileSystemUtils.getInstance().setAttributes(lvEntry.getLocalItem(), attrs);
                                        }
                                    }
                                } catch (final Exception e) {
                                    workspace.getClient().getEventEngine().fireNonFatalError(
                                        new NonFatalErrorEvent(EventSource.newFromHere(), workspace, e));
                                }
                            } else if (lvEntry.getItemType() == ItemType.FOLDER
                                && !new File(lvEntry.getLocalItem()).exists()) {
                                new File(lvEntry.getLocalItem()).mkdirs();
                            }
                        }
                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }
        }
    }
}
