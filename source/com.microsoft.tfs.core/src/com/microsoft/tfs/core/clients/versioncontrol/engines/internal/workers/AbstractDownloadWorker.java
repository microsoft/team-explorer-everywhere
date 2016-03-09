// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * Abstract class that contains basic download functionality for downloading to
 * stream and to file.
 */
abstract class AbstractDownloadWorker implements Worker {
    private final static Log log = LogFactory.getLog(AbstractDownloadWorker.class);

    /**
     * Describes the event origination point so the consumer of events fired by
     * this class (possibly in new threads) can tie those events to work it
     * initiated.
     */
    private final EventSource eventSource;

    /**
     * This is the object we use to detect user cancelation from the UI or other
     * layer. We must poll on this object for its state; it can't interrupt
     * worker threads.
     */
    private final TaskMonitor cancelMonitor;

    private final VersionControlClient client;

    public AbstractDownloadWorker(
        final EventSource eventSource,
        final TaskMonitor cancelMonitor,
        final VersionControlClient client) {
        this.eventSource = eventSource;
        this.cancelMonitor = cancelMonitor;
        this.client = client;
    }

    protected EventSource getEventSource() {
        return eventSource;
    }

    protected TaskMonitor getCancelMonitor() {
        return cancelMonitor;
    }

    protected VersionControlClient getClient() {
        return client;
    }

    protected void closeDownloadOutputStreamsSafely(final Iterable<DownloadOutput> outputs) {
        for (final DownloadOutput output : outputs) {
            closeDownloadOutputStreamSafely(output);
        }
    }

    protected void closeDownloadOutputStreamSafely(final DownloadOutput output) {
        if (output != null) {
            try {
                output.closeOutputStream();
            } catch (final IOException e) {
                log.warn("Error closing output stream " + output, e); //$NON-NLS-1$
            }
        }
    }
}
