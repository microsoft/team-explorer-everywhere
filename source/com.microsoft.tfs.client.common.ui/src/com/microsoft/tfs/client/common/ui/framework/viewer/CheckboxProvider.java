// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * A {@link CheckboxProvider} is an interface that is implemented by UI controls
 * that support elements with a checked state. It is similar in purpose and use
 * to the {@link ISelectionProvider} interface.
 */
public interface CheckboxProvider {
    /**
     * Adds a {@link CheckboxListener} to this {@link CheckboxProvider}. When
     * the set of checked elements on this provider has changed, the listener
     * will be notified.
     *
     * @param listener
     *        a {@link CheckboxListener} to add (must not be <code>null</code>)
     */
    public void addCheckboxListener(CheckboxListener listener);

    /**
     * Removes a previously added {@link CheckboxListener} from this
     * {@link CheckboxProvider}.
     *
     * @param listener
     *        a {@link CheckboxListener} to remove (must not be
     *        <code>null</code>)
     */
    public void removeCheckboxListener(CheckboxListener listener);

    /**
     * @return the current set of checked elements from this
     *         {@link CheckboxProvider} (never <code>null</code>)
     */
    public Object[] getCheckedElements();
}
