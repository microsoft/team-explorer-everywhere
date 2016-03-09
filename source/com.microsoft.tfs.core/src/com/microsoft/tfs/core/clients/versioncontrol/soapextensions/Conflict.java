// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.AutoResolveOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.MergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.PropertiesMergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.HashUtils;

import ms.tfs.versioncontrol.clientservices._03._ChangeType;
import ms.tfs.versioncontrol.clientservices._03._Conflict;

/**
 * Represents a single conflict between two items.
 *
 * @since TEE-SDK-10.1
 */
public final class Conflict extends WebServiceObjectWrapper implements Comparable<Conflict> {
    private static final Log log = LogFactory.getLog(Conflict.class);

    /**
     * Local and server path, for display.
     */
    private String localPath;
    private String serverPath;

    /**
     * Set with {@link #setMergedFileName(String)}, holds the output file name.
     */
    private String mergedFileName = null;

    /**
     * Set with {@link #setResolutionOptions(ResolutionOptions)}, holds options
     * desired for use when this conflict is resolved.
     */
    private ResolutionOptions resolutionOptions = null;

    /**
     * Set with {@link #setContentMergeSummary(MergeSummary)} when the conflict
     * is merged, contains a summary of the result of the merge.
     */
    private MergeSummary mergeSummary = null;

    /**
     * Set with {@link #mergeProperties(Workspace)}.
     */
    private PropertiesMergeSummary propertiesMergeSummary = null;

    /**
     * Cached calculated file name for this conflict.
     */
    private String fileName;

    /**
     * Not populated until {@link #downloadProperties(Workspace)} or
     * {@link #mergeProperties(Workspace)} is called.
     */
    private PropertyValue[] yourProperties;
    private PropertyValue[] theirProperties;
    private PropertyValue[] baseProperties;

    /** True if this conflict was resolved automatically. */
    private boolean autoResolved;

    private byte[] localHashValue;
    private long localFileLastModifiedDateUsedForHashValue;

    private long localFileLastModifiedDateUsedForThreeWayMerge;

    private Boolean mergeValidForFileType;

    public Conflict(final _Conflict conflict) {
        super(conflict);
    }

    /**
     * Creates a clone of the given conflict. This is useful because conflict
     * resolution is eaiser when it can modify a copy, and discard the copy if
     * things go bad.
     *
     * @param conflict
     *        the conflict to copy (must not be <code>null</code>)
     */
    public Conflict(final Conflict conflict) {
        this(cloneWebServiceObject(conflict.getWebServiceObject()));
    }

    private static _Conflict cloneWebServiceObject(final _Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        /*
         * To make a full clone, mutable field types must be cloned (flag sets
         * and arrays).
         */

        return new _Conflict(
            conflict.getCid(),
            conflict.getPcid(),
            (conflict.getYchg() != null) ? (_ChangeType) conflict.getYchg().clone() : null,
            conflict.getYchgEx(),
            conflict.getYsitem(),
            conflict.getYsitemsrc(),
            conflict.getYenc(),
            conflict.getYprop(),
            conflict.getYtype(),
            conflict.getYver(),
            conflict.getYitemid(),
            conflict.getYdid(),
            (conflict.getYlchg() != null) ? (_ChangeType) conflict.getYlchg().clone() : null,
            conflict.getYlchgEx(),
            conflict.getYlmver(),
            conflict.getBsitem(),
            conflict.getBenc(),
            conflict.getBprop(),
            conflict.getBitemid(),
            conflict.getBver(),
            (conflict.getBhash() != null) ? (byte[]) conflict.getBhash().clone() : null,
            conflict.getBdid(),
            conflict.getBtype(),
            (conflict.getBchg() != null) ? (_ChangeType) conflict.getBchg().clone() : null,
            conflict.getBchgEx(),
            conflict.getTitemid(),
            conflict.getTver(),
            conflict.getTsitem(),
            conflict.getTenc(),
            conflict.getTprop(),
            (conflict.getThash() != null) ? (byte[]) conflict.getThash().clone() : null,
            conflict.getTdid(),
            conflict.getTtype(),
            conflict.getTlmver(),
            conflict.getTverf(),
            conflict.getTctyp(),
            conflict.isIsc(),
            conflict.getTsn(),
            conflict.getTson(),
            conflict.getSrclitem(),
            conflict.getTgtlitem(),
            conflict.getCtype(),
            conflict.getReason(),
            conflict.isIsnamecflict(),
            conflict.isIsforced(),
            conflict.getRes(),
            conflict.isIsresolved(),
            conflict.getBdurl(),
            conflict.getTdurl(),
            conflict.getYdurl(),
            conflict.getCopt());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Conflict getWebServiceObject() {
        return (_Conflict) webServiceObject;
    }

    @Override
    public int compareTo(final Conflict other) {
        if (getYourServerItem() != null && other.getYourServerItem() != null) {
            final int num = ServerPath.compareTopDown(getYourServerItem(), other.getYourServerItem());
            if (num != 0) {
                return num;
            }
        }

        if (getTheirServerItem() != null && other.getTheirServerItem() != null) {
            final int num = ServerPath.compareTopDown(getTheirServerItem(), other.getTheirServerItem());
            if (num != 0) {
                return num;
            }
        }

        if (getConflictID() > other.getConflictID()) {
            return 1;
        }

        if (getConflictID() < other.getConflictID()) {
            return -1;
        }

        return 0;
    }

    public ChangeType getBaseChangeType() {
        return new ChangeType(getWebServiceObject().getBchg(), getWebServiceObject().getBchgEx());
    }

    public int getBaseDeletionID() {
        return getWebServiceObject().getBdid();
    }

    public FileEncoding getBaseEncoding() {
        return new FileEncoding(getWebServiceObject().getBenc());
    }

    public int getBasePropertyID() {
        return getWebServiceObject().getBprop();
    }

    public byte[] getBaseHashValue() {
        return getWebServiceObject().getBhash();
    }

    public int getBaseItemID() {
        return getWebServiceObject().getBitemid();
    }

    public ItemType getBaseItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getBtype());
    }

    public String getBaseServerItem() {
        return getWebServiceObject().getBsitem();
    }

    public int getBaseVersion() {
        return getWebServiceObject().getBver();
    }

    public int getConflictID() {
        return getWebServiceObject().getCid();
    }

    public int getPendingChangeID() {
        return getWebServiceObject().getPcid();
    }

    public int getReason() {
        return getWebServiceObject().getReason();
    }

    public Resolution getResolution() {
        return Resolution.fromWebServiceObject(getWebServiceObject().getRes());
    }

    public String getSourceLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getSrclitem());
    }

    public String getTargetLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getTgtlitem());
    }

    public ChangeType getTheirChangeType() {
        /*
         * Not the most elegant way to construct the change type, but it works
         * for this special case where all the change information is in an
         * "extra flags" integer format. Depends on the constructor to simply OR
         * the values together and not filter non-extended change types from the
         * second parameter.
         */
        return new ChangeType(ChangeType.NONE.getWebServiceObject(), getWebServiceObject().getTctyp());
    }

    public int getTheirDeletionID() {
        return getWebServiceObject().getTdid();
    }

    public FileEncoding getTheirEncoding() {
        return new FileEncoding(getWebServiceObject().getTenc());
    }

    public int getTheirPropertyID() {
        return getWebServiceObject().getTprop();
    }

    public byte[] getTheirHashValue() {
        return getWebServiceObject().getThash();
    }

    public int getTheirItemID() {
        return getWebServiceObject().getTitemid();
    }

    public ItemType getTheirItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getTtype());
    }

    public int getTheirLastMergedVersion() {
        return getWebServiceObject().getTlmver();
    }

    public String getTheirServerItem() {
        return getWebServiceObject().getTsitem();
    }

    public int getTheirVersion() {
        return getWebServiceObject().getTver();
    }

    public int getTheirVersionFrom() {
        return getWebServiceObject().getTverf();
    }

    public ConflictType getType() {
        return ConflictType.fromWebServiceObject(getWebServiceObject().getCtype());
    }

    public ChangeType getYourChangeType() {
        return new ChangeType(getWebServiceObject().getYchg(), getWebServiceObject().getYchgEx());
    }

    public int getYourDeletionID() {
        return getWebServiceObject().getYdid();
    }

    public FileEncoding getYourEncoding() {
        return new FileEncoding(getWebServiceObject().getYenc());
    }

    public int getYourPropertyID() {
        return getWebServiceObject().getYprop();
    }

    public int getYourItemID() {
        return getWebServiceObject().getYitemid();
    }

    public ItemType getYourItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getYtype());
    }

    public int getYourLastMergedVersion() {
        return getWebServiceObject().getYlmver();
    }

    public ChangeType getYourLocalChangeType() {
        return new ChangeType(getWebServiceObject().getYlchg(), getWebServiceObject().getYlchgEx());
    }

    public String getYourServerItem() {
        return getWebServiceObject().getYsitem();
    }

    public String getYourServerItemSource() {
        return getWebServiceObject().getYsitemsrc();
    }

    public int getYourVersion() {
        return getWebServiceObject().getYver();
    }

    public ConflictOptions getConflictOptions() {
        return ConflictOptions.fromIntFlags(getWebServiceObject().getCopt());
    }

    public boolean isForced() {
        return getWebServiceObject().isIsforced();
    }

    public boolean isNamespaceConflict() {
        return getWebServiceObject().isIsnamecflict();
    }

    public boolean isShelvesetConflict() {
        return getWebServiceObject().isIsc();
    }

    public boolean isResolved() {
        return getWebServiceObject().isIsresolved();
    }

    public void setBaseChangeType(final ChangeType baseChangeType) {
        getWebServiceObject().setBchg(baseChangeType.getWebServiceObject());
    }

    public void setBaseDeletionID(final int baseDeletionID) {
        getWebServiceObject().setBdid(baseDeletionID);
    }

    public void setBaseEncoding(final int baseEncoding) {
        getWebServiceObject().setBenc(baseEncoding);
    }

    public void setBaseHashValue(final byte[] baseHashValue) {
        getWebServiceObject().setBhash(baseHashValue);
    }

    public void setBaseItemID(final int baseItemID) {
        getWebServiceObject().setBitemid(baseItemID);
    }

    public void setBaseItemType(final ItemType baseItemType) {
        getWebServiceObject().setBtype(baseItemType.getWebServiceObject());
    }

    public void setBaseServerItem(final String baseServerItem) {
        getWebServiceObject().setBsitem(baseServerItem);
    }

    public void setBaseVersion(final int baseVersion) {
        getWebServiceObject().setBver(baseVersion);
    }

    public void setConflictID(final int conflictID) {
        getWebServiceObject().setCid(conflictID);
    }

    public void setForced(final boolean isForced) {
        getWebServiceObject().setIsforced(isForced);
    }

    public void setNamespaceConflict(final boolean isNamespaceConflict) {
        getWebServiceObject().setIsnamecflict(isNamespaceConflict);
    }

    public void setResolved(final boolean isResolved) {
        getWebServiceObject().setIsresolved(isResolved);
    }

    public void setPendingChangeID(final int pendingChangeID) {
        getWebServiceObject().setPcid(pendingChangeID);
    }

    public void setReason(final int reason) {
        getWebServiceObject().setReason(reason);
    }

    public void setResolution(final Resolution resolution) {
        getWebServiceObject().setRes(resolution.getWebServiceObject());
    }

    public void setSourceLocalItem(final String sourceLocalItem) {
        getWebServiceObject().setSrclitem(LocalPath.nativeToTFS(sourceLocalItem));
    }

    public void setTargetLocalItem(final String targetLocalItem) {
        getWebServiceObject().setTgtlitem(LocalPath.nativeToTFS(targetLocalItem));
    }

    public void setTheirDeletionID(final int theirDeletionID) {
        getWebServiceObject().setTdid(theirDeletionID);
    }

    public void setTheirEncoding(final int theirEncoding) {
        getWebServiceObject().setTenc(theirEncoding);
    }

    public void setTheirHashValue(final byte[] theirHashValue) {
        getWebServiceObject().setThash(theirHashValue);
    }

    public void setTheirItemID(final int theirItemID) {
        getWebServiceObject().setTitemid(theirItemID);
    }

    public void setTheirItemType(final ItemType theirItemType) {
        getWebServiceObject().setTtype(theirItemType.getWebServiceObject());
    }

    public void setTheirLastMergedVersion(final int theirLastMergedVersion) {
        getWebServiceObject().setTlmver(theirLastMergedVersion);
    }

    public void setTheirServerItem(final String theirServerItem) {
        getWebServiceObject().setTsitem(theirServerItem);
    }

    public void setTheirVersion(final int theirVersion) {
        getWebServiceObject().setTver(theirVersion);
    }

    public void setType(final ConflictType type) {
        getWebServiceObject().setCtype(type.getWebServiceObject());
    }

    public void setYourChangeType(final ChangeType yourChangeType) {
        getWebServiceObject().setYchg(yourChangeType.getWebServiceObject());
    }

    public void setYourDeletionID(final int yourDeletionID) {
        getWebServiceObject().setYdid(yourDeletionID);
    }

    public void setYourEncoding(final int yourEncoding) {
        getWebServiceObject().setYenc(yourEncoding);
    }

    public void setYourItemID(final int yourItemID) {
        getWebServiceObject().setYitemid(yourItemID);
    }

    public void setYourItemType(final ItemType yourItemType) {
        getWebServiceObject().setYtype(yourItemType.getWebServiceObject());
    }

    public void setYourLastMergedVersion(final int yourLastMergedVersion) {
        getWebServiceObject().setYlmver(yourLastMergedVersion);
    }

    public void setYourLocalChangeType(final ChangeType yourLocalChangeType) {
        getWebServiceObject().setYlchg(yourLocalChangeType.getWebServiceObject());
    }

    public void setYourServerItem(final String yourServerItem) {
        getWebServiceObject().setYsitem(yourServerItem);
    }

    public void setYourServerItemSource(final String yourServerItemSource) {
        getWebServiceObject().setYsitemsrc(yourServerItemSource);
    }

    public void setYourVersion(final int yourVersion) {
        getWebServiceObject().setYver(yourVersion);
    }

    public void setConflictOptions(final ConflictOptions options) {
        getWebServiceObject().setCopt(options.toIntFlags());
    }

    public String getBaseDownloadURL() {
        return getWebServiceObject().getBdurl();
    }

    public String getTheirDownloadURL() {
        return getWebServiceObject().getTdurl();
    }

    public String getYourDownloadURL() {
        return getWebServiceObject().getYdurl();
    }

    public String getTheirShelvesetName() {
        return getWebServiceObject().getTsn();
    }

    public String getTheirShelvesetOwnerName() {
        return getWebServiceObject().getTson();
    }

    /*
     * Manual additions to the TFS object.
     */

    public String getMergedFileName() {
        return mergedFileName;
    }

    public void setMergedFileName(final String file) {
        if (file == null || file.length() == 0) {
            mergedFileName = null;
        } else {
            mergedFileName = LocalPath.canonicalize(file);
        }
    }

    public void setResolutionOptions(final ResolutionOptions options) {
        if (options == null) {
            resolutionOptions = new ResolutionOptions();
        } else {
            resolutionOptions = options;
        }
    }

    public ResolutionOptions getResolutionOptions() {
        if (resolutionOptions == null) {
            resolutionOptions = new ResolutionOptions();
        }

        return resolutionOptions;
    }

    /**
     * Sets the summary of the merge operation.
     *
     * @param summary
     *        the merge summary.
     */
    public void setContentMergeSummary(final MergeSummary summary) {
        mergeSummary = summary;
    }

    /**
     * Gets the summary of the work performed by a content merge for this
     * getWebServiceObject(). Null if not set (no merge done).
     *
     * @return the merge summary (null if not set).
     */
    public MergeSummary getContentMergeSummary() {
        return mergeSummary;
    }

    /**
     * Tests whether this conflict needs a content merge.
     *
     * @return <code>true</code> if this conflict's content can be merged,
     *         <code>false</code> if the type of conflict would not allow a
     *         content merge. Does not check whether this conflict has already
     *         been resolved (can return <code>true</code> after a resolution
     *         has been set)
     */
    public boolean canMergeContent() {
        final boolean isGetCheckin = getType() == ConflictType.GET || getType() == ConflictType.CHECKIN;
        final boolean isNamespace = isGetCheckin && isNamespaceConflict();

        // Unlike the others, this is a negative check. Bail out now if this
        // item is a folder
        // or this is a namespace conflict.
        if (getYourItemType() == ItemType.FOLDER || isNamespace) {
            return false;
        }

        // We only need to merge content if there's an edit involved.
        if (getYourChangeType().contains(ChangeType.EDIT) && getBaseChangeType().contains(ChangeType.EDIT)) {
            return true;
        }

        // Additionally for merge, the change being pended (BaseChangeType) must
        // include edit.
        if (getType() == ConflictType.MERGE && getBaseChangeType().contains(ChangeType.EDIT)) {
            // We already had an edit locally, so merge with it.
            if (getYourLocalChangeType().contains(ChangeType.EDIT)) {
                return true;
            }

            // It was forced, so do the content merge.
            if (isForced()) {
                return true;
            }

            // rollback conflicts don't have lastmerged versions.
            if (getBaseChangeType().contains(ChangeType.ROLLBACK) == false) {
                // There have been changes that need to be merged.
                if (getTheirLastMergedVersion() != getBaseVersion() || getYourLastMergedVersion() != getYourVersion()) {
                    return true;
                }
            }
        }

        // Content should not be merged for this conflict.
        return false;
    }

    /**
     * @return this conflict's local path or intended local path
     */
    public String getLocalPath() {
        if (localPath == null) {
            if (getSourceLocalItem() != null && getSourceLocalItem().length() > 0) {
                localPath = getSourceLocalItem();
            } else if (getTargetLocalItem() != null) {
                localPath = getTargetLocalItem();
            } else {
                localPath = ""; //$NON-NLS-1$
            }
        }

        return localPath;
    }

    /**
     * @return this conflict's server path
     */
    public String getServerPath() {
        if (serverPath == null) {
            if (getYourServerItemSource() != null && getYourServerItemSource().length() > 0) {
                serverPath = getYourServerItemSource();
            } else if (getYourServerItem() != null && getYourServerItem().length() > 0) {
                serverPath = getYourServerItem();
            } else if (getTheirServerItem() != null) {
                serverPath = getTheirServerItem();
            } else {
                serverPath = ""; //$NON-NLS-1$
            }
        }

        return serverPath;
    }

    /**
     * The shelveset display name consists of the shelveset name and username if
     * it's different than the authorized user. If this is an unshelve conflict,
     * but the shelveset was deleted, returns "Shelveset deleted". If this is
     * not an unshelve conflict, returns null.
     *
     * @return The shelveset display name (may be <code>null</code>)
     */
    public String getTheirShelvesetDisplayName(final Workspace workspace) {
        String shelvesetName = null;

        if (isShelvesetConflict()) {
            if (getTheirShelvesetName() == null || getTheirShelvesetName().length() == 0) {
                shelvesetName = Messages.getString("Conflict.ShelvesetDeletedDisplayName"); //$NON-NLS-1$
            } else if (UserNameUtil.equals(
                workspace.getClient().getConnection().getAuthorizedIdentity().getDisplayName(),
                getTheirShelvesetOwnerName())) {
                shelvesetName = getTheirShelvesetName();
            } else {
                shelvesetName = new WorkspaceSpec(getTheirShelvesetName(), getTheirShelvesetOwnerName()).toString();
            }
        }

        return shelvesetName;
    }

    /**
     * Downloads the content for the base (original) file in the conflict.
     * Throws if there is no base file, which is only valid for conflicts of
     * type Merge.
     *
     * @param absolutePath
     *        where to save the downloaded file contents
     */
    public void downloadBaseFile(final VersionControlClient client, final String absolutePath) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(absolutePath, "absolutePath"); //$NON-NLS-1$
        Check.notNull(getBaseDownloadURL(), "getBaseDownloadURL()"); //$NON-NLS-1$

        client.downloadFile(new DownloadSpec(getBaseDownloadURL()), new File(absolutePath), true);
    }

    /**
     * Downloads the content for their file in the conflict. Throws if there is
     * no their file.
     *
     * @param absolutePath
     *        where to save the downloaded file contents
     */
    public void downloadTheirFile(final VersionControlClient client, final String absolutePath) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(absolutePath, "absolutePath"); //$NON-NLS-1$
        Check.notNull(getTheirDownloadURL(), "getTheirDownloadURL()"); //$NON-NLS-1$

        client.downloadFile(new DownloadSpec(getTheirDownloadURL()), new File(absolutePath), true);
    }

    /**
     * Downloads the content for your file in the conflict. Throws if there is
     * no your file.
     *
     * @param absolutePath
     *        where to save the downloaded file contents
     */
    public void downloadYourFile(final VersionControlClient client, final String absolutePath) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(absolutePath, "absolutePath"); //$NON-NLS-1$

        client.downloadFile(new DownloadSpec(getYourDownloadURL()), new File(absolutePath), true);
    }

    /**
     * @return true if their side of the conflict has a file associated with it.
     */
    public boolean theirFileExists() {
        return getTheirDownloadURL() != null;
    }

    /**
     * Returns true if the AcceptMerge resolution is not conservative and may
     * produce unexpected results. Examples are undeleting a file (edit-delete
     * conflict) or deleting it (delete-edit conflicts).
     */
    public boolean requiresExplicitAcceptMerge() {
        /*
         * In most cases we are looking only for Delete bit. The reason being,
         * Delete is present on one side of the conflict whenever file was
         * deleted in source branch (BaseChangeType=Delete) or file was deleted
         * in target (YourChangeType=Delete). Testing for Undelete may give us
         * false positives, since undelete can conflict with edit. However if
         * file was deleted or renamed in target and we are doing baseless
         * merge, BaseChangeType=Undelete and YourChangeType=None. That's why we
         * have special condition for baseless merge. We also check
         * TheirLastMergedVersion != BaseVersion in the case the user did a
         * cherry pick merge baseless merge.
         */
        final boolean baselessUndelete = (isBaseless()
            | (ConflictType.MERGE.equals(getType())
                && getTheirLastMergedVersion() != getBaseVersion()
                && !isShelvesetConflict()))
            && getBaseChangeType().contains(ChangeType.UNDELETE);

        return getBaseChangeType().contains(ChangeType.DELETE)
            || getYourChangeType().contains(ChangeType.DELETE)
            || getYourLocalChangeType().contains(ChangeType.DELETE)
            || baselessUndelete;
    }

    /**
     * @return true if base, their and your encoding values are different.
     */
    public boolean isEncodingMismatched() {
        return getTheirEncoding().getCodePage() != getYourEncoding().getCodePage();
    }

    /**
     * @return true if this is the result of a baseless merge.
     */
    public boolean isBaseless() {
        return ConflictType.MERGE.equals(getType()) && getBaseItemID() == 0;
    }

    /**
     * @return true if either base, their or your encoding is binary.
     */
    public boolean isBinary() {
        final boolean anyBinary = getTheirEncoding().getCodePage() == VersionControlConstants.ENCODING_BINARY
            || getYourEncoding().getCodePage() == VersionControlConstants.ENCODING_BINARY
            || (!isBaseless() && getBaseEncoding().getCodePage() == VersionControlConstants.ENCODING_BINARY);

        return anyBinary;
    }

    /**
     * Check the basic requirements for merge operation. Does not validate that
     * content needs merging (CanMergeContent). Allows different encodings,
     * because user can override them.
     */
    public boolean isBasicMergeAllowed(final Workspace workspace) {
        if (!ItemType.FILE.equals(getYourItemType())) {
            return false;
        }

        /*
         * File encoding must be non-binary, OM does not support resolving
         * binary files as AcceptMerge If Encoding has changed, user will have
         * option to override it, so we allow.
         */
        if (isBinary() && !isEncodingChanged()) {
            return false;
        }

        /* Enable / Disable any options suggested by the server. */
        if (getConflictOptions().contains(ConflictOptions.DISALLOW_AUTO_MERGE)) {
            return false;
        }

        /* Check against the file types. */
        if (!mergeValidForFileType(workspace)) {
            return false;
        }
        /* Disallow for deleted shelvesets */
        if (isFromDeletedShelveset()) {
            return false;
        }

        /*
         * If there is no server item at the requested version, no auto-merge.
         */
        if (isVersionConflictAndServerItemDoesNotExist()) {
            return false;
        }

        // Merge options are available for this conflict.
        return true;

    }

    /**
     * Check to see if the encoding has changed.
     */
    public boolean isEncodingChanged() {
        if (!this.isLocalOrTargetFileInVersionControl()) {
            return false;
        }

        /*
         * Conflict must have an encoding change. The check against
         * BaseChangeType is for merge. If this is baseless merge, we need to
         * compare the encoding so we ignore this condition if we are merging
         * content, we defer this to IsEncodingMismatched, since you need the
         * encodings right even if there is no YourChangeType or BaseChangeType
         */
        if (!isBaseless()
            && !getYourChangeType().contains(ChangeType.ENCODING)
            && !getBaseChangeType().contains(ChangeType.ENCODING)
            && !canMergeContent()) {
            return false;
        }

        // Check for mismatched encoding
        return isEncodingMismatched();
    }

    /**
     * @return true if this conflict has conflicting properties
     */
    public boolean isPropertyConflict() {
        return ((getYourChangeType().combine(getYourLocalChangeType()).contains(ChangeType.PROPERTY) || isBaseless())
            && getBaseChangeType().contains(ChangeType.PROPERTY));
    }

    /**
     * Check to see if the local/target file is in version control.
     */
    public boolean isLocalOrTargetFileInVersionControl() {
        if (getYourVersion() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Check to see if the merge operations are valid for file of this type. The
     * check is made against the file types from the server.
     *
     * File types with exclusive checkout enabled cannot be merged,
     * automatically or with a merge tool.
     */
    private boolean mergeValidForFileType(final Workspace workspace) {
        if (mergeValidForFileType == null) {
            final FileType fileType = workspace.getClient().queryCachedFileType(getFileExtension());
            mergeValidForFileType = Boolean.valueOf((fileType == null || fileType.isAllowMultipleCheckout()));
        }

        return mergeValidForFileType.booleanValue();
    }

    /**
     * @return the calculated file name for this conflict
     */
    public String getFileName() {
        if (fileName == null) {
            final String localPath = getLocalPath();
            final String serverPath = getServerPath();

            if (localPath != null && localPath.length() > 0) {
                fileName = LocalPath.getFileName(localPath);
            } else if (serverPath != null && serverPath.length() > 0) {
                fileName = ServerPath.getFileName(serverPath);
            } else {
                fileName = ""; //$NON-NLS-1$
            }
        }

        return fileName;
    }

    /**
     * @return the file extension for this conflict or the empty string if the
     *         file lacks an extension (never <code>null</code>)
     */
    public String getFileExtension() {
        /* We calculate it every time, because it's called extremely rarely */
        String fileExtension = ""; //$NON-NLS-1$
        final String fileName = getFileName();

        if (fileName != null && fileName.length() > 0) {
            fileExtension = LocalPath.getFileExtension(fileName);
        }

        if (fileExtension.startsWith(".")) //$NON-NLS-1$
        {
            fileExtension = fileExtension.substring(1);
        }

        return fileExtension;
    }

    /**
     * True if this conflict was resolved automatically.
     */
    public boolean isAutoResolved() {
        return autoResolved;
    }

    public void setAutoResolved(final boolean autoResolved) {
        this.autoResolved = autoResolved;
    }

    /**
     * @return true if IsFromShelveset = true and their shelveset was deleted.
     */
    public boolean isFromDeletedShelveset() {
        return isShelvesetConflict()
            && ((getTheirShelvesetName() == null || getTheirShelvesetName().length() == 0)
                || (getTheirShelvesetOwnerName() == null || getTheirShelvesetOwnerName().length() == 0));
    }

    /**
     * @return true if this is a version conflict and the server item doesn't
     *         exst at the requested version.
     */
    public boolean isVersionConflictAndServerItemDoesNotExist() {
        return isVersionGetCheckinConflict()
            && ChangeType.NONE.equals(getBaseChangeType())
            && getTheirVersion() == 0
            && getTheirServerItem() == null
            && getTheirDeletionID() == 0;
    }

    /** @return true if this conflict is a version get/checkin conflict. */
    public boolean isVersionGetCheckinConflict() {
        return !isNamespaceConflict()
            && !isRollbackConflict()
            && (!ConflictType.MERGE.equals(getType()) || isShelvesetConflict());
    }

    /**
     * @return true if this is a rollback conflict.
     */
    public boolean isRollbackConflict() {
        return getBaseChangeType().contains(ChangeType.ROLLBACK);
    }

    /**
     * Check to see if the file name has changed.
     */
    public boolean isNameChanged() {
        if (!isLocalOrTargetFileInVersionControl()) {
            return false;
        }

        /*
         * We depend on the change bits, because comparing path makes us think
         * that there is rename conflict when parent was renamed.
         */

        if (ConflictType.MERGE.equals(getType())) {
            return isTheirNameChanged();
        } else {
            return isYourNameChanged() || isTheirNameChanged();
        }
    }

    public boolean isYourNameChanged() {
        return (getYourChangeType() != null && getYourChangeType().contains(ChangeType.RENAME))
            || (getYourLocalChangeType() != null && getYourLocalChangeType().contains(ChangeType.RENAME));
    }

    public boolean isTheirNameChanged() {
        return (getBaseChangeType() != null && getBaseChangeType().contains(ChangeType.RENAME));
    }

    public boolean isNameChangeIsRedundant() {
        // We purposefully do a case sensitive comparison here because case-only
        // renames are not redundant.
        return isVersionGetCheckinConflict() && getYourServerItem().equals(getTheirServerItem());
    }

    /**
     * @return true if there is no local/target (YourChangeType) rename, false
     *         otherwise
     */
    public boolean hasNoLocalRenames() {
        return !getYourChangeType().combine(getYourLocalChangeType()).contains(ChangeType.RENAME);
    }

    /**
     * @return true if this conflict is valid for AcceptMerge with no additional
     *         arguments
     */
    public boolean isValidForAutoMerge(final Workspace workspace) {
        return isBasicMergeAllowed(workspace)
            && !isEncodingChanged()
            && (!isNameChanged() || hasNoLocalRenames())
            && !requiresExplicitAcceptMerge()
            && !isNamespaceConflict();
    }

    /**
     * Return True if conflict can be auto resolved as AcceptMerge in the given
     * mode. This method requires ContentMergeSummary to be calculated.
     */
    public boolean isAutoMergeApplicable(final AutoResolveOptions resolveOptions) {
        if (getContentMergeSummary() == null) {
            return false;
        }
        if (AutoResolveOptions.NONE.equals(resolveOptions)) {
            return false;
        }
        if (hasNoContentChange()) {
            return true;
        }
        if (resolveOptions.contains(AutoResolveOptions.ONLY_LOCAL_TARGET) && hasLocalTargetContentChangeOnly()) {
            return true;
        }
        if (resolveOptions.contains(AutoResolveOptions.ONLY_SERVER_SOURCE) && hasSourceServerContentChangeOnly()) {
            return true;
        }
        if (resolveOptions.contains(AutoResolveOptions.ALL_CONTENT) && !hasConflictingContentChange()) {
            return true;
        }
        return false;
    }

    /**
     * Last Modified UTC time of the local file, which was used to calculate
     * local hash value. Value is expressed in Ticks.
     */
    public long getLocalFileLastModifiedDateUsedForHashValue() {
        return localFileLastModifiedDateUsedForHashValue;
    }

    public void setLocalFileLastModifiedDateUsedForHashValue(final long localFileLastModifiedDateUsedForHashValue) {
        this.localFileLastModifiedDateUsedForHashValue = localFileLastModifiedDateUsedForHashValue;
    }

    /**
     * Updates LocalHashValue if it's invalid. The hash is calculated even if
     * the conflict content has not changed (e.g. rename conflict).
     */
    public void updateLocalHashValue() {
        if (getLocalHashValue() == null) {
            if (getLocalPath() != null && getLocalPath().length() > 0) {
                final File localFile = new File(getLocalPath());

                if (localFile.exists()) {
                    try {
                        localHashValue = HashUtils.hashFile(localFile, HashUtils.ALGORITHM_MD5);
                        setLocalFileLastModifiedDateUsedForHashValue(getLocalFileLastModifiedDate());
                    } catch (final Exception e) {
                        log.warn("Could not determine local file hash value for conflict", e); //$NON-NLS-1$
                    }
                }
            }
        }
    }

    /**
     * Return True only if the conflict has ContentMergeSummary calculated and
     * the source and target are identical - TotalConflicting = 0, TotalLatest =
     * 0, TotalModified = 0. False otherwise.
     */
    private boolean hasNoContentChange() {
        if (getContentMergeSummary() != null) {
            return getContentMergeSummary().getTotalConflictingLines() == 0
                && getContentMergeSummary().getLatestChangedLines() == 0
                && getContentMergeSummary().getLocalChangedLines() == 0;
        }

        return false;
    }

    /**
     * Verifies that this conflict is caused by 2 identical changes.
     *
     * @param quick
     *        If true, does not recalculate local file hash and quickly fails.
     *        If false, computes hash if it's not valid.
     */
    public boolean isRedundant(final boolean quick, final Workspace workspace) {
        if (!mayBeRedundant(workspace)) {
            return false;
        }

        boolean redundant;
        if (contentMayHaveChanged()) {
            /*
             * If changeSummary exists and is up-to-date, let's use it,
             * otherwise let's compare md5.
             */
            resetChangeSummaryIfLocalFileModified();

            if (getContentMergeSummary() != null) {
                redundant = hasNoContentChange();
            } else {
                if (!quick) {
                    updateLocalHashValue();
                }

                /*
                 * We don't calculate hash here! This property quickly checks if
                 * we are redundant using data we already have.
                 */
                final byte[] theirHashValue = getTheirHashValue();
                final byte[] localHashValue = getLocalHashValue();

                if (theirHashValue != null && localHashValue != null && theirHashValue.length != 0) {
                    redundant = Arrays.equals(theirHashValue, localHashValue);
                } else {
                    redundant = false;
                }
            }
        } else {
            /* We validated everything in conflict.mayBeRedundant() */
            redundant = true;
        }

        if (isPropertyConflict()) {
            /* Download and compare properties */
            mergeProperties(workspace);

            if (redundant) {
                redundant = getPropertiesMergeSummary().isRedundant();
            }
        }

        return redundant;
    }

    /**
     * Lightweight way to verify if it's Redundant Conflict. Does not compare
     * content. Does not compare property values.
     */
    private boolean mayBeRedundant(final Workspace workspace) {
        /*
         * If this isn't at least a Dev10 server then the conflict cannot be
         * redundant.
         */
        if (workspace.getClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            return false;
        }

        if (!isVersionGetCheckinConflict()) {
            return false;
        }

        /* sanity check, that catches deleted shelvesets. */
        if (getYourItemType() != getTheirItemType()) {
            return false;
        }

        /*
         * We ignore edit, in case the content is identical and edit was undone
         * on the server
         *
         * We ignore rename - when parent has rename, theirChangeType of the
         * child is rename as well
         */
        final ChangeType changesToCompare =
            ChangeType.ALL.remove(ChangeType.LOCK.combine(ChangeType.EDIT.combine(ChangeType.RENAME)));
        final ChangeType coreTheirs = getTheirChangeType().retain(changesToCompare);
        ChangeType coreYours;

        if (isShelvesetConflict() && ConflictType.MERGE.equals(getType())) {
            coreYours = getYourLocalChangeType().retain(changesToCompare);
        } else {
            coreYours = getYourChangeType().retain(changesToCompare);
        }

        if (!coreTheirs.equals(coreYours)) {
            return false;
        }

        /*
         * If TheirServerItem or YouServerItem are null then it's can't be
         * redundant conflict - it can happen only when user syncs to not
         * existing version of the file.
         */
        if (isNameChanged()
            && (getTheirServerItem() == null
                || getTheirServerItem().length() == 0
                || getYourServerItem() == null
                || getYourServerItem().length() == 0
                || !ServerPath.equals(getTheirServerItem(), getYourServerItem(), false))) {
            return false;
        }
        if (isEncodingChanged() && getTheirEncoding().getCodePage() != getYourEncoding().getCodePage()) {
            return false;
        }
        return true;
    }

    /**
     * Removes ChangeSummary and merge file if local file was modified after
     * those values were calculated.
     */
    public void resetChangeSummaryIfLocalFileModified() {
        /*
         * if LocalFileLastModifiedDateUsedForThreeWayMerge is zero it means it
         * has never been calculated or LocalFileLastModifiedDate failed in both
         * cases we want to calulate the value again because we don't have
         * reliable LocalFileLastModifiedDate
         */
        if (getContentMergeSummary() != null
            && ((getLocalFileLastModifiedDate() != getLocalFileLastModifiedDateUsedForThreeWayMerge())
                || (getLocalFileLastModifiedDateUsedForThreeWayMerge() == 0))) {
            setContentMergeSummary(null);
            cleanUpMergedResultFile();
        }
    }

    /**
     * Return True only if the conflict has ContentMergeSummary calculated and
     * the conflicting and source/server chunks are 0, but local/target are not
     * zero. False otherwise.
     */
    public boolean hasLocalTargetContentChangeOnly() {
        if (getContentMergeSummary() != null) {
            return getContentMergeSummary().getTotalConflictingLines() == 0
                && getContentMergeSummary().getLatestChangedLines() == 0
                && getContentMergeSummary().getLocalChangedLines() != 0;
        }
        return false;
    }

    /**
     * Return True only if the conflict has ContentMergeSummary calculated and
     * the conflicting and target/local chunks are 0, but source/server are not
     * zero. False otherwise.
     */
    public boolean hasSourceServerContentChangeOnly() {
        if (getContentMergeSummary() != null) {
            return getContentMergeSummary().getTotalConflictingLines() == 0
                && getContentMergeSummary().getLocalChangedLines() == 0
                && getContentMergeSummary().getLatestChangedLines() != 0;
        }
        return false;
    }

    /**
     * Determine if the conflict contains one or more conflicting change.
     */
    public boolean hasConflictingContentChange() {
        if (getContentMergeSummary() != null) {
            return getContentMergeSummary().getTotalConflictingLines() != 0;
        }
        return false;
    }

    /**
     * Determine if the conflict contains conflicting property changes.
     */
    public boolean hasConflictingPropertyChange() {
        if (propertiesMergeSummary != null) {
            return propertiesMergeSummary.getTotalConflicts() != 0;
        }

        return false;
    }

    private void cleanUpMergedResultFile() {
        if (getMergedFileName() != null && getMergedFileName().length() > 0) {
            try {
                new File(getMergedFileName()).delete();
            } catch (final Exception e) {
                /*
                 * Really don't care about the merged file not cleaned up
                 * properly. Rare case! Just don't want any exception leaking
                 * beyond this point.
                 */
            } finally {
                setMergedFileName(null);
            }
        }
    }

    private boolean contentMayHaveChanged() {
        return (getYourChangeType().combine(getTheirChangeType().combine(getYourLocalChangeType()))).containsAny(
            ChangeType.EDIT.combine(ChangeType.BRANCH))
            && (ItemType.FILE.equals(getYourItemType()) || ItemType.FILE.equals(getTheirItemType()));
    }

    public byte[] getLocalHashValue() {
        if ((getLocalFileLastModifiedDate() != getLocalFileLastModifiedDateUsedForHashValue())
            || (getLocalFileLastModifiedDateUsedForHashValue() == 0)) {
            localHashValue = null;
        }

        return localHashValue;
    }

    public void setLocalHashValue(final byte[] localHashValue) {
        this.localHashValue = localHashValue;
    }

    /**
     * Last Write UTC time of the local file, expressed in Ticks. 0 if local
     * file does not exist or error occurred.
     */
    private long getLocalFileLastModifiedDate() {
        try {
            if (getLocalPath() != null && getLocalPath().length() > 0) {
                final FileSystemTime fileTime =
                    FileSystemUtils.getInstance().getAttributes(getLocalPath()).getModificationTime();

                if (fileTime != null) {
                    return fileTime.getWindowsFilesystemTime();
                }
            }

            return 0;
        } catch (final Exception e) {
            log.warn("Could not determine local file last modified date", e); //$NON-NLS-1$
            return 0;
        }
    }

    /**
     * Last Modified UTC time of the local file, which was used for performing
     * three way merge and creating merge file. Value is expressed in ticks.
     */
    private long getLocalFileLastModifiedDateUsedForThreeWayMerge() {
        return localFileLastModifiedDateUsedForThreeWayMerge;
    }

    /**
     * Last Modified UTC time of the local file, which was used for performing
     * three way merge and creating merge file.
     */
    public void setLocalFileLastModifiedDateUsedForThreeWayMerge(
        final long localFileLastModifiedDateUsedForThreeWayMerge) {
        this.localFileLastModifiedDateUsedForThreeWayMerge = localFileLastModifiedDateUsedForThreeWayMerge;
    }

    /**
     * Generates the properties merge summary. Returns <code>null</code> if
     * there is not a property conflict.
     */
    public PropertiesMergeSummary mergeProperties(final Workspace workspace) {
        if (!isPropertyConflict()) {
            return null;
        }

        if (propertiesMergeSummary == null) {
            if (getYourProperties() == null || getTheirProperties() == null || getBaseProperties() == null) {
                downloadProperties(workspace);
            }

            propertiesMergeSummary =
                PropertiesMergeSummary.calculateSummary(getBaseProperties(), getYourProperties(), getTheirProperties());
        }

        return propertiesMergeSummary;
    }

    /**
     * Returns the property merge summary if one has been generated.
     */
    public PropertiesMergeSummary getPropertiesMergeSummary() {
        return propertiesMergeSummary;
    }

    /**
     * Properties for YourServerItem. Not populated until
     * {@link #downloadProperties(Workspace)} or
     * {@link #mergeProperties(Workspace)} is called.
     */
    public PropertyValue[] getYourProperties() {
        return yourProperties;
    }

    /**
     * Properties for TheirServerItem. Not populated until
     * {@link #downloadProperties(Workspace)} or
     * {@link #mergeProperties(Workspace)} is called.
     */
    public PropertyValue[] getTheirProperties() {
        return theirProperties;
    }

    /**
     * Properties for BaseServerItem. Not populated until
     * {@link #downloadProperties(Workspace)} or
     * {@link #mergeProperties(Workspace)} is called.
     */
    public PropertyValue[] getBaseProperties() {
        return baseProperties;
    }

    public void downloadProperties(final Workspace workspace) {
        /* Determine what version to request for "Your" properties. */
        VersionSpec yourVersion = null;
        String yourServerPath = null;

        if (isVersionGetCheckinConflict() || isShelvesetConflict()) {
            yourVersion = new WorkspaceVersionSpec(workspace);
            yourServerPath = getYourServerItem();
        } else {
            yourVersion = new ChangesetVersionSpec(getYourVersion());
            yourServerPath = getServerPath();
        }

        yourProperties = downloadPropertiesHelper(workspace, yourServerPath, yourVersion);

        if (getBaseServerItem() != null && getBaseServerItem().length() > 0) {
            baseProperties =
                downloadPropertiesHelper(workspace, getBaseServerItem(), new ChangesetVersionSpec(getBaseVersion()));
        }

        /*
         * Download their properties using QueryItems or QueryShelvedChanges
         * depending on what type of conflict it is.
         */
        if (!(isShelvesetConflict() && ConflictType.MERGE.equals(getType()))) {
            theirProperties =
                downloadPropertiesHelper(workspace, getTheirServerItem(), new ChangesetVersionSpec(getTheirVersion()));
        } else {
            final PendingSet[] pendingSets = workspace.getClient().queryShelvedChanges(
                null,
                null,
                getTheirShelvesetName(),
                getTheirShelvesetOwnerName(),
                new ItemSpec[] {
                    new ItemSpec(getTheirServerItem(), RecursionType.NONE)
            }, false, PropertyConstants.QUERY_ALL_PROPERTIES_FILTERS);

            if (pendingSets.length > 0 && pendingSets[0].getPendingChanges().length > 0) {
                theirProperties = pendingSets[0].getPendingChanges()[0].getPropertyValues();
            } else {
                theirProperties = new PropertyValue[0];
            }
        }
    }

    private PropertyValue[] downloadPropertiesHelper(
        final Workspace workspace,
        final String serverItem,
        final VersionSpec version) {
        final ItemSet[] itemSets = workspace.getClient().getItems(
            new ItemSpec[] {
                new ItemSpec(serverItem, RecursionType.NONE)
        },
            version,
            DeletedState.ANY,
            ItemType.ANY,
            GetItemsOptions.NONE,
            PropertyConstants.QUERY_ALL_PROPERTIES_FILTERS);

        if (itemSets.length > 0 && itemSets[0].getItems().length > 0) {
            return itemSets[0].getItems()[0].getPropertyValues();
        } else {
            return new PropertyValue[0];
        }
    }

    /**
     * Converts this conflict into a displayable message.
     * <p>
     * This method returns rather short messages. It's mainly for the CLC. See
     * {@link ConflictDescription} for use in a graphical environment.
     *
     * @param asConflict
     *        the displayable message for this conflict
     * @return the message
     */
    public String getDetailedMessage(final boolean asConflict) {
        if (getType() == ConflictType.MERGE) {
            String source;
            String target;

            if (getBaseChangeType().contains(ChangeType.ROLLBACK)) {
                /*
                 * if the operation is a rollback, the BaseServerItem is the
                 * source path also the base version is the version to.
                 */
                source = VersionedFileSpec.formatForPath(
                    getBaseServerItem(),
                    new ChangesetVersionSpec(getTheirVersionFrom()),
                    new ChangesetVersionSpec(getBaseVersion()));

            } else if (!getBaseChangeType().contains(ChangeType.MERGE)) {
                /*
                 * For changes pended that are not merges, there is no item in
                 * the source tree. These are cases such as a rename where the
                 * file on the target side simply needs to be moved because the
                 * parent moved on the source side.
                 */
                source = null;
            } else if (getBaseChangeType().contains(ChangeType.BRANCH)) {
                /*
                 * A branch that is being merged from source to target has no
                 * meaningful "from" version.
                 */
                final ChangesetVersionSpec sourceVersionTo = new ChangesetVersionSpec(getTheirVersion());
                source = VersionedFileSpec.formatForPath(getTheirServerItem(), sourceVersionTo);
            } else {
                /*
                 * The TheirVersionFrom data is only sent from server to client
                 * on Dev10 or later servers.
                 */
                final ChangesetVersionSpec sourceVersionFrom = (getTheirVersion() > 0)
                    ? new ChangesetVersionSpec(getTheirVersion()) : new ChangesetVersionSpec(getBaseVersion());
                final ChangesetVersionSpec sourceVersionTo = new ChangesetVersionSpec(getTheirVersion());
                source = VersionedFileSpec.formatForPath(getTheirServerItem(), sourceVersionFrom, sourceVersionTo);
            }

            if (getBaseChangeType().contains(ChangeType.BRANCH)
                || getYourLocalChangeType().contains(ChangeType.BRANCH)
                || getYourLocalChangeType().contains(ChangeType.ADD)) {
                /*
                 * A branch that is being merged from source to target has no
                 * meaningful "target pended" version. VersionPended in this
                 * case is simply the VersionFrom of the source item. (which
                 * isn't necessarily the SourceVersionTo data) Additionally, a
                 * pending branch/add in the target tree that is being affected
                 * by a parent rename will only have a ChangeType of Rename and
                 * thus there is no TargetVersionPended.
                 */
                target = getYourServerItemSource();
            } else {
                final ChangesetVersionSpec targetVersionPended = new ChangesetVersionSpec(getYourVersion());
                target = VersionedFileSpec.formatForPath(getYourServerItemSource(), targetVersionPended);
            }

            /*
             * The pending merge happens regardless of problems on the local
             * disk.
             */
            String message;
            if (asConflict) {
                message = MessageFormat.format(
                    Messages.getString("Conflict.MergeConflictFormat"), //$NON-NLS-1$
                    getBaseChangeType().toUIString(false),
                    source,
                    target);
            } else if (source == null) {
                message = MessageFormat.format(
                    Messages.getString("Conflict.MergeAssociatedPendingChangeFormat"), //$NON-NLS-1$
                    getBaseChangeType().toUIString(false),
                    target);
            } else {
                message = MessageFormat.format(
                    Messages.getString("Conflict.MergePendedFormat"), //$NON-NLS-1$
                    getBaseChangeType().toUIString(false),
                    source,
                    target);
            }

            return message;
        }

        // This is a Get/Checkin conflict so show things as local paths
        String path = null;
        if (getTargetLocalItem() != null) {
            path = getTargetLocalItem();
        } else if (getSourceLocalItem() != null) {
            path = getSourceLocalItem();
        }

        // Make sure we have a path.
        if (path == null || path.length() == 0) {
            path = getTheirServerItem();
        }

        return MessageFormat.format(
            Messages.getString("Conflict.MergeAssociatedPendingChangeFormat"), //$NON-NLS-1$
            getYourChangeType().combine(getYourLocalChangeType()).toUIString(false),
            path);
    }
}
