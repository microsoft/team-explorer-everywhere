// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import java.util.EventListener;

/**
 * {@link CheckboxListener}s can be added to {@link CheckboxProvider}s and
 * receive notifications when the provider's set of checked elements has
 * changed.
 */
public interface CheckboxListener extends EventListener {
    /**
     * Called when the set of checked elements managed by the
     * {@link CheckboxProvider} this {@link CheckboxListener} is attached to has
     * changed.
     *
     * @param event
     *        a {@link CheckboxEvent} that contains the details of this event
     *        (never <code>null</code>)
     */
    public void checkedElementsChanged(CheckboxEvent event);
}
