// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.CheckinEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.ScannerModifiedFilesEvent;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.EnumeratedLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalDataAccessLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalItemEnumerable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalItemEnumerator;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalPendingChangesTable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.KeyValuePair;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class LocalWorkspaceScanner {
    private static final Log log = LogFactory.getLog(LocalWorkspaceScanner.class);
    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

    private static final int c_defaultCandidateAddsLimit = 50000;
    private static final int c_defaultEnumeratedItemsLimit = 500000;

    private static final int s_candidateAddsLimit;
    private static final int s_enumeratedItemsLimit;

    static {
        // VS does:
        // s_candidateAddsLimit =
        // TFCommonUtil.GetAppSettingAsInt("VersionControl.CandidateAddsLimit",
        // c_defaultCandidateAddsLimit);
        // s_enumeratedItemsLimit =
        // TFCommonUtil.GetAppSettingAsInt("VersionControl.EnumeratedItemsLimit",
        // c_defaultEnumeratedItemsLimit);

        s_candidateAddsLimit = c_defaultCandidateAddsLimit;
        s_enumeratedItemsLimit = c_defaultEnumeratedItemsLimit;
    }

    private final LocalWorkspaceProperties wp;
    private final WorkspaceVersionTable lv;
    private final LocalPendingChangesTable pc;
    private final Set<String> skippedItems = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private final List<WorkspaceLocalItem> markForRemoval = new ArrayList<WorkspaceLocalItem>();
    private final List<WorkspaceLocalItem> reappearedOnDisk = new ArrayList<WorkspaceLocalItem>();
    private final List<KeyValuePair<String, Long>> toUndo = new ArrayList<KeyValuePair<String, Long>>();
    private final Set<String> candidateChanges = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Collects the local paths of items whose on-disk attributes were changed
     * during the scan so one event can be fired at the end of the scan.
     */
    private final Set<String> changedByScan = new TreeSet<String>(LocalPath.TOP_DOWN_COMPARATOR);

    public LocalWorkspaceScanner(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final Iterable<String> skippedItems) {
        this.wp = wp;
        this.lv = lv;
        this.pc = pc;

        for (final String skippedItem : skippedItems) {
            this.skippedItems.add(skippedItem);
        }
    }

    public void fullScan() throws CoreCancelException {
        final long start = System.currentTimeMillis();
        int firstPassItems = 0;

        // First, set the Scanned bit for each item in the local version table
        // to false
        for (final WorkspaceLocalItem lvEntry : lv.queryByLocalItem(null, RecursionType.FULL, null)) {
            lvEntry.setScanned(false);
        }

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        final WorkingFolder[] workingFolders = this.wp.getWorkingFolders();

        int candidateAddsCount = 0;
        int enumeratedItemsCount = 0;

        // For each item in the mapped local space of the workspace (first pass)
        PassOne: for (final LocalItemEnumerator localItemEnum : LocalItemEnumerable.getEnumeratorsForWorkingFolders(
            workingFolders)) {
            final LocalItemExclusionEvaluator ignoreFileStack =
                new LocalItemExclusionEvaluator(this.wp, localItemEnum.getStartPath());

            while (localItemEnum.hasNext()) {
                if (taskMonitor.isCanceled()) {
                    throw new CoreCancelException();
                }

                final EnumeratedLocalItem fromDisk = localItemEnum.next();

                if (candidateAddsCount >= s_candidateAddsLimit || enumeratedItemsCount >= s_enumeratedItemsLimit) {
                    // We're done walking mapped local space. Finish up the
                    // items that are actually in the local version
                    // table by skipping directly to pass two.

                    // No goto in Java, break the PassOne loop
                    break PassOne;
                }

                enumeratedItemsCount++;

                final WorkspaceLocalItem lvEntry = lv.getByLocalItem(fromDisk.getFullPath());

                if (null != lvEntry) {
                    // We've hit this item in the first pass and will not
                    // need
                    // to check it again
                    // in the second.
                    lvEntry.setScanned(true);
                    firstPassItems++;

                    diffItem(fromDisk, lvEntry);
                } else if (!fromDisk.isDirectory() || fromDisk.isSymbolicLink()) {
                    // Check to see if this is a candidate add.
                    if (!ignoreFileStack.isExcluded(fromDisk.getFullPath())) {
                        final WorkingFolder closestMapping = (WorkingFolder) localItemEnum.getTag();
                        fromDisk.setServerItem(closestMapping.translateLocalItemToServerItem(fromDisk.getFullPath()));

                        // Check for illegal characters or a $ at the
                        // beginning of a path part
                        if (ServerPath.isServerPath(fromDisk.getServerItem())) {
                            if (addCandidateAdd(fromDisk)) {
                                candidateAddsCount++;
                            }
                        }
                    }
                }
            }
        }

        // PassTwo starts here

        // If we hit everything in the first pass (common case) then there's no
        // additional work to do here.
        if (firstPassItems != lv.getLocalItemsCount()) {
            // For each item in the local version table that we missed (second
            // pass)
            for (final WorkspaceLocalItem lvEntry : lv.queryByLocalItem(null, RecursionType.FULL, null)) {
                if (lvEntry.isScanned()) {
                    // We already hit this item
                    continue;
                }

                final File localFile = new File(lvEntry.getLocalItem());
                final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(localFile);

                if (!attrs.exists()) {
                    // Missing on disk, and candidate delete
                    markForRemoval.add(lvEntry);
                    continue;
                }

                final EnumeratedLocalItem fromDisk = new EnumeratedLocalItem(localFile, attrs);

                diffItem(fromDisk, lvEntry);
            }
        }

        scanPartTwo();

        // Remove from the pending changes table those candidates which we don't
        // have in our list.
        final Set<String> candidatesToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        for (final LocalPendingChange candidateChange : pc.queryCandidatesByTargetServerItem(
            ServerPath.ROOT,
            RecursionType.FULL,
            null)) {
            if (!candidateChanges.contains(candidateChange.getTargetServerItem())) {
                candidatesToRemove.add(candidateChange.getTargetServerItem());
            }
        }

        for (final String candidateToRemove : candidatesToRemove) {
            pc.removeCandidateByTargetServerItem(candidateToRemove);
        }

        if (candidatesToRemove.size() > 0) {
            LocalWorkspaceTransaction.getCurrent().setRaisePendingChangeCandidatesChanged(true);
        }

        fireChangedByScanEvent();

        log.debug(MessageFormat.format(
            "Full scan took {0} ms ({1} enum items, {2} fp items, {3} adds, {4} removes)", //$NON-NLS-1$
            (System.currentTimeMillis() - start),
            enumeratedItemsCount,
            firstPassItems,
            candidateAddsCount,
            candidatesToRemove.size()));
    }

    public void partialScan(final Iterable<String> changedPaths) throws CoreCancelException {
        final List<String> itemsToScan = new ArrayList<String>();
        boolean fallBackToFullScan = false;

        for (final String changedPath : changedPaths) {
            try {
                itemsToScan.add(LocalPath.canonicalize(changedPath));
            } catch (final Throwable t) {
                fallBackToFullScan = true;
                break;
            }
        }

        // If we need to fall back to a full scan, do so.
        if (fallBackToFullScan) {
            fullScan();
            return;
        }

        final List<String> itemsToScanNotOnDisk = new ArrayList<String>();
        final Iterable<String> workspaceRoots = WorkingFolder.getWorkspaceRoots(this.wp.getWorkingFolders());

        // Otherwise proceed with a partial scan -- only those items which were
        // invalidated
        for (final String localItem : itemsToScan) {
            final WorkspaceLocalItem lvEntry = lv.getByLocalItem(localItem);
            final File localFile = new File(localItem);
            final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(localFile);

            if (!attrs.exists()) {
                // Missing on disk, and candidate delete
                if (null != lvEntry) {
                    markForRemoval.add(lvEntry);
                }

                if (null == lvEntry || !lvEntry.isCommitted()) {
                    itemsToScanNotOnDisk.add(localItem);
                }

                continue;
            }

            final EnumeratedLocalItem fromDisk = new EnumeratedLocalItem(localFile, attrs);

            if (null != lvEntry) {
                diffItem(fromDisk, lvEntry);
            } else if (!fromDisk.isDirectory()) {
                String workspaceRoot = null;

                for (final String potentialRoot : workspaceRoots) {
                    if (LocalPath.isChild(potentialRoot, fromDisk.getFullPath())) {
                        workspaceRoot = potentialRoot;
                        break;
                    }
                }

                if (null != workspaceRoot) {
                    // Start the ignore file stack's evaluation at the workspace
                    // root of this local item.
                    final LocalItemExclusionEvaluator ignoreFileStack =
                        new LocalItemExclusionEvaluator(this.wp, workspaceRoot);

                    if (!ignoreFileStack.isExcluded(fromDisk.getFullPath())) {
                        fromDisk.setServerItem(
                            WorkingFolder.getServerItemForLocalItem(fromDisk.getFullPath(), wp.getWorkingFolders()));

                        if (null != fromDisk.getServerItem() && ServerPath.isServerPath(fromDisk.getServerItem())) {
                            addCandidateAdd(fromDisk);
                        }
                    }
                }
            }
        }

        scanPartTwo();

        // Remove from the pending changes table those candidates which we don't
        // have in our list.
        final Set<String> candidatesToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        for (final String itemToScan : itemsToScan) {
            final String targetServerItem = WorkingFolder.getServerItemForLocalItem(itemToScan, wp.getWorkingFolders());

            if (null != targetServerItem) {
                final LocalPendingChange candidateChange = pc.getCandidateByTargetServerItem(targetServerItem);

                if (null != candidateChange && !candidateChanges.contains(candidateChange.getTargetServerItem())) {
                    // This candidate was in the scoped scan manifest, but we
                    // didn't create a candidate for it. We'll remove this
                    // candidate change.
                    candidatesToRemove.add(candidateChange.getTargetServerItem());
                }
            }
        }

        // If we removed any items from disk, we need to check if any candidate
        // adds beneath it should be removed as well. There is no index by local
        // item on the candidates, so we have to walk the entire candidates
        // table.
        if (itemsToScanNotOnDisk.size() > 0) {
            for (final LocalPendingChange candidateChange : pc.queryCandidatesByTargetServerItem(
                ServerPath.ROOT,
                RecursionType.FULL,
                null)) {
                if (candidateChange.isAdd() && !candidateChanges.contains(candidateChange.getTargetServerItem())) {
                    final String localItem = WorkingFolder.getLocalItemForServerItem(
                        candidateChange.getTargetServerItem(),
                        wp.getWorkingFolders(),
                        true /* detectImplicitCloak */);

                    if (null != localItem) {
                        for (final String parent : itemsToScanNotOnDisk) {
                            if (LocalPath.isChild(parent, localItem)) {
                                candidatesToRemove.add(candidateChange.getTargetServerItem());
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (final String candidateToRemove : candidatesToRemove) {
            pc.removeCandidateByTargetServerItem(candidateToRemove);
        }

        if (candidatesToRemove.size() > 0) {
            LocalWorkspaceTransaction.getCurrent().setRaisePendingChangeCandidatesChanged(true);
        }

        fireChangedByScanEvent();
    }

    private void scanPartTwo() {
        // The items in this set may be folders or files. They have local
        // version rows, but no local item on disk. We will mark the local
        // version row as MissingOnDisk. The row will persist until the
        // reconcile before the next Get. If it is still MissingOnDisk then, we
        // will remove the row and reconcile that delete to the server so that
        // the item will come back.
        for (final WorkspaceLocalItem lvEntry : markForRemoval) {
            // If the item is in the exclusion list for this scanner, do not
            // process the item.
            if (skippedItems.contains(lvEntry.getLocalItem())) {
                continue;
            }

            final LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

            // Pending adds do not have their local version row removed marked
            // as MissingFromDisk.
            if (null == pcEntry || !pcEntry.isAdd()) {
                if (!lvEntry.isMissingOnDisk()) {
                    lv.removeByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted(), false);
                    lvEntry.setMissingOnDisk(true);
                    lv.add(lvEntry);
                }

                final String targetServerItem = pc.getTargetServerItemForLocalVersion(lvEntry);

                if (lvEntry.isCommitted() && null == pcEntry && null != pc.getByTargetServerItem(targetServerItem)) {
                    // if we don't have a pending change against the item, but
                    // have a pending change against the target i.e. add, branch
                    // or rename we don't want mark this as a candidate as there
                    // is no way to actually pend the delete without moving the
                    // item out of the way, we will let check-in take care of it
                    // with a namespace conflict
                    continue;
                }

                final LocalPendingChange newChange =
                    new LocalPendingChange(lvEntry, targetServerItem, ChangeType.DELETE);
                newChange.setCandidate(true);
                addCandidateChange(newChange);
            }
        }

        // The items in this set may be folders or files. They have local
        // version rows, and were previously marked as MissingOnDisk. However,
        // their local item has reappeared. We will remove the MissingOnDisk
        // bit.
        for (final WorkspaceLocalItem lvEntry : reappearedOnDisk) {
            // If the item is in the exclusion list for this scanner, do not
            // process the item.
            if (skippedItems.contains(lvEntry.getLocalItem())) {
                continue;
            }

            lv.removeByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted(), false);
            lvEntry.setMissingOnDisk(false);
            lv.add(lvEntry);
        }

        // The items in this set are all files. They were identified as having
        // identical content as the workspace version, but a different
        // timestamp. We will update the local version table to contain the new
        // timestamp. Additionally, if the item has a pending edit, we will
        // selective-undo the edit.

        for (final KeyValuePair<String, Long> undoOp : toUndo) {
            final String localItem = undoOp.getKey();
            final long onDiskModifiedTime = undoOp.getValue();

            if (skippedItems.contains(localItem)) {
                continue;
            }

            final WorkspaceLocalItem lvEntry = lv.getByLocalItem(localItem);

            // Bring the last-modified time on the item forward to match the
            // latest scan.
            if (lvEntry.getLastModifiedTime() != onDiskModifiedTime) {
                lvEntry.setLastModifiedTime(onDiskModifiedTime);
                lv.setDirty(true);
            }

            final LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

            // If the item has a pending edit, undo the pending edit, because
            // the content is identical to the workspace version. The only
            // uncommitted server items which can have their pending change
            // undone are those with changetype "branch, edit".
            if (null != pcEntry
                && pcEntry.isEdit()
                && !pcEntry.isAdd()
                && (!pcEntry.isEncoding() || pcEntry.getEncoding() == lvEntry.getEncoding())) {
                final AtomicReference<Failure[]> outFailures = new AtomicReference<Failure[]>();
                final AtomicBoolean outOnlineOperationRequired = new AtomicBoolean();
                final List<LocalPendingChange> pcEntries = new ArrayList<LocalPendingChange>(1);
                pcEntries.add(pcEntry);

                // The GetOperations returned are not processed.
                final GetOperation[] getOps = LocalDataAccessLayer.undoPendingChanges(
                    LocalWorkspaceTransaction.getCurrent().getWorkspace(),
                    wp,
                    lv,
                    pc,
                    pcEntries,
                    ChangeType.EDIT,
                    outFailures,
                    outOnlineOperationRequired);

                // No renames or locks are being undone,
                Check.isTrue(
                    !outOnlineOperationRequired.get() && (1 == getOps.length),
                    "!outOnlineOperationRequired.get() && (1 == getOps.length)"); //$NON-NLS-1$

                // Since we've modified the pending changes table in a silent
                // way, we want to set the flag on the transaction we're a part
                // of that indicates the PendingChangesChanged event should be
                // raised for this workspace, once the transaction completes.
                LocalWorkspaceTransaction.getCurrent().setRaisePendingChangesChanged(true);
            }
        }
    }

    private void diffItem(final EnumeratedLocalItem fromDisk, final WorkspaceLocalItem lvEntry) {
        if (fromDisk.isDirectory()) {
            if (!lvEntry.isDirectory()) {
                // Item is a directory on disk, but a file in the local version
                // table. Delete the local version row.
                markForRemoval.add(lvEntry);
            } else {
                if (lvEntry.isMissingOnDisk()) {
                    reappearedOnDisk.add(lvEntry);
                }
            }
        } else {
            if (lvEntry.isDirectory()) {
                // Item is a file on disk, but a directory in the local version
                // table. Delete the local version row.
                markForRemoval.add(lvEntry);
            } else {
                if (lvEntry.isMissingOnDisk()) {
                    reappearedOnDisk.add(lvEntry);
                }

                boolean pendEdit = false;
                boolean symlink = false;

                if (lvEntry.isSymbolicLink() || fromDisk.isSymbolicLink()) {
                    symlink = true;
                }

                if (-1 == lvEntry.getLength() || 0 == lvEntry.getHashValue().length) {
                    // The local version row does not contain the data we need
                    // to compare against.
                    pendEdit = false;
                } else if (lvEntry.getLength() != fromDisk.getFileSize() && !symlink) {
                    // File size has changed. This is a pending edit.
                    pendEdit = true;
                } else {
                    final long onDiskModifiedTime = fromDisk.getLastWriteTime();

                    // 1. check content if modify time changed for normal file
                    // 2. check whether link target has changed for symlink
                    if (symlink || lvEntry.getLastModifiedTime() != onDiskModifiedTime) {
                        // Last modified date has changed. Hash the file to see
                        // if it has changed
                        pendEdit = true;

                        // If MD5 is a banned algorithm then the array will come
                        // back zero-length
                        byte[] onDiskHash = new byte[0];
                        try {
                            onDiskHash = CheckinEngine.computeMD5Hash(lvEntry.getLocalItem(), null);
                        } catch (final CoreCancelException e) {
                            // Won't happen because we passed a null TaskMonitor
                        }

                        if (onDiskHash.length > 0 && Arrays.equals(onDiskHash, lvEntry.getHashValue())) {
                            pendEdit = false;

                            // We will update the local version row to reflect
                            // the new last-modified time.
                            // Additionally, if the item has a pending edit, we
                            // will selective undo that pending edit.

                            toUndo.add(new KeyValuePair<String, Long>(lvEntry.getLocalItem(), onDiskModifiedTime));
                        }
                    }
                }

                if (pendEdit && !skippedItems.contains(lvEntry.getLocalItem())) {
                    final LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                    if (null == pcEntry || !pcEntry.isEdit()) {
                        final ChangeRequest changeRequest = new ChangeRequest(
                            new ItemSpec(lvEntry.getLocalItem(), RecursionType.NONE),
                            LatestVersionSpec.INSTANCE,
                            RequestType.EDIT,
                            ItemType.FILE,
                            VersionControlConstants.ENCODING_UNCHANGED,
                            LockLevel.UNCHANGED,
                            0,
                            null,
                            false);

                        final AtomicReference<Failure[]> outDummy = new AtomicReference<Failure[]>();
                        final ChangeRequest[] changeRequests = new ChangeRequest[1];
                        changeRequests[0] = changeRequest;

                        LocalDataAccessLayer.pendEdit(
                            LocalWorkspaceTransaction.getCurrent().getWorkspace(),
                            wp,
                            lv,
                            pc,
                            changeRequests,
                            true,
                            outDummy,
                            null);

                        // Since we've modified the pending changes table in a
                        // silent way, we want to set the flag on the
                        // transaction we're a part of that indicates the
                        // PendingChangesChanged event should be raised for this
                        // workspace, once the transaction completes.
                        LocalWorkspaceTransaction.getCurrent().setRaisePendingChangesChanged(true);
                    }
                }

                /*
                 * TEE-specific code to detect Unix symbolic links and execute
                 * bit.
                 */
                if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)
                    && LocalWorkspaceTransaction.getCurrent().getWorkspace().getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                    PropertyValue pendProperty = null;
                    if (PlatformMiscUtils.getInstance().getEnvironmentVariable(
                        EnvironmentVariables.DISABLE_SYMBOLIC_LINK_PROP) == null) {

                        final boolean isSymlink = PropertyConstants.IS_SYMLINK.equals(
                            PropertyUtils.selectMatching(lvEntry.getPropertyValues(), PropertyConstants.SYMBOLIC_KEY));

                        if (isSymlink != fromDisk.isSymbolicLink()) {
                            pendProperty = fromDisk.isSymbolicLink() ? PropertyConstants.IS_SYMLINK
                                : PropertyConstants.NOT_SYMLINK;
                        }

                        if (pendProperty != null && !skippedItems.contains(lvEntry.getLocalItem())) {
                            final ChangeRequest changeRequest = new ChangeRequest(
                                new ItemSpec(lvEntry.getLocalItem(), RecursionType.NONE),
                                LatestVersionSpec.INSTANCE,
                                RequestType.PROPERTY,
                                ItemType.FILE,
                                VersionControlConstants.ENCODING_UNCHANGED,
                                LockLevel.UNCHANGED,
                                0,
                                null,
                                false);

                            changeRequest.setProperties(new PropertyValue[] {
                                pendProperty
                            });

                            final AtomicBoolean outOnlineOperationRequired = new AtomicBoolean();
                            final AtomicReference<Failure[]> outDummy = new AtomicReference<Failure[]>();
                            final ChangeRequest[] changeRequests = new ChangeRequest[1];
                            changeRequests[0] = changeRequest;

                            // Include property filters so any listeners have
                            // them
                            LocalDataAccessLayer.pendPropertyChange(
                                LocalWorkspaceTransaction.getCurrent().getWorkspace(),
                                wp,
                                lv,
                                pc,
                                changeRequests,
                                true,
                                outDummy,
                                outOnlineOperationRequired,
                                new String[] {
                                    PropertyConstants.SYMBOLIC_KEY
                            });

                            LocalWorkspaceTransaction.getCurrent().setRaisePendingChangesChanged(true);
                        }
                    }

                    if (pendProperty == null
                        && PlatformMiscUtils.getInstance().getEnvironmentVariable(
                            EnvironmentVariables.DISABLE_DETECT_EXECUTABLE_PROP) == null) {

                        final boolean lvExecutable = PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(
                            PropertyUtils.selectMatching(
                                lvEntry.getPropertyValues(),
                                PropertyConstants.EXECUTABLE_KEY));

                        if (lvExecutable != fromDisk.isExecutable()) {
                            pendProperty = fromDisk.isExecutable() ? PropertyConstants.EXECUTABLE_ENABLED_VALUE
                                : PropertyConstants.EXECUTABLE_DISABLED_VALUE;
                        }

                        if (pendProperty != null && !skippedItems.contains(lvEntry.getLocalItem())) {
                            final ChangeRequest changeRequest = new ChangeRequest(
                                new ItemSpec(lvEntry.getLocalItem(), RecursionType.NONE),
                                LatestVersionSpec.INSTANCE,
                                RequestType.PROPERTY,
                                ItemType.FILE,
                                VersionControlConstants.ENCODING_UNCHANGED,
                                LockLevel.UNCHANGED,
                                0,
                                null,
                                false);

                            changeRequest.setProperties(new PropertyValue[] {
                                pendProperty
                            });

                            final AtomicBoolean outOnlineOperationRequired = new AtomicBoolean();
                            final AtomicReference<Failure[]> outDummy = new AtomicReference<Failure[]>();
                            final ChangeRequest[] changeRequests = new ChangeRequest[1];
                            changeRequests[0] = changeRequest;

                            // Include property filters so any listeners have
                            // them
                            LocalDataAccessLayer.pendPropertyChange(
                                LocalWorkspaceTransaction.getCurrent().getWorkspace(),
                                wp,
                                lv,
                                pc,
                                changeRequests,
                                true,
                                outDummy,
                                outOnlineOperationRequired,
                                new String[] {
                                    PropertyConstants.EXECUTABLE_KEY
                            });

                            /*
                             * Since we've modified the pending changes table in
                             * a silent way, we want to set the flag on the
                             * transaction we're a part of that indicates the
                             * PendingChangesChanged event should be raised for
                             * this workspace, once the transaction completes.
                             */
                            LocalWorkspaceTransaction.getCurrent().setRaisePendingChangesChanged(true);
                        }
                    }
                }
            }
        }
    }

    private boolean addCandidateAdd(final EnumeratedLocalItem item) {
        if (item.isDirectory() && !item.isSymbolicLink()) {
            return false;
        }

        if (item.getServerItem().lastIndexOf(ServerPath.PREFERRED_SEPARATOR_CHARACTER) <= 1) {
            // Do not allow candidate adds in the root folder ($/)
            return false;
        }

        final LocalPendingChange pc = new LocalPendingChange(
            item.getServerItem(),
            null,
            0,
            ItemType.FILE,
            VersionControlConstants.ENCODING_UNCHANGED,
            ZERO_LENGTH_BYTE_ARRAY,
            0,
            ChangeType.ADD_EDIT_ENCODING);

        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            if (item.isSymbolicLink()) {
                pc.setChangeType(pc.getChangeType().combine(ChangeType.PROPERTY));
                pc.setPropertyValues(new PropertyValue[] {
                    PropertyConstants.IS_SYMLINK
                });
            } else if (item.isExecutable()
                && PlatformMiscUtils.getInstance().getEnvironmentVariable(
                    EnvironmentVariables.DISABLE_DETECT_EXECUTABLE_PROP) == null) {
                pc.setChangeType(pc.getChangeType().combine(ChangeType.PROPERTY));
                pc.setPropertyValues(new PropertyValue[] {
                    PropertyConstants.EXECUTABLE_ENABLED_VALUE
                });
            }
        }

        pc.setCandidate(true);

        addCandidateChange(pc);

        return true;
    }

    private void addCandidateChange(final LocalPendingChange candidate) {
        final LocalPendingChange existingCandidate = pc.getCandidateByTargetServerItem(candidate.getTargetServerItem());

        if (null == existingCandidate
            || existingCandidate.getChangeType().equals(candidate.getChangeType()) == false
            || existingCandidate.isCommitted() != candidate.isCommitted()
            ||
            // Use an ordinal comparison here to pick up changes in casing on
            // candidate adds
        (existingCandidate.getCommittedServerItem() != null
            && !existingCandidate.getCommittedServerItem().equals(candidate.getCommittedServerItem()))) {
            if (null != existingCandidate) {
                pc.removeCandidateByTargetServerItem(existingCandidate.getTargetServerItem());
            }

            pc.addCandidate(candidate);
            LocalWorkspaceTransaction.getCurrent().setRaisePendingChangeCandidatesChanged(true);
        }

        candidateChanges.add(candidate.getTargetServerItem());
    }

    private void fireChangedByScanEvent() {
        if (changedByScan.size() > 0) {
            final Workspace workspace = LocalWorkspaceTransaction.getCurrent().getWorkspace();

            workspace.getClient().getEventEngine().fireScannerModifiedFile(
                new ScannerModifiedFilesEvent(EventSource.newFromHere(), workspace, changedByScan));
        }
    }
}
