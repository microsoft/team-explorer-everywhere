// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import java.util.EventObject;

import com.microsoft.tfs.util.Check;

/**
 * A {@link CheckboxEvent} is an {@link EventObject} that is sent to
 * {@link CheckboxListener}s that are registered with a {@link CheckboxProvider}
 * . It contains the set of checked elements held by the
 * {@link CheckboxProvider} at the time of the event.
 */
public class CheckboxEvent extends EventObject {
    private final Object[] checkedElements;

    /**
     * Create a new {@link CheckboxEvent}.
     *
     * @param source
     *        the {@link CheckboxProvider} that is sending this event
     * @param checkedElements
     *        the current set of checked elements for the provider
     */
    public CheckboxEvent(final CheckboxProvider source, final Object[] checkedElements) {
        super(source);

        Check.notNull(checkedElements, "checkedElements"); //$NON-NLS-1$

        this.checkedElements = checkedElements;
    }

    /**
     * @return the set of checked elements of the {@link CheckboxProvider} at
     *         the time this event was fired
     */
    public Object[] getCheckedElements() {
        return checkedElements;
    }

    /**
     * @return the {@link CheckboxProvider} that is firing this event
     */
    public CheckboxProvider getCheckboxProvider() {
        return (CheckboxProvider) getSource();
    }
}
