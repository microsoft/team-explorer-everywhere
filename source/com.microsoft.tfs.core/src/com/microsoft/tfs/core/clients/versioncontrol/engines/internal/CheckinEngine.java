// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.CheckinWorker;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService.ExecutionExceptionHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService.ResultProcessor;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeNames;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeValues;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesFile;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.StringPairFileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.internal.AppleSingleUtil;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.HashUtils;
import com.microsoft.tfs.util.NewlineUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * Uploads file contents for pending changes being checked in.
 *
 * @threadsafety thread-safe
 */
public class CheckinEngine {
    /**
     * We yield the workspace lock after uploading this many items (local
     * workspaces only).
     */
    private static final int UPLOAD_YIELD_COUNT = 8;

    private static final Log log = LogFactory.getLog(CheckinEngine.class);

    private final VersionControlClient client;
    private final Workspace workspace;

    /**
     * Create a {@link CheckinEngine} which uses the specified
     * {@link VersionControlClient} and {@link Workspace}.
     *
     * @param client
     *        the version control client to use (must not be <code>null</code>)
     * @param workspace
     *        the workspace to use (must not be <code>null</code>)
     */
    public CheckinEngine(final VersionControlClient client, final Workspace workspace) {
        super();

        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.client = client;
        this.workspace = workspace;
    }

    /**
     * Uploads changes to the server for a checkin or shelve operation.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param changes
     *        the changes to upload (must not be <code>null</code>)
     * @param forShelve
     *        if the upload is for a shelve, set to true so the correct event is
     *        raised, if for a normal checkin set to false.
     * @param saveUploadedContentAsBaselines
     *        if <code>true</code> the uploaded files are stored in the local
     *        workspace baseline folders, if <code>false</code> they are not.
     *        Pass <code>false</code> when calling for shelve (not check-in) and
     *        for check-in on server workspaces.
     * @throws CheckinException
     *         if conflicts caused the checkin to fail or if other errors
     *         occurred.
     * @throws CoreCancelException
     *         if the upload was cancelled by the user.
     */
    public void uploadChanges(
        final PendingChange[] changes,
        final boolean forShelve,
        final boolean saveUploadedContentAsBaselines) throws CheckinException, CoreCancelException {
        Check.notNull(changes, "changes"); //$NON-NLS-1$

        /*
         * If this is a local workspace, and we are checking in (not shelving),
         * then the caller will have set the flag saveUploadedContentAsBaselines
         * to true. In that case we need to take a workspace lock over the
         * entire upload process, since we will be putting baselines into the
         * baseline folders during the upload process.
         */
        final WorkspaceLock workspaceLock = saveUploadedContentAsBaselines ? this.workspace.lock() : null;
        try {
            final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();
            monitor.begin("", changes.length); //$NON-NLS-1$

            // We need to reconcile before uploading pending change content
            // because
            // the upload handler will reject uploads for items that don't have
            // pending change rows on the server.
            final AtomicBoolean pendingChangesUpdatedByServer = new AtomicBoolean();
            workspace.reconcile(false /* reconcileMissingLocalItems */, pendingChangesUpdatedByServer);

            // if the server updated pending changes behind our back, abort so
            // that
            // the user confirms that what they are checking in is correct.
            if (pendingChangesUpdatedByServer.get()) {
                throw new VersionControlException(Messages.getString("CheckinEngine.PendingChangesModified")); //$NON-NLS-1$
            }

            /*
             * Since we will dispatch some uploads to be completed in a
             * different thread (managed by an Executor), we have to keep track
             * of them here. The completion service wraps the bounded executor
             * shared by all GetEngines that use the same VersionControlClients.
             * The completion service only tracks the jobs we have submitted for
             * execution, so we can simply count the completed jobs to know when
             * they have all been processed.
             */
            final AccountingCompletionService<WorkerStatus> completions =
                new AccountingCompletionService<WorkerStatus>(client.getUploadDownloadWorkerExecutor());

            final AsyncCheckinOperation asyncOp = new AsyncCheckinOperation(workspaceLock);

            try {
                int uploadedCount = 0;

                for (final PendingChange change : changes) {
                    throwIfCanceled(monitor);

                    throwIfFatalError(asyncOp);

                    final ChangeType changeType = change.getChangeType();

                    final PendingChangeEvent event = new PendingChangeEvent(
                        EventSource.newFromHere(),
                        workspace,
                        change,
                        OperationStatus.GETTING,
                        ChangePendedFlags.UNKNOWN);

                    if (forShelve) {
                        client.getEventEngine().fireBeforeShelvePendingChange(event);
                    } else {
                        client.getEventEngine().fireBeforeCheckinPendingChange(event);
                    }

                    /*
                     * The edit flag will always be set for changes of type Add
                     * and Edit. During a merge, a delete flag and an edit flag
                     * may both exist on one item.
                     */

                    if (changeType.contains(ChangeType.EDIT)
                        && (changeType.contains(ChangeType.MERGE) && changeType.contains(ChangeType.DELETE)) == false) {
                        // Upload the file.
                        monitor.setCurrentWorkDescription(
                            MessageFormat.format(
                                Messages.getString("CheckinEngine.UploadingFormat"), //$NON-NLS-1$
                                change.getServerItem()));

                        /*
                         * In a strange circumstance in which a user pends a
                         * change on a file, changes his workspace so that the
                         * directory for that file points to somewhere else in
                         * the repository, then pends an add on the same file,
                         * the server loses the local item location of a pending
                         * change this can also happen if the user has made
                         * incorrect calls to update local version
                         */
                        if (change.getLocalItem() == null) {
                            throw new VersionControlException(
                                MessageFormat.format(
                                    Messages.getString("CheckinEngine.NoLocalFileForPendingChangeFormat"), //$NON-NLS-1$
                                    change.getServerItem()));
                        }

                        /*
                         * In a local workspace, uploading changes is a
                         * yieldable operation. Check every N pending changes
                         * for a waiter and yield.
                         */
                        if (null != asyncOp.getBaselineFolders() && 0 == (uploadedCount++ % UPLOAD_YIELD_COUNT)) {
                            asyncOp.getWorkspaceLock().yield();
                        }

                        uploadFile(change, completions, asyncOp);
                    } else if (changeType.contains(ChangeType.DELETE) == false
                        && changeType.contains(ChangeType.LOCK) == false
                        && changeType.contains(ChangeType.RENAME) == false
                        && changeType.contains(ChangeType.UNDELETE) == false
                        && changeType.contains(ChangeType.BRANCH) == false
                        && changeType.contains(ChangeType.ENCODING) == false
                        && changeType.contains(ChangeType.MERGE) == false) {
                    }

                    monitor.worked(1);
                }
            } finally {
                /*
                 * Wait for all the background threads to finish.
                 */
                waitForCompletions(completions);

                monitor.done();
            }

            throwIfCanceled(monitor);

            throwIfFatalError(asyncOp);
        } finally {
            if (workspaceLock != null) {
                workspaceLock.close();
            }
        }
    }

    private void throwIfCanceled(final TaskMonitor taskMonitor) throws CoreCancelException {
        if (taskMonitor.isCanceled()) {
            throw new CoreCancelException();
        }
    }

    private void throwIfFatalError(final AsyncCheckinOperation asyncOp) throws CheckinException {
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$

        final Throwable fatalError = asyncOp.getFatalError();
        if (fatalError != null) {
            throw new CheckinException(null, false, false, fatalError);
        }
    }

    /**
     * Waits for all the tasks that have been submitted to the given
     * {@link AccountingCompletionService} to finish. This method may be called
     * multiple times on a single completion service instance.
     *
     * @param completionService
     *        the {@link AccountingCompletionService} to wait on (must not be
     *        <code>null</code>)
     */
    private void waitForCompletions(final AccountingCompletionService<WorkerStatus> completionService) {
        Check.notNull(completionService, "completionService"); //$NON-NLS-1$

        completionService.waitForCompletions(new ResultProcessor<WorkerStatus>() {
            @Override
            public void processResult(final WorkerStatus result) {
                final WorkerStatus status = result;

                if (status.getFinalState() == FinalState.ERROR) {
                    log.debug("Checkin worker thread finished with EXCEPTION"); //$NON-NLS-1$
                } else if (status.getFinalState() == FinalState.CANCELED) {
                    log.debug("Checkin worker thread finished with CANCELED"); //$NON-NLS-1$
                }
            }
        }, new ExecutionExceptionHandler() {
            @Override
            public void handleException(final ExecutionException e) {
                log.warn("Checkin worker exception", e); //$NON-NLS-1$
            }
        });
    }

    /**
     * Uploads the file for the given change, unless the MD5 sum of the local
     * file matches the upload hash and we can skip the upload for this file
     * entirely.
     *
     * @param change
     *        the pending change whose file should be uploaded.
     * @param completionService
     *        where the uploads were submitted (must not be <code>null</code>)
     * @param state
     *        the state kept during checkin to note errors.
     * @throws CheckinException
     *         if the local file was missing.
     * @throws CoreCancelException
     *         if the upload was cancelled by the user.
     */
    private void uploadFile(
        PendingChange change,
        final CompletionService<WorkerStatus> completionService,
        final AsyncCheckinOperation state) throws CheckinException, CoreCancelException {
        Check.notNull(change, "change"); //$NON-NLS-1$

        /*
         * Callers should only use these methods for pending adds or edits, and
         * we always have a local item for these.
         */
        Check.notNull(change.getLocalItem(), "change.getLocalItem()"); //$NON-NLS-1$

        final String localItem = change.getLocalItem();
        final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(localItem);
        if (new File(change.getLocalItem()).exists() == false && !attrs.isSymbolicLink()) {
            throw new CheckinException(
                null,
                false,
                false,
                MessageFormat.format(
                    Messages.getString("CheckinEngine.LocalItemNoLongerExistsFormat"), //$NON-NLS-1$
                    change.getLocalItem()));
        }

        /*
         * Handle tpattributes: EOL and AppleSingle encoding. The change
         * variable is set to a cloned change so we can modify the local item
         * for the upload without affecting the caller's use of the original
         * change.
         */
        String filterTempFile = null;
        final GetEngine getEngine = new GetEngine(client);
        final FileAttributesCollection attributes = getEngine.getAttributesForFile(
            localItem,
            change.getServerItem(),
            (FileEncoding.BINARY.getCodePage() != change.getEncoding()));

        if (attributes != null) {
            /*
             * Convert end-of-line characters for files that have the extended
             * attribute set.
             */
            final StringPairFileAttribute eolAttribute =
                attributes.getStringPairFileAttribute(FileAttributeNames.SERVER_EOL);

            if (eolAttribute != null && eolAttribute.getValue() != null) {
                final String desiredNewlineSequence =
                    FileAttributeValues.getEndOfLineStringForAttributeValue(eolAttribute);

                if (desiredNewlineSequence == null) {
                    throw new CheckinException(
                        null,
                        false,
                        false,
                        MessageFormat.format(
                            Messages.getString("CheckinEngine.UnsupportedServerEOLStyleFormat"), //$NON-NLS-1$
                            eolAttribute.getValue(),
                            change.getLocalItem(),
                            FileAttributesFile.DEFAULT_FILENAME));
                } else if (desiredNewlineSequence.equals("")) //$NON-NLS-1$
                {
                    log.debug(MessageFormat.format("Not converting line endings in {0}", change.getLocalItem())); //$NON-NLS-1$
                } else {
                    log.debug(MessageFormat.format(
                        "Converting line endings for {0} to {1}", //$NON-NLS-1$
                        change.getLocalItem(),
                        eolAttribute.getValue()));

                    /*
                     * Create a temporary file for the conversion so we don't
                     * modify the working folder file.
                     */

                    try {
                        if (filterTempFile == null) {
                            filterTempFile = createTempFile(change);
                        }

                        Charset charset = CodePageMapping.getCharset(change.getEncoding(), false);

                        if (charset == null) {
                            charset = Charset.defaultCharset();
                        }

                        NewlineUtils.convertFile(new File(filterTempFile), charset, desiredNewlineSequence);

                        log.info(MessageFormat.format(
                            "Converted line endings in {0} to {1}", //$NON-NLS-1$
                            filterTempFile,
                            eolAttribute.getValue(),
                            charset.name()));
                    } catch (final UnsupportedEncodingException e) {
                        final String message = MessageFormat.format(
                            Messages.getString("CheckinEngine.CouldNotChangeEOLStyleUnknownJavaEncodingFormat"), //$NON-NLS-1$
                            change.getLocalItem(),
                            e.getLocalizedMessage());

                        log.error(message, e);
                        throw new CheckinException(null, false, false, message);
                    } catch (final IOException e) {
                        final String message = MessageFormat.format(
                            Messages.getString("CheckinEngine.CouldNotChangeEOLStyleIOExceptionFormat"), //$NON-NLS-1$
                            change.getLocalItem(),
                            e.getLocalizedMessage());

                        log.error(message, e);
                        throw new CheckinException(null, false, false, message);
                    }
                }
            }

            /*
             * Encode data fork and resource fork into an AppleSingle file if
             * requested. This should come last (as other filters, above, may
             * modify the data fork and should not modify the AppleSingle file.)
             */
            final StringPairFileAttribute transformAttribute =
                attributes.getStringPairFileAttribute(FileAttributeNames.TRANSFORM);

            if (transformAttribute != null && "apple".equals(transformAttribute.getValue())) //$NON-NLS-1$
            {
                if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
                    try {
                        if (filterTempFile == null) {
                            filterTempFile = createTempFile(change);
                        }

                        AppleSingleUtil.encodeFile(new File(filterTempFile), change.getLocalItem());
                    } catch (final IOException e) {
                        final String message = MessageFormat.format(
                            Messages.getString("CheckinEngine.CouldNotDecodeAppleSingleFileFormat"), //$NON-NLS-1$
                            change.getLocalItem(),
                            e.getLocalizedMessage());

                        log.error(message, e);
                        throw new CheckinException(null, false, false, message);
                    }
                } else {
                    log.warn(MessageFormat.format(
                        "Not preserving Apple metadata for {0} on platform {1}", //$NON-NLS-1$
                        change.getLocalItem(),
                        Platform.getCurrentPlatformString()));
                }
            }
        }

        if (attrs.isSymbolicLink()) {
            /*
             * for symlinks, create temporary file containing the symlink info;
             * upload the temporary file rather than the symlinks
             */
            try {
                final String link = FileSystemUtils.getInstance().getSymbolicLink(localItem);
                filterTempFile = createTempFileForSymbolicLink(localItem, link);
            } catch (final IOException e) {
                final String message =
                    MessageFormat.format(
                        Messages.getString("CheckinEngine.CouldNotCreateTempFileForSymlinkFormat"), //$NON-NLS-1$
                        localItem,
                        e.getLocalizedMessage());

                log.error(message, e);
                throw new CheckinException(null, false, false, message);
            }
        }

        /*
         * We may have done some filtering (EOL conversion or AppleSingle
         * encoding), update the change)
         */
        if (filterTempFile != null) {
            /**
             * Clone the pending change for the upload process, so we can change
             * the local item to the temp item and not affect the working folder
             * updates applied after the upload process finishes (which uses the
             * original change object.
             */
            change = new PendingChange(change);
            change.setLocalItem(filterTempFile);
        }

        // See if we can skip the upload for non-symbolic files.
        final byte[] localMD5Hash = computeMD5Hash(change.getLocalItem(), TaskMonitorService.getTaskMonitor());
        byte[] serverHash = change.getUploadContentHashValue();
        if (serverHash == null) {
            serverHash = change.getHashValue();
        }

        if (serverHash != null && serverHash.length > 0 && Arrays.equals(serverHash, localMD5Hash)) {
            log.trace(MessageFormat.format("skipped upload of {0} because hash codes match", change.getLocalItem())); //$NON-NLS-1$

            /*
             * We may have done some sort of upload filtering (EOL conversion or
             * AppleSingle encoding), clean up the file in this case.
             */
            if (filterTempFile != null) {
                TempStorageService.getInstance().cleanUpItem(new File(filterTempFile));
            }

            return;
        }

        /*
         * Let our thread pool execute this task. submit() will block if all the
         * workers are busy (because the completion service wraps a
         * BoundedExecutor), which is what we want. This keeps our upload
         * connections limited so we don't saturate the network with TCP/IP or
         * HTTP overhead or the TFS server with connections.
         *
         * We don't do the MD5 checkin in these threads because keeping that
         * serial is pretty efficient. Parallel MD5 checking may cause us to go
         * disk-bound when we have many small files spread all over the disk
         * (out of cache).
         */
        completionService.submit(
            new CheckinWorker(TaskMonitorService.getTaskMonitor(), client, workspace, change, localMD5Hash, state));
    }

    /**
     * Creates a temporary file for filters to work with before uploading. This
     * is called by EOL and AppleSingle filters, amongst others.
     *
     * @param change
     *        The PendingChange that is being filtered
     * @return A string representing the temp file we created
     * @throws IOException
     *         If the file could not be duplicated
     */
    private String createTempFile(final PendingChange change) throws IOException {
        final String tempFile = TempStorageService.getInstance().createTempFile().getAbsolutePath();
        log.trace(MessageFormat.format("Using temporary file {0} for EOL conversion output", tempFile)); //$NON-NLS-1$

        FileCopyHelper.copy(change.getLocalItem(), tempFile);
        log.trace(MessageFormat.format("Content copied from {0} to {1}", change.getLocalItem(), tempFile)); //$NON-NLS-1$

        return tempFile;
    }

    /**
     * Creates a temporary file for symbolic links to work with before
     * uploading.
     *
     * @param change
     *        The PendingChange that is being filtered
     * @return A string representing the temporary file we created
     * @throws IOException
     *         If the file could not be created
     */
    private String createTempFileForSymbolicLink(final String localItem, final String targetLink) throws IOException {
        final String tempFile = TempStorageService.getInstance().createTempFile().getAbsolutePath();
        log.trace(MessageFormat.format("Using temporary file {0} for symbolic link {1}", tempFile, localItem)); //$NON-NLS-1$

        final Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
        out.write(targetLink);
        out.close();

        log.trace(MessageFormat.format(
            "Symbolic link target {0} written to temporary file {1} as contents", //$NON-NLS-1$
            targetLink,
            tempFile));

        return tempFile;
    }

    /**
     * Computes the MD5 hash for the given file path.
     *
     * @param fileName
     *        the file path to compute the MD5 hash for (the file must exist)
     *        (must not be <code>null</code> or empty)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to test for cancelation (if
     *        <code>null</code> no cancelation is detected)
     * @return the computed MD5 hash for the file contents.
     * @throws CoreCancelException
     *         if the hash operation was canceled
     */
    public static byte[] computeMD5Hash(final String fileName, final TaskMonitor taskMonitor)
        throws CoreCancelException {
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$

        final FileSystemUtils util = FileSystemUtils.getInstance();
        final FileSystemAttributes attrs = util.getAttributes(fileName);
        if (attrs.isSymbolicLink()) {
            final String linkTarget = util.getSymbolicLink(fileName);
            return HashUtils.hashString(linkTarget, null, HashUtils.ALGORITHM_MD5);
        }

        final File file = new File(fileName);
        try {
            return HashUtils.hashFile(file, HashUtils.ALGORITHM_MD5, taskMonitor);
        } catch (final CanceledException e) {
            throw new CoreCancelException();
        } catch (final FileNotFoundException e) {
            throw new VersionControlException(e);
        } catch (final IOException e) {
            throw new VersionControlException(e);
        }
    }
}
