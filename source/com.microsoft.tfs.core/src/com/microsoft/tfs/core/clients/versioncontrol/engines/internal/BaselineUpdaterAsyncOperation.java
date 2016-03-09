// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService.ExecutionExceptionHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService.ResultProcessor;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineRequest;
import com.microsoft.tfs.util.Check;

/**
 * Contains state information used by the {@link BaselineUpdater} class to track
 * its progress and errors.
 *
 * @threadsafety thread-safe
 */
public final class BaselineUpdaterAsyncOperation extends AsyncOperation {
    private static final Log log = LogFactory.getLog(BaselineUpdaterAsyncOperation.class);

    private final List<BaselineRequest> failedRequests = new ArrayList<BaselineRequest>();
    private final BaselineFolderCollection baselineFolders;

    public BaselineUpdaterAsyncOperation(final BaselineFolderCollection baselineFolders) {
        super();

        this.baselineFolders = baselineFolders;
    }

    public BaselineFolderCollection getBaselineFolderCollection() {
        return baselineFolders;
    }

    public synchronized List<BaselineRequest> getFailedRequests() {
        return failedRequests;
    }

    public synchronized void addFailedRequest(final BaselineRequest baselineRequst) {
        failedRequests.add(baselineRequst);
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
    public static void waitForCompletions(final AccountingCompletionService<WorkerStatus> completionService) {
        Check.notNull(completionService, "completionService"); //$NON-NLS-1$

        completionService.waitForCompletions(new ResultProcessor<WorkerStatus>() {
            @Override
            public void processResult(final WorkerStatus result) {
                final WorkerStatus status = result;

                if (status.getFinalState() == FinalState.ERROR) {
                    log.debug("Baseline updater finished with ERROR"); //$NON-NLS-1$
                } else if (status.getFinalState() == FinalState.CANCELED) {
                    log.debug("Baseline updater thread finished with CANCELED"); //$NON-NLS-1$
                }
            }
        }, new ExecutionExceptionHandler() {
            @Override
            public void handleException(final ExecutionException e) {
                log.warn("Baseline updater exception", e); //$NON-NLS-1$
            }
        });
    }
}
