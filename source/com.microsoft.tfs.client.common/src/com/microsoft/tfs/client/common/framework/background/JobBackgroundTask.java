// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.background;

import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.util.Check;

/**
 * An {@link IBackgroundTask} that is backed by an Eclipse {@link Job}.
 */
public class JobBackgroundTask implements IBackgroundTask {
    private final Job job;

    public JobBackgroundTask(final Job job) {
        Check.notNull(job, "job"); //$NON-NLS-1$

        this.job = job;
    }

    @Override
    public String getName() {
        return job.getName();
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    @Override
    public boolean cancel() {
        return job.cancel();
    }
}
