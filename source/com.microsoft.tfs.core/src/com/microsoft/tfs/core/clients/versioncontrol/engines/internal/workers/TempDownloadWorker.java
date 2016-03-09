// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.io.File;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * An {@link Worker} that provides very simple downloading functionality (no
 * working folder baseline updates, no file attributes setting, etc.).
 * {@link TempDownloadWorker} just gets data from a download URL and writes it
 * to a file on disk. The caller is responsible for managing the resulting file.
 */
public class TempDownloadWorker extends AbstractDownloadWorker {
    private final String downloadURL;
    private final File localFile;
    private final ItemType itemType;

    public TempDownloadWorker(
        final EventSource eventSource,
        final TaskMonitor cancelMonitor,
        final VersionControlClient client,
        final String downloadURL,
        final File localFile,
        final ItemType itemType) {
        super(eventSource, cancelMonitor, client);

        Check.notNull(localFile, "localFile"); //$NON-NLS-1$
        Check.notNull(itemType, "itemType"); //$NON-NLS-1$

        this.downloadURL = downloadURL;
        this.localFile = localFile;
        this.itemType = itemType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkerStatus call() throws Exception {
        try {
            if (getCancelMonitor().isCanceled()) {
                return new WorkerStatus(this, FinalState.CANCELED);
            }

            if (itemType == ItemType.FOLDER) {
                localFile.mkdirs();
                return new WorkerStatus(this, FinalState.NORMAL);
            }

            final File parentDirectory = localFile.getParentFile();

            // Make sure the directory exists.
            if (parentDirectory.exists() == false) {
                parentDirectory.mkdirs();
            }

            try {
                getClient().downloadFile(new DownloadSpec(downloadURL), localFile, true);
            } catch (final CanceledException e) {
                // Doesn't hurt to try to delete again
                if (localFile.exists()) {
                    localFile.delete();
                }

                return new WorkerStatus(this, FinalState.CANCELED);
            }
        } catch (final Throwable t) {
            if (localFile.exists()) {
                localFile.delete();
            }

            /*
             * An actual error happened. We have to communicate this problem to
             * the thread submitting tasks so it can take the correct action
             * (shut down other workers).
             */
            return new WorkerStatus(this, FinalState.ERROR);
        }

        return new WorkerStatus(this, FinalState.NORMAL);
    }
}
