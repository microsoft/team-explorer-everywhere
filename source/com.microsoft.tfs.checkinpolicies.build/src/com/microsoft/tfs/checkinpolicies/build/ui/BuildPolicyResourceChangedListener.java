// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

import com.microsoft.tfs.util.Check;

/**
 * Listens for Eclipse's post build events and tells the policy about them (so
 * it can re-evaluate).
 */
public class BuildPolicyResourceChangedListener implements IResourceChangeListener {
    private final BuildPolicyUI policy;

    public BuildPolicyResourceChangedListener(final BuildPolicyUI policy) {
        Check.notNull(policy, "policy"); //$NON-NLS-1$
        this.policy = policy;
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_BUILD) {
            policy.onBuild();
        }
    }
}
