// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * An abstract base class useful for writing {@link Validator} implementations.
 * </p>
 *
 * <p>
 * Subclasses will typically take in a subclass-specific subject in the subclass
 * constructor and call this base class constructor with that subject.
 * Subclasses will then perform the initial validation of the subject and call
 * {@link #setValidity(IValidity)} to record the result.
 * </p>
 *
 * <p>
 * In the subclass constructor, subclasses should register any listeners with
 * the subject necessary to track changes to the state of the subject. When the
 * subject changes, the subclass should re-validate the subject and call
 * {@link #setValidity(IValidity)} to record the result.
 * </p>
 *
 * <p>
 * If the subclass registers listeners with the subject, the subclass should
 * override the {@link #dispose()} method to remove those listeners when this
 * {@link Validator} is disposed.
 * </p>
 *
 * <p>
 * This {@link Validator} implementation is threadsafe.
 * </p>
 */
public abstract class AbstractValidator implements Validator {
    /**
     * The {@link ValidityChangedListener}s attached to this {@link Validator}.
     */
    private final SingleListenerFacade validityChangedListeners =
        new SingleListenerFacade(ValidityChangedListener.class);

    /**
     * The subject of this {@link Validator} (see {@link #getSubject()}).
     */
    private final Object subject;

    /**
     * The current {@link IValidity} (never <code>null</code>).
     */
    private IValidity validity;

    /**
     * A lock that is acquired to modify or read the {@link #validity} field.
     */
    private final Object validityLock = new Object();

    /**
     * <code>true</code> if we are currently in suspended validation mode.
     */
    private boolean suspend;

    /**
     * The {@link IValidity} that we report through {@link #getValidity()} while
     * in suspended validation mode (never <code>null</code> while in suspended
     * mode).
     */
    private IValidity suspendedValidity;

    /**
     * A lock that is acquired to modify or read the {@link #suspend} and
     * {@link #suspendedValidity} fields.
     */
    private final Object suspendLock = new Object();

    /**
     * Creates a new {@link AbstractValidator} that validates the specified
     * subject. The initial {@link IValidity} held by this validator represents
     * a valid state. Subclasses should perform initial validation and then call
     * {@link #setValidity(IValidity)} to record the result.
     *
     * @param subject
     *        the subject being validated by this {@link Validator} (must not be
     *        <code>null</code>)
     */
    protected AbstractValidator(final Object subject) {
        Check.notNull(subject, "subject"); //$NON-NLS-1$

        this.subject = subject;

        synchronized (validityLock) {
            validity = Validity.VALID;
        }

        synchronized (suspendLock) {
            suspend = false;
        }
    }

    /**
     * Subclasses should override to perform subclass-specific cleanup. The base
     * class implementation of this {@link Validator} method does nothing.
     * Subclasses should remove any registered listeners from the subject.
     */
    @Override
    public void dispose() {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.Validator#getSubject()
     */
    @Override
    public Object getSubject() {
        return subject;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.valid.Validator#addValidityChangedListener(com
     * .microsoft.tfs.util.valid.ValidityChangedListener)
     */
    @Override
    public void addValidityChangedListener(final ValidityChangedListener listener) {
        validityChangedListeners.addListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.valid.Validator#removeValidityChangedListener(
     * com.microsoft.tfs.util.valid.ValidityChangedListener)
     */
    @Override
    public void removeValidityChangedListener(final ValidityChangedListener listener) {
        validityChangedListeners.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.Validator#getValidity()
     */
    @Override
    public IValidity getValidity() {
        synchronized (suspendLock) {
            if (suspend) {
                return suspendedValidity;
            }
        }

        synchronized (validityLock) {
            return validity;
        }
    }

    /**
     * Called by subclasses to record the result of validating the subject.
     * Typically, subclasses will call this method once in the subclass
     * constructor to record the initial validation state. After that, the
     * subclasses should call this method whenever the validation state has been
     * recomputed due to a change in the subject.
     *
     * @param newValidity
     *        the new {@link IValidity} computed by this {@link Validator} (must
     *        not be <code>null</code>)
     * @return <code>true</code> if the new {@link IValidity} was different than
     *         the current {@link IValidity}
     */
    protected boolean setValidity(final IValidity newValidity) {
        Check.notNull(newValidity, "newValidity"); //$NON-NLS-1$

        synchronized (this) {
            if (ValidationUtils.validitysEqual(newValidity, validity)) {
                return false;
            }

            validity = newValidity;
        }

        broadcastValidity();

        return true;
    }

    /**
     * Called to broadcast the current validation state to any registered
     * {@link ValidityChangedListener}s. This method is not normally called by
     * subclasses.
     */
    protected void broadcastValidity() {
        synchronized (suspendLock) {
            if (suspend) {
                return;
            }
        }

        internalBroadcastValidity();
    }

    /**
     * Called to suspend the broadcasting of {@link ValidityChangedEvent}s. This
     * is used when the validation state may change rapidly over a short period
     * of time to avoid flooding listeners with many events. This is usually
     * matched by a subsequent call to {@link #resumeValidation()}.
     */
    public void suspendValidation() {
        suspendValidation(false);
    }

    /**
     * Called to suspend the broadcasting of {@link ValidityChangedEvent}s. If
     * the reset parameter is <code>true</code>, a new {@link IValidity}
     * representing a valid state will be broadcast before suspending further
     * broadcasts. This is used to temporarily disable this {@link Validator}.
     * This is usually matched by a subsequent call to
     * {@link #resumeValidation()}.
     *
     * @param reset
     *        <code>true</code> to reset this validator's validation state to
     *        valid while validation is suspended
     */
    public void suspendValidation(final boolean reset) {
        // do we need to update subscribers as a result of this suspension?
        boolean needBroadcast = false;

        // outside the suspendLock as this acquires a different lock
        final IValidity realValidity = getValidity();

        synchronized (suspendLock) {
            if (!suspend) {
                suspend = true;

                if (reset) {
                    suspendedValidity = Validity.VALID;
                    needBroadcast = !ValidationUtils.validitysEqual(suspendedValidity, realValidity);
                } else {
                    suspendedValidity = realValidity;
                }
            } else {
                if (reset) {
                    final IValidity newSuspendedValidity = Validity.VALID;
                    needBroadcast = !ValidationUtils.validitysEqual(newSuspendedValidity, suspendedValidity);
                    suspendedValidity = newSuspendedValidity;
                }
            }
        }

        if (needBroadcast) {
            internalBroadcastValidity();
        }
    }

    /**
     * Called to resume the broadcasting of {@link ValidityChangedEvent}s, after
     * previously calling {@link #suspendValidation()} or
     * {@link #suspendValidation(boolean)}. Calling this method will immediately
     * send a {@link ValidityChangedEvent} to all registered listeners to catch
     * them up with validation state changes that may have happened since
     * broadcasting was suspended.
     */
    public void resumeValidation() {
        boolean needToBroadcast = false;

        IValidity currentValidity;
        synchronized (validityLock) {
            currentValidity = validity;
        }

        synchronized (suspendLock) {
            if (!suspend) {
                return;
            }

            suspend = false;

            needToBroadcast = !ValidationUtils.validitysEqual(currentValidity, suspendedValidity);
        }

        if (needToBroadcast) {
            internalBroadcastValidity();
        }
    }

    /**
     * A convenience method that can be called by a subclass to set the
     * validation state of this {@link Validator}. This method will set a new
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
    protected boolean setValid(final boolean valid) {
        return setValidity(valid ? Validity.VALID : Validity.INVALID);
    }

    /**
     * A convenience method that can be called by a subclass to set the
     * validation state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have no
     * {@link IValidationMessage}s and will have a severity of
     * {@link Severity#OK}.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    protected boolean setValid() {
        return setValid(true);
    }

    /**
     * A convenience method that can be called by a subclass to set the
     * validation state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have no
     * {@link IValidationMessage}s and will have a severity of
     * {@link Severity#ERROR}.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    protected boolean setInvalid() {
        return setValid(false);
    }

    /**
     * A convenience method that can be called by a subclass to set the
     * validation state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have a single
     * {@link IValidationMessage} containing the specified {@link String}
     * message and a severity of {@link Severity#ERROR}.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    protected boolean setInvalid(final String errorMessage) {
        return setValidity(Validity.invalid(errorMessage));
    }

    /**
     * A convenience method that can be called by a subclass to set the
     * validation state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have a single
     * {@link IValidationMessage} as specified by the argument to this method.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    protected boolean setValidationMessage(final IValidationMessage message) {
        return setValidity(new Validity(message));
    }

    /**
     * A convenience method that can be called by a subclass to set the
     * validation state of this {@link Validator}. This method will set a new
     * {@link IValidity} on this validator. The new validity will have the
     * {@link IValidationMessage}s as specified by the argument to this method.
     *
     * @return <code>true</code> if the validation state of this
     *         {@link Validator} changed as a result of the call
     */
    protected boolean setValidationMessages(final IValidationMessage[] messages) {
        return setValidity(new Validity(messages));
    }

    /**
     * Broadcasts a {@link ValidityChangedEvent} to all currently registered
     * {@link ValidityChangedListener}s. This method always broadcasts - it does
     * not check whether we are in suspended mode first.
     */
    private void internalBroadcastValidity() {
        final ValidityChangedEvent event = new ValidityChangedEvent(this);

        final ValidityChangedListener listener = (ValidityChangedListener) validityChangedListeners.getListener();
        listener.validityChanged(event);
    }
}