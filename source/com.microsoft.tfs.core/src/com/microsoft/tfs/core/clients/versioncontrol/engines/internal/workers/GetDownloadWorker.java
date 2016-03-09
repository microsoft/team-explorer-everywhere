// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.AsyncGetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.BaselineFileDownloadOutput;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.FileDownloadOutput;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.datetime.DotNETDate;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * A worker that can be run in its own thread to perform file downloads and the
 * completion stuff that happens after (moving from temp file to working folder
 * file, queueing a local version update).
 * <p>
 * <b>Event Policy</b>
 * <p>
 * Any {@link VersionControlEventEngine} events fired from here MUST use the
 * {@link EventSource} instance given during construction to uphold the event
 * origination point policy guaranteed by public core methods (in
 * {@link Workspace}, etc.).
 * <p>
 * <b>Cancelation Policy</b>
 * <p>
 * A worker can be interrupted (canceled) through the {@link TaskMonitor} given
 * to it during construction. If the {@link TaskMonitor} becomes canceled before
 * {@link #call()} is invoked, or while it is running, the worker will return a
 * {@link WorkerStatus} indicating the cancellation. If the {@link TaskMonitor}
 * becomes canceled after {@link #call()} completes, the {@link WorkerStatus}
 * will indicate a normal completion.
 * <p>
 * When a non-fatal error is encountered, the exception is reported through
 * {@link GetEngine#onNonFatalError(Throwable, Workspace)} and the thread
 * completes with a NORMAL final state. Only a fatal error (one that should stop
 * all get operation processing) causes an ERROR final state.
 *
 * @threadsafety thread-safe
 */
public class GetDownloadWorker extends AbstractDownloadWorker {
    private static final Log log = LogFactory.getLog(GetDownloadWorker.class);

    private final GetEngine getEngine;
    private final GetOperation operation;
    private final AsyncGetOperation asyncOp;
    private final FileSystemAttributes existingLocalAttrs;
    private final BaselineFolderCollection baselineFolders;
    private final byte[] baselineFileGUID;

    /**
     * Create a worker that can download and complete a get operation. Use the
     * {@link #call()} method to do the work.
     *
     * @param eventSource
     *        describes the context at the event origination point so events
     *        fired from here can be related to an action performed by the
     *        consumer (must not be <code>null</code>)
     * @param cancelMonitor
     *        the {@link TaskMonitor} to detect cancelation on (must not be
     *        <code>null</code>)
     * @param client
     *        the client (must not be <code>null</code>)
     * @param getEngine
     *        the get engine (must not be <code>null</code>)
     * @param operation
     *        the {@link GetOperation} being processed (must not be
     *        <code>null</code>)
     * @param asyncOp
     *        the {@link AsyncGetOperation} to store fatal exceptions in (must
     *        not be <code>null</code>)
     * @param existingLocalAttrs
     *        the existing local item's attributes (must not be
     *        <code>null</code>)
     * @param baselineFolders
     *        the {@link BaselineFolderCollection} for the workspace if the
     *        download is for a local workspace, otherwise <code>null</code>
     * @param baselineFileGUID
     *        the GUID that identifies this file if this download is for a local
     *        workspace, otherwise <code>null</code>
     */
    public GetDownloadWorker(
        final EventSource eventSource,
        final TaskMonitor cancelMonitor,
        final VersionControlClient client,
        final GetEngine getEngine,
        final GetOperation operation,
        final AsyncGetOperation asyncOp,
        final FileSystemAttributes existingLocalAttrs,
        final BaselineFolderCollection baselineFolders,
        final byte[] baselineFileGUID) {
        super(eventSource, cancelMonitor, client);

        Check.notNull(getEngine, "getEngine"); //$NON-NLS-1$
        Check.notNull(operation, "operation"); //$NON-NLS-1$
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$
        Check.notNull(existingLocalAttrs, "existingLocalAttrs"); //$NON-NLS-1$

        this.getEngine = getEngine;
        this.operation = operation;
        this.asyncOp = asyncOp;
        this.existingLocalAttrs = existingLocalAttrs;
        this.baselineFolders = baselineFolders;
        this.baselineFileGUID = baselineFileGUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkerStatus call() throws Exception {
        /*
         * Try not to run any code outside this try block, so our Throwable
         * catch below can report every error (even RuntimeExceptions).
         */
        try {
            if (getCancelMonitor().isCanceled()) {
                return new WorkerStatus(this, FinalState.CANCELED);
            }

            final AtomicBoolean targetSymLink = new AtomicBoolean();
            final AtomicBoolean targetSymLinkDestinationUnmapped = new AtomicBoolean();

            // Get the target file, updating local workspace baselines if
            // appropriate.
            downloadFile(targetSymLink, targetSymLinkDestinationUnmapped);

            // Sends version updates to the server, sets attributes
            completeGetOperation(targetSymLink.get(), targetSymLinkDestinationUnmapped.get());
        } catch (final VersionControlException e) {
            // Convert to a non-fatal (this handles IOExceptions wrapped as
            // VersionControlExceptions)

            log.warn("Converted to non-fatal", e); //$NON-NLS-1$

            getEngine.onNonFatalError(
                new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("GetEngineDownloadWorker.AColonBFormat"), //$NON-NLS-1$
                        operation.getTargetLocalItem(),
                        e.getLocalizedMessage()),
                    e),
                asyncOp.getWorkspace());
        } catch (final CanceledException e) {
            /*
             * CoreCancelException is thrown if the user cancels during the
             * download or completion. There's no cleanup to do because all
             * temporary resources were cleaned up as the exception travelled
             * through the stack.
             */
            return new WorkerStatus(this, FinalState.CANCELED);
        } catch (final Throwable t) {
            /*
             * An unexpected (fatal) error happened. We have to communicate this
             * problem to the thread submitting tasks so it can take the correct
             * action (shut down other workers).
             *
             * VS checks for certain kinds of fatal vs. non-fatal exceptions
             * here, but we'll just consider them all fatal for now.
             */
            asyncOp.setFatalError(t);
            return new WorkerStatus(this, FinalState.ERROR);
        }

        return new WorkerStatus(this, FinalState.NORMAL);
    }

    /**
     * Download the file for {@link #operation}. If {@link #baselineFolders} and
     * {@link #baselineFileGUID} are non-<code>null</code>, updates the baseline
     * files for the workspace.
     *
     * @param targetSymLink
     *        if not <code>null</code>, the method sets <code>true</code> if the
     *        user wants the target to be a symbolic link, <code>false</code> if
     *        it is a normal file
     * @param targetSymLinkDestinationUnmapped
     *        if not <code>null</code>, the method sets <code>true</code> on
     *        this holder if the target was supposed to be a symbolic link but
     *        was not created because the destination was not mapped, sets
     *        <code>false</code> if it was a successful symbolc link or a normal
     *        (non-link) target item
     * @throws {@link
     *         CanceledException} if the {@link TaskMonitor} was canceled during
     *         execution. This method tries its best to delete partial or
     *         temporary files before throwing this exception.
     * @throws VersionControlException
     *         if the download failed (local file access issue, network issue,
     *         etc.)
     */
    protected void downloadFile(final AtomicBoolean targetSymLink, final AtomicBoolean targetSymLinkDestinationUnmapped)
        throws CanceledException {
        int readLockToken = BaselineFolderCollection.UNINITIALIZED_READ_LOCK_TOKEN;
        final boolean updateBaseline = baselineFolders != null && baselineFileGUID != null;

        File baselineTempFile = null;
        File workingFolderTempFile = null;

        final List<DownloadOutput> outputs = new ArrayList<DownloadOutput>(2);

        try {
            /*
             * This method differs from the VS code because of our different
             * download implementation in VersionControlClient. If using a local
             * workspace, TEE downloads the baseline and working folder files at
             * the same time (baseline file stays gzip if the server gave us
             * that, working folder file always uncompressed).
             */

            // Configure the baseline output (if needed)
            BaselineFileDownloadOutput baselineOutput = null;
            if (updateBaseline) {
                // Lock the baseline folder collection from change until this
                // file download is finished.
                readLockToken = baselineFolders.lockForRead();

                // Gets the name, but doesn't create anything on disk
                final String baselineFileNoSuffix = baselineFolders.getNewBaselineLocation(
                    baselineFileGUID,
                    operation.getTargetLocalItem(),
                    readLockToken);

                // Never gunzip this one
                baselineOutput = new BaselineFileDownloadOutput(new File(baselineFileNoSuffix), false);
                outputs.add(baselineOutput);
            }

            // Configure the working folder file output (in a block for
            // aesthetics)
            FileDownloadOutput workingFolderOutput = null;
            if (true) {
                final File targetLocalItemDirectory = new File(operation.getTargetLocalItem()).getParentFile();

                // Make sure a file isn't taking our dir name
                if (targetLocalItemDirectory.exists() && !targetLocalItemDirectory.isDirectory()) {
                    throw new VersionControlException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("GetEngineDownloadWorker.CannotCreateDirectoryBecauseFileAlreadyExistsFormat"), //$NON-NLS-1$
                        //@formatter:on
                        targetLocalItemDirectory));
                }

                if (!targetLocalItemDirectory.exists()) {
                    if (!targetLocalItemDirectory.mkdirs()) {
                        // We can use the same generic message as above if
                        // mkdirs
                        // fails because Java doesn't give us many details
                        if (!targetLocalItemDirectory.isDirectory()) {
                            throw new VersionControlException(MessageFormat.format(
                                //@formatter:off
                                Messages.getString("GetEngineDownloadWorker.CannotCreateDirectoryBecauseFileAlreadyExistsFormat"), //$NON-NLS-1$
                                //@formatter:on
                                targetLocalItemDirectory));
                        }
                    }
                }

                try {
                    workingFolderTempFile = File.createTempFile("teamexplorer", ".tmp", targetLocalItemDirectory); //$NON-NLS-1$ //$NON-NLS-2$
                    log.trace(MessageFormat.format("Using temp file {0} for download", workingFolderTempFile)); //$NON-NLS-1$
                } catch (final IOException e) {
                    throw new VersionControlException(
                        MessageFormat.format(
                            Messages.getString("GetEngineDownloadWorker.CouldNotCreateTemporaryFileInDirectoryFormat"), //$NON-NLS-1$
                            targetLocalItemDirectory,
                            e.getLocalizedMessage()),
                        e);
                }

                // Always gunzip this one
                workingFolderOutput = new FileDownloadOutput(workingFolderTempFile, true);
                outputs.add(workingFolderOutput);
            }

            getClient().downloadFileToStreams(
                operation.createDownloadSpec(),
                outputs.toArray(new DownloadOutput[outputs.size()]),
                getEventSource(),
                getCancelMonitor());

            /*
             * Pull the baseline's actual file from the output. We only know
             * this after a download because the file extension depends on the
             * MIME type the server returned.
             */
            if (baselineOutput != null) {
                // If BaselineFolderCollection gave us a temp file, make sure it
                // gets cleaned up in this method.
                if (baselineOutput.isTempFileCreatedInsteadOfBaseline()) {
                    baselineTempFile = baselineOutput.getOutputStreamFile();
                } else {
                    // Real baseline file; no temp cleanup
                    baselineTempFile = null;
                }
            }

            // Close the outputs so we can use them.
            closeDownloadOutputStreamsSafely(outputs);

            // Renames the temp file to the operation's target local item.
            moveTempFileToTargetFile(workingFolderTempFile, targetSymLink, targetSymLinkDestinationUnmapped);

            // After rename there's no working folder temp file to clean up
            workingFolderTempFile = null;
        } catch (final CanceledException e) {
            // The finally block closes streams, deletes temps
            throw e;
        } finally {
            if (updateBaseline) {
                if (readLockToken != BaselineFolderCollection.UNINITIALIZED_READ_LOCK_TOKEN) {
                    baselineFolders.unlockForRead(readLockToken);
                }
            }

            closeDownloadOutputStreamsSafely(outputs);

            if (baselineTempFile != null) {
                baselineTempFile.delete();
            }

            if (workingFolderTempFile != null) {
                workingFolderTempFile.delete();
            }
        }
    }

    /**
     * Moves a temp file to the operation's target local item (or possibly
     * creates a symbolic link instead), but doesn't set any attributes on the
     * target item.
     *
     * @param tempFile
     *        the temp file to move into the operation's target local item (must
     *        not be <code>null</code>)
     * @param targetSymLink
     *        if not <code>null</code>, the method sets <code>true</code> if the
     *        user wants the target to be a symbolic link, <code>false</code> if
     *        it is a normal file
     * @param targetSymLinkDestinationUnmapped
     *        if not <code>null</code>, the method sets <code>true</code> on
     *        this holder if the target was supposed to be a symbolic link but
     *        was not created because the destination was not mapped, sets
     *        <code>false</code> if it was a successful symbolc link or a normal
     *        (non-link) target item
     * @throws VersionControlException
     *         if the temp file could not be moved to the target file
     */
    private void moveTempFileToTargetFile(
        final File tempFile,
        final AtomicBoolean targetSymLink,
        final AtomicBoolean targetSymLinkDestinationUnmapped) {
        Check.notNull(tempFile, "tempFile"); //$NON-NLS-1$

        if (!tempFile.exists()) {
            throw new VersionControlException(
                MessageFormat.format(
                    Messages.getString("GetEngineDownloadWorker.TempFileIsMissingCantCompleteTheDownloadFormat"), //$NON-NLS-1$
                    tempFile));
        }

        /*
         * Load any symbolic link attributes first. An empty string returned
         * means the file is supposed to be a symbolic link but the destination
         * was unresolvable.
         */
        final String targetSymLinkDestination =
            getEngine.getSymbolicLinkDestination(tempFile, operation, asyncOp.getWorkspace());

        /*
         * If the target file exists, delete it so we can move the temp file
         * there. Overwrite during rename/move not supported on all systems.
         */
        final File targetFile = new File(operation.getTargetLocalItem());

        try {
            /*
             * The temp file may be subject to line ending conversion or other
             * content changes.
             */
            if (targetSymLinkDestination == null) {
                getEngine.applyFileAttributesToTempFile(
                    operation.getTargetServerItem(),
                    operation.getTargetLocalItem(),
                    operation.getEncoding(),
                    tempFile,
                    operation);
            }

            final FileSystemAttributes originalTargetAttrs = FileSystemUtils.getInstance().getAttributes(targetFile);

            // rely on FileSystemAttributes to check file/symlink exists or not
            if (originalTargetAttrs.exists()) {
                log.trace(MessageFormat.format("target file {0} exists, deleting", targetFile)); //$NON-NLS-1$

                /*
                 * Mac OS X requires us to make the original file writeable
                 * before we can delete it (this clears the immutable bit, which
                 * would also block us). Skip if the target is a symbolic link
                 * so we don't set the link destination file's attributes.
                 */

                if (targetSymLinkDestination == null && originalTargetAttrs.isReadOnly()) {
                    log.trace(MessageFormat.format("setting target file {0} writable before delete", targetFile)); //$NON-NLS-1$
                    originalTargetAttrs.setReadOnly(false);

                    if (FileSystemUtils.getInstance().setAttributes(targetFile, originalTargetAttrs)) {
                        log.trace(MessageFormat.format("target file {0} now writable before delete", targetFile)); //$NON-NLS-1$
                    } else {
                        log.warn(
                            MessageFormat.format(
                                "error setting file {0} writable before delete, expect trouble finishing the get", //$NON-NLS-1$
                                targetFile));
                    }
                }

                if (!targetFile.delete()) {
                    // Delete the temp file; we never got to use it.
                    tempFile.delete();

                    throw new VersionControlException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("GetEngineDownloadWorker.DeleteOfTargetFileFailedMakeSureNotInUseFormat"), //$NON-NLS-1$
                        //@formatter:on
                        targetFile));
                }
            }

            if (targetSymLink != null) {
                targetSymLink.set(false);
            }
            if (targetSymLinkDestinationUnmapped != null) {
                targetSymLinkDestinationUnmapped.set(false);
            }

            if (targetSymLinkDestination != null) {
                if (targetSymLink != null) {
                    targetSymLink.set(true);
                }

                log.trace(MessageFormat.format("target {0} should be a symbolic link", operation.getTargetLocalItem())); //$NON-NLS-1$

                if (targetSymLinkDestination.length() > 0) {
                    log.trace(MessageFormat.format("creating link to mapped path {0}", targetSymLinkDestination)); //$NON-NLS-1$

                    FileSystemUtils.getInstance().createSymbolicLink(
                        targetSymLinkDestination,
                        operation.getTargetLocalItem());
                } else {
                    if (targetSymLinkDestinationUnmapped != null) {
                        targetSymLinkDestinationUnmapped.set(true);
                    }

                    log.info(
                        MessageFormat.format(
                            "Symbolic link at {0} would point to unmapped item, not creating link", //$NON-NLS-1$
                            operation.getTargetLocalItem()));
                }

                // Delete the temp file because the contents are unused for
                // symlinks
                TempStorageService.getInstance().deleteItem(tempFile);

                log.trace(MessageFormat.format("link at {0} created", operation.getTargetLocalItem())); //$NON-NLS-1$
            } else {
                // Rename the temp file to the real file name.
                log.trace(MessageFormat.format(
                    "renaming temp file {0} to target {1}", //$NON-NLS-1$
                    tempFile,
                    operation.getTargetLocalItem()));

                TempStorageService.getInstance().renameItem(tempFile, targetFile);
            }
        } finally {
            // Ensure we don't leave the one passed in to us
            if (tempFile.exists()) {
                TempStorageService.getInstance().deleteItem(tempFile);
            }
        }
    }

    /**
     * Analog of VS's AsyncGetFileState.Completed method.
     *
     * This is the completion routine for async get operations. It sends version
     * updates to the server and applies file attributes to the target local
     * item.
     *
     * @param targetSymLink
     *        pass <code>true</code> if the target item is supposed to be a
     *        symbolic link, <code>false</code> if it's a normal file
     * @param targetSymLinkDestinationUnmapped
     *        pass <code>true</code> if the target item was not created because
     *        it was supposed to be a symbolic link but the destination was
     *        unmapped; pass <code>false</code> for a normal file item or a
     *        successful symbolic link
     * @throws VersionControlException
     *         if the operation could not be completed
     */
    private void completeGetOperation(final boolean targetSymLink, final boolean targetSymLinkDestinationUnmapped) {
        Check.notNullOrEmpty(operation.getTargetLocalItem(), "operation.getTargetLocalItem()"); //$NON-NLS-1$

        log.trace(MessageFormat.format(
            "completing get operation for {0} ({1})", //$NON-NLS-1$
            operation.getTargetLocalItem(),
            operation.getTargetServerItem()));

        /*
         * We need to delete the existingLocalItem if it is different. Get the
         * lock on the action object as the main loop may null out the localItem
         * if the delete is to be skipped.
         */
        synchronized (operation) {
            /*
             * Don't change attributes of symbolic links.
             */
            if (!targetSymLink) {
                // Handle last write time and the +R bit for items which don't
                // have the Edit bit set.
                if (operation.getEffectiveChangeType().contains(ChangeType.EDIT) == false || operation.isUndo()) {
                    final File targetLocalFile = new File(operation.getTargetLocalItem());
                    final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(targetLocalFile);

                    if (0 != operation.getVersionServer()
                        && !DotNETDate.MIN_CALENDAR.equals(operation.getVersionServerDate())
                        && asyncOp.getWorkspace().getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
                        // Strip the +R bit before calling SetLastWriteTime, if
                        // it's set.
                        if (attrs.isReadOnly()) {
                            attrs.setReadOnly(false);
                            FileSystemUtils.getInstance().setAttributes(targetLocalFile, attrs);
                        }

                        // Set the last modified time of the file.
                        targetLocalFile.setLastModified(operation.getVersionServerDate().getTimeInMillis());
                    }

                    // In a server workspace, make sure that the item has the +R
                    // bit.
                    if (WorkspaceLocation.SERVER == asyncOp.getWorkspace().getLocation() && !attrs.isReadOnly()) {
                        attrs.setReadOnly(true);
                        FileSystemUtils.getInstance().setAttributes(targetLocalFile, attrs);
                    }
                }
            }

            /*
             * Don't send the version update if the sym link destination was
             * unmapped.
             */
            if (!targetSymLinkDestinationUnmapped) {
                // If a baseline wasn't created along with this download, then
                // it's because there was a pending edit reported in the
                // ChangeType of this GetOperation. That implies that the
                // content placed on disk isn't the committed content. The
                // HashValue member of the GetOperation refers to the content
                // which was referred to by the download URL -- so we can't
                // trust it by default.

                byte[] hashValue = null;
                long committedLength = -1;

                if (null != operation.getBaselineFileGUID()) {
                    // OK, a baseline was generated with this download. So we'll
                    // go ahead and provide the HashValue from the getop and the
                    // committed length from disk to UpdateLocalVersion.

                    Check.isTrue(
                        operation.getEffectiveChangeType().contains(ChangeType.EDIT) == false,
                        "operation.getEffectiveChangeType().contains(ChangeType.EDIT) == false"); //$NON-NLS-1$

                    hashValue = operation.getHashValue();
                    committedLength = new File(operation.getTargetLocalItem()).length();
                }

                String pendingChangeTargetServerItem = operation.getTargetServerItem();

                // If there's no pending change on the item, or if the item is a
                // pending add, then we don't want to use QueryPendingChanges as
                // a data source for missing fields.
                if (operation.getChangeType().equals(ChangeType.NONE)
                    || operation.getChangeType().contains(ChangeType.ADD)) {
                    pendingChangeTargetServerItem = null;
                }

                final ClientLocalVersionUpdate update = new ClientLocalVersionUpdate(
                    operation.getSourceServerItem(),
                    operation.getItemID(),
                    operation.getTargetLocalItem(),
                    operation.getVersionServer(),
                    operation.getVersionServerDate(),
                    operation.getEncoding(),
                    hashValue,
                    committedLength,
                    operation.getBaselineFileGUID(),
                    pendingChangeTargetServerItem,
                    operation.getPropertyValues());

                asyncOp.queueLocalVersionUpdate(update);
            }

            if (existingLocalAttrs.exists()
                && operation.getCurrentLocalItem() != null
                && !LocalPath.equals(operation.getCurrentLocalItem(), operation.getTargetLocalItem())) {
                Check.isTrue(operation.getItemType() != ItemType.FOLDER, "Should not try to delete a folder here: " //$NON-NLS-1$
                    + operation);

                log.trace(MessageFormat.format("deleting source {0}", operation.getCurrentLocalItem())); //$NON-NLS-1$

                getEngine.deleteSource(operation, existingLocalAttrs);
            }

            /*
             * Apply any of the extended file attributes that were stored in a
             * .tpattributes file.
             */
            getEngine.applyFileAttributesAfterGet(asyncOp, operation);

            // Tell the main get loop that we have downloaded this item so that
            // it doesn't send the delete ULV if it decides to clear the
            // action's local item.
            operation.setDownloadCompleted(true);
        }
    }
}
