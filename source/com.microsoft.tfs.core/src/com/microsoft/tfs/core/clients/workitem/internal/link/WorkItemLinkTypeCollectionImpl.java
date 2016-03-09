// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.HashMap;
import java.util.HashSet;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypeMetadata;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkType;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;

/**
 * Models the link type definition contained within WIT metadata. This
 * information first appeared in WIT version 3.
 *
 * @threadsafety thread-safe
 */
public class WorkItemLinkTypeCollectionImpl implements WorkItemLinkTypeCollection {
    private final HashSet<Integer> forwardIds = new HashSet<Integer>();
    private final HashSet<Integer> reverseIds = new HashSet<Integer>();
    private final HashMap<Integer, String> idToReferenceName = new HashMap<Integer, String>();
    private final HashMap<Integer, String> idToDisplayName = new HashMap<Integer, String>();

    private final HashMap<String, WorkItemLinkType> mapByName = new HashMap<String, WorkItemLinkType>();
    private final WorkItemLinkTypeEndCollection endsCollection;

    /**
     * Construct. Retrieves metadata from the specified WIT context and extracts
     * the raw data. Maps are build to provide quick look ups based on link type
     * identifiers.
     *
     *
     * @param context
     *        The WIT context.
     */
    public WorkItemLinkTypeCollectionImpl(final WITContext context) {
        final WorkItemLinkTypeMetadata[] metadataLinkTypes = context.getMetadata().getLinkTypesTable().getLinkTypes();

        for (int i = 0; i < metadataLinkTypes.length; i++) {
            final WorkItemLinkTypeMetadata linkTypeMetaData = metadataLinkTypes[i];

            final WorkItemLinkType linkType = new WorkItemLinkType(context, linkTypeMetaData);
            mapByName.put(linkType.getReferenceName(), linkType);

            forwardIds.add(new Integer(linkTypeMetaData.getForwardID()));
            reverseIds.add(new Integer(linkTypeMetaData.getReverseID()));

            idToReferenceName.put(new Integer(linkTypeMetaData.getForwardID()), linkTypeMetaData.getReferenceName());
            idToDisplayName.put(new Integer(linkTypeMetaData.getForwardID()), linkTypeMetaData.getForwardName());

            if (linkTypeMetaData.getForwardID() != linkTypeMetaData.getReverseID()) {
                idToReferenceName.put(
                    new Integer(linkTypeMetaData.getReverseID()),
                    linkTypeMetaData.getReferenceName());
                idToDisplayName.put(new Integer(linkTypeMetaData.getReverseID()), linkTypeMetaData.getReverseName());
            }
        }

        endsCollection = new WorkItemLinkTypeEndCollectionImpl(
            context,
            mapByName.values().toArray(new WorkItemLinkType[mapByName.values().size()]));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName(final int linkTypeId) {
        final Integer boxedId = new Integer(linkTypeId);

        if (idToDisplayName.containsKey(boxedId)) {
            return idToDisplayName.get(boxedId);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReferenceName(final int linkTypeId) {
        final Integer boxedId = new Integer(linkTypeId);

        if (idToReferenceName.containsKey(boxedId)) {
            return idToReferenceName.get(boxedId);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isForwardLink(final int linkTypeId) {
        return forwardIds.contains(new Integer(linkTypeId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReverseLink(final int linkTypeId) {
        return reverseIds.contains(new Integer(linkTypeId));
    }

    @Override
    public boolean contains(final String linkTypeReferenceName) {
        return mapByName.containsKey(linkTypeReferenceName);
    }

    @Override
    public WorkItemLinkType get(final String referenceName) {
        return mapByName.get(referenceName);
    }

    @Override
    public int getCount() {
        return mapByName.size();
    }

    @Override
    public WorkItemLinkTypeEndCollection getLinkTypeEnds() {
        return endsCollection;
    }
}
