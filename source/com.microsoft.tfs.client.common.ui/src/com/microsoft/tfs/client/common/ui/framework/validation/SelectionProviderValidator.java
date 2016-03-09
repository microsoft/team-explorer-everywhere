// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.Validity;

/**
 * <p>
 * A {@link SelectionProviderValidator} is a concrete {@link Validator}
 * implementation that has an {@link ISelectionProvider} as a validation
 * subject. It is used to validate that an {@link ISelectionProvider} contains a
 * certain number of selected elements. Its error message can be customized or
 * omitted.
 * </p>
 *
 * <p>
 * To perform customized validation of an {@link ISelectionProvider} subject,
 * override {@link AbstractSelectionProviderValidator}. This class is not
 * designed to be subclassed.
 * </p>
 *
 * @see Validator
 * @see ISelectionProvider
 * @see AbstractSelectionProviderValidator
 */
public final class SelectionProviderValidator extends AbstractSelectionProviderValidator {
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
     * Creates a new {@link SelectionProviderValidator} that validates that the
     * specified {@link ISelectionProvider}'s {@link ISelection} is a non-empty
     * {@link IStructuredSelection}. If the selection provider is invalid, no
     * error message is contained in the {@link IValidity} returned by this
     * {@link Validator}.
     *
     * @param subject
     *        the {@link ISelectionProvider} subject to validate (must not be
     *        <code>null</code>)
     */
    public SelectionProviderValidator(final ISelectionProvider subject) {
        this(subject, NumericConstraint.ONE_OR_MORE, null);
    }

    /**
     * Creates a new {@link SelectionProviderValidator} that validates that the
     * {@link ISelectionProvider}'s {@link ISelection} is a non-empty
     * {@link IStructuredSelection}. If the selection provider is invalid, the
     * specified error message is used in the {@link IValidity} returned by this
     * {@link Validator}.
     *
     * @param subject
     *        the {@link ISelectionProvider} subject to validate (must not be
     *        <code>null</code>)
     * @param errorMessage
     *        the error message to use in the {@link IValidity} produced when
     *        the selection is invalid, or <code>null</code> for no error
     *        message
     */
    public SelectionProviderValidator(final ISelectionProvider subject, final String errorMessage) {
        this(subject, NumericConstraint.ONE_OR_MORE, errorMessage);
    }

    /**
     * Creates a new {@link SelectionProviderValidator}. The validator validates
     * that the specified {@link ISelectionProvider}'s {@link ISelection} is an
     * {@link IStructuredSelection} that has a number of elements that passes
     * the specified {@link NumericConstraint}. If the selection provider is
     * invalid, no error message is contained in the {@link IValidity} returned
     * by this {@link Validator}.
     *
     * @param subject
     *        the {@link ISelectionProvider} subject to validate (must not be
     *        <code>null</code>)
     * @param numericConstraint
     *        the {@link NumericConstraint} that controls whether a selection is
     *        considered valid (must not be <code>null</code>)
     */
    public SelectionProviderValidator(final ISelectionProvider subject, final NumericConstraint numericConstraint) {
        this(subject, numericConstraint, null);
    }

    /**
     * Creates a new {@link SelectionProviderValidator}. The validator validates
     * that the specified {@link ISelectionProvider}'s {@link ISelection} is an
     * {@link IStructuredSelection} that has a number of elements that passes
     * the specified {@link NumericConstraint}. If the selection provider is
     * invalid, the specified error message is used in the {@link IValidity}
     * returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link ISelectionProvider} subject to validate (must not be
     *        <code>null</code>)
     * @param numericConstraint
     *        the {@link NumericConstraint} that controls whether a selection is
     *        considered valid (must not be <code>null</code>)
     * @param errorMessage
     *        the error message to use in the {@link IValidity} produced when
     *        the selection is invalid, or <code>null</code> for no error
     *        message
     */
    public SelectionProviderValidator(
        final ISelectionProvider subject,
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
     * AbstractSelectionProviderValidator
     * #computeValidity(org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected IValidity computeValidity(final ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            final int size = structuredSelection.size();
            return numericConstraint.passes(size) ? Validity.VALID : Validity.invalid(errorMessage);
        }

        return Validity.invalid(errorMessage);
    }
}
