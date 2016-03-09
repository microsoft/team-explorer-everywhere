// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import java.util.ArrayList;
import java.util.Arrays;

import com.microsoft.tfs.core.clients.workitem.category.Category;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.GUID;

/**
 * Provides information about groups that can be used in work item queries.
 *
 * @since TEE-SDK-10.1
 */
public class GroupDataProvider {
    private static final GUID GLOBAL_GROUP_DOMAIN_NAME = new GUID("488bb442-0beb-4c1e-98b6-4eddc604bd9e"); //$NON-NLS-1$

    private final WorkItemClient client;
    private final Project project;

    private String[] categoryNames;
    private String[] groupNames;

    /**
     * Constructor.
     *
     * @param client
     *        The work item client.
     *
     * @param projectName
     *        The project associated with this provider.
     */
    public GroupDataProvider(final WorkItemClient client, final String projectName) {
        this.client = client;
        project = client.getProjects().get(projectName);
    }

    /**
     * Returns the user groups which are allowed values for an "IN GROUP" clause
     * for any WIT field of type string.
     *
     * @return An array of allowed group names.
     */
    public String[] getGroups() {
        if (groupNames == null) {
            final GUID projectGuid = project != null ? project.getGUID() : GUID.EMPTY;
            groupNames = client.getGlobalAndProjectGroups(GLOBAL_GROUP_DOMAIN_NAME, projectGuid);
            Arrays.sort(groupNames);
        }

        return groupNames;
    }

    /**
     * Returns the categories which are allowed values for an "IN GROUP" clause
     * when the target field is "Work item type".
     *
     * @return An array of allowed category names.
     */
    public String[] getWorkItemCategories() {
        if (categoryNames == null) {
            final ArrayList<String> names = new ArrayList<String>();
            if (project != null) {
                for (final Category category : project.getCategories()) {
                    names.add(category.getName());
                }
            }

            categoryNames = names.toArray(new String[names.size()]);
            Arrays.sort(categoryNames);
        }

        return categoryNames;
    }
}
