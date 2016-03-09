// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.Collection;
import java.util.Iterator;

import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateFinishedCallback;

public class DescriptionUpdateRunnable implements Runnable {
    private final DescriptionUpdateErrorCallback errorCallback;
    private final DescriptionUpdateFinishedCallback finishedCallback;
    private final Collection runnables;

    public DescriptionUpdateRunnable(
        final DescriptionUpdateErrorCallback errorCallback,
        final DescriptionUpdateFinishedCallback finishedCallback,
        final Collection runnables) {
        this.errorCallback = errorCallback;
        this.finishedCallback = finishedCallback;
        this.runnables = runnables;
    }

    @Override
    public void run() {
        for (final Iterator it = runnables.iterator(); it.hasNext();) {
            final Runnable runnable = (Runnable) it.next();
            try {
                runnable.run();
            } catch (final Throwable t) {
                if (errorCallback != null) {
                    errorCallback.onDescriptionUpdateError(t);
                }
            }
        }

        if (finishedCallback != null) {
            finishedCallback.onDescriptionUpdateFinished();
        }
    }
}
