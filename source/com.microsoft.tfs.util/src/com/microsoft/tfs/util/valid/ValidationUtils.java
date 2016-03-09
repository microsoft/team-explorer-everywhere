// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Messages;

/**
 * {@link ValidationUtils} contains static utility methods for use with the
 * validation framework.
 */
public class ValidationUtils {
    /**
     * Compares to {@link IValidity}s for equality. Two {@link IValidity}s are
     * considered equal if they have the same {@link Severity}s and the same
     * {@link IValidationMessage}s.
     *
     * @param v1
     *        the first {@link IValidity} (must not be <code>null</code>)
     * @param v2
     *        the second {@link IValidity} (must not be <code>null</code>)
     * @return <code>true</code> if the two {@link IValidity}s are equal
     */
    public static boolean validitysEqual(final IValidity v1, final IValidity v2) {
        Check.notNull(v1, "v1"); //$NON-NLS-1$
        Check.notNull(v2, "v2"); //$NON-NLS-1$

        if (v1 == v2) {
            return true;
        }

        if (v1.getSeverity() != v2.getSeverity()) {
            return false;
        }

        final IValidationMessage[] messages1 = v1.getMessages();
        final IValidationMessage[] messages2 = v2.getMessages();

        if (messages1.length != messages2.length) {
            return false;
        }

        for (int i = 0; i < messages1.length; i++) {
            if (messages1[i] == null && messages2[i] == null) {
                continue;
            }

            if ((messages1[i] == null && messages2[i] != null) || (messages1[i] != null && messages2[i] == null)) {
                return false;
            }

            if (!validationMessagesEqual(messages1[i], messages2[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares to {@link IValidationMessage}s for equality. Two
     * {@link IValidationMessage}s are considered equal if they have the same
     * {@link Severity} and the same {@link String} message.
     *
     * @param m1
     *        the first {@link IValidationMessage} (must not be
     *        <code>null</code>)
     * @param m2
     *        the second {@link IValidationMessage} (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the two {@link IValidationMessage}s are
     *         equal
     */
    public static boolean validationMessagesEqual(final IValidationMessage m1, final IValidationMessage m2) {
        Check.notNull(m1, "m1"); //$NON-NLS-1$
        Check.notNull(m2, "m2"); //$NON-NLS-1$

        if (m1 == m2) {
            return true;
        }

        if (m1.getSeverity() != m2.getSeverity()) {
            return false;
        }

        if (m1.getMessage() == null) {
            return m2.getMessage() == null;
        }

        return m1.getMessage().equals(m2.getMessage());
    }

    /**
     * Throws an {@link IllegalArgumentException} is the specified
     * {@link Validator} currently has an invalid {@link IValidity}. This method
     * is intended to help enforce API contracts.
     *
     * @param validator
     *        the {@link Validator} to verify (must not be <code>null</code>)
     */
    public static void throwIfInvalid(final Validator validator) {
        Check.notNull(validator, "validator"); //$NON-NLS-1$

        final IValidity validity = validator.getValidity();

        if (validity.isValid()) {
            return;
        }

        final IValidationMessage m = validity.getFirstMessage();
        final String className = validator.getClass().getName();
        String message;

        if (m != null) {
            final String messageFormat = Messages.getString("ValidationUtils.ValidationErrorWithMessageFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, m.getMessage(), className);
        } else {
            final String messageFormat = Messages.getString("ValidationUtils.ValidationErrorNoMessageFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, className);
        }

        throw new IllegalArgumentException(message);
    }

    /**
     * Combines some number of {@link IValidity}s, producing a composite
     * {@link IValidity} that represents the combined validation state.
     *
     * @param validitys
     *        the {@link IValidity}s to combine (must not be <code>null</code>,
     *        must have at least one element, and must not contain any
     *        <code>null</code> elements)
     * @return a composite {@link IValidity} (never <code>null</code>)
     */
    public static IValidity combineValiditys(final IValidity[] validitys) {
        Check.notNull(validitys, "validitys"); //$NON-NLS-1$

        if (validitys.length == 0) {
            throw new IllegalArgumentException("validitys length is 0"); //$NON-NLS-1$
        }

        int validitysWithMessagesCount = 0;

        for (int i = 0; i < validitys.length; i++) {
            if (validitys[i] == null) {
                throw new IllegalArgumentException("validitys " + i + " is null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (validitys[i].getMessages().length > 0) {
                ++validitysWithMessagesCount;
            }
        }

        if (validitysWithMessagesCount == 0) {
            Severity maxSeverity = null;
            for (int i = 0; i < validitys.length; i++) {
                if (maxSeverity == null || maxSeverity.getPriority() < validitys[i].getSeverity().getPriority()) {
                    maxSeverity = validitys[i].getSeverity();
                }
            }

            return new Validity(maxSeverity);
        }

        final List validationMessages = new ArrayList();

        for (int i = 0; i < validitys.length; i++) {
            final IValidationMessage[] currentMessages = validitys[i].getMessages();

            if (currentMessages != null) {
                validationMessages.addAll(Arrays.asList(currentMessages));
            } else {
                final IValidationMessage syntheticMessage = new ValidationMessage(validitys[i].getSeverity());

                validationMessages.add(syntheticMessage);
            }
        }

        final IValidationMessage[] messagesArray =
            (IValidationMessage[]) validationMessages.toArray(new IValidationMessage[validationMessages.size()]);

        return new Validity(messagesArray);
    }
}
