// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import java.util.Locale;

import com.microsoft.tfs.util.Check;

/**
 * A standard implementation of the {@link IValidationMessage} interface that
 * does not support localization.
 *
 * @see IValidationMessage
 */
public class ValidationMessage implements IValidationMessage {
    private final String message;
    private final Severity severity;

    /**
     * Constructs a new {@link ValidationMessage} with the specified message and
     * a severity of {@link Severity#ERROR}.
     *
     * @param message
     *        the {@link String} message (can be <code>null</code>)
     */
    public ValidationMessage(final String message) {
        this(message, Severity.ERROR);
    }

    /**
     * Constructs a new {@link ValidationMessage} with no message and the
     * specified severity.
     *
     * @param severity
     *        the {@link Severity} (must not be <code>null</code>)
     */
    public ValidationMessage(final Severity severity) {
        this(null, severity);
    }

    /**
     * Constructs a new {@link ValidationMessage} with the specified message and
     * severity.
     *
     * @param message
     *        the {@link String} message (can be <code>null</code>)
     * @param severity
     *        the {@link Severity} (must not be <code>null</code>)
     */
    public ValidationMessage(final String message, final Severity severity) {
        Check.notNull(severity, "severity"); //$NON-NLS-1$

        this.message = message;
        this.severity = severity;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.valid.IValidationMessage#getLocalizedMessage(java
     * .util.Locale)
     */
    @Override
    public String getLocalizedMessage(final Locale locale) {
        return message;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.IValidationMessage#getMessage()
     */
    @Override
    public String getMessage() {
        return message;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.IValidationMessage#getSeverity()
     */
    @Override
    public Severity getSeverity() {
        return severity;
    }
}
