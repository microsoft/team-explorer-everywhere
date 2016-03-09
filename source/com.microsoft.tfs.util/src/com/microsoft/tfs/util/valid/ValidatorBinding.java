// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * <p>
 * A {@link ValidatorBinding} is an object that can bind to a {@link Validator}
 * and provides some service that is tied to the {@link Validator}'s results.
 * For example, a {@link ValidatorBinding} implementation could bind to a
 * {@link Validator} and enable and disable a button in a GUI as the validation
 * changes.
 * </p>
 *
 * <p>
 * A {@link ValidatorBinding} is bound to at most one {@link Validator} at a
 * time. Any services provided by the {@link ValidatorBinding} are not specified
 * in this interface, but are completely up to the implementation.
 * </p>
 *
 * <p>
 * {@link AbstractValidatorBinding} is a useful base class to help write
 * {@link ValidatorBinding} implementations.
 * </p>
 *
 * @see Validator
 * @see AbstractValidatorBinding
 */
public interface ValidatorBinding {
    /**
     * Binds to the specified {@link Validatable}'s {@link Validator}. Calling
     * this method is equivalent to calling {@link #bind(Validator)} with the
     * {@link Validatable}'s {@link Validator}.
     *
     * @param validatable
     *        a {@link Validatable} which supplies a {@link Validator} to bind
     *        to, or <code>null</code> to only unbind the current
     *        {@link Validator}
     */
    public void bind(Validatable validatable);

    /**
     * Bind to the specified {@link Validator}. Any existing bound
     * {@link Validator} will be unbound.
     *
     * @param validator
     *        the {@link Validator} to bind to, or <code>null</code> to only
     *        unbind the current {@link Validator}
     */
    public void bind(Validator validator);

    /**
     * @return the {@link Validator} that this {@link ValidatorBinding} is bound
     *         to, or <code>null</code> if this {@link ValidatorBinding} is not
     *         currently bound to a {@link Validator}
     */
    public Validator getBoundValidator();

    /**
     * Unbind from the bound {@link Validator} (if any).
     */
    public void unbind();
}
