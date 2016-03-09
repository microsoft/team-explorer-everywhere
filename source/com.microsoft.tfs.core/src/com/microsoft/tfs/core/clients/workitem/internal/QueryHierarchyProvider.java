// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.SupportedFeatures;
import com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy.QueryHierarchyImpl;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;

public class QueryHierarchyProvider {
    private final WITContext context;

    /* Maps Project id (Integer) to Query Hierarchy */
    private final Object lock = new Object();
    private final Map<Integer, QueryHierarchy> projectQueryHierarchies = new HashMap<Integer, QueryHierarchy>();

    // Methods
    public QueryHierarchyProvider(final WITContext context) {
        this.context = context;
    }

    public QueryHierarchy getQueryHierarchy(final Project project) {
        synchronized (lock) {
            final Integer projectIdKey = new Integer(project.getID());
            QueryHierarchy hierarchy = projectQueryHierarchies.get(projectIdKey);

            if (hierarchy == null) {
                hierarchy = new QueryHierarchyImpl(project);
                projectQueryHierarchies.put(projectIdKey, hierarchy);
            }

            return hierarchy;
        }
    }

    public boolean supportsFolders() {
        return context.getServerInfo().isSupported(SupportedFeatures.QUERY_FOLDERS);
    }

    public boolean supportsPermissions() {
        return context.getServerInfo().isSupported(SupportedFeatures.QUERY_FOLDER_PERMISSIONS);
    }
}
