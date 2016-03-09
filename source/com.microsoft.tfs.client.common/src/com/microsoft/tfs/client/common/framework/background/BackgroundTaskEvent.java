// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.background;

import java.util.EventObject;

import com.microsoft.tfs.util.Check;

/**
 * A simple background task notification.
 *
 * @threadsafety thread-safe
 */
public class BackgroundTaskEvent extends EventObject {
    private static final long serialVersionUID = 6023284464568139187L;

    private final IBackgroundTask task;

    public BackgroundTaskEvent(final Object source, final IBackgroundTask task) {
        super(source);

        Check.notNull(task, "task"); //$NON-NLS-1$

        this.task = task;
    }

    public final IBackgroundTask getBackgroundTask() {
        return task;
    }
}
