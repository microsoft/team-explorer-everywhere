// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import java.util.EventListener;

/**
 * <p>
 * A listener that can be attached to instances of {@link Validator}. A
 * {@link ValidityChangedListener} receives a {@link ValidityChangedEvent}
 * whenever the validation state computed by a {@link Validator} has potentially
 * changed.
 * </p>
 *
 * <p>
 * For example, a {@link ValidityChangedListener} could be used by a UI
 * component to update an error message shown in the UI whenever an underlying
 * {@link Validator} re-computes validity.
 * </p>
 *
 * @see Validator
 * @see ValidityChangedEvent
 */
public interface ValidityChangedListener extends EventListener {
    /**
     * <p>
     * A method used to indicate that the {@link Validator} instance that this
     * listener is attached to has performed validation and <b>may</b> have
     * computed a new {@link IValidity}. Typically this means that the
     * {@link Validator}'s {@link IValidity} has changed in some way.
     * </p>
     *
     * <p>
     * Note that this method is allowed to be invoked by a {@link Validator}
     * even if the {@link IValidity} has not changed since the last call. This
     * behavior is discouraged but implementations of
     * {@link ValidityChangedListener} shouldn't assume that it won't happen.
     * </p>
     *
     * @param event
     *        the {@link ValidityChangedEvent} sent with this notification (must
     *        not be <code>null</code>)
     */
    public void validityChanged(ValidityChangedEvent event);
}
