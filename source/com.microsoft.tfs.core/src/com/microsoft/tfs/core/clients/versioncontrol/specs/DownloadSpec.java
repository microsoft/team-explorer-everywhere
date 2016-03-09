// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.util.Check;

/**
 * Specifies a resource that can be downloaded via {@link GetEngine}.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class DownloadSpec {
    private final String downloadURL;

    public DownloadSpec(final String downloadURL) {
        Check.notNullOrEmpty(downloadURL, "downloadURL"); //$NON-NLS-1$

        this.downloadURL = downloadURL;
    }

    /**
     * Returns the HTTP query string for the Item.aspx page on a TFS appropriate
     * for this item.
     *
     * @return the query arguments part of the HTTP request string that would
     *         retrieve the item described by this object.
     */
    public String getQueryString() {
        return downloadURL;
    }
}
