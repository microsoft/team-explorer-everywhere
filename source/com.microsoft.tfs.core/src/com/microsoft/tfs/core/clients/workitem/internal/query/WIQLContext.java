// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

public class WIQLContext {
    private final String product;
    private final String currentUserDisplayName;
    private final int projectId;
    private final String wiql;

    public WIQLContext(
        final String wiql,
        final int projectId,
        final String product,
        final String currentUserDisplayName) {
        this.product = product;
        this.currentUserDisplayName = currentUserDisplayName;
        this.projectId = projectId;
        this.wiql = wiql;
    }

    public String getCurrentUserDisplayName() {
        return currentUserDisplayName;
    }

    public String getProduct() {
        return product;
    }

    public int getProjectID() {
        return projectId;
    }

    public String getWIQL() {
        return wiql;
    }
}
