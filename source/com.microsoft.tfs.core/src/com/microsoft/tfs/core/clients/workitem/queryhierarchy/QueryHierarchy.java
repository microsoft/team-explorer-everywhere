// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.util.GUID;

/**
 * A {@link QueryHierarchy} represents the tree of stored Work Item queries and
 * folders that contain them.
 *
 * @since TEE-SDK-10.1
 */
public interface QueryHierarchy extends QueryFolder {
    /**
     * Determines whether the server supports folders in the query hierarchy.
     * (Team Foundation Server 2010 and newer support folders, older versions of
     * Team Foundation Server do not.)
     *
     * @return <code>true</code> if the server supports folders,
     *         <code>false</code> otherwise.
     */
    public boolean supportsFolders();

    /**
     * Determines whether the server supports permissions on query items.
     *
     * (Team Foundation Server 2010 and newer support permissions, older
     * versions of Team Foundation Server do not.)
     *
     * @return <code>true</code> if the server supports permissions,
     *         <code>false</code> otherwise.
     */
    public boolean supportsPermissions();

    /**
     * Scans the query hierarchy recursively for a {@link QueryItem} with an id
     * matching the given id.
     *
     * @param id
     *        The id of the {@link QueryItem} to query for (not
     *        <code>null</code>)
     * @return The located {@link QueryItem} in the hierarchy, or
     *         <code>null</code> if no {@link QueryItem} exists in the hierarchy
     *         with the given id.
     */
    public QueryItem find(final GUID id);

    /**
     * Reloads the query hierarchy from the server. Any unsaved changes to the
     * query hierarchy will be lost.
     */
    public void refresh();

    /**
     * Resets the query hierarchy to its original data, removing any unsaved
     * changes.
     */
    public void reset();

    /**
     * Saves any changes to the query hierarchy to the server.
     *
     * @throws InvalidQueryTextException
     *         if any stored query's WIQL text is not valid
     */
    public void save();
}