// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import com.microsoft.tfs.client.common.ui.framework.viewer.ElementProvider;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.Validity;

/**
 * <p>
 * A {@link ElementProviderValidator} is a concrete {@link Validator}
 * implementation that has an {@link ElementProvider} as a validation subject.
 * It is used to validate that an {@link ElementProvider} contains a certain
 * number of elements. Its error message can be customized or omitted.
 * </p>
 *
 * <p>
 * To perform customized validation of a {@link ElementProvider} subject,
 * override {@link AbstractElementProviderValidator}. This class is not designed
 * to be subclassed.
 * </p>
 *
 * @see Validator
 * @see ElementProvider
 * @see AbstractElementProviderValidator
 */
public final class ElementProviderValidator extends AbstractElementProviderValidator {
    /**
     * The {@link NumericConstraint} supplied in the constructor (never
     * <code>null</code>).
     */
    private final NumericConstraint numericConstraint;

    /**
     * The error message supplied in the constructor (can be <code>null</code>).
     */
    private final String errorMessage;

    /**
     * Creates a new {@link ElementProviderValidator} that validates that the
     * specified {@link ElementProvider}'s element set is non-empty. If the
     * element provider is invalid, no error message is contained in the
     * {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link ElementProvider} subject to validate (must not be
     *        <code>null</code>)
     */
    public ElementProviderValidator(final ElementProvider subject) {
        this(subject, NumericConstraint.ONE_OR_MORE, null);
    }

    /**
     * Creates a new {@link ElementProviderValidator} that validates that the
     * {@link ElementProvider}'s element set is non-empty. If the element
     * provider is invalid, the specified error message is used in the
     * {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link ElementProvider} subject to validate (must not be
     *        <code>null</code>)
     * @param errorMessage
     *        the error message to use in the {@link IValidity} produced when
     *        the selection is invalid, or <code>null</code> for no error
     *        message
     */
    public ElementProviderValidator(final ElementProvider subject, final String errorMessage) {
        this(subject, NumericConstraint.ONE_OR_MORE, errorMessage);
    }

    /**
     * Creates a new {@link ElementProviderValidator}. The validator validates
     * that the specified {@link ElementProvider}'s element set has a number of
     * elements that passes the specified {@link NumericConstraint}. If the
     * element provider is invalid, no error message is contained in the
     * {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link ElementProvider} subject to validate (must not be
     *        <code>null</code>)
     * @param numericConstraint
     *        the {@link NumericConstraint} that controls whether a checked
     *        element set is considered valid (must not be <code>null</code>)
     */
    public ElementProviderValidator(final ElementProvider subject, final NumericConstraint numericConstraint) {
        this(subject, numericConstraint, null);
    }

    /**
     * Creates a new {@link ElementProviderValidator}. The validator validates
     * that the specified {@link ElementProvider}'s element set has a number of
     * elements that passes the specified {@link NumericConstraint}. If the
     * element provider is invalid, the specified error message is used in the
     * {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link ElementProvider} subject to validate (must not be
     *        <code>null</code>)
     * @param numericConstraint
     *        the {@link NumericConstraint} that controls whether a selection is
     *        considered valid (must not be <code>null</code>)
     * @param errorMessage
     *        the error message to use in the {@link IValidity} produced when
     *        the selection is invalid, or <code>null</code> for no error
     *        message
     */
    public ElementProviderValidator(
        final ElementProvider subject,
        final NumericConstraint numericConstraint,
        final String errorMessage) {
        super(subject);

        Check.notNull(numericConstraint, "numericConstraint"); //$NON-NLS-1$
        this.numericConstraint = numericConstraint;
        this.errorMessage = errorMessage;

        validate();
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.valid.
     * AbstractElementProviderValidator#computeValidity(java.lang.Object[])
     */
    @Override
    protected IValidity computeValidity(final Object[] elements) {
        final int size = elements.length;
        return numericConstraint.passes(size) ? Validity.VALID : Validity.invalid(errorMessage);
    }
}
