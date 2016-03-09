// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.project;

import java.util.Iterator;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

/**
 * A collection of {@link Project} objects.
 *
 * @since TEE-SDK-10.1
 */
public interface ProjectCollection extends Iterable<Project> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<Project> iterator();

    /**
     * @return the number of {@link Project}s in the collection.
     */
    public int size();

    /**
     * Gets a {@link Project} by name.
     *
     * @param projectName
     *        the name of the {@link Project} to get (must not be
     *        <code>null</code>)
     * @return the {@link Project} for the specified name
     */
    public Project get(String projectName);

    /**
     * Gets a {@link Project} by ID.
     *
     * @param projectID
     *        the ID of the {@link Project} to get.
     * @return the {@link Project} for the specified ID
     */
    public Project getByID(int projectID);

    /**
     * @return an array of all {@link Project}s
     */
    public Project[] getProjects();

    /**
     * @return a reference to the {@link WorkItemClient} associated with this
     *         collection.
     */
    public WorkItemClient getClient();
}
