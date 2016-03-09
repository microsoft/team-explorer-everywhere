// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for dealing with {@link QueryFolder}s.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public final class QueryFolderUtil {
    public final static String PATH_HIERARCHY_SEPARATOR = "\\"; //$NON-NLS-1$

    private QueryFolderUtil() {
        /* Prevent instantiation */
    }

    /**
     * Gets a string representation of the full path of this {@link QueryFolder}
     * suitable for display to a user. Each path component is displayed,
     * separated by a "\".
     *
     * @param folder
     *        The {@link QueryFolder} to get a hierarchical path to
     * @return The full hierarchical path to this folder
     */
    public static String getHierarchicalPath(final QueryFolder folder) {
        final List<String> parts = new ArrayList<String>();
        for (QueryFolder currentFolder = folder; currentFolder != null; currentFolder = currentFolder.getParent()) {
            parts.add(currentFolder.getName());
        }

        Collections.reverse(parts);

        final StringBuffer pathName = new StringBuffer();

        for (final Iterator<String> i = parts.iterator(); i.hasNext();) {
            if (pathName.length() > 0) {
                pathName.append(PATH_HIERARCHY_SEPARATOR);
            }
            pathName.append(i.next());
        }

        return pathName.toString();
    }
}
