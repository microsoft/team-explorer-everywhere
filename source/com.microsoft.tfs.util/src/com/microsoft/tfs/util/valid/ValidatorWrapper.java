// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * A very simple wrapper-based {@link Validator}. This class wraps another
 * {@link Validator} and allows clients to specify a custom error message and do
 * things like suspend and resume validation. <b>Important</b>: this class is a
 * very simple wrapper and has not been designed to be a general purpose
 * {@link Validator} wrapper.
 */
public class ValidatorWrapper extends ValidatorHelper {
    /**
     * The wrapped {@link Validator} (never <code>null</code>).
     */
    private final Validator wrappedValidator;

    /**
     * The client-supplied error message (can be <code>null</code>).
     */
    private final String errorMessage;

    /**
     * The {@link ValidityChangedListener} attached to the wrapped
     * {@link Validator} (never <code>null</code>).
     */
    private final ValidityChangedListener validityChangedListener;

    /**
     * Creates a new {@link ValidatorWrapper} to wrap the specified
     * {@link Validator}. The {@link ValidatorWrapper} has no error message.
     *
     * @param wrappedValidator
     *        the {@link Validator} to wrap (must not be <code>null</code>)
     */
    public ValidatorWrapper(final Validator wrappedValidator) {
        this(wrappedValidator, null);
    }

    /**
     * Creates a new {@link ValidatorWrapper} to wrap the specified
     * {@link Validator}. The {@link ValidatorWrapper} has the specified error
     * message.
     *
     * @param wrappedValidator
     *        the {@link Validator} to wrap (must not be <code>null</code>)
     * @param errorMessage
     *        the error message or <code>null</code>
     */
    public ValidatorWrapper(final Validator wrappedValidator, final String errorMessage) {
        super(wrappedValidator.getSubject());

        this.wrappedValidator = wrappedValidator;
        this.errorMessage = errorMessage;

        validityChangedListener = new ValidityChangedListener() {
            @Override
            public void validityChanged(final ValidityChangedEvent event) {
                computeValidity(event.getValidity());
            }
        };

        wrappedValidator.addValidityChangedListener(validityChangedListener);
        computeValidity(wrappedValidator.getValidity());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.AbstractValidator#dispose()
     */
    @Override
    public void dispose() {
        wrappedValidator.removeValidityChangedListener(validityChangedListener);
    }

    private void computeValidity(final IValidity wrappedValidity) {
        final Severity severity = wrappedValidity.getSeverity();

        if (severity == Severity.ERROR) {
            setInvalid(errorMessage);
        } else {
            setValid();
        }
    }
}
