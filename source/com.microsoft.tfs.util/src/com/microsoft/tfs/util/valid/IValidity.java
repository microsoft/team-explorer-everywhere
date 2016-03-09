// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * <p>
 * An {@link IValidity} is a status object that represents the validation state
 * of some object. {@link IValidity}s are often produced by a {@link Validator}.
 * </p>
 *
 * <p>
 * An {@link IValidity} is composed of 0 or more {@link IValidationMessage}s.
 * Each message has a {@link Severity}. In addition, the {@link IValidity}
 * itself has a overall severity, which is defined as being the highest priority
 * severity of any of the messages (or a {@link Severity} defined at
 * construction time if the {@link IValidity} has no messages).
 * </p>
 *
 * <p>
 * It is acceptable for an {@link IValidity} implementation to have 0
 * {@link IValidationMessage}s. In this case, the {@link IValidity} expresses a
 * severity only, with no additional information that the messages would
 * normally provide.
 * </p>
 *
 * <p>
 * Two {@link IValidity}s can be compared for equality by using the
 * {@link ValidationUtils#validitysEqual(IValidity, IValidity)} method.
 * {@link IValidity} instances should be immutable.
 * </p>
 *
 * <p>
 * The {@link Validity} class provides a standard implementation of the
 * {@link IValidity} interface.
 * </p>
 *
 * @see Validator
 * @see IValidationMessage
 * @see Severity
 * @see ValidationUtils
 * @see Validity
 */
public interface IValidity {
    /**
     * A convenience method that returns <code>true</code> if this
     * {@link IValidity}'s {@link Severity} is not {@link Severity#ERROR}.
     *
     * @return <code>true</code> if the {@link Severity} of this
     *         {@link IValidity} is not {@link Severity#ERROR}, or
     *         <code>false</code> otherwise
     */
    public boolean isValid();

    /**
     * Obtains the {@link Severity} of this {@link IValidity}. If this
     * {@link IValidity} has one or more {@link IValidationMessage}s, the
     * {@link Severity} returned by this method is the highest-priority
     * {@link Severity} of any of the {@link IValidationMessage}s. Otherwise,
     * the {@link Severity} can be anything and represents the primary state of
     * this {@link IValidity}.
     *
     * @return the {@link Severity} of this {@link IValidity} (never
     *         <code>null</code>)
     */
    public Severity getSeverity();

    /**
     * Obtains the first {@link IValidationMessage}. The message returned by
     * this method is the first {@link IValidationMessage} that has the highest
     * priority of all of the messages.
     *
     * @return the first of the highest-priority {@link IValidationMessage}s, or
     *         <code>null</code> if there are no {@link IValidationMessage}s in
     *         this {@link IValidity}
     */
    public IValidationMessage getFirstMessage();

    /**
     * Obtains all of the {@link IValidationMessage}s of this {@link IValidity}.
     * The returned array is "safe" in that modifications to the returned array
     * do not affect this {@link IValidity}.
     *
     * @return all of the {@link IValidationMessage}s, never <code>null</code>
     *         (will be a 0-length array if there are no
     *         {@link IValidationMessage}s in this {@link IValidity})
     */
    public IValidationMessage[] getMessages();
}
