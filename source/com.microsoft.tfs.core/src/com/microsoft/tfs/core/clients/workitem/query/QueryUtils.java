// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;

/**
 * Utilities for query classes.
 *
 * @since TEE-SDK-10.1
 */
public class QueryUtils {
    private static final Log log = LogFactory.getLog(QueryUtils.class);

    public static String getExtendedDescription(final StoredQuery query) {
        final QueryItem queryItem = query.getProject().getQueryHierarchy().find(query.getQueryGUID());

        String name, hierarchy;

        if (queryItem == null) {
            /* Should not happen, but fall back to the data within the query. */
            log.warn(MessageFormat.format("Could not locate query {0} in query hierarchy", query.getQueryGUID())); //$NON-NLS-1$

            name = query.getName();
            hierarchy = query.getProject().getName();
        } else {
            name = queryItem.getName();

            QueryItem parent = queryItem.getParent();

            if (parent == null) {
                /* Should never happen */
                return name;
            } else {
                /* Add our parent to the hierarchy stack */
                hierarchy = parent.getName();

                /*
                 * Now start with our grandparent, walking back up the tree to
                 * the root
                 */
                while (parent.getParent() != null) {
                    hierarchy = MessageFormat.format("{0} / {1}", parent.getParent().getName(), hierarchy); //$NON-NLS-1$

                    parent = parent.getParent();
                }
            }
        }

        return MessageFormat.format("{0} ({1})", name, hierarchy); //$NON-NLS-1$
    }
}
