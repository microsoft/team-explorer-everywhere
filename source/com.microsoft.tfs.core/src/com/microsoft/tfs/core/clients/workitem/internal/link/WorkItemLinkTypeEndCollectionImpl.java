// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.HashMap;
import java.util.Iterator;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkType;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;
import com.microsoft.tfs.util.Check;

public class WorkItemLinkTypeEndCollectionImpl implements WorkItemLinkTypeEndCollection {
    private final HashMap<String, WorkItemLinkTypeEnd> mapByName = new HashMap<String, WorkItemLinkTypeEnd>();
    private final HashMap<Integer, WorkItemLinkTypeEnd> mapById = new HashMap<Integer, WorkItemLinkTypeEnd>();

    public WorkItemLinkTypeEndCollectionImpl(final WITContext witContext, final WorkItemLinkType[] linkTypes) {
        Check.notNull(witContext, "witContext"); //$NON-NLS-1$
        Check.notNullOrEmpty(linkTypes, "linkTypes"); //$NON-NLS-1$

        for (int i = 0; i < linkTypes.length; i++) {
            final WorkItemLinkType linkType = linkTypes[i];
            mapByName.put(linkType.getForwardEnd().getImmutableName(), linkType.getForwardEnd());
            mapByName.put(linkType.getForwardEnd().getName(), linkType.getForwardEnd());
            mapById.put(new Integer(linkType.getForwardEnd().getID()), linkType.getForwardEnd());

            if (linkType.isDirectional()) {
                mapByName.put(linkType.getReverseEnd().getImmutableName(), linkType.getReverseEnd());
                mapByName.put(linkType.getReverseEnd().getName(), linkType.getReverseEnd());
                mapById.put(new Integer(linkType.getReverseEnd().getID()), linkType.getReverseEnd());
            }
        }
    }

    @Override
    public boolean contains(final int id) {
        return mapById.containsKey(new Integer(id));
    }

    @Override
    public boolean contains(final String linkTypeName) {
        return mapByName.containsKey(linkTypeName);
    }

    @Override
    public WorkItemLinkTypeEnd get(final String linkTypeEndName) {
        return mapByName.get(linkTypeEndName);
    }

    @Override
    public WorkItemLinkTypeEnd getByID(final int id) {
        return mapById.get(new Integer(id));
    }

    @Override
    public int getCount() {
        return mapById.size();
    }

    @Override
    public Iterator<WorkItemLinkTypeEnd> iterator() {
        return mapById.values().iterator();
    }

    @Override
    public WorkItemLinkTypeEnd[] toArray(final WorkItemLinkTypeEnd[] array) {
        return mapById.values().toArray(array);
    }
}
