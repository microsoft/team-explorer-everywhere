// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.DeniedOrNotExistException;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeImpl;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;

public class ProjectCollectionImpl implements ProjectCollection {
    private final WITContext witContext;
    private final Set<Project> projects = new HashSet<Project>();
    private final Map<String, Project> nameToProjectMap = new HashMap<String, Project>();
    private final Map<Integer, Project> idToProjectMap = new HashMap<Integer, Project>();

    public ProjectCollectionImpl(final WITContext witContext) {
        this.witContext = witContext;
        populate();
    }

    /*
     * ************************************************************************
     * START of implementation of ProjectCollection interface
     * ***********************************************************************
     */

    @Override
    public Iterator<Project> iterator() {
        return Collections.unmodifiableCollection(projects).iterator();
    }

    @Override
    public int size() {
        return projects.size();
    }

    @Override
    public Project get(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null"); //$NON-NLS-1$
        }
        return nameToProjectMap.get(name.toLowerCase());
    }

    @Override
    public Project getByID(final int id) {
        final Integer idKey = new Integer(id);

        if (!idToProjectMap.containsKey(idKey)) {
            throw new DeniedOrNotExistException();
        }

        return idToProjectMap.get(new Integer(id));
    }

    @Override
    public Project[] getProjects() {
        final Project[] returnArray = projects.toArray(new Project[] {});
        Arrays.sort(returnArray);
        return returnArray;
    }

    @Override
    public WorkItemClient getClient() {
        return witContext.getClient();
    }

    /*
     * ************************************************************************
     * END of implementation of ProjectCollection interface
     * ***********************************************************************
     */

    private void populate() {
        for (final Iterator<Node> it = witContext.getRootNode().getChildNodes().iterator(); it.hasNext();) {
            final Node projectNode = it.next();

            if (projectNode instanceof NodeImpl) {
                addProject(new ProjectImpl((NodeImpl) projectNode, witContext));
            }
        }
    }

    private void addProject(final Project project) {
        projects.add(project);
        nameToProjectMap.put(project.getName().toLowerCase(), project);
        idToProjectMap.put(new Integer(project.getID()), project);
    }
}
