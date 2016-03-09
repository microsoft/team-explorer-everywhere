// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementProvider;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;

/**
 * <p>
 * {@link AbstractElementProviderValidator} is an abstract {@link Validator}
 * implementation that has an {@link ElementProvider} as a validation subject.
 * </p>
 *
 * <p>
 * Subclasses must override the {@link #computeValidity(Object[])} method to
 * determine whether the element set in the {@link ElementProvider} subject is
 * valid.
 * </p>
 *
 * <p>
 * <b>Important:</b> subclasses must call the {@link #validate()} method before
 * the subclass constructor finishes. This call is needed to ensure that the
 * validator reflects the initial validation state of the
 * {@link ElementProvider} subject correctly.
 * </p>
 *
 * @see Validator
 * @see ElementProvider
 */
public abstract class AbstractElementProviderValidator extends AbstractValidator {
    /**
     * The {@link ElementListener} attached to the {@link ElementProvider}
     * subject. Attached in the constructor and removed in {@link #dispose()}.
     */
    private final ElementListener elementListener;

    /**
     * Creates a new {@link AbstractElementProviderValidator} that will validate
     * the specified {@link ElementProvider} subject. This constructor attaches
     * an {@link ElementListener} to the subject. This listener will be removed
     * in the {@link #dispose()} method. <b>Important:</b> this constructor does
     * not perform initial validation of the subject. The subclass <b>must</b>
     * call the {@link #validate()} method before the subclass constructor
     * finishes.
     *
     * @param subject
     *        the {@link ElementProvider} subject to validate (must not be
     *        <code>null</code>)
     */
    protected AbstractElementProviderValidator(final ElementProvider subject) {
        super(subject);

        elementListener = new ElementListener() {
            @Override
            public void elementsChanged(final ElementEvent event) {
                AbstractElementProviderValidator.this.onElementsChanged(event);
            }
        };

        subject.addElementListener(elementListener);
    }

    /**
     * @return the {@link ElementProvider} subject of this {@link Validator}
     *         (never <code>null</code>)
     */
    public final ElementProvider getElementProviderSubject() {
        return (ElementProvider) getSubject();
    }

    /**
     * {@link AbstractElementProviderValidator} overrides {@link #dispose()} to
     * remove the {@link ElementListener} from the subject. If subclasses
     * override to perform their own cleanup, they must invoke
     * <code>super.dispose()</code>.
     */
    @Override
    public void dispose() {
        final ElementProvider subject = getElementProviderSubject();
        subject.removeElementListener(elementListener);
    }

    /**
     * Called when a {@link ElementEvent} is sent by the {@link ElementProvider}
     * subject. This implementation validates the new element set in the
     * subject. Normally, there is no need for subclasses to override or call
     * this method.
     *
     * @param event
     *        the {@link ElementEvent} (never <code>null</code>)
     */
    protected void onElementsChanged(final ElementEvent event) {
        final ElementProvider subject = event.getElementProvider();
        validate(subject.getElements());
    }

    /**
     * Called by subclasses inside the subclass constructor to perform the
     * initial validation of the subject. Normally, there is no need for
     * subclasses to override.
     */
    protected void validate() {
        final ElementProvider subject = getElementProviderSubject();
        final Object[] elements = subject.getElements();
        validate(elements);
    }

    /**
     * Called to validate the specified element set. The {@link IValidity} of
     * this {@link Validator} will be updated to reflect the validation result.
     * Normally, there is no need for subclasses to override or call this
     * method.
     *
     * @param elements
     *        the element set (never <code>null</code>)
     */
    protected void validate(final Object[] elements) {
        final IValidity validity = computeValidity(elements);
        setValidity(validity);
    }

    /**
     * Called to compute an {@link IValidity} for the specified element set.
     * Subclasses must override to perform subclass-specific validation. If
     * needed, subclasses can obtain the {@link ElementProvider} subject by
     * calling {@link #getElementProviderSubject()}.
     *
     * @param elements
     *        the element set to validate (never <code>null</code>)
     * @return an {@link IValidity} that represents the validation of the
     *         specified value (must not be <code>null</code>)
     */
    protected abstract IValidity computeValidity(Object[] elements);
}
