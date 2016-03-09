// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;

/**
 * <p>
 * {@link AbstractSelectionProviderValidator} is an abstract {@link Validator}
 * implementation that has an {@link ISelectionProvider} as a validation
 * subject.
 * </p>
 *
 * <p>
 * Subclasses must override the {@link #computeValidity(ISelection)} method to
 * determine whether the {@link ISelection} value in the
 * {@link ISelectionProvider} subject is valid.
 * </p>
 *
 * <p>
 * <b>Important:</b> subclasses must call the {@link #validate()} method before
 * the subclass constructor finishes. This call is needed to ensure that the
 * validator reflects the initial validation state of the
 * {@link ISelectionProvider} subject correctly.
 * </p>
 *
 * @see Validator
 * @see ISelectionProvider
 */
public abstract class AbstractSelectionProviderValidator extends AbstractValidator {
    /**
     * The {@link ISelectionChangedListener} attached to the
     * {@link ISelectionProvider} subject. Attached in the constructor and
     * removed in {@link #dispose()}.
     */
    private final ISelectionChangedListener selectionChangedListener;

    /**
     * Creates a new {@link AbstractSelectionProviderValidator} that will
     * validate the specified {@link ISelectionProvider} subject. This
     * constructor attaches an {@link ISelectionChangedListener} to the subject.
     * This listener will be removed in the {@link #dispose()} method.
     * <b>Important:</b> this constructor does not perform initial validation of
     * the subject. The subclass <b>must</b> call the {@link #validate()} method
     * before the subclass constructor finishes.
     *
     * @param subject
     *        the {@link ISelectionProvider} subject to validate (must not be
     *        <code>null</code>)
     */
    protected AbstractSelectionProviderValidator(final ISelectionProvider subject) {
        super(subject);

        selectionChangedListener = new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                AbstractSelectionProviderValidator.this.onSelectionChanged(event);
            }
        };

        subject.addSelectionChangedListener(selectionChangedListener);
    }

    /**
     * @return the {@link ISelectionProvider} subject of this {@link Validator}
     *         (never <code>null</code>)
     */
    public final ISelectionProvider getSelectionProviderSubject() {
        return (ISelectionProvider) getSubject();
    }

    /**
     * {@link AbstractSelectionProviderValidator} overrides {@link #dispose()}
     * to remove the {@link ISelectionChangedListener} from the subject. If
     * subclasses override to perform their own cleanup, they must invoke
     * <code>super.dispose()</code>.
     */
    @Override
    public void dispose() {
        final ISelectionProvider subject = getSelectionProviderSubject();
        subject.removeSelectionChangedListener(selectionChangedListener);
    }

    /**
     * Called when a {@link SelectionChangedEvent} is sent by the
     * {@link ISelectionProvider} subject. This implementation validates the new
     * {@link ISelection} value in the subject. Normally, there is no need for
     * subclasses to override or call this method.
     *
     * @param event
     *        the {@link SelectionChangedEvent} (never <code>null</code>)
     */
    protected void onSelectionChanged(final SelectionChangedEvent event) {
        final ISelectionProvider subject = event.getSelectionProvider();
        validate(subject.getSelection());
    }

    /**
     * Called by subclasses inside the subclass constructor to perform the
     * initial validation of the subject. Normally, there is no need for
     * subclasses to override.
     */
    protected void validate() {
        final ISelectionProvider subject = getSelectionProviderSubject();
        final ISelection selection = subject.getSelection();
        validate(selection);
    }

    /**
     * Called to validate the specified {@link ISelection} value. The
     * {@link IValidity} of this {@link Validator} will be updated to reflect
     * the validation result. Normally, there is no need for subclasses to
     * override or call this method.
     *
     * @param selection
     *        the {@link ISelection} value (never <code>null</code>)
     */
    protected void validate(final ISelection selection) {
        final IValidity validity = computeValidity(selection);
        setValidity(validity);
    }

    /**
     * Called to compute an {@link IValidity} for the specified
     * {@link ISelection} value. Subclasses must override to perform
     * subclass-specific validation. If needed, subclasses can obtain the
     * {@link ISelectionProvider} subject by calling
     * {@link #getSelectionProviderSubject()}.
     *
     * @param selection
     *        the {@link ISelection} value to validate (never <code>null</code>)
     * @return an {@link IValidity} that represents the validation of the
     *         specified value (must not be <code>null</code>)
     */
    protected abstract IValidity computeValidity(ISelection selection);
}
