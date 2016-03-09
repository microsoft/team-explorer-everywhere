// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy;

import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Parser;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.SyntaxException;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;

/**
 * Helper methods for dealing with {@link QueryDefinition}s.
 *
 * @threadsafety unknown
 */
public final class QueryDefinitionUtil {
    private QueryDefinitionUtil() {
        /* Prevent instantiation */
    }

    /**
     * Determines the {@link QueryType} for the given WIQL query.
     *
     * @param queryText
     *        The WIQL text of the query (not <code>null</code>)
     * @return The {@link QueryType} for the given query, or
     *         {@link QueryType#INVALID} if the given query could not be parsed
     *         as well-formed WIQL.
     */
    public static QueryType getQueryType(final String queryText) {
        LinkQueryMode queryMode;

        try {
            queryMode = WIQLAdapter.getQueryMode(Parser.parseSyntax(queryText));
        } catch (final SyntaxException e) {
            return QueryType.INVALID;
        }

        return getQueryType(queryMode);
    }

    /**
     * Determines the {@link QueryType} for the given {@link LinkQueryMode}.
     *
     * @param queryMode
     *        The {@link LinkQueryMode} to determine the {@link QueryType} for.
     * @return The {@link QueryType} for the given {@link LinkQueryMode}.
     */
    public static QueryType getQueryType(final LinkQueryMode queryMode) {
        if (LinkQueryMode.LINKS_MUST_CONTAIN.equals(queryMode)
            || LinkQueryMode.LINKS_MAY_CONTAIN.equals(queryMode)
            || LinkQueryMode.LINKS_DOES_NOT_CONTAIN.equals(queryMode)) {
            return QueryType.ONE_HOP;
        }

        if (LinkQueryMode.LINKS_RECURSIVE.equals(queryMode)) {
            return QueryType.TREE;
        }

        return QueryType.LIST;
    }

    /**
     * Gets the default {@link LinkQueryMode} for the given {@link QueryType}.
     *
     * @param queryType
     *        The {@link QueryType}.
     * @return The default {@link LinkQueryMode} for the given {@link QueryType}
     *         .
     */
    public static LinkQueryMode getDefaultLinkQueryMode(final QueryType queryType) {
        if (QueryType.TREE.equals(queryType)) {
            return LinkQueryMode.LINKS_RECURSIVE;
        } else if (QueryType.ONE_HOP.equals(queryType)) {
            return LinkQueryMode.LINKS_MAY_CONTAIN;
        } else {
            return LinkQueryMode.WORK_ITEMS;
        }
    }
}
