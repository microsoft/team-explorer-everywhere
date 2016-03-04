// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.util.Map;

public class DemandExists extends Demand {

    public DemandExists(final String name) {
        super(name, null);
    }

    @Override
    public Demand clone() {
        return new DemandExists(this.getName());
    }

    @Override
    protected String getExpression() {
        return this.getName();
    }

    @Override
    public boolean IsSatisfied(final Map<String, String> capabilities) {
        return capabilities.containsKey(this.getName());
    }
}
