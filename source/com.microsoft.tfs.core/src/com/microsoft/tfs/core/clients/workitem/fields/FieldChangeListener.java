// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * Interface for listeners of the {@link FieldChangeEvent}.
 *
 * @since TEE-SDK-10.1
 */
public interface FieldChangeListener {
    /**
     * Called when a {@link WorkItem} {@link Field} has changed.
     *
     * @param event
     *        the {@link FieldChangeEvent} (must not be <code>null</code>)
     */
    public void fieldChanged(FieldChangeEvent event);
}
