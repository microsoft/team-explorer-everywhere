// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

import java.net.URI;

import com.microsoft.tfs.core.util.ServerURIComparator;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

public abstract class ServerListEntry {
    private final String name;
    private final ServerListEntryType type;
    private final URI uri;

    public ServerListEntry(final String name, final ServerListEntryType type, final URI uri) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        this.name = name;
        this.type = type;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public ServerListEntryType getType() {
        return type;
    }

    public URI getURI() {
        return uri;
    }

    public int compareTo(final ServerListEntry other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        int result = name.compareTo(other.name);

        if (result != 0) {
            return result;
        }

        result = ServerURIComparator.INSTANCE.compare(uri, other.uri);

        return result;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + ((name == null) ? 0 : LocaleInvariantStringHelpers.caseInsensitiveHashCode(name));
        result = result * 37 + ((type == null) ? 0 : type.getValue());

        result = result * 37
            + ((uri == null) ? 0
                : LocaleInvariantStringHelpers.caseInsensitiveHashCode(ServerURIUtils.normalizeURI(uri).toString()));

        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof ServerListEntry)) {
            return false;
        }

        if (o == this) {
            return true;
        }

        final ServerListEntry other = (ServerListEntry) o;

        if (name != null && other.name != null && name.equalsIgnoreCase(other.name)) {
            return false;
        } else if (name != null ^ other.name != null) {
            return false;
        }

        if (type != null && other.type != null && !type.equals(other.type)) {
            return false;
        } else if (type != null ^ other.type != null) {
            return false;
        }

        if (uri != null && other.uri != null && ServerURIComparator.INSTANCE.compare(uri, other.uri) != 0) {
            return false;
        } else if (uri != null ^ other.uri != null) {
            return false;
        }

        return true;
    }
}
