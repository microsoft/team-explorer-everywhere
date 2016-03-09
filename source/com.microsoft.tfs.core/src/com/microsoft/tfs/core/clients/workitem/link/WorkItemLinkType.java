// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.link.LinkTypeRuleMasks;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypeMetadata;

/**
 * @since TEE-SDK-10.1
 */
public class WorkItemLinkType {
    private final WITContext witContext;
    private final WorkItemLinkTypeMetadata linkType;
    private final int rules;
    private final String referenceName;
    private final boolean isDeleted = false;
    private final WorkItemLinkTypeEnd forwardEnd;
    private final WorkItemLinkTypeEnd reverseEnd;

    public WorkItemLinkType(final WITContext witContext, final WorkItemLinkTypeMetadata linkType) {
        this.witContext = witContext;
        this.linkType = linkType;
        referenceName = this.linkType.getReferenceName();
        rules = this.linkType.getRules();
        forwardEnd = new WorkItemLinkTypeEnd(this, linkType.getForwardName(), linkType.getForwardID());

        if (isDirectional()) {
            reverseEnd = new WorkItemLinkTypeEnd(this, linkType.getReverseName(), linkType.getReverseID());
            reverseEnd.setOppositeEnd(forwardEnd);
            forwardEnd.setOppositeEnd(reverseEnd);
        } else {
            // End1 and End2 are the same for non-directional link types.
            forwardEnd.setOppositeEnd(forwardEnd);
            reverseEnd = forwardEnd;
        }
    }

    public WITContext getWITContext() {
        return witContext;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public WorkItemLinkTypeEnd getForwardEnd() {
        return forwardEnd;
    }

    public WorkItemLinkTypeEnd getReverseEnd() {
        return reverseEnd;
    }

    /**
     * Returns 'true' if this link type is directional, false otherwise.
     */
    public boolean isDirectional() {
        return (rules & LinkTypeRuleMasks.DIRECTIONAL) == LinkTypeRuleMasks.DIRECTIONAL;
    }

    public boolean isNonCircular() {
        return (rules & LinkTypeRuleMasks.NON_CIRCULAR) == LinkTypeRuleMasks.NON_CIRCULAR;
    }

    public boolean isOneToMany() {
        return (rules & LinkTypeRuleMasks.SINGLE_TARGET) == LinkTypeRuleMasks.SINGLE_TARGET;
    }

    public boolean isActive() {
        return (rules & LinkTypeRuleMasks.DISABLED) == 0 && !isDeleted;
    }

    public boolean canDelete() {
        return (rules & LinkTypeRuleMasks.DENY_DELETE) == 0 && !isDeleted;
    }

    public boolean canEdit() {
        return (rules & LinkTypeRuleMasks.DENY_EDIT) == 0 && !isDeleted;
    }

    public Topology getLinkTopology() {
        return Topology.getTopology(rules);
    }
}
