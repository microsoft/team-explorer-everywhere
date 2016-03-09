// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

/**
 * @since TEE-SDK-10.1
 */
public class WorkItemLinkTypeEnd {
    private final WorkItemLinkType linkType;
    private final String name;
    private final int id;
    private WorkItemLinkTypeEnd oppositeEnd;

    public WorkItemLinkTypeEnd(final WorkItemLinkType linkType, final String name, final int id) {
        this.linkType = linkType;
        this.name = name;
        this.id = id;
    }

    public WorkItemLinkType getLinkType() {
        return linkType;
    }

    public String getName() {
        return name;
    }

    public String getImmutableName() {
        return isForwardLink() ? linkType.getReferenceName() + "-Forward" : linkType.getReferenceName() + "-Reverse"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public int getID() {
        return id;
    }

    public boolean isForwardLink() {
        return equals(linkType.getForwardEnd());
    }

    public WorkItemLinkTypeEnd getOppositeEnd() {
        return oppositeEnd;
    }

    public void setOppositeEnd(final WorkItemLinkTypeEnd oppositeEnd) {
        this.oppositeEnd = oppositeEnd;
    }

}
