// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import java.util.Date;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.GUID;

/**
 * Represents a stored query.
 *
 * @since TEE-SDK-10.1
 */
public interface StoredQuery extends Comparable<StoredQuery> {
    /**
     * Resets all changes that were made to this query to the initial state.
     */
    public void reset();

    /**
     * Updates the stored query.
     */
    public void update();

    /**
     * @return the date and time that this stored query was created.
     */
    public Date getCreationTime();

    /**
     * @return the description of this stored query.
     */
    public String getDescription();

    /**
     * Sets the description of this stored query.
     *
     * @param description
     *        the new description (may be <code>null</code> or empty)
     */
    public void setDescription(String description);

    /**
     * @return <code>true</code> if this stored query has been saved,
     *         <code>false</code> otherwise
     */
    public boolean isSaved();

    /**
     * @return the date and time that this stored query was created.
     */
    public Date getLastWriteTime();

    /**
     * @return the name of this stored query.
     */
    public String getName();

    /**
     * Sets the name of this stored query.
     *
     * @param name
     *        the new name (must not be <code>null</code>)
     */
    public void setName(String name);

    /**
     * @return the owner of this stored query.
     */
    public String getOwner();

    /**
     * @return he project that is associated with this stored query.
     */
    public Project getProject();

    /**
     * @return the {@link GUID} that is associated with this stored query.
     */
    public GUID getQueryGUID();

    /**
     * @return the scope of this stored query.
     */
    public QueryScope getQueryScope();

    /**
     * Sets the scope of this stored query.
     *
     * @param scope
     *        the {@link QueryScope} (must not be <code>null</code>)
     */
    public void setQueryScope(QueryScope scope);

    /**
     * @return the query string.
     */
    public String getQueryText();

    /**
     * Sets the query string.
     *
     * @param text
     *        the query string (must not be <code>null</code> or empty)
     * @throws InvalidQueryTextException
     *         if the query text was not valid WIQL
     */
    public void setQueryText(String text) throws InvalidQueryTextException;

    // note - below methods do not exist in MS WIT OM

    /**
     * Creates a new {@link Query} from this {@link StoredQuery}.
     *
     * @return the new {@link Query}
     * @throws InvalidQueryTextException
     *         if this {@link StoredQuery}'s query text is not valid WIQL
     */
    public Query createQuery(Map<String, Object> queryContext) throws InvalidQueryTextException;

    /**
     * Runs this stored query directly. Equivalent to {@link #createQuery()}.
     * {@link #runQuery()}.
     *
     * @return the work items returned by the query
     * @throws InvalidQueryTextException
     *         if this {@link StoredQuery}'s query text is not valid WIQL
     */
    public WorkItemCollection runQuery(Map<String, Object> queryContext) throws InvalidQueryTextException;

    /**
     * @return <code>true</code> if the current query text is valid WIQL,
     *         <code>false</code> otherwise.
     */
    public boolean isParsable();
}
