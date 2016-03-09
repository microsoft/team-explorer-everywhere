// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.buildmanager;

import java.util.EventListener;

public interface BuildManagerListener extends EventListener {
    public void onBuildPropertyChanged(BuildPropertyChangedEvent event);

    public void onBuildDetailsChanged(BuildManagerEvent event);

    public void onBuildQueued(BuildManagerEvent event);

    public void onBuildStopped(BuildManagerEvent event);

    public void onBuildPostponedOrResumed(BuildManagerEvent event);

    public void onBuildPrioritiesChanged(BuildManagerEvent event);

    public void onBuildDeleted(final BuildManagerEvent event);

    public void onBuildsDeleted(final BuildManagerEvent event);
}
