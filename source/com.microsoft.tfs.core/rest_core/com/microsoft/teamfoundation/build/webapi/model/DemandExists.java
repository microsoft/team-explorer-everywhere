// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.build.webapi.model;

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
}
