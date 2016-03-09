// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.util.Comparator;

public class WorkItemRelationComparator implements Comparator<WorkItemRelation> {
    public final boolean ascending;

    public WorkItemRelationComparator(final boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(final WorkItemRelation x, final WorkItemRelation y) {
        // Compare based on WorkItemRelationComparer behaviour in .NET
        int diff = x.getSourceID() - y.getSourceID();
        if (diff == 0) {
            diff = x.getTargetID() - y.getTargetID();
        }
        return ascending ? diff : -diff;
    }

}
