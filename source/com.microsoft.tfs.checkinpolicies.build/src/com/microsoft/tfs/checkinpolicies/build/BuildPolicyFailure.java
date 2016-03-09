// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.util.Check;

public class BuildPolicyFailure extends PolicyFailure {
    private final IResource resource;

    public BuildPolicyFailure(final String message, final PolicyInstance policy, final IResource resource) {
        super(message, policy);

        Check.notNull(resource, "resource"); //$NON-NLS-1$
        this.resource = resource;
    }

    public IResource getResource() {
        return resource;
    }
}
