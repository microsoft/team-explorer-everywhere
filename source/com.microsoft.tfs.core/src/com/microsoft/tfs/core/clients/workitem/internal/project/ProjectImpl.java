// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.project;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.CoreCategoryReferenceNames;
import com.microsoft.tfs.core.clients.workitem.SupportedFeatures;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.category.Category;
import com.microsoft.tfs.core.clients.workitem.category.CategoryCollection;
import com.microsoft.tfs.core.clients.workitem.category.CategoryMemberCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.category.CategoryCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.category.CategoryMemberCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeImpl;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeStructureType;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.wittype.WorkItemTypeCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.node.Node.TreeType;
import com.microsoft.tfs.core.clients.workitem.node.NodeCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectModificationEvent;
import com.microsoft.tfs.core.clients.workitem.project.ProjectModificationListener;
import com.microsoft.tfs.core.clients.workitem.query.StoredQueryCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemTypeCollection;
import com.microsoft.tfs.util.GUID;

public class ProjectImpl implements Project {
    /*
     * The Hierarchy node that represents this team project
     */
    private final NodeImpl node;

    /*
     * The cached, lazily loaded WorkItemTypeCollection
     */
    private WorkItemTypeCollectionImpl workItemTypeCollection;

    /*
     * The cached, lazily loaded StoredQueryCollection
     */
    private StoredQueryCollectionImpl storedQueryCollection;

    /*
     * The cached, lazily loaded CategoryCollection
     */
    private CategoryCollectionImpl categoryCollection;

    /*
     * The cached, lazily loaded CategoryMemberCollection
     */
    private CategoryMemberCollectionImpl categoryMemberCollection;

    /*
     * The cached, lazily loaded array of visible work item types.
     */
    private WorkItemType[] visibleWorkItemTypes;

    /*
     * The cached, lazily loaded NodeCollection of area roots
     */
    private NodeCollectionImpl areaRootNodes;

    /*
     * The cached, lazily loaded NodeCollection of iteration roots
     */
    private NodeCollectionImpl iterationRootNodes;

    /*
     * The WIT context
     */
    private final WITContext witContext;

    private final ArrayList<ProjectModificationListener> projectModificationListeners =
        new ArrayList<ProjectModificationListener>();

    public ProjectImpl(final NodeImpl node, final WITContext witContext) {
        this.node = node;
        this.witContext = witContext;
    }

    @Override
    public String toString() {
        return MessageFormat.format("project: {0}/{1}", Integer.toString(node.getID()), node.getName()); //$NON-NLS-1$
    }

    @Override
    public int compareTo(final Project other) {
        return getName().compareToIgnoreCase(other.getName());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ProjectImpl) {
            final ProjectImpl other = (ProjectImpl) obj;

            return (other.witContext == witContext) && (other.getID() == getID());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return witContext.hashCode() + getID();
    }

    /*
     * ************************************************************************
     * START of implementation of Project interface
     * ***********************************************************************
     */

    @Override
    public synchronized NodeCollection getAreaRootNodes() {
        if (areaRootNodes == null) {
            final NodeImpl areaChildNode = node.findFirstChildOfStructureType(NodeStructureType.AREA);
            areaRootNodes = areaChildNode.getChildNodesInternal();
        }
        return areaRootNodes;
    }

    @Override
    public synchronized NodeCollection getIterationRootNodes() {
        if (iterationRootNodes == null) {
            final NodeImpl iterationChildNode = node.findFirstChildOfStructureType(NodeStructureType.ITERATION);
            iterationRootNodes = iterationChildNode.getChildNodesInternal();
        }
        return iterationRootNodes;
    }

    @Override
    public int getID() {
        return node.getID();
    }

    @Override
    public GUID getGUID() {
        return node.getGUID();
    }

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public String getURI() {
        return node.getURI();
    }

    @Override
    public synchronized WorkItemTypeCollection getWorkItemTypes() {
        if (workItemTypeCollection == null) {
            workItemTypeCollection = new WorkItemTypeCollectionImpl(this, witContext);
        }
        return workItemTypeCollection;
    }

    @Override
    public synchronized WorkItemType[] getVisibleWorkItemTypes() {
        if (visibleWorkItemTypes == null) {
            if (!witContext.getServerInfo().isSupported(SupportedFeatures.WORK_ITEM_TYPE_CATEGORY_MEMBERS)
                || !getCategories().contains(CoreCategoryReferenceNames.HIDDEN)) {
                visibleWorkItemTypes = getWorkItemTypes().getTypes();
            } else {
                final Category hiddenCategory = getCategories().get(CoreCategoryReferenceNames.HIDDEN);
                final WorkItemType[] hiddenTypes = getCategoryMembers().getCategoryMembers(hiddenCategory.getID());

                final Set<Integer> hiddenTypeIDs = new HashSet<Integer>();
                if (hiddenTypes != null) {
                    for (final WorkItemType hiddenType : hiddenTypes) {
                        hiddenTypeIDs.add(hiddenType.getID());
                    }
                }

                final List<WorkItemType> list = new ArrayList<WorkItemType>();
                for (final WorkItemType type : getWorkItemTypes()) {
                    if (!hiddenTypeIDs.contains(type.getID())) {
                        list.add(type);
                    }
                }

                visibleWorkItemTypes = list.toArray(new WorkItemType[list.size()]);
            }
        }

        return visibleWorkItemTypes;
    }

    @Override
    public synchronized StoredQueryCollection getStoredQueries() {
        if (storedQueryCollection == null) {
            storedQueryCollection = new StoredQueryCollectionImpl(this, witContext);
        }
        return storedQueryCollection;
    }

    @Override
    public QueryHierarchy getQueryHierarchy() {
        return witContext.getQueryHierarchyProvider().getQueryHierarchy(this);
    }

    @Override
    public synchronized StoredQueryCollection getAndRefreshStoredQueries() {
        if (storedQueryCollection == null) {
            return getStoredQueries();
        }
        storedQueryCollection.refresh();
        return storedQueryCollection;
    }

    @Override
    public synchronized CategoryCollection getCategories() {
        if (categoryCollection == null) {
            categoryCollection = new CategoryCollectionImpl(getWITContext(), getID());
        }
        return categoryCollection;
    }

    @Override
    public synchronized CategoryMemberCollection getCategoryMembers() {
        if (categoryMemberCollection == null) {
            categoryMemberCollection = new CategoryMemberCollectionImpl(getWITContext(), getWorkItemTypes());
        }
        return categoryMemberCollection;
    }

    @Override
    public Object resolvePath(final String path, final TreeType treeType) {
        if (path == null) {
            return null;
        }

        final int structureType =
            (treeType == Node.TreeType.AREA ? NodeStructureType.AREA : NodeStructureType.ITERATION);
        final NodeImpl targetNode = node.findNodeDownwards(path, true, structureType);

        if (targetNode == node) {
            return this;
        } else {
            return targetNode;
        }
    }

    @Override
    public boolean addProjectModificationListener(final ProjectModificationListener listener) {
        return projectModificationListeners.add(listener);
    }

    @Override
    public boolean removeProjectModificationListener(final ProjectModificationListener listener) {
        return projectModificationListeners.remove(listener);
    }

    @Override
    public void notifyModicationListeners() {
        final ProjectModificationEvent event = new ProjectModificationEvent(this);

        for (final Iterator<ProjectModificationListener> it = projectModificationListeners.iterator(); it.hasNext();) {
            final ProjectModificationListener listener = it.next();
            listener.onModification(event);
        }
    }

    @Override
    public WorkItemClient getWorkItemClient() {
        return witContext.getClient();
    }

    @Override
    public WITContext getWITContext() {
        return witContext;
    }

    @Override
    public void clearCachedWITMetadata() {
        areaRootNodes = null;
        iterationRootNodes = null;
        workItemTypeCollection = null;
        categoryCollection = null;
        categoryMemberCollection = null;
        visibleWorkItemTypes = null;
    }

    /*
     * ************************************************************************
     * END of implementation of Project interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of implementation of internal (ProjectImpl) methods
     * ***********************************************************************
     */

    public NodeImpl getAreaRootNode() {
        return node.findFirstChildOfStructureType(NodeStructureType.AREA);
    }

    public NodeImpl getNodeInternal() {
        return node;
    }

    /*
     * ************************************************************************
     * END of implementation of internal (ProjectImpl) methods
     * ***********************************************************************
     */
}
