// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.util.GUID;

/**
 * Represents a folder in Team Foundation Server that can be queried.
 *
 * @since TEE-SDK-10.1
 */
public interface QueryFolder extends QueryItem {
    /**
     * Gets the child {@link QueryItem}s contained by this folder.
     *
     * @return An array of {@link QueryItem}s (never <code>null</code>)
     */
    public QueryItem[] getItems();

    /**
     * Determines whether this folder contains the given {@link QueryItem}. Note
     * that this is not recursive.
     *
     * @param item
     *        The potential child {@link QueryItem} (not <code>null</code>).
     * @return <code>true</code> if this folder contains the given
     *         {@link QueryItem}, <code>false</code> otherwise.
     */
    public boolean contains(final QueryItem item);

    /**
     * Determines whether this folder contains a child {@link QueryItem} with
     * the given id. Note that this is not recursive.
     *
     * @param id
     *        The id of the potential child {@link QueryItem} (not
     *        <code>null</code>).
     * @return <code>true</code> if this folder contains a {@link QueryItem}
     *         with the given id, <code>false</code> otherwise.
     */
    public boolean containsID(final GUID id);

    /**
     * Determines whether this folder contains a child {@link QueryItem} with
     * the given name (ignoring case.) Note that this is not recursive.
     *
     * @param name
     *        The name of the potential child {@link QueryItem} (not
     *        <code>null</code>).
     * @return <code>true</code> if this folder contains a {@link QueryItem}
     *         with the given name, <code>false</code> otherwise.
     */
    public boolean containsName(final String name);

    /**
     * Returns the child {@link QueryItem} with the given id.
     *
     * @param id
     *        The id of the child {@link QueryItem} (not <code>null</code>).
     * @throws IllegalArgumentException
     *         if no child {@link QueryItem} exists with the given id.
     * @return The child {@link QueryItem} with the given id.
     */
    public QueryItem getItemByID(final GUID id);

    /**
     * Returns the child {@link QueryItem} with the given name.
     *
     * @param name
     *        The name of the child {@link QueryItem} (not <code>null</code>).
     * @throws IllegalArgumentException
     *         if no child {@link QueryItem} exists with the given name.
     * @return The child {@link QueryItem} with the given name.
     */
    public QueryItem getItemByName(final String name);

    /**
     * Adds the given {@link QueryItem} as a child of this folder.
     *
     * @param item
     *        The child {@link QueryItem} to add to this folder (not
     *        <code>null</code>).
     * @throws IllegalArgumentException
     *         if the given {@link QueryItem} cannot be added to this folder
     *         because this folder is the root of the query hierarchy, the
     *         folder is being deleted from the server, the given
     *         {@link QueryItem} is a folder and the server is a pre-TFS 2010
     *         version that does not support query hierarchies, the given
     *         {@link QueryItem} is associated with a different Team Project
     *         than this folder, or the given {@link QueryItem} already exists
     *         in the query hierarchy as a parent of this folder.
     * @throws WorkItemException
     *         if a child with the same name already exists
     */
    public void add(final QueryItem item);

    /**
     * Creates a new {@link QueryFolder} beneath this folder.
     *
     * @param folderName
     *        The name of the folder to create (not <code>null</code>)
     * @return The created {@link QueryFolder}.
     */
    public QueryFolder newFolder(String folderName);

    /**
     * Creates a new {@link QueryDefinition} beneath this folder.
     *
     * @param definitionName
     *        The name of the query to create (not <code>null</code>)
     * @param queryText
     *        The WIQL to create the query with (not <code>null</code> or empty)
     * @return The created {@link QueryDefinition}.
     */
    public QueryDefinition newDefinition(String definitionName, String queryText);
}