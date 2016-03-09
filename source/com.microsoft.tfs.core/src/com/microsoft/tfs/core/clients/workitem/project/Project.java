// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.project;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.category.CategoryCollection;
import com.microsoft.tfs.core.clients.workitem.category.CategoryMemberCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.node.Node.TreeType;
import com.microsoft.tfs.core.clients.workitem.node.NodeCollection;
import com.microsoft.tfs.core.clients.workitem.query.StoredQueryCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemTypeCollection;
import com.microsoft.tfs.util.GUID;

/**
 * Represents a work item tracking project.
 *
 * @since TEE-SDK-10.1
 */
public interface Project extends Comparable<Project> {
    /**
     * @return the name of this project.
     */
    public String getName();

    /**
     * @return the ID of this project.
     */
    public int getID();

    /**
     * @return the GUID of the {@link Node} which belongs to this project.
     */
    public GUID getGUID();

    /**
     * @return the URI of this project.
     */
    public String getURI();

    /**
     * @return a collection of {@link WorkItemType} objects that belong to this
     *         project.
     */
    public WorkItemTypeCollection getWorkItemTypes();

    /**
     *
     *
     *
     * @return an array of {@link WorkItemType} objects that belong to this
     *         project and are not in the "Hidden" category.
     */
    public WorkItemType[] getVisibleWorkItemTypes();

    /**
     * @deprecated please use getQueryHierarchy instead
     */
    @Deprecated
    public StoredQueryCollection getStoredQueries();

    /**
     * @deprecated please use getQueryHierarchy instead
     */
    @Deprecated
    public StoredQueryCollection getAndRefreshStoredQueries();

    /**
     * @return the collection of area root nodes.
     */
    public NodeCollection getAreaRootNodes();

    /**
     * @return the collection of iteration root nodes.
     */
    public NodeCollection getIterationRootNodes();

    /**
     * @return the collection of query items that is associated with this
     *         project.
     */
    public QueryHierarchy getQueryHierarchy();

    /**
     * @return the collection of work item type categories that belong to this
     *         project.
     */
    public CategoryCollection getCategories();

    /**
     * @return the collection of work item type category membership that belong
     *         to this project.
     */
    public CategoryMemberCollection getCategoryMembers();

    /**
     * Gets the {@link Project} or {@link Node} or other object which
     * corresponds to the specified Area or Iteration path and type.
     *
     * @param path
     *        the path to resolve (must not be <code>null</code>)
     * @param treeType
     *        the {@link TreeType} of the item to resolve as (must not be
     *        <code>null</code>)
     * @return the object found at the specified path, or <code>null</code> if
     *         none was found
     */
    public Object resolvePath(String path, TreeType treeType);

    /**
     * Adds a listener for the project modification event.
     *
     * @param listener
     *        the listener to add
     * @return <code>true</code> always
     */
    public boolean addProjectModificationListener(ProjectModificationListener listener);

    /**
     * Removes a listener for the project modification event.
     *
     * @param listener
     *        the listener to remove
     * @return <code>true</code> if the {@link Project} contained the listener
     *         and it was removed, <code>false</code> if the {@link Project} did
     *         not contain the listener
     */
    public boolean removeProjectModificationListener(ProjectModificationListener listener);

    /**
     * Notifies all project modification listeners.
     */
    public void notifyModicationListeners();

    /**
     * @return the {@link WorkItemClient} for this {@link Project}
     */
    public WorkItemClient getWorkItemClient();

    /**
     * @return the {@link WITContext} for this {@link Project}
     */
    public WITContext getWITContext();

    /**
     * Removes the locally cached work item metadata, which will be repopulated
     * on demand.
     */
    public void clearCachedWITMetadata();
}
