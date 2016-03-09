// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * <p>
 * A {@link Validator} is an object that can perform validation of some other
 * object (the "subject"). The result of this validation is expressed as an
 * {@link IValidity}.
 * </p>
 *
 * <p>
 * Clients of a {@link Validator} can query the current validation state by
 * calling the {@link #getValidity()} method. In addition, many clients will
 * want to be notified of changes to the validation state. This can be done by
 * registering a {@link ValidityChangedListener} by calling the
 * {@link #addValidityChangedListener(ValidityChangedListener)} method.
 * </p>
 *
 * <p>
 * {@link Validator} implementations are typically passed a reference to the
 * subject at construction time. A {@link Validator} is responsible for
 * inspecting the current state of the subject, as well as responding to changes
 * in the subject's state. The {@link #dispose()} method of a {@link Validator}
 * should be called when the {@link Validator} is no longer needed. When a
 * {@link Validator} is disposed, it should deregister any listeners it has
 * registered with the subject.
 * </p>
 *
 * @see IValidity
 * @see ValidityChangedListener
 */
public interface Validator {
    /**
     * @return the current validation state that has been computed by this
     *         {@link Validator} as an {@link IValidity} (never
     *         <code>null</code>)
     */
    public IValidity getValidity();

    /**
     * Adds a new {@link ValidityChangedListener} to this {@link Validator}. The
     * listener will be notified of changes to the validation state.
     *
     * @param listener
     *        the {@link ValidityChangedListener} to add (must not be
     *        <code>null</code>)
     */
    public void addValidityChangedListener(ValidityChangedListener listener);

    /**
     * Removes a previously added {@link ValidityChangedListener} from this
     * {@link Validator}.
     *
     * @param listener
     *        the {@link ValidityChangedListener} to remove (must not be
     *        <code>null</code>)
     */
    public void removeValidityChangedListener(ValidityChangedListener listener);

    /**
     * @return the {@link Object} being validated by this {@link Validator}
     *         (never <code>null</code>)
     */
    public Object getSubject();

    /**
     * Should be called when this {@link Validator} is no longer needed.
     * Implementations should de-register any listeners that are observing the
     * subject.
     */
    public void dispose();
}
