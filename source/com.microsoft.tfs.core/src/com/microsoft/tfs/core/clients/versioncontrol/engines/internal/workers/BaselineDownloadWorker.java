// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.AsyncOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.BaselineFileDownloadOutput;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * A worker that can be run in its own thread to download files to the baseline
 * directory, but cannot update working folder files (see
 * {@link GetDownloadWorker}).
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
 * When a non-fatal error is encountered, the event is fired through the
 * connection's {@link VersionControlEventEngine} and the thread completes with
 * a NORMAL final state. Only a fatal error causes an ERROR final state.
 *
 * @threadsafety thread-safe
 */
public class BaselineDownloadWorker extends AbstractDownloadWorker {
    private static final Log log = LogFactory.getLog(BaselineDownloadWorker.class);

    private final AsyncOperation asyncOp;
    private final String downloadURL;
    private final BaselineFolderCollection baselineFolders;
    private final byte[] baselineFileGUID;

    /**
     * Create a worker that can download baseline file data. Use the
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
     * @param asyncOp
     *        the {@link AsyncOperation} to store fatal exceptions in (must not
     *        be <code>null</code>)
     * @param downloadURL
     *        the download URL (must not be <code>null</code>)
     * @param baselineFolders
     *        the {@link BaselineFolderCollection} for the workspace (must not
     *        be <code>null</code>)
     * @param baselineFileGUID
     *        the GUID that identifies this file (must not be <code>null</code>)
     */
    public BaselineDownloadWorker(
        final EventSource eventSource,
        final TaskMonitor cancelMonitor,
        final VersionControlClient client,
        final AsyncOperation asyncOp,
        final String downloadURL,
        final BaselineFolderCollection baselineFolders,
        final byte[] baselineFileGUID) {
        super(eventSource, cancelMonitor, client);

        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$
        Check.notNull(downloadURL, "downloadURL"); //$NON-NLS-1$
        Check.notNull(baselineFolders, "baselineFolders"); //$NON-NLS-1$
        Check.notNull(baselineFileGUID, "baselineFileGUID"); //$NON-NLS-1$

        this.asyncOp = asyncOp;
        this.downloadURL = downloadURL;
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

            downloadBaselineFile();
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
             * An actual error happened. We have to communicate this problem to
             * the thread submitting tasks so it can take the correct action
             * (shut down other workers).
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
     * Download the baseline file the {@link #baselineFileGUID} field specifies.
     *
     * @throws {@link
     *         CanceledException} if the {@link TaskMonitor} was canceled during
     *         execution. This method tries its best to delete partial or
     *         temporary files before throwing this exception.
     */
    protected void downloadBaselineFile() throws CanceledException {
        int readLockToken = BaselineFolderCollection.UNINITIALIZED_READ_LOCK_TOKEN;
        File baselineTempFile = null;
        BaselineFileDownloadOutput baselineOutput = null;

        try {
            // Lock the baseline folder collection from change until this
            // file download is finished.
            readLockToken = baselineFolders.lockForRead();

            // Gets the name, but doesn't create anything on disk
            final String baselineFileNoSuffix =
                baselineFolders.getNewBaselineLocation(baselineFileGUID, null, readLockToken);

            // Never gunzip this one
            baselineOutput = new BaselineFileDownloadOutput(new File(baselineFileNoSuffix), false);

            getClient().downloadFileToStreams(new DownloadSpec(downloadURL), new DownloadOutput[] {
                baselineOutput
            }, getEventSource(), getCancelMonitor());

            /*
             * Pull the baseline's actual file from the output. We only know
             * this after a download because the file extension depends on the
             * MIME type the server returned.
             */
            // If BaselineFolderCollection gave us a temp file, make sure it
            // gets cleaned up in this method.
            if (baselineOutput.isTempFileCreatedInsteadOfBaseline()) {
                baselineTempFile = baselineOutput.getOutputStreamFile();
            } else {
                // Real baseline file; no temp cleanup
                baselineTempFile = null;
            }

            closeDownloadOutputStreamSafely(baselineOutput);
        } catch (final CanceledException e) {
            // The finally block closes streams, deletes temps
            throw e;
        } catch (final VersionControlException e) {
            // The finally block closes streams, deletes temps
            final String message =
                MessageFormat.format(
                    Messages.getString("BaselineDownloadWorker.ErrorDownloadingBaselineFileFormat"), //$NON-NLS-1$
                    baselineFileGUID,
                    e.getLocalizedMessage());

            log.warn(message, e);
            throw new VersionControlException(message, e);
        } finally {
            if (readLockToken != BaselineFolderCollection.UNINITIALIZED_READ_LOCK_TOKEN) {
                baselineFolders.unlockForRead(readLockToken);
            }

            closeDownloadOutputStreamSafely(baselineOutput);

            if (baselineTempFile != null) {
                baselineTempFile.delete();
            }
        }
    }
}
