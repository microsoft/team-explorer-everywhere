// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a branch object has been successfully created or updated.
 *
 * @since TEE-SDK-10.1
 */
public class BranchObjectUpdatedEvent extends CoreClientEvent {
    private final BranchProperties branchProperties;

    public BranchObjectUpdatedEvent(final EventSource source, final BranchProperties branchProperties) {
        super(source);

        Check.notNull(branchProperties, "branchProperties"); //$NON-NLS-1$

        this.branchProperties = branchProperties;
    }

    public BranchProperties getBranchProperties() {
        return branchProperties;
    }
}
