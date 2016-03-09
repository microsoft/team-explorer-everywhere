// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal;

import java.net.URI;

import com.microsoft.tfs.core.util.ServerURIComparator;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

/**
 * These objects test for equality with
 * {@link ServerURIComparator#compare(URI, URI)}, which is case-insensitive and
 * normalizes some {@link URI} parts. Be aware of this behavior when putting
 * these objects in collections.
 *
 * @threadsafety thread-safe
 */
public class InternalServerInfo {
    private URI uri;
    private final GUID guid;

    public InternalServerInfo(final URI uri, final GUID guid) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$
        Check.notNull(guid, "guid"); //$NON-NLS-1$

        this.uri = uri;
        this.guid = guid;
    }

    public synchronized URI getURI() {
        return uri;
    }

    public synchronized void setURI(final URI value) {
        Check.notNull(value, "value"); //$NON-NLS-1$
        uri = value;
    }

    public GUID getServerGUID() {
        return guid;
    }

    @Override
    public synchronized boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof InternalServerInfo == false) {
            return false;
        }

        // URI comparison is normalized for trailing slashes, case-insensitive

        return ServerURIComparator.INSTANCE.compare(uri, ((InternalServerInfo) obj).uri) == 0
            && guid.equals(((InternalServerInfo) obj).guid);
    }

    @Override
    public synchronized int hashCode() {
        int result = 17;

        // URI hash code computed on normalized for trailing slashes,
        // case-insensitive string

        result = result * 37
            + LocaleInvariantStringHelpers.caseInsensitiveHashCode(ServerURIUtils.normalizeURI(uri).toString());
        result = result * 37 + guid.hashCode();

        return result;
    }
}
