// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.logging;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.FileAppender;

import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;
import com.microsoft.tfs.util.locking.AdvisoryFileLock;

/**
 * An extension of Log4J's normal FileAppender with the following properties: 1)
 * The log file is written to the Team Explorer settings directory, instead of
 * the process working directory as is the default with FileAppender. 2) Each
 * TEAppender will use a separate log file (old log files are pruned after a
 * certain threshold is met)
 *
 */
public class TEAppender extends FileAppender {
    private static final int CLEANUP_THRESHOLD = 5;

    @Override
    public void setFile(final String logType) {
        /*
         * Create the logs directory if it doesn't already exist
         */
        final VersionedVendorFilesystemPersistenceStore logStore = TELogUtils.getTeamExplorerLogsLocation();
        try {
            logStore.initialize();
        } catch (final IOException e) {
            /*
             * Would be nice to log this, but we don't have logs yet. :) Print
             * the error and continue on; logging may actually succeed.
             */
            e.printStackTrace();
        }

        /*
         * Prune old log files in the directory to keep the litter of the file
         * system down
         */
        cleanup(logType, logStore);

        /*
         * create the log file object
         */
        final File logFile = TELogUtils.createLogFileObject(logType, logStore.getStoreFile(), true);

        /*
         * call the super implementation in FileAppender with the file's full
         * path
         */
        super.setFile(logFile.getAbsolutePath());
    }

    private void cleanup(final String logType, final FilesystemPersistenceStore logStore) {
        /*
         * The basic algorithm here is to get an exclusive lock on the settings
         * location. If that exclusive lock can't be had, we return without
         * doing any cleanup (some other instance of the application is
         * currently performing cleanup on this directory).
         */

        AdvisoryFileLock lock = null;

        try {
            lock = logStore.getStoreLock(false);

            /*
             * A null lock means the lock was not immediately available.
             */
            if (lock == null) {
                return;
            }

            /*
             * Here's the call to actually perform the cleanup on the directory.
             * At this point we know we have the exclusive lock.
             */
            doCleanup(logType, logStore);
        } catch (final InterruptedException e) {
            /*
             * Shouldn't ever happen because we aren't blocking on getting the
             * lock.
             */
            return;
        } catch (final IOException e) {
            /*
             * Exception trying to get lock - return without doing cleanup
             */
            return;
        } finally {
            try {
                /*
                 * Always release the lock
                 */
                if (lock != null) {
                    lock.release();
                }
            } catch (final IOException e) {
            }
        }
    }

    private void doCleanup(final String logType, final FilesystemPersistenceStore logFileLocation) {
        final File[] logFiles = TELogUtils.getAllLogFilesForLogType(logType, logFileLocation.getStoreFile(), true);

        /*
         * If the number of files is not under the cleanup threshold, this
         * method has nothing to do
         */
        if (logFiles.length < CLEANUP_THRESHOLD) {
            return;
        }

        /*
         * Attempt to delete enough files to bring us below the cleanup
         * threshold. If the deletes don't succeed, that's fine, as another
         * instance may currently have the file open and locked.
         */
        final int numToDelete = logFiles.length - CLEANUP_THRESHOLD + 1;
        int numDeleted = 0;
        for (int i = 0; i < logFiles.length && numDeleted < numToDelete; i++) {
            if (logFiles[i].delete()) {
                ++numDeleted;
            }
        }
    }
}
