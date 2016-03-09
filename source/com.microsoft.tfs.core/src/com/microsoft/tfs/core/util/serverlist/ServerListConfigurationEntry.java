// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

import java.net.URI;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 *
 * @threadsafety unknown
 */
public class ServerListConfigurationEntry extends ServerListEntry implements Comparable<ServerListConfigurationEntry> {
    private final Set<ServerListCollectionEntry> collections = new TreeSet<ServerListCollectionEntry>();

    public ServerListConfigurationEntry(final String name, final ServerListEntryType type, final URI uri) {
        super(name, type, uri);
    }

    public Set<ServerListCollectionEntry> getCollections() {
        return collections;
    }

    @Override
    public int compareTo(final ServerListConfigurationEntry other) {
        int result = super.compareTo(other);

        if (result != 0) {
            return result;
        }

        if (collections == null && other.collections != null) {
            return -1;
        } else if (collections != null && other.collections == null) {
            return 1;
        } else if (collections != null && other.collections != null) {
            if (collections.size() != other.collections.size()) {
                return (collections.size() > other.collections.size()) ? 1 : -1;
            }

            for (Iterator<ServerListCollectionEntry> i = collections.iterator(), j =
                other.collections.iterator(); i.hasNext() && j.hasNext();) {
                result = i.next().compareTo(j.next());

                if (result != 0) {
                    return result;
                }
            }
        }

        return 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();

        if (collections != null) {
            for (final ServerListCollectionEntry collection : collections) {
                result = result * 37 + collection.hashCode();
            }
        }

        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof ServerListConfigurationEntry)) {
            return false;
        }

        if (o == this) {
            return true;
        }

        final ServerListConfigurationEntry other = (ServerListConfigurationEntry) o;

        if (!super.equals(other)) {
            return false;
        }

        if (collections != null ^ other.collections != null) {
            return false;
        } else if (collections != null && other.collections != null && collections.size() != other.collections.size()) {
            return false;
        } else if (collections != null && other.collections != null) {
            for (Iterator<ServerListCollectionEntry> i = collections.iterator(), j =
                other.collections.iterator(); i.hasNext() && j.hasNext();) {
                if (!i.next().equals(j.next())) {
                    return false;
                }
            }
        }

        return true;
    }
}
