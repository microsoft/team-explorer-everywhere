// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.buildmanager;

import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class BuildManager {
    private final SingleListenerFacade listeners = new SingleListenerFacade(BuildManagerListener.class);

    public final void addBuildManagerListener(final BuildManagerListener listener) {
        listeners.addListener(listener);
    }

    public final void removeBuildManagerListener(final BuildManagerListener listener) {
        listeners.removeListener(listener);
    }

    public final void fireBuildPropertyChangedEvent(
        final Object source,
        final String buildUri,
        final String buildProperty) {
        final BuildPropertyChangedEvent event = new BuildPropertyChangedEvent(source, buildUri, buildProperty);
        ((BuildManagerListener) listeners.getListener()).onBuildPropertyChanged(event);
    }

    public final void fireBuildDetailsChangedEvent(final Object source, final IBuildDetail[] buildDetails) {
        final BuildManagerEvent event = new BuildManagerEvent(source, buildDetails);
        ((BuildManagerListener) listeners.getListener()).onBuildDetailsChanged(event);
    }

    public final void fireBuildQueuedEvent(final Object source, final IQueuedBuild queuedBuild) {
        final BuildManagerEvent event = new BuildManagerEvent(source, queuedBuild);
        ((BuildManagerListener) listeners.getListener()).onBuildQueued(event);
    }

    public final void fireBuildStoppedEvent(final Object source, final String buildUri) {
        final BuildManagerEvent event = new BuildManagerEvent(source, buildUri);
        ((BuildManagerListener) listeners.getListener()).onBuildStopped(event);
    }

    public final void fireBuildPostponedOrResumedEvent(final Object source, final String buildUri) {
        final BuildManagerEvent event = new BuildManagerEvent(source, buildUri);
        ((BuildManagerListener) listeners.getListener()).onBuildPostponedOrResumed(event);
    }

    public final void fireBuildPrioritiesChangedEvent(final Object source, final IQueuedBuild[] affectedBuilds) {
        final BuildManagerEvent event = new BuildManagerEvent(source, affectedBuilds);
        ((BuildManagerListener) listeners.getListener()).onBuildPrioritiesChanged(event);
    }

    public final void fireBuildDeletedEvent(final Object source, final String buildUri) {
        final BuildManagerEvent event = new BuildManagerEvent(source, buildUri);
        ((BuildManagerListener) listeners.getListener()).onBuildDeleted(event);
    }

    public final void fireBuildsDeletedEvent(final Object source, final IBuildDetail[] deletedBuilds) {
        final BuildManagerEvent event = new BuildManagerEvent(source, deletedBuilds);
        ((BuildManagerListener) listeners.getListener()).onBuildsDeleted(event);
    }
}
