// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

public class WorkItemRelation implements Comparable<WorkItemRelation> {
    public static final int MISSING_ID = LinkQueryResultXMLConstants.MISSING_ID;

    private int sourceId;
    private int targetId;
    private int linkTypeId;
    private boolean isLocked;

    public WorkItemRelation(final int sourceId, final int targetId, final int linkTypeId, final boolean isLocked) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.linkTypeId = linkTypeId;
        this.isLocked = isLocked;
    }

    public int getSourceID() {
        return sourceId;
    }

    public void setSourceID(final int sourceId) {
        this.sourceId = sourceId;
    }

    public int getTargetID() {
        return targetId;
    }

    public void setTargetID(final int targetId) {
        this.targetId = targetId;
    }

    public int getLinkTypeID() {
        return linkTypeId;
    }

    public void setLinkTypeID(final int linkTypeId) {
        this.linkTypeId = linkTypeId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(final boolean isLocked) {
        this.isLocked = isLocked;
    }

    @Override
    public int compareTo(final WorkItemRelation other) {
        // Compare based on WorkItemRelationComparer behaviour in .NET
        final int diff = sourceId - other.sourceId;
        if (diff == 0) {
            return targetId - other.targetId;
        }
        return diff;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime * 1 + sourceId;
        result = prime * result + targetId;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WorkItemRelation other = (WorkItemRelation) obj;
        if (sourceId != other.sourceId) {
            return false;
        }
        if (targetId != other.targetId) {
            return false;
        }
        return true;
    }

}
