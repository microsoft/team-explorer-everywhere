// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * An abstract base class used for implementing the {@link ValidatorBinding}
 * interface. Subclasses need only implement the abstract
 * {@link #update(IValidity)} method.
 *
 * @see ValidatorBinding
 */
public abstract class AbstractValidatorBinding implements ValidatorBinding {
    /**
     * The {@link ValidityChangedListener} that tracks changes to the bound
     * {@link Validator}.
     */
    private final ValidityChangedListener validityChangedListener = new ValidityChangedListener();

    /**
     * The bound {@link Validator} or <code>null</code>.
     */
    private Validator validator;

    /**
     * A lock that must be acquired when the {@link #validator} field is read or
     * modified.
     */
    private final Object validatorLock = new Object();

    /**
     * <p>
     * Subclasses must override this method to update in response to an
     * {@link IValidity}. Subclasses perform whatever subclass-specific
     * processing is necessary given an {@link IValidity} instance. For example,
     * a {@link ValidatorBinding} that enables and disables a GUI button would
     * enable the button if the {@link IValidity} was valid and disable it if
     * the {@link IValidity} was invalid.
     * </p>
     *
     * <p>
     * Implementors of this method must handle the case where the argument is
     * <code>null</code>. This happens whenever a {@link ValidatorBinding} is
     * unbound from a {@link Validator} but is not re-bound to a new
     * {@link Validator}. This can happen either when {@link #unbind()} is
     * called or when {@link #bind(Validator)} is called with a
     * <code>null</code> argument. When the {@link IValidity} passed to this
     * argument is <code>null</code>, implementation should undo any actions
     * they have previously taken, returning objects affected by the
     * {@link ValidatorBinding} to their default state. Keep in mind that a
     * common use case for unbinding a {@link ValidatorBinding} is to
     * temporarily "disable" a {@link Validator}.
     * </p>
     *
     * @param validity
     *        the {@link IValidity} to update with, or <code>null</code> if this
     *        {@link ValidatorBinding} has been unbound from a {@link Validator}
     */
    protected abstract void update(IValidity validity);

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.valid.ValidatorBinding#bind(com.microsoft.tfs.
     * util.valid.Validatable)
     */
    @Override
    public void bind(final Validatable validatable) {
        if (validatable == null) {
            bind((Validator) null);
        } else {
            bind(validatable.getValidator());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.valid.ValidatorBinding#bind(com.microsoft.tfs.
     * util.valid.Validator)
     */
    @Override
    public void bind(final Validator newValidator) {
        IValidity validity = null;

        synchronized (validatorLock) {
            if (validator != null) {
                validator.removeValidityChangedListener(validityChangedListener);
            }
            validator = newValidator;
            if (validator != null) {
                validator.addValidityChangedListener(validityChangedListener);
                validity = validator.getValidity();
            }
        }

        update(validity);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.ValidatorBinding#getBoundValidator()
     */
    @Override
    public Validator getBoundValidator() {
        return validator;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.ValidatorBinding#unbind()
     */
    @Override
    public void unbind() {
        synchronized (validatorLock) {
            if (validator != null) {
                validator.removeValidityChangedListener(validityChangedListener);
                validator = null;
            }
        }

        update(null);
    }

    private class ValidityChangedListener implements com.microsoft.tfs.util.valid.ValidityChangedListener {
        @Override
        public void validityChanged(final ValidityChangedEvent event) {
            update(event.getValidity());
        }
    }
}
