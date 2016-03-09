// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitemconfiguration;

import ms.tfs.workitemtracking.configurationsettingsservice._03._ConfigurationSettingsServiceSoap;

/**
 * Accesses the Team Foundation Server work item configurations settings web
 * service. Currently only able to query limits on work item sizes.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class WorkItemConfigurationSettingsClient {
    /**
     * Even if the web service returns a larger maximum size, this value is
     * returned to callers of {@link #getMaxAttachmentSize()} and
     * {@link #updateMaxAttachmentSize()}.
     *
     * I'm not exactly sure why this limit is required. Maybe 2GB limit in
     * 32-bit file I/O?
     */
    private static final long BUILTIN_MAX_SIZE = 2000000000;

    private final _ConfigurationSettingsServiceSoap webService;

    private long cachedMaxAttachmentSize;
    private boolean maxAttachementSizeCached = false;

    public WorkItemConfigurationSettingsClient(final _ConfigurationSettingsServiceSoap webService) {
        this.webService = webService;
    }

    /**
     * Gets the cached maximum work item attachment size. To force an update of
     * the cached size, call {@link #updateMaxAttachmentSize()} and use the
     * value it returns (or subsequently call this method).
     *
     * @return the maximum work item attachment size (possibly cached from a
     *         previous call to this method or
     *         {@link #updateMaxAttachmentSize()})
     */
    public long getMaxAttachmentSize() {
        synchronized (this) {
            if (!maxAttachementSizeCached) {
                updateMaxAttachmentSize();
            }
            return cachedMaxAttachmentSize;
        }
    }

    /**
     * Updates the maximum work item attachment size from the web service, and
     * returns the new value.
     *
     * @return the maximum size of a work item attachment
     */
    public long updateMaxAttachmentSize() {
        long size = webService.getMaxAttachmentSize();
        if (size > BUILTIN_MAX_SIZE) {
            size = BUILTIN_MAX_SIZE;
        }

        synchronized (this) {
            cachedMaxAttachmentSize = size;
            maxAttachementSizeCached = true;
            return cachedMaxAttachmentSize;
        }
    }
}
