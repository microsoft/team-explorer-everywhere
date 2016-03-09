// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.util.Check;

/**
 * Abstract base class for {@link DownloadOutput} that handles autoGunzip and
 * content type.
 *
 * @threadsafety thread-safe
 */
public abstract class BaseDownloadOutput implements DownloadOutput {
    private final boolean autoGunzip;
    private String actualContentType;

    public BaseDownloadOutput(final boolean autoGunzip) {
        this.autoGunzip = autoGunzip;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isAutoGunzip() {
        return autoGunzip;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setActualContentType(final String type) {
        Check.notNull(type, "type"); //$NON-NLS-1$
        actualContentType = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getActualContentType() {
        return actualContentType;
    }
}