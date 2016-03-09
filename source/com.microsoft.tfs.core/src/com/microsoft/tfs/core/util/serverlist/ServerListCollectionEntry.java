// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

import java.net.URI;

/**
 *
 *
 * @threadsafety unknown
 */
public class ServerListCollectionEntry extends ServerListEntry implements Comparable<ServerListCollectionEntry> {
    private Boolean offline;

    public ServerListCollectionEntry(final String name, final ServerListEntryType type, final URI uri) {
        this(name, type, uri, null);
    }

    public ServerListCollectionEntry(
        final String name,
        final ServerListEntryType type,
        final URI uri,
        final Boolean offline) {
        super(name, type, uri);

        this.offline = offline;
    }

    public Boolean getOffline() {
        return offline;
    }

    public void setOffline(final Boolean offline) {
        this.offline = offline;
    }

    @Override
    public int compareTo(final ServerListCollectionEntry other) {
        final int result = super.compareTo(other);

        if (result != 0) {
            return result;
        }

        if (offline == null && other.offline != null) {
            return -1;
        } else if (offline != null && other.offline == null) {
            return 1;
        } else if (offline != null && other.offline != null) {
            if (offline.booleanValue() == false && other.offline.booleanValue() == true) {
                return -1;
            } else if (offline.booleanValue() == true && other.offline.booleanValue() == false) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();

        if (offline != null) {
            result = result * 37 + (offline ? 1 : 0);
        }

        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof ServerListCollectionEntry)) {
            return false;
        }

        if (o == this) {
            return true;
        }

        final ServerListCollectionEntry other = (ServerListCollectionEntry) o;

        if (!super.equals(other)) {
            return false;
        }

        if (offline != null ^ other.offline != null) {
            return false;
        } else if (offline != null && other.offline != null && !offline.equals(other.offline)) {
            return false;
        }

        return true;
    }
}
