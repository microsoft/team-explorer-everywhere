// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import java.util.Locale;

/**
 * <p>
 * An {@link IValidationMessage} is a component of validation state as
 * represented by an {@link IValidity}. Each {@link IValidationMessage} has a
 * {@link String} message an a {@link Severity}.
 * </p>
 *
 * <p>
 * Two {@link IValidationMessage}s can be compared for equality by using the
 * {@link ValidationUtils#validationMessagesEqual(IValidationMessage, IValidationMessage)}
 * method. {@link IValidationMessage} implementations should be immutable.
 * </p>
 *
 * <p>
 * The {@link ValidationMessage} class provides a standard implementation of the
 * {@link IValidationMessage} interface.
 * </p>
 *
 * @see IValidity
 * @see ValidationUtils
 * @see ValidationMessage
 */
public interface IValidationMessage {
    /**
     * @return this {@link IValidationMessage}'s {@link String} message (may be
     *         <code>null</code>)
     */
    public String getMessage();

    /**
     * Obtains a localized version of this {@link IValidationMessage}'s
     * {@link String} message (optional operation). If this
     * {@link IValidationMessage} implementation does not support this
     * operation, this method returns the same result as {@link #getMessage()}.
     *
     * @param locale
     *        a {@link Locale} to obtain the message for or <code>null</code> to
     *        use the default {@link Locale}
     * @return a localized message
     */
    public String getLocalizedMessage(Locale locale);

    /**
     * @return the {@link Severity} of this {@link IValidationMessage} (never
     *         <code>null</code>)
     */
    public Severity getSeverity();
}
