// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.background;

import com.microsoft.tfs.util.Check;

/**
 * A non-cancellable implementation of {@link IBackgroundTask} that allows for
 * notification of ongoing background work only.
 *
 * @threadsafety unknown
 */
public final class BackgroundTask implements IBackgroundTask {
    private final String name;

    public BackgroundTask(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public boolean cancel() {
        return false;
    }
}
