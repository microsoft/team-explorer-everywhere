// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * <p>
 * A {@link ValidatorHelper} is a concrete {@link Validator} implementation. It
 * is designed to be embedded in objects which perform internal validation
 * (often, these objects will implement the {@link Validatable} interface).
 * </p>
 *
 * <p>
 * {@link ValidatorHelper} does not actually contain validation logic itself.
 * Instead, some external object sets the validation state of a
 * {@link ValidatorHelper}. A {@link ValidatorHelper} is initially valid.
 * Clients must call the {@link #setValidity(IValidity)} or one of the
 * convenience methods to set a new validation state.
 * </p>
 *
 * @see Validator
 * @see Validatable
 */
public class ValidatorHelper extends AbstractValidator {
    /**
     * Creates a new {@link ValidatorHelper} that validates the specified
     * subject.
     *
     * @param subject
     *        the subject to validate (must not be <code>null</code>)
     */
    public ValidatorHelper(final Object subject) {
        super(subject);
    }

    /**
     * Called to set a new validation state on this {@link Validator}.
     * Typically, client will call this method once at creation time to record
     * the initial validation state. After that, the host should call this
     * method whenever the validation state has been recomputed due to a change
     * in the subject.
     *
     * @param newValidity
     *        the new {@link IValidity} (must not be <code>null</code>)
     * @return <code>true</code> if the new {@link IValidity} was different than
     *         the current {@link IValidity}
     */
    @Override
    public boolean setValidity(final IValidity newValidity) {
        return super.setValidity(newValidity);
    }

    /**
     * A convenience method that can be called by a client to set the validation
     * state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have no
     * {@link IValidationMessage}s and will have a severity of
     * {@link Severity#OK} if the parameter is <code>true</code> or
     * {@link Severity#ERROR} if the parameter is <code>false</code>.
     *
     * @param valid
     *        controls the new validation state
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    @Override
    public boolean setValid(final boolean valid) {
        return super.setValid(valid);
    }

    /**
     * A convenience method that can be called by a client to set the validation
     * state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have no
     * {@link IValidationMessage}s and will have a severity of
     * {@link Severity#OK}.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    @Override
    public boolean setValid() {
        return super.setValid();
    }

    /**
     * A convenience method that can be called by a client to set the validation
     * state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have no
     * {@link IValidationMessage}s and will have a severity of
     * {@link Severity#ERROR}.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    @Override
    public boolean setInvalid() {
        return super.setInvalid();
    }

    /**
     * A convenience method that can be called by a client to set the validation
     * state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have a single
     * {@link IValidationMessage} containing the specified {@link String}
     * message and a severity of {@link Severity#ERROR}.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    @Override
    public boolean setInvalid(final String errorMessage) {
        return super.setInvalid(errorMessage);
    }

    /**
     * A convenience method that can be called by a client to set the validation
     * state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have a single
     * {@link IValidationMessage} as specified by the argument to this method.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    @Override
    public boolean setValidationMessage(final IValidationMessage message) {
        return super.setValidationMessage(message);
    }

    /**
     * A convenience method that can be called by a client to set the validation
     * state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have the
     * {@link IValidationMessage}s as specified by the argument to this method.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    @Override
    public boolean setValidationMessages(final IValidationMessage[] messages) {
        return super.setValidationMessages(messages);
    }

    /**
     * Called to broadcast the current validation state to any registered
     * {@link ValidityChangedListener}s. This method is not normally called by
     * clients.
     */
    @Override
    public void broadcastValidity() {
        super.broadcastValidity();
    }
}
