// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link MultiValidator} is a concrete {@link Validator} implementation that
 * composes together multiple sub-validators.
 * </p>
 *
 * <p>
 * A {@link MultiValidator} starts off as initially valid. As sub-validators are
 * added and removed, the overall validation state is recomputed and reported as
 * needed.
 * </p>
 *
 * @see Validator
 */
public class MultiValidator extends AbstractValidator {
    /**
     * The {@link ValidityChangedListener} that will be added to each
     * sub-validator to respond to validity changes in that sub-validator.
     */
    private final ValidityChangedListener subValidatorListener = new SubValidatorListener();

    /**
     * The sub-validators that have been added to this {@link MultiValidator}.
     * This field should be synchronized on whenever the collection needs to be
     * modified or read.
     */
    private final List subValidators = new ArrayList();

    /**
     * Creates a new {@link MultiValidator} for the given subject. The subject
     * is passed in any {@link ValidityChangedEvent}s published by this
     * {@link Validator}.
     *
     * @param subject
     *        the validation subject (must not be <code>null</code>)
     */
    public MultiValidator(final Object subject) {
        super(subject);
    }

    /**
     * Adds a new sub-validator to this {@link MultiValidator}. The equivalent
     * of calling {@link #addValidator(Validator)} with the specified
     * {@link Validatable}'s {@link Validator}.
     *
     * @param v
     *        the {@link Validatable} that supplies the {@link Validator} to add
     *        (must not be <code>null</code>)
     * @return <code>true</code> if this {@link MultiValidator} did not already
     *         contain the specified {@link Validator}
     */
    public boolean addValidatable(final Validatable v) {
        Check.notNull(v, "v"); //$NON-NLS-1$

        return addValidator(v.getValidator());
    }

    /**
     * Adds a new sub-validator to this {@link MultiValidator}. If the overall
     * {@link IValidity} of this {@link MultiValidator} is changed as a result
     * of the new sub-validator, any registered {@link ValidityChangedListener}s
     * are notified before this method completes.
     *
     * @param v
     *        the sub-{@link Validator} to add (must not be <code>null</code>)
     * @return <code>true</code> if this {@link MultiValidator} did not already
     *         contain the specified {@link Validator}
     */
    public boolean addValidator(final Validator v) {
        synchronized (subValidators) {
            if (subValidators.contains(v)) {
                return false;
            }

            subValidators.add(v);
        }

        v.addValidityChangedListener(subValidatorListener);
        recomputeValidity();
        return true;
    }

    /**
     * Removes a previously added sub-validator from this {@link MultiValidator}
     * . The equivalent of calling {@link #removeValidator(Validator)} with the
     * specified {@link Validatable}'s {@link Validator}.
     *
     * @param v
     *        the {@link Validatable} that supplies the {@link Validator} to
     *        remove (must not be <code>null</code>)
     * @return <code>true</code> if this {@link MultiValidator} contained the
     *         sub-validator
     */
    public boolean removeValidatable(final Validatable v) {
        Check.notNull(v, "v"); //$NON-NLS-1$

        return removeValidator(v.getValidator());
    }

    /**
     * Removes a previously added sub-validator from this {@link MultiValidator}
     * . If the overall {@link IValidity} of this {@link MultiValidator} is
     * changed as a result of removing the sub-validator, any registered
     * {@link ValidityChangedListener}s are notified before this method
     * completes.
     *
     * @param v
     *        the sub-{@link Validator} to remove (must not be <code>null</code>
     *        )
     * @return <code>true</code> if this {@link MultiValidator} contained the
     *         sub-validator
     */
    public boolean removeValidator(final Validator v) {
        synchronized (subValidators) {
            if (!subValidators.contains(v)) {
                return false;
            }

            subValidators.remove(v);
        }

        v.removeValidityChangedListener(subValidatorListener);
        recomputeValidity();
        return true;
    }

    private void recomputeValidity() {
        Validator[] currentSubValidators;

        synchronized (subValidators) {
            currentSubValidators = (Validator[]) subValidators.toArray(new Validator[subValidators.size()]);
        }

        final IValidity[] subValiditys = new IValidity[currentSubValidators.length];

        for (int i = 0; i < currentSubValidators.length; i++) {
            subValiditys[i] = currentSubValidators[i].getValidity();
        }

        IValidity validity;

        if (subValiditys.length == 0) {
            validity = Validity.VALID;
        } else {
            validity = ValidationUtils.combineValiditys(subValiditys);
        }

        setValidity(validity);
    }

    private class SubValidatorListener implements ValidityChangedListener {
        @Override
        public void validityChanged(final ValidityChangedEvent event) {
            recomputeValidity();
        }
    }
}
