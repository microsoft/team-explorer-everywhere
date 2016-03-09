// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.BaselineUpdaterAsyncOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineRequest;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * A worker that can be run in its own thread to update local workspace baseline
 * files from working folder files. Since it does not download items from the
 * server, it is not an {@link AbstractDownloadWorker}.
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
 * No cancelation via {@link TaskMonitor} is currently implemented.
 * <p>
 * Some baseline file update problems can be ignored (gzip compression errors)
 * and the uncompressed file used instead. Only fatal errors causes an ERROR
 * final state.
 *
 * @threadsafety thread-safe
 */
public class BaselineUpdaterWorker implements Worker {
    private static final Log log = LogFactory.getLog(BaselineUpdaterWorker.class);

    private final TaskMonitor monitor;
    private final BaselineRequest request;
    private final BaselineUpdaterAsyncOperation state;

    /**
     * Create a worker that can download and complete a get operation. Use the
     * {@link #call()} method to do the work.
     *
     * @param monitor
     *        the {@link TaskMonitor} to use to report progress (must not be
     *        <code>null</code>)
     * @param baselineRequest
     *        the update to process (must not be <code>null</code>)
     * @param state
     *        where cancellation and errors are reported when they happen (must
     *        not be <code>null</code>)
     */
    public BaselineUpdaterWorker(
        final TaskMonitor monitor,
        final BaselineRequest baselineRequest,
        final BaselineUpdaterAsyncOperation state) {
        Check.notNull(monitor, "monitor"); //$NON-NLS-1$
        Check.notNull(baselineRequest, "baselineRequest"); //$NON-NLS-1$
        Check.notNull(state, "state"); //$NON-NLS-1$

        this.monitor = monitor;
        this.request = baselineRequest;
        this.state = state;
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
            boolean completedSuccessfully = false;
            final int token = state.getBaselineFolderCollection().lockForRead();

            try {
                // Present the read lock token that we already hold to prevent
                // it from attempting to acquire another read lock for the call
                // to GetNewBaselineLocation.
                String baselineFilePath = state.getBaselineFolderCollection().getNewBaselineLocation(
                    request.getBaselineFileGUID(),
                    request.getBaselinePartitionLocalItem(),
                    token);

                // Get the uncompressed file size.
                final File fi = new File(baselineFilePath);
                final long uncompressedFileSize = fi.length();

                // Set status message for the monitor.
                monitor.setCurrentWorkDescription(
                    MessageFormat.format(
                        Messages.getString("BaselineUpdaterWorker.UpdatingBaselineFormat"), //$NON-NLS-1$
                        baselineFilePath));

                // Add the .gz extension to this baseline.
                baselineFilePath = baselineFilePath + BaselineFolder.getGzipExtension();

                if (uncompressedFileSize < Worker.MAX_GZIP_INPUT_SIZE) {
                    final byte[] buffer = new byte[4096];
                    byte[] hashValue = null;

                    MessageDigest md5Digest = null;
                    if (request.getHashValue() != null) {
                        md5Digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
                    }

                    final GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(baselineFilePath));
                    final String sourceLocalItem = request.getSourceLocalItem();
                    final FileSystemUtils util = FileSystemUtils.getInstance();
                    final FileSystemAttributes attrs = util.getAttributes(sourceLocalItem);

                    InputStream inputStream;
                    if (attrs.isSymbolicLink()) {
                        final String linkTarget = util.getSymbolicLink(sourceLocalItem);
                        inputStream = new ByteArrayInputStream(linkTarget.getBytes("UTF-8")); //$NON-NLS-1$
                    } else {
                        inputStream = new FileInputStream(request.getSourceLocalItem());
                    }

                    try {
                        int bytesRead;

                        while (true) {
                            bytesRead = inputStream.read(buffer, 0, buffer.length);

                            if (bytesRead <= 0) {
                                break;
                            }

                            if (null != md5Digest) {
                                md5Digest.update(buffer, 0, bytesRead);
                            }

                            outputStream.write(buffer, 0, bytesRead);
                        }

                        if (null != md5Digest) {
                            hashValue = md5Digest.digest();
                        }
                    } finally {
                        if (outputStream != null) {
                            IOUtils.closeSafely(outputStream);
                        }
                        if (inputStream != null) {
                            IOUtils.closeSafely(inputStream);
                        }
                    }

                    if (null != hashValue
                        && 16 == hashValue.length
                        && null != request.getHashValue()
                        && 16 == request.getHashValue().length
                        && !Arrays.equals(request.getHashValue(), hashValue)) {
                        // The hash value didn't match the provided hash value.
                        // Delete the baseline from disk.
                        FileHelpers.deleteFileWithoutException(baselineFilePath);
                    } else {
                        completedSuccessfully = true;
                    }
                } else {
                    // TODO: We didn't attempt to gzip.
                }
            } catch (final Exception ex) {
                log.trace("BaselineUpdater", ex); //$NON-NLS-1$
            } finally {
                state.getBaselineFolderCollection().unlockForRead(token);

                if (!completedSuccessfully) {
                    state.addFailedRequest(request);
                }
            }
        } catch (final Throwable t) {
            /*
             * An actual error happened. We have to communicate this problem to
             * the thread submitting tasks so it can take the correct action
             * (shut down other workers).
             */
            state.setFatalError(t);
            return new WorkerStatus(this, FinalState.ERROR);
        }

        return new WorkerStatus(this, FinalState.NORMAL);
    }
}
