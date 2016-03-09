// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A standard implementation of the {@link IValidity} interface.
 * </p>
 *
 * <p>
 * {@link Validity} instances are immutable and threadsafe.
 * </p>
 *
 * @see IValidity
 */
public class Validity implements IValidity {
    /**
     * A global {@link IValidity} implementation that represents a valid state.
     * This {@link IValidity} has a severity of {@link Severity#OK} and no
     * messages.
     */
    public static final Validity VALID = new Validity(Severity.OK);

    /**
     * A global {@link IValidity} implementation that represents an invalid
     * state. This {@link IValidity} has a severity of {@link Severity#ERROR}
     * and no messages.
     */
    public static final Validity INVALID = new Validity(Severity.ERROR);

    /**
     * Creates a new {@link Validity} instance that represents an invalid state
     * with the specified error message. If the specified error message is
     * <code>null</code>, {@link #INVALID} is returned. Otherwise, a new
     * {@link IValidity} is returned that has a single
     * {@link IValidationMessage} with the specified {@link String} message and
     * a severity of {@link Severity#ERROR}.
     *
     * @param errorMessage
     *        the error message or <code>null</code>
     * @return an {@link Validity} instance as describe above (never
     *         <code>null</code>)
     */
    public static Validity invalid(final String errorMessage) {
        if (errorMessage == null) {
            return INVALID;
        } else {
            final IValidationMessage validationMessage = new ValidationMessage(errorMessage, Severity.ERROR);
            return new Validity(validationMessage);
        }
    }

    private final Severity severity;
    private final IValidationMessage[] messages;

    /**
     * Creates a new {@link Validity} instance with a single
     * {@link IValidationMessage}.
     *
     * @param message
     *        the {@link IValidationMessage} this {@link IValidity} should have
     *        (must not be <code>null</code>)
     */
    public Validity(final IValidationMessage message) {
        this(new IValidationMessage[] {
            message
        });
    }

    /**
     * Creates a new {@link Validity} instance with the specified
     * {@link IValidationMessage}s.
     *
     * @param messages
     *        the {@link IValidationMessage}s this {@link IValidity} should have
     *        (must not be <code>null</code>, must be non-zero length, and must
     *        not contain any <code>null</code> elements)
     */
    public Validity(final IValidationMessage[] messages) {
        Check.notNull(messages, "messages"); //$NON-NLS-1$

        if (messages.length == 0) {
            throw new IllegalArgumentException("messages length is 0"); //$NON-NLS-1$
        }

        this.messages = messages.clone();
        Severity maxSeverity = null;
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] == null) {
                throw new IllegalArgumentException("message " + i + " is null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (maxSeverity == null || messages[i].getSeverity().getPriority() > maxSeverity.getPriority()) {
                maxSeverity = messages[i].getSeverity();
            }
        }
        severity = maxSeverity;
    }

    /**
     * Creates a new {@link Validity} instance with no
     * {@link IValidationMessage}s and the specified {@link Severity}.
     *
     * @param message
     *        the {@link IValidationMessage} this {@link IValidity} should have
     *        (must not be <code>null</code>)
     */
    public Validity(final Severity severity) {
        Check.notNull(severity, "severity"); //$NON-NLS-1$

        this.severity = severity;
        messages = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.IValidity#getFirstMessage()
     */
    @Override
    public IValidationMessage getFirstMessage() {
        if (messages == null) {
            return null;
        }

        for (int i = 0; i < messages.length; i++) {
            if (messages[i].getSeverity() == severity) {
                return messages[i];
            }
        }

        throw new IllegalStateException("unexpected condition"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.IValidity#getMessages()
     */
    @Override
    public IValidationMessage[] getMessages() {
        if (messages == null) {
            return new IValidationMessage[0];
        }

        return messages.clone();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.IValidity#getSeverity()
     */
    @Override
    public Severity getSeverity() {
        return severity;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.IValidity#isValid()
     */
    @Override
    public boolean isValid() {
        return Severity.ERROR != severity;
    }
}
