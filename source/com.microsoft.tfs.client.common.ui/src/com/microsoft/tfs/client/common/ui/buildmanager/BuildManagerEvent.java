// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.buildmanager;

import java.util.EventObject;

import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;

public class BuildManagerEvent extends EventObject {
    private static final long serialVersionUID = -8323089808870263557L;

    private final IBuildDetail[] buildDetails;
    private final IQueuedBuild[] queuedBuilds;

    public BuildManagerEvent(final Object source) {
        super(source);

        this.buildDetails = null;
        this.queuedBuilds = null;
    }

    public BuildManagerEvent(final Object source, final String buildUri) {
        super(source);

        this.buildDetails = null;
        this.queuedBuilds = null;
    }

    public BuildManagerEvent(final Object source, final IBuildDetail[] buildDetails) {
        super(source);

        this.buildDetails = buildDetails;
        this.queuedBuilds = null;
    }

    public BuildManagerEvent(final Object source, final IQueuedBuild[] queuedBuilds) {
        super(source);

        this.buildDetails = null;
        this.queuedBuilds = queuedBuilds;
    }

    public BuildManagerEvent(final Object source, final IQueuedBuild queuedBuild) {
        super(source);

        this.buildDetails = null;
        this.queuedBuilds = new IQueuedBuild[] {
            queuedBuild
        };
    }

    public IBuildDetail[] getBuildDetails() {
        return buildDetails;
    }

    public IQueuedBuild[] getQueuedBuilds() {
        return queuedBuilds;
    }
}
