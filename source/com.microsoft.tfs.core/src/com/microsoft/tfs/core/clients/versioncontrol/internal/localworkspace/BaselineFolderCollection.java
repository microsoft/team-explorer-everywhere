// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.AsyncOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.BaselineDownloadAsyncOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.BaselineUpdaterAsyncOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.BaselineDownloadWorker;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.BaselineUpdaterWorker;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CorruptBaselineException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MissingBaselineException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.HashUtils;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.temp.TempStorageService;

public class BaselineFolderCollection {
    private static final Log log = LogFactory.getLog(BaselineFolderCollection.class);

    /*
     * Extension for temporary files in the baseline directory.
     */
    private static final String TMP_EXTENSION = ".tmp"; //$NON-NLS-1$

    /*
     * When decompressing a gzipped baseline, the size of the buffer to use
     * while streaming the decompressed content to disk.
     */
    private static final int DECOMPRESSION_BUFFER_SIZE = 4096;

    /*
     * Value to indicate a read lock token has not been initialized.
     */
    public static final int UNINITIALIZED_READ_LOCK_TOKEN = 0;

    private final TokenReaderWriterLock rwLock;
    private final Workspace workspace;
    private List<BaselineFolder> baselineFolders;

    public BaselineFolderCollection(final Workspace workspace, final List<BaselineFolder> baselineFolders) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.rwLock = new TokenReaderWriterLock();
        this.workspace = workspace;

        final int token = this.rwLock.enterWriteLock();

        try {
            updateFrom(baselineFolders);
        } finally {
            this.rwLock.exitWriteLock(token);
        }
    }

    /**
     * Takes a read lock on the BaselineFolderCollection. Writes to the
     * BaselineFolderCollection are blocked until you unlock.
     */
    public int lockForRead() {
        return rwLock.enterReadLock();
    }

    /**
     * Releases a read lock previously obtained on the BaselineFolderCollection.
     */
    public void unlockForRead(final int token) {
        rwLock.exitReadLock(token);
    }

    /**
     * Takes a write lock on the BaselineFolderCollection. Your thread will
     * block until all readers have drained.
     */
    public int lockForWrite() {
        return rwLock.enterWriteLock();
    }

    /**
     * Releases a write lock previously obtained on the
     * BaselineFolderCollection.
     */
    public void unlockForWrite(final int token) {
        rwLock.exitWriteLock(token);
    }

    /**
     * Given a new set of baseline folders, update the cached copy of the
     * baseline folders held by this object. The caller must be holding a write
     * lock.
     *
     * @param baselineFolders
     *        New set of BaselineFolder objects
     */
    public void updateFrom(final List<BaselineFolder> baselineFolders) {
        Check.notNull(baselineFolders, "baselineFolders"); //$NON-NLS-1$

        this.baselineFolders = new ArrayList<BaselineFolder>(baselineFolders.size());

        for (int i = 0; i < baselineFolders.size(); i++) {
            this.baselineFolders.add(baselineFolders.get(i).clone());
        }
    }

    /**
     * Given a baseline file GUID, and the target local path where the item will
     * be placed in the workspace, returns the path (without file extension)
     * where the baseline file should be created on disk. This method is used to
     * place new baselines on disk.
     *
     * Do not use this method to look up the location of a baseline that already
     * exists. Instead, call GetBaselineLocation().
     *
     * This overload of GetNewBaselineLocation is intended to be used by those
     * callers who are already holding a read lock on the
     * BaselineFolderCollection. Pass the read lock token you received from
     * LockForRead() as the readLockToken parameter to this method.
     *
     * @param baselineFileGuid
     *        Baseline file GUID for the baseline
     * @param targetLocalItem
     *        The location on disk of the file
     * @return The filename on disk where the baseline should be placed (except
     *         for file extension)
     */
    public String getNewBaselineLocation(
        final byte[] baselineFileGuid,
        final String targetLocalItem,
        final int readLockToken) {
        Check.isTrue(readLockToken > 0, "readLockToken"); //$NON-NLS-1$

        return getNewBaselineLocation(workspace, baselineFolders, baselineFileGuid, targetLocalItem);
    }

    /**
     * Called by the client-side Get logic when it is about to delete a folder
     * from the local disk. This routine checks to see if that folder parents a
     * baseline folder.
     *
     *
     * @param sourceLocalItem
     *        Folder which is about to be deleted by the client-side Get logic
     * @return
     */
    public boolean isImmediateParentOfBaselineFolder(final String sourceLocalItem) {
        Check.notEmpty(sourceLocalItem, "sourceLocalItem"); //$NON-NLS-1$

        final int token = rwLock.enterReadLock();

        try {
            BaselineFolder baselineFolder = null;

            for (final BaselineFolder bf : baselineFolders) {
                if (null == bf.path) {
                    continue;
                }

                if (LocalPath.isDirectChild(bf.path, sourceLocalItem)) {
                    baselineFolder = bf;
                    break;
                }
            }

            return null != baselineFolder;
        } finally {
            rwLock.exitReadLock(token);
        }
    }

    /**
     * Given a baseline file GUID and a target location on disk, copies the
     * baseline from the baseline store to the target location. (The target
     * location always receives a decompressed copy of the baseline, even if it
     * is stored compressed in the baseline
     *
     *
     * @param baselineFileGuid
     *        Baseline file GUID to copy
     * @param targetLocalItem
     *        Target location for the baseline file
     * @param baselineFileLength
     *        (optional) If provided, the uncompressed baseline length will be
     *        compared against this value and checked after decompression. If
     *        the values do not match, an exception will be thrown.
     * @param baselineHashValue
     *        (optional) If provided, the uncompressed baseline will be hashed
     *        and its hash compared to this value after decompression. If the
     *        values to not match, an exception will be thrown.
     */
    public void copyBaselineToTarget(
        final byte[] baselineFileGuid,
        final String targetLocalItem,
        final long baselineFileLength,
        final byte[] baselineHashValue,
        final boolean symlink) {
        final int token = rwLock.enterReadLock();

        try {
            copyBaselineToTarget(
                workspace,
                baselineFolders,
                baselineFileGuid,
                targetLocalItem,
                baselineFileLength,
                baselineHashValue,
                symlink);
        } finally {
            rwLock.exitReadLock(token);
        }
    }

    /**
     * Given a baseline file GUID, removes it from the baseline folder in which
     * it resides.
     *
     *
     * @param baselineFileGuid
     *        Baseline file GUID to remove
     */
    public void deleteBaseline(final byte[] baselineFileGuid) {
        final int token = rwLock.enterReadLock();

        try {
            deleteBaseline(workspace, baselineFolders, baselineFileGuid);
        } finally {
            rwLock.exitReadLock(token);
        }
    }

    /**
     * Given a baseline file GUID, and the target local path where the item will
     * be placed in the workspace, returns the path (without file extension)
     * where the baseline file should be created on disk. This method is used to
     * place new baselines on disk.
     *
     * Do not use this method to look up the location of a baseline that already
     * exists.
     *
     * @param workspace
     * @param baselineFolders
     * @param baselineFileGuid
     *        Baseline file GUID for the baseline
     * @param targetLocalItem
     *        The location on disk of the file
     * @return The filename on disk where the baseline should be placed (except
     *         for file extension)
     */
    public static String getNewBaselineLocation(
        final Workspace workspace,
        final List<BaselineFolder> baselineFolders,
        final byte[] baselineFileGuid,
        final String targetLocalItem) {
        BaselineFolder.checkForValidBaselineFileGUID(baselineFileGuid);

        BaselineFolder baselineFolder = null;

        if (targetLocalItem != null && targetLocalItem.length() > 0) {
            baselineFolder =
                getBaselineFolderForPartition(baselineFolders, BaselineFolder.getPartitionForPath(targetLocalItem));
        }

        if (null == baselineFolder && baselineFolders.size() > 0) {
            baselineFolder = baselineFolders.get(0);
        }

        final AtomicReference<String> outIndividualBaselineFolder = new AtomicReference<String>();
        String toReturn;

        if (null == baselineFolder) {
            // There were no baseline folders available to host this baseline.
            // We will instead store it in our fallback location in the
            // ProgramData location.
            BaselineFolder.ensureLocalMetadataDirectoryExists(workspace);

            toReturn = BaselineFolder.getPathFromGUID(
                workspace.getLocalMetadataDirectory(),
                baselineFileGuid,
                outIndividualBaselineFolder);

            final File directory = new File(outIndividualBaselineFolder.get());
            if (!directory.exists()) {
                directory.mkdirs();
            }

            return toReturn;
        } else {
            BaselineFolder.ensureBaselineDirectoryExists(workspace, baselineFolder.getPath());

            toReturn =
                BaselineFolder.getPathFromGUID(baselineFolder.getPath(), baselineFileGuid, outIndividualBaselineFolder);

            final File directory = new File(outIndividualBaselineFolder.get());
            if (!directory.exists()) {
                directory.mkdirs();
            }

            return toReturn;
        }
    }

    /**
     * Given a baseline file GUID, returns the full path to the baseline file in
     * a baseline folder. The baseline file may be compressed or uncompressed.
     *
     *
     * @param workspace
     * @param baselineFolders
     * @param baselineFileGuid
     *        Baseline GUID to look up
     * @return The full path of the baseline file, if found
     */
    public static String getBaselineLocation(
        final Workspace workspace,
        final List<BaselineFolder> baselineFolders,
        final byte[] baselineFileGuid) {
        // This overload just eats the isBaselineCompressed bit for callers that
        // do not need it.
        final AtomicBoolean isBaselineCompressed = new AtomicBoolean();
        return getBaselineLocation(workspace, baselineFolders, baselineFileGuid, isBaselineCompressed);
    }

    /**
     * Given a baseline file GUID, returns the full path to the baseline file in
     * a baseline folder. From the path that's returned, you can tell whether or
     * not it is compressed (compressed baselines will end in ".gz",
     * uncompressed in ".rw"), but for convenience it is also returned as an out
     * parameter.
     *
     *
     * @param workspace
     * @param baselineFolders
     * @param baselineFileGuid
     *        Baseline GUID to look up
     * @param isBaselineCompressed
     *        True if the baseline is compressed, false otherwise
     * @return The full path of the baseline file, if found
     */
    public static String getBaselineLocation(
        final Workspace workspace,
        final List<BaselineFolder> baselineFolders,
        final byte[] baselineFileGuid,
        final AtomicBoolean isBaselineCompressed) {
        BaselineFolder.checkForValidBaselineFileGUID(baselineFileGuid);

        isBaselineCompressed.set(false);
        String baselineLocation = null;

        for (final BaselineFolder baselineFolder : baselineFolders) {
            if (null == baselineFolder.path) {
                continue;
            }

            // An example path returned by this method might be:
            // @"D:\workspace\$tf\1\408bed21-9023-47c3-8280-b1ec3ffacd94"
            final String potentialLocation = baselineFolder.getPathFromGUID(baselineFileGuid);

            if (null == potentialLocation) {
                continue;
            }

            // 1. Check the .gz extension (more common)
            final String gzPotentialLocation = potentialLocation + BaselineFolder.getGzipExtension();

            if (new File(gzPotentialLocation).exists()) {
                isBaselineCompressed.set(true);
                baselineLocation = gzPotentialLocation;
                break;
            }

            // 2. Check the .rw extension
            final String rawPotentialLocation = potentialLocation + BaselineFolder.getRawExtension();

            if (new File(rawPotentialLocation).exists()) {
                baselineLocation = rawPotentialLocation;
                break;
            }
        }

        if (null == baselineLocation) {
            // An example path returned by this method might be:
            // @"C:\ProgramData\TFS\Offline\11c92875-fac4-4277-afba-d16f6eeb2189\ws1;domain;username\1\408bed21-9023-47c3-8280-b1ec3ffacd94"
            final String programDataLocation =
                BaselineFolder.getPathFromGUID(workspace.getLocalMetadataDirectory(), baselineFileGuid);

            if (null != programDataLocation) {
                // 1. Check the .gz extension (more common)
                final String gzProgramDataLocation = programDataLocation + BaselineFolder.getGzipExtension();

                if (new File(gzProgramDataLocation).exists()) {
                    isBaselineCompressed.set(true);
                    baselineLocation = gzProgramDataLocation;
                }

                if (null == baselineLocation) {
                    // 2. Check the .rw extension
                    final String rawProgramDataLocation = programDataLocation + BaselineFolder.getRawExtension();

                    if (new File(rawProgramDataLocation).exists()) {
                        baselineLocation = rawProgramDataLocation;
                    }
                }
            }
        }

        return baselineLocation;
    }

    /**
     * Given a baseline file GUID and a target location on disk, copies the
     * baseline from the baseline store to the target location. (The target
     * location always receives a decompressed copy of the baseline, even if it
     * is stored compressed in the baseline folder.)
     *
     *
     * @param workspace
     * @param baselineFolders
     * @param baselineFileGuid
     *        Baseline file GUID to copy
     * @param targetLocalItem
     *        Target location for the baseline file
     * @param baselineFileLength
     *        (optional) If provided, the uncompressed baseline length will be
     *        compared against this value and checked after decompression. If
     *        the values do not match, an exception will be thrown.
     * @param baselineHashValue
     *        (optional) If provided, the uncompressed baseline will be hashed
     *        and its hash compared to this value after decompression. If the
     *        values to not match, an exception will be thrown.
     */
    public static void copyBaselineToTarget(
        final Workspace workspace,
        final List<BaselineFolder> baselineFolders,
        final byte[] baselineFileGuid,
        final String targetLocalItem,
        final long baselineFileLength,
        final byte[] baselineHashValue,
        final boolean symlink) {
        Check.notNullOrEmpty(targetLocalItem, "targetLocalItem"); //$NON-NLS-1$
        BaselineFolder.checkForValidBaselineFileGUID(baselineFileGuid);

        // Clear the target location.
        final File file = new File(targetLocalItem);
        file.delete();

        final AtomicBoolean outIsBaselineCompressed = new AtomicBoolean();
        final String baselineLocation =
            getBaselineLocation(workspace, baselineFolders, baselineFileGuid, outIsBaselineCompressed);

        if (null == baselineLocation) {
            // The baseline could not be located on disk.
            throw new MissingBaselineException(targetLocalItem);
        }

        String decompressedBaselineLocation = baselineLocation;

        try {
            byte[] decompressedHashValue = null;
            final boolean haveBaselineHashValue = null != baselineHashValue && 16 == baselineHashValue.length;

            MessageDigest md5Digest = null;
            if (haveBaselineHashValue) {
                md5Digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
            }

            if (outIsBaselineCompressed.get()) {
                // The temporary file is created in the folder where the
                // compressed baseline currently exists. We use the temporary
                // file extension so that we can clean up the file later if we
                // happen to lose it.
                decompressedBaselineLocation =
                    LocalPath.combine(LocalPath.getParent(baselineLocation), GUID.newGUIDString()) + TMP_EXTENSION;

                // Decompress the baseline to a temporary file. Then move the
                // temporary file to the target location.
                final byte[] buffer = new byte[DECOMPRESSION_BUFFER_SIZE];

                InputStream inputStream = null;
                OutputStream outputStream = null;

                try {
                    inputStream = new GZIPInputStream(new FileInputStream(baselineLocation));
                    if (!symlink) {
                        outputStream = new FileOutputStream(decompressedBaselineLocation);
                    }

                    int bytesRead;
                    while (true) {
                        bytesRead = inputStream.read(buffer, 0, buffer.length);

                        if (bytesRead < 0) {
                            break;
                        } else if (bytesRead == 0) {
                            continue;
                        }

                        if (null != md5Digest) {
                            md5Digest.update(buffer, 0, bytesRead);
                        }

                        if (symlink) {
                            final String targetLink = new String(buffer, 0, bytesRead);
                            FileSystemUtils.getInstance().createSymbolicLink(targetLink, targetLocalItem);
                        } else {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    if (null != md5Digest) {
                        decompressedHashValue = md5Digest.digest();
                    }
                } finally {
                    if (inputStream != null) {
                        IOUtils.closeSafely(inputStream);
                    }
                    if (outputStream != null) {
                        IOUtils.closeSafely(outputStream);
                    }
                }
            }

            // First, check to see if the length of the file matches.
            if (-1 != baselineFileLength && baselineFileLength != new File(decompressedBaselineLocation).length()) {
                throw new CorruptBaselineException(
                    targetLocalItem,
                    Messages.getString("BaselineFolderCollection.BaselineLengthDoesNotMatch")); //$NON-NLS-1$
            }

            if (null != md5Digest && null == decompressedHashValue && !symlink) {
                // Calculate the decompressed hash value for a raw file (.rw
                // extension) as we will not have gone through the streaming
                // logic above
                decompressedHashValue =
                    HashUtils.hashFile(new File(decompressedBaselineLocation), HashUtils.ALGORITHM_MD5);
            }

            if (haveBaselineHashValue && null != decompressedHashValue && 16 == decompressedHashValue.length) {
                if (!Arrays.equals(baselineHashValue, decompressedHashValue)) {
                    throw new CorruptBaselineException(
                        targetLocalItem,
                        Messages.getString("BaselineFolderCollection.BaselineHashValueDoesNotMatch")); //$NON-NLS-1$
                }
            }

            // Put the decompressed baseline at the target location. We've
            // verified its contents are correct.
            if (!symlink) {
                if (outIsBaselineCompressed.get()) {
                    FileHelpers.rename(decompressedBaselineLocation, targetLocalItem);
                } else {
                    FileCopyHelper.copy(decompressedBaselineLocation, targetLocalItem);
                }
            }
        } catch (final Exception ex) {
            // If the baseline is corrupt, delete it so we'll throw a missing
            // baseline exception next time. (This is not strictly necessary.)
            if (ex instanceof CorruptBaselineException && null != baselineLocation) {
                FileHelpers.deleteFileWithoutException(baselineLocation);
            }

            // Try not to leak a temp file on the way out if we're throwing.
            final File tempFile = new File(decompressedBaselineLocation);
            if (outIsBaselineCompressed.get() && null != decompressedBaselineLocation && tempFile.exists()) {
                FileHelpers.deleteFileWithoutException(decompressedBaselineLocation);
            }

            throw new VersionControlException(ex);
        }
    }

    /**
     * Given a baseline file GUID, removes it from the baseline folder in which
     * it resides.
     *
     *
     * @param workspace
     * @param baselineFolders
     * @param baselineFileGuid
     *        Baseline file GUID to remove
     */
    public static void deleteBaseline(
        final Workspace workspace,
        final List<BaselineFolder> baselineFolders,
        final byte[] baselineFileGuid) {
        BaselineFolder.checkForValidBaselineFileGUID(baselineFileGuid);

        // Remove this baseline file GUID from all baseline folders.
        for (final BaselineFolder baselineFolder : baselineFolders) {
            if (null == baselineFolder.path) {
                continue;
            }

            final String baselineLocation = baselineFolder.getPathFromGUID(baselineFileGuid);

            new File(baselineLocation + BaselineFolder.getGzipExtension()).delete();
            new File(baselineLocation + BaselineFolder.getRawExtension()).delete();
        }

        // Also remove this baseline file GUID from the ProgramData location.
        final String programDataBaselineLocation =
            BaselineFolder.getPathFromGUID(workspace.getLocalMetadataDirectory(), baselineFileGuid);

        new File(programDataBaselineLocation + BaselineFolder.getGzipExtension()).delete();
        new File(programDataBaselineLocation + BaselineFolder.getRawExtension()).delete();
    }

    /**
     * Given a local path on disk and the baseline file GUID corresponding to
     * that item, ensures that the baseline file for that item is in the correct
     * BaselineFolder.
     *
     *
     * @param workspace
     * @param baselineFolders
     * @param baselineFileGuid
     *        Baseline file GUID of the item
     * @param currentLocalItem
     *        Current local path of the item
     */
    public static void updateBaselineLocation(
        final Workspace workspace,
        final List<BaselineFolder> baselineFolders,
        final byte[] baselineFileGuid,
        final String currentLocalItem) {
        Check.notNullOrEmpty(currentLocalItem, "currentLocalItem"); //$NON-NLS-1$
        BaselineFolder.checkForValidBaselineFileGUID(baselineFileGuid);

        final String newBaselinePartition = BaselineFolder.getPartitionForPath(currentLocalItem);

        // Find the baseline file for this baseline file GUID. It should not be
        // the case that both the .gz and .rw baseline files exist for the same
        // GUID. (If they do, in a debug build, the GetBaselineLocation method
        // will assert.)
        final String baselineLocation = getBaselineLocation(workspace, baselineFolders, baselineFileGuid);

        // Do we need to move this item at all?
        if (null != baselineLocation
            && !LocalPath.equals(BaselineFolder.getPartitionForPath(baselineLocation), newBaselinePartition)) {
            // Is there a BaselineFolder for the new partition which is in the
            // Valid state?
            String newBaselineLocation;
            final BaselineFolder newBaselineFolder =
                getBaselineFolderForPartition(baselineFolders, newBaselinePartition);

            if (null != newBaselineFolder) {
                newBaselineLocation = newBaselineFolder.getPathFromGUID(baselineFileGuid);
            } else {
                // There's no baseline folder for the new partition. We'll move
                // the file from its old baseline folder to the ProgramData
                // location.
                newBaselineLocation =
                    BaselineFolder.getPathFromGUID(workspace.getLocalMetadataDirectory(), baselineFileGuid);
            }

            new File(baselineLocation).renameTo(
                new File(LocalPath.combine(newBaselineLocation, LocalPath.getFileExtension(baselineLocation))));
        }
    }

    /**
     * Given a partition (i.e. "E:\" or "\\tfsstor\share"), return the
     * BaselineFolder object for the valid baseline folder for this workspace on
     * that partition, if it exists.
     *
     *
     * @param baselineFolders
     * @param partition
     *        Partition to look up
     * @return A BaselineFolder object with state Valid, or null if not found
     */
    private static BaselineFolder getBaselineFolderForPartition(
        final List<BaselineFolder> baselineFolders,
        final String partition) {
        for (final BaselineFolder baselineFolder : baselineFolders) {
            if (null != baselineFolder.partition
                && LocalPath.equals(partition, baselineFolder.partition)
                && BaselineFolderState.VALID == baselineFolder.state) {
                return baselineFolder;
            }
        }

        return null;
    }

    public void processBaselineRequests(final Workspace workspace, final Iterable<BaselineRequest> requests) {
        final AtomicReference<Iterable<BaselineRequest>> outFailedLocalRequests =
            new AtomicReference<Iterable<BaselineRequest>>();

        try {
            processBaselineRequests(workspace, requests, false, outFailedLocalRequests);
        } catch (final CoreCancelException e) {
            // Can't happen because we're passing false for throwIfCanceled.
        }
    }

    public void processBaselineRequests(
        final Workspace workspace,
        final Iterable<BaselineRequest> requests,
        final boolean throwIfCanceled,
        final AtomicReference<Iterable<BaselineRequest>> outFailedLocalRequests) throws CoreCancelException {
        outFailedLocalRequests.set(null);

        // 1. The first async operation gzips content from the local disk and
        // places the resulting baseline files into the baseline folder.
        final BaselineUpdaterAsyncOperation localDiskAsyncOp = new BaselineUpdaterAsyncOperation(this);

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();

        final AccountingCompletionService<WorkerStatus> completionService =
            new AccountingCompletionService<WorkerStatus>(workspace.getClient().getUploadDownloadWorkerExecutor());

        try {
            for (final BaselineRequest request : requests) {
                if (null != request.getSourceLocalItem()) {
                    if (throwIfCanceled && taskMonitor.isCanceled()) {
                        throw new CoreCancelException();
                    }

                    completionService.submit(new BaselineUpdaterWorker(taskMonitor, request, localDiskAsyncOp));
                }
            }
        } catch (final CoreCancelException e) {
            BaselineUpdaterAsyncOperation.waitForCompletions(completionService);
            throw e;
        }

        BaselineUpdaterAsyncOperation.waitForCompletions(completionService);
        testForFatalError(localDiskAsyncOp);

        // 2. The second async operation fetches content from the server and
        // places it into the baseline folder. It also handles delete requests
        // (no DownloadUrl or SourceLocalItem).
        final BaselineDownloadAsyncOperation downloadAsyncOp = new BaselineDownloadAsyncOperation();

        final VersionControlClient client = workspace.getClient();

        try {
            for (final BaselineRequest request : requests) {
                if (null == request.getSourceLocalItem()) {
                    if (null != request.getDownloadURL()) {
                        final BaselineDownloadWorker worker = new BaselineDownloadWorker(
                            EventSource.newFromHere(),
                            taskMonitor,
                            client,
                            downloadAsyncOp,
                            request.getDownloadURL(),
                            this,
                            request.getBaselineFileGUID());

                        if (throwIfCanceled && taskMonitor.isCanceled()) {
                            throw new CoreCancelException();
                        }

                        completionService.submit(worker);
                    } else {
                        // This is a delete request.
                        deleteBaseline(request.getBaselineFileGUID());
                    }
                }
            }

            // 2B. Re-process the requests which failed in step #1, but have a
            // DownloadUrl for fallback. One reason a request might be in this
            // list is failing a hash check.
            for (final BaselineRequest request : localDiskAsyncOp.getFailedRequests()) {
                if (null != request.getDownloadURL()) {
                    final BaselineDownloadWorker worker = new BaselineDownloadWorker(
                        EventSource.newFromHere(),
                        taskMonitor,
                        client,
                        downloadAsyncOp,
                        request.getDownloadURL(),
                        this,
                        request.getBaselineFileGUID());

                    if (throwIfCanceled && taskMonitor.isCanceled()) {
                        throw new CoreCancelException();
                    }

                    completionService.submit(worker);
                }
            }
        } catch (final CoreCancelException e) {
            BaselineDownloadAsyncOperation.waitForCompletions(completionService);
            throw e;
        }

        BaselineDownloadAsyncOperation.waitForCompletions(completionService);
        testForFatalError(downloadAsyncOp);
        outFailedLocalRequests.set(localDiskAsyncOp.getFailedRequests());
    }

    /**
     * Tests the given state object for a fatal error, and throws an exception
     * if one is encountered.
     *
     * @param asyncOp
     *        the state object to test (must not be <code>null</code>)
     */
    private void testForFatalError(final AsyncOperation asyncOp) throws VersionControlException {
        Check.notNull(asyncOp, "state"); //$NON-NLS-1$

        final Throwable fatalError = asyncOp.getFatalError();
        if (fatalError != null) {
            throw new VersionControlException(
                Messages.getString("BaselineUpdater.FatalErrorUpdatingBaselineFiles"), //$NON-NLS-1$
                fatalError);
        }
    }

    /**
     * Creates and opens a file with the specified path. If the parent folder
     * does not exist, we create it and mark it and its parent as hidden -- we
     * assume that file reside in $tf\10\ and we need to mark both $tf and 10 as
     * hidden.
     *
     *
     * @param filePath
     *        the path to create the file at
     * @return
     * @throws IOException
     */
    public static FileOutputStream createFile(final String filePath) throws IOException {
        final AtomicBoolean tempCreated = new AtomicBoolean();
        return createFile(new AtomicReference<String>(filePath), false, null, tempCreated);
    }

    /**
     * Creates and opens a file with the specified path. If the parent folder
     * does not exist, we create it and mark it and its parent as hidden -- we
     * assume that file reside in $tf\10\ and we need to mark both $tf and 10 as
     * hidden. If filePath was not specified or its creation failed, and
     * createTempOnFailure=true we will create a new temporary file, using
     * tempUniqueString.
     *
     *
     * @param filePath
     *        in: the path to create the file at, out: the path actually created
     *        (possibly a temporary file in another directory) (must not be
     *        <code>null</code>)
     * @param createTempOnFailure
     * @param tempUniqueString
     * @param tempCreated
     * @return
     * @throws IOException
     */
    public static FileOutputStream createFile(
        final AtomicReference<String> filePath,
        final boolean createTempOnFailure,
        final String tempUniqueString,
        final AtomicBoolean tempCreated) throws IOException {
        Check.notNull(filePath, "filePath"); //$NON-NLS-1$

        FileOutputStream localStream = null;
        tempCreated.set(false);

        Exception createException = null;
        if (filePath.get() != null && filePath.get().length() > 0) {
            try {
                localStream = new FileOutputStream(filePath.get());
            } catch (final Exception ex) {
                createException = ex;
                if (!createTempOnFailure) {
                    throw new VersionControlException(ex);
                }
            }
        }
        if (localStream == null && createTempOnFailure) {
            tempCreated.set(true);

            final File tempFile = TempStorageService.getInstance().createTempFile();
            localStream = new FileOutputStream(tempFile);

            log.info(
                MessageFormat.format(
                    "Could not create baseline folder collection file {0}, using temporary file {1}", //$NON-NLS-1$
                    filePath.get(),
                    tempFile),
                createException);

            filePath.set(tempFile.getAbsolutePath());
        } else {
            Check.notNullOrEmpty(filePath.get(), "filePath.get()"); //$NON-NLS-1$
        }

        return localStream;
    }

    /**
     * A reader-writer lock that does not use thread affinity.
     *
     * @threadsafety unknown
     */
    private class TokenReaderWriterLock {
        private final Object lock;
        private int readerCount;
        private int declaredWriters;
        private boolean hasWriter;
        private int lastReadToken;
        private int lastWriteToken;

        public TokenReaderWriterLock() {
            lock = new Object();
            lastReadToken = 0;
            lastWriteToken = 0;
        }

        public int enterReadLock() {
            synchronized (lock) {
                while (hasWriter || declaredWriters > 0) {
                    // Wait for all declared writers to drain.
                    try {
                        lock.wait();
                    } catch (final InterruptedException e) {
                        // ignore
                    }
                }

                readerCount++;

                return ++lastReadToken;
            }
        }

        public void exitReadLock(final int token) {
            // Read lock tokens are positive integers.
            Check.isTrue(token >= 1 && token <= lastReadToken, "token"); //$NON-NLS-1$

            synchronized (lock) {
                readerCount--;

                if (0 == readerCount && declaredWriters > 0) {
                    // All readers have drained, and we have waiting declared
                    // writers.
                    lock.notifyAll();
                }
            }
        }

        public int enterWriteLock() {
            synchronized (lock) {
                declaredWriters++;

                // Wait for all readers to drain, and for there to be no writer.
                while (readerCount > 0 || hasWriter) {
                    try {
                        lock.wait();
                    } catch (final InterruptedException e) {
                        // ignore
                    }
                }

                declaredWriters--;
                hasWriter = true;

                return --lastWriteToken;
            }
        }

        public void exitWriteLock(final int token) {
            // Write lock tokens are negative integers.
            if (token != lastWriteToken) {
                throw new IllegalStateException("token"); //$NON-NLS-1$
            }

            synchronized (lock) {
                hasWriter = false;

                // Wake up whoever is next in line. If there is a writer
                // waiting, he will win.
                lock.notifyAll();
            }
        }
    }

}
