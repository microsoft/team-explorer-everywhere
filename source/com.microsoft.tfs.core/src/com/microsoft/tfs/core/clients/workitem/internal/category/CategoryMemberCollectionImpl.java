// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.category.CategoryMemberCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMemberMetadata;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemTypeCollection;
import com.microsoft.tfs.util.Check;

public class CategoryMemberCollectionImpl implements CategoryMemberCollection {
    /*
     * Map of CategoryID to list of WorkItemTypes (the work item type members of
     * the category).
     */
    private final Map<Integer, List<WorkItemType>> map = new HashMap<Integer, List<WorkItemType>>();

    public CategoryMemberCollectionImpl(final WITContext context, final WorkItemTypeCollection workItemTypes) {
        WorkItemTypeCategoryMemberMetadata[] membersMetadata;
        membersMetadata = context.getMetadata().getWorkItemTypeCategoryMembersTable().getCategoryMembers();

        for (final WorkItemTypeCategoryMemberMetadata member : membersMetadata) {
            final int categoryID = member.getCategoryID();
            final WorkItemType workItemType = workItemTypes.get(member.getWorkItemTypeID());

            if (!map.containsKey(categoryID)) {
                map.put(categoryID, new ArrayList<WorkItemType>());
            }

            map.get(categoryID).add(workItemType);
        }
    }

    @Override
    public WorkItemType[] getCategoryMembers(final int categoryID) {
        final List<WorkItemType> list = map.get(categoryID);
        Check.notNull(list, "must have members for work item type category"); //$NON-NLS-1$

        return list.toArray(new WorkItemType[list.size()]);
    }
}
