// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import java.util.EventObject;

/**
 * <p>
 * An {@link EventObject} sent to {@link ValidityChangedListener}s to notify
 * that the validation state computed by a {@link Validator} may have changed.
 * </p>
 *
 * <p>
 * The source of this {@link EventObject} is the {@link Validator} that
 * generated the notification.
 * </p>
 *
 * @see ValidityChangedListener
 * @see Validator
 */
public class ValidityChangedEvent extends EventObject {
    private final IValidity validity;

    /**
     * Creates a new {@link ValidityChangedEvent}. This constructor records the
     * specified {@link Validator}'s {@link IValidity} when it is invoked, and
     * this {@link ValidityChangedEvent} will return that validity from
     * {@link #getValidity()}.
     *
     * @param validator
     *        the source of this {@link ValidityChangedEvent} (must not be
     *        <code>null</code>)
     */
    public ValidityChangedEvent(final Validator validator) {
        super(validator);

        validity = validator.getValidity();
    }

    /**
     * @return the {@link Validator} that generated this
     *         {@link ValidityChangedEvent}
     */
    public Validator getValidator() {
        return (Validator) getSource();
    }

    /**
     * @return the {@link IValidity} that was current for the {@link Validator}
     *         at the time this event was created
     */
    public IValidity getValidity() {
        return validity;
    }
}
