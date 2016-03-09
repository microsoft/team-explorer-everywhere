// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxProvider;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;

/**
 * <p>
 * {@link AbstractCheckboxProviderValidator} is an abstract {@link Validator}
 * implementation that has a {@link CheckboxProvider} as a validation subject.
 * </p>
 *
 * <p>
 * Subclasses must override the {@link #computeValidity(Object[])} method to
 * determine whether the checked element set in the {@link CheckboxProvider}
 * subject is valid.
 * </p>
 *
 * <p>
 * <b>Important:</b> subclasses must call the {@link #validate()} method before
 * the subclass constructor finishes. This call is needed to ensure that the
 * validator reflects the initial validation state of the
 * {@link CheckboxProvider} subject correctly.
 * </p>
 *
 * @see Validator
 * @see CheckboxProvider
 */
public abstract class AbstractCheckboxProviderValidator extends AbstractValidator {
    /**
     * The {@link CheckboxListener} attached to the {@link CheckboxProvider}
     * subject. Attached in the constructor and removed in {@link #dispose()}.
     */
    private final CheckboxListener checkboxListener;

    /**
     * Creates a new {@link AbstractCheckboxProviderValidator} that will
     * validate the specified {@link CheckboxProvider} subject. This constructor
     * attaches an {@link CheckboxListener} to the subject. This listener will
     * be removed in the {@link #dispose()} method. <b>Important:</b> this
     * constructor does not perform initial validation of the subject. The
     * subclass <b>must</b> call the {@link #validate()} method before the
     * subclass constructor finishes.
     *
     * @param subject
     *        the {@link CheckboxProvider} subject to validate (must not be
     *        <code>null</code>)
     */
    protected AbstractCheckboxProviderValidator(final CheckboxProvider subject) {
        super(subject);

        checkboxListener = new CheckboxListener() {
            @Override
            public void checkedElementsChanged(final CheckboxEvent event) {
                AbstractCheckboxProviderValidator.this.onCheckedElementsChanged(event);
            }
        };

        subject.addCheckboxListener(checkboxListener);
    }

    /**
     * @return the {@link CheckboxProvider} subject of this {@link Validator}
     *         (never <code>null</code>)
     */
    public final CheckboxProvider getCheckboxProviderSubject() {
        return (CheckboxProvider) getSubject();
    }

    /**
     * {@link AbstractCheckboxProviderValidator} overrides {@link #dispose()} to
     * remove the {@link CheckboxListener} from the subject. If subclasses
     * override to perform their own cleanup, they must invoke
     * <code>super.dispose()</code>.
     */
    @Override
    public void dispose() {
        final CheckboxProvider subject = getCheckboxProviderSubject();
        subject.removeCheckboxListener(checkboxListener);
    }

    /**
     * Called when a {@link CheckboxEvent} is sent by the
     * {@link CheckboxProvider} subject. This implementation validates the new
     * checked element set in the subject. Normally, there is no need for
     * subclasses to override or call this method.
     *
     * @param event
     *        the {@link CheckboxEvent} (never <code>null</code>)
     */
    protected void onCheckedElementsChanged(final CheckboxEvent event) {
        final CheckboxProvider subject = event.getCheckboxProvider();
        validate(subject.getCheckedElements());
    }

    /**
     * Called by subclasses inside the subclass constructor to perform the
     * initial validation of the subject. Normally, there is no need for
     * subclasses to override.
     */
    protected void validate() {
        final CheckboxProvider subject = getCheckboxProviderSubject();
        final Object[] checkedElements = subject.getCheckedElements();
        validate(checkedElements);
    }

    /**
     * Called to validate the specified checked element set. The
     * {@link IValidity} of this {@link Validator} will be updated to reflect
     * the validation result. Normally, there is no need for subclasses to
     * override or call this method.
     *
     * @param checkedElements
     *        the checked element set (never <code>null</code>)
     */
    protected void validate(final Object[] checkedElements) {
        final IValidity validity = computeValidity(checkedElements);
        setValidity(validity);
    }

    /**
     * Called to compute an {@link IValidity} for the specified checked element
     * set. Subclasses must override to perform subclass-specific validation. If
     * needed, subclasses can obtain the {@link CheckboxProvider} subject by
     * calling {@link #getCheckboxProviderSubject()}.
     *
     * @param checkedElements
     *        the checked element set to validate (never <code>null</code>)
     * @return an {@link IValidity} that represents the validation of the
     *         specified value (must not be <code>null</code>)
     */
    protected abstract IValidity computeValidity(Object[] checkedElements);
}
