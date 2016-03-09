// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.build.webapi.model;

import com.microsoft.vss.client.core.utils.ArgumentUtility;

public class DemandEquals extends Demand {

    public DemandEquals(final String name, final String value) {
        super(name, value);
        ArgumentUtility.checkStringForNullOrEmpty(value, "value"); //$NON-NLS-1$
    }

    @Override
    public Demand clone() {
        return new DemandEquals(this.getName(), this.getValue());
    }

    @Override
    protected String getExpression() {
        return this.getName() + " -equals " + this.getValue(); //$NON-NLS-1$
    }
}
