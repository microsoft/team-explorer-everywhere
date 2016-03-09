// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wittype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemTypeCollection;

public class WorkItemTypeCollectionImpl implements WorkItemTypeCollection {
    private final Set<WorkItemType> workItemTypes = new HashSet<WorkItemType>();
    private final Map<Integer, WorkItemType> idToWorkItemTypeMap = new HashMap<Integer, WorkItemType>();
    private final Map<String, WorkItemType> nameToWorkItemTypeMap = new HashMap<String, WorkItemType>();

    public WorkItemTypeCollectionImpl(final ProjectImpl project, final WITContext witContext) {
        final WorkItemTypeMetadata[] workItemTypesMetadata =
            witContext.getMetadata().getWorkItemTypeTable().getWorkItemTypes(project.getID());

        for (int i = 0; i < workItemTypesMetadata.length; i++) {
            final WorkItemTypeImpl workItemType = new WorkItemTypeImpl(workItemTypesMetadata[i], project, witContext);

            // Exclude work item types with non-positive ID's to exclude
            // project-level and
            // collection level workflows.
            if (workItemType.getID() > 0) {
                workItemTypes.add(workItemType);
            }
            nameToWorkItemTypeMap.put(workItemType.getName().toLowerCase(), workItemType);
            idToWorkItemTypeMap.put(workItemType.getID(), workItemType);
        }
    }

    /***************************************************************************
     * START of implementation of WorkItemTypeCollection interface
     **************************************************************************/

    @Override
    public WorkItemType get(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null"); //$NON-NLS-1$
        }

        return nameToWorkItemTypeMap.get(name.toLowerCase());
    }

    @Override
    public WorkItemType get(final int id) {
        return idToWorkItemTypeMap.get(id);
    }

    @Override
    public Iterator<WorkItemType> iterator() {
        final List<WorkItemType> types = new ArrayList<WorkItemType>(workItemTypes);
        Collections.sort(types);
        return Collections.unmodifiableCollection(types).iterator();
    }

    @Override
    public int size() {
        return workItemTypes.size();
    }

    @Override
    public WorkItemType[] getTypes() {
        final WorkItemType[] types = workItemTypes.toArray(new WorkItemType[] {});
        Arrays.sort(types);
        return types;
    }

    /***************************************************************************
     * END of implementation of WorkItemTypeCollection interface
     **************************************************************************/
}
