// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.background;

/**
 * An {@link IBackgroundTask} represents an abstract piece of work that is being
 * executed in the background. This is used for notification of components so
 * that they can display status and (optionally) provide cancellation.
 */
public interface IBackgroundTask {
    /**
     * Gets the description of this background task, in the user's locale.
     *
     * @return The description of this background task suitable for presentation
     *         to the user.
     */
    String getName();

    /**
     * Determines if this background task is cancellable using the
     * {@link #cancel()} method.
     *
     * @return <code>true</code> if this task can be cancelled,
     *         <code>false</code> otherwise.
     */
    boolean isCancellable();

    /**
     * Attempts to cancel the background task.
     *
     * @return <code>true</code> if the task was successfully cancelled,
     *         <code>false</code> otherwise.
     */
    boolean cancel();
}
