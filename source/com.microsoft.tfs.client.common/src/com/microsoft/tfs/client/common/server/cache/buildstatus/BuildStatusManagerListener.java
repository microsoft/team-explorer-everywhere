// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server.cache.buildstatus;

import java.util.EventListener;

import com.microsoft.tfs.core.clients.build.IQueuedBuild;

/**
 * Listens for events from the {@link BuildStatusManager}.
 */
public interface BuildStatusManagerListener extends EventListener {
    /**
     * Called before a batch of build added / removed / status changed events
     * are to be fired. Listeners will be guaranteed to see an
     * {@link #onUpdateFinished()} after this call.
     */
    public void onUpdateStarted();

    /**
     * Called after a batch of build added / removed / status changed events
     * have been fired. Listeners are guaranteed to have seen an
     * {@link #onUpdateStarted()} before this call.
     */
    public void onUpdateFinished();

    /**
     * Called when a new build is being watched. This could be due to a gated
     * checkin build being started or a user explicitly "pinning" a build to be
     * watched. This event could be fired due to actions in another client (eg,
     * user checking in to a gated checkin via the CLC.)
     *
     * @param watchedBuild
     *        The build being watched
     */
    public void onWatchedBuildAdded(IQueuedBuild watchedBuild);

    /**
     * Called when a build is no longer being watched. This could be due to a
     * user reconciling a gated checkin or a user explicitly "unpinning" a build
     * from being watched. This event could be fired due to actions in another
     * client (eg, user checking in to a gated checkin via the CLC.)
     *
     * @param watchedBuild
     *        The build no longer being watched
     */
    public void onWatchedBuildRemoved(IQueuedBuild watchedBuild);

    /**
     * Called when a watched build's status has changed.
     *
     * @param queuedBuild
     *        The newly updated IQueuedBuild
     */
    public void onBuildStatusChanged(IQueuedBuild queuedBuild);
}
