// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a rollback operation has started.
 *
 * @since TEE-SDK-11.0
 */
public class RollbackOperationStartedEvent extends OperationStartedEvent {
    private final ItemSpec[] itemSpecs;
    private final RollbackOptions options;

    public RollbackOperationStartedEvent(
        final EventSource source,
        final Workspace workspace,
        final ItemSpec[] itemSpecs,
        final RollbackOptions options) {
        super(source, workspace, ProcessType.ROLLBACK);

        Check.notNull(options, "options"); //$NON-NLS-1$

        this.itemSpecs = itemSpecs;
        this.options = options;
    }

    public ItemSpec[] getItemSpecs() {
        return itemSpecs;
    }

    public RollbackOptions getOptions() {
        return options;
    }
}
