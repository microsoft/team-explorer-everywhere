// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;

/**
 * Represents a stored query definition in the query hierarchy.
 *
 * @threadsafety unknown
 * @since TEE-SDK-10.1
 */
public interface QueryDefinition extends QueryItem {
    /**
     * Returns the WIQL query text for this query definition.
     *
     * @return The WIQL query text for this query definition, or
     *         <code>null</code>.
     */
    public String getQueryText();

    /**
     * Returns the original WIQL query text for this query definition as it was
     * loaded from the server, before any unsaved modifications.
     *
     * @return The original WIQL query text for this query definition, or
     *         <code>null</code>.
     */
    public String getOriginalQueryText();

    /**
     * Sets the WIQL query text for this query definition.
     *
     * @param queryText
     *        The WIQL query text for this query definition (not
     *        <code>null</code> or empty).
     * @throws WorkItemException
     *         If this query definition has been deleted.
     * @throws InvalidQueryTextException
     *         If the specified WIQL is not valid.
     */
    public void setQueryText(final String queryText) throws InvalidQueryTextException;

    /**
     * Returns the hierarchical return type of this query.
     *
     * @return The {@link QueryType} of this query.
     */
    public QueryType getQueryType();
}