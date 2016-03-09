// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.swt.widgets.Combo;

import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.ValidationMessage;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.Validity;

/**
 * <p>
 * A {@link ComboControlValidator} is a concrete {@link Validator}
 * implementation that has a {@link Combo} control as a validation subject. It
 * is used to validate that a {@link Combo} control's text is not empty. Its
 * error message can be customized or omitted.
 * </p>
 *
 * <p>
 * To perform customized validation of a {@link Combo} subject, override
 * {@link AbstractTextControlValidator}. This class is not designed to be
 * subclassed.
 * </p>
 *
 * @see Validator
 * @see Combo
 * @see AbstractTextControlValidator
 */
public final class ComboControlValidator extends AbstractComboControlValidator {
    /**
     * The error message supplied in the constructor (can be <code>null</code>).
     */
    private final String errorMessage;

    /**
     * The {@link Severity} supplied in the constructor (never <code>null</code>
     * ).
     */
    private final Severity severity;

    /**
     * Creates a new {@link ComboControlValidator} that validates that the
     * specified combom control is non-empty. If the combo control is invalid,
     * no error message and a default severity ({@link Severity#ERROR}) are used
     * in the {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link Combo} control subject to validate (must not be
     *        <code>null</code>)
     */
    public ComboControlValidator(final Combo subject) {
        this(subject, null, null);
    }

    /**
     * Creates a new {@link ComboControlValidator} that validates that the
     * specified combo control is non-empty. If the combo control is invalid,
     * the specified error message and a default severity (
     * {@link Severity#ERROR}) are used in the {@link IValidity} returned by
     * this {@link Validator}.
     *
     * @param subject
     *        the {@link Combo} control subject to validate (must not be
     *        <code>null</code>)
     * @param errorMessage
     *        the {@link IValidity} error message to use, or <code>null</code>
     *        for no error message
     */
    public ComboControlValidator(final Combo subject, final String errorMessage) {
        this(subject, errorMessage, null);
    }

    /**
     * Creates a new {@link ComboControlValidator} that validates that the
     * specified combo control is non-empty. If the combo control is invalid, no
     * error message and the specified severity are used in the
     * {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link Combo} control subject to validate (must not be
     *        <code>null</code>)
     * @param severity
     *        the {@link Severity} to use, or <code>null</code> for the default
     *        severity ({@link Severity#ERROR})
     */
    public ComboControlValidator(final Combo subject, final Severity severity) {
        this(subject, null, severity);
    }

    /**
     * Creates a new {@link ComboControlValidator} that validates that the
     * specified combo control is non-empty. If the combo control is invalid,
     * the specified error message and severity are used in the
     * {@link IValidity} returned by this {@link Validator}.
     *
     * @param subject
     *        the {@link Combo} control subject to validate (must not be
     *        <code>null</code>)
     * @param errorMessage
     *        the {@link IValidity} error message to use, or <code>null</code>
     *        for no error message
     * @param severity
     *        the {@link Severity} to use, or <code>null</code> for the default
     *        severity ({@link Severity#ERROR})
     */
    public ComboControlValidator(final Combo subject, final String errorMessage, final Severity severity) {
        super(subject);
        this.errorMessage = errorMessage;
        this.severity = severity == null ? Severity.ERROR : severity;
        validate();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.valid.
     * AbstractTextControlValidator #computeValidity(java.lang.String)
     */
    @Override
    protected IValidity computeValidity(final String text) {
        if (text.trim().length() > 0) {
            return Validity.VALID;
        }

        if (errorMessage == null) {
            return new Validity(severity);
        }

        final IValidationMessage message = new ValidationMessage(errorMessage, severity);
        return new Validity(message);
    }
}