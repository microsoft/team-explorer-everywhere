// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;

import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;

/**
 * <p>
 * {@link AbstractComboControlValidator} is an abstract {@link Validator}
 * implementation that has a {@link Combo} control as a validation subject.
 * </p>
 *
 * <p>
 * Subclasses must override the {@link #computeValidity(String)} method to
 * determine whether the {@link String} value in the {@link Combo} control
 * subject is valid.
 * </p>
 *
 * <p>
 * <b>Important:</b> subclasses must call the {@link #validate()} method before
 * the subclass constructor finishes. This call is needed to ensure that the
 * validator reflects the initial validation state of the {@link Combo} subject
 * correctly.
 * </p>
 *
 * @see Validator
 * @see Combo
 */
public abstract class AbstractComboControlValidator extends AbstractValidator {
    /**
     * The {@link ModifyListener} attached to the {@link Combo} control subject.
     * Attached in the constructor and removed in {@link #dispose()}.
     */
    private final ModifyListener modifyListener;

    /**
     * Creates a new {@link AbstractComboControlValidator} that will validate
     * the specified {@link Combo} subject. This constructor attaches a
     * {@link ModifyListener} to the subject. This listener will be removed in
     * the {@link #dispose()} method. <b>Important:</b> this constructor does
     * not perform initial validation of the subject. The subclass <b>must</b>
     * call the {@link #validate()} method before the subclass constructor
     * finishes.
     *
     * @param subject
     *        the {@link Combo} control subject to validate (must not be
     *        <code>null</code>)
     */
    protected AbstractComboControlValidator(final Combo subject) {
        super(subject);

        modifyListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                AbstractComboControlValidator.this.onTextModified(e);
            }
        };

        subject.addModifyListener(modifyListener);
    }

    /**
     * @return the {@link Combo} control subject of this {@link Validator}
     *         (never <code>null</code>)
     */
    public final Combo getComboControlSubject() {
        return (Combo) getSubject();
    }

    /**
     * {@link AbstractComboControlValidator} overrides {@link #dispose()} to
     * remove the {@link ModifyListener} from the subject. If subclasses
     * override to perform their own cleanup, they must invoke
     * <code>super.dispose()</code>.
     */
    @Override
    public void dispose() {
        final Combo subject = getComboControlSubject();
        subject.removeModifyListener(modifyListener);
    }

    /**
     * Called when a {@link ModifyEvent} is sent by the {@link Combo} subject.
     * This implementation validates the new {@link String} value in the
     * subject. Normally, there is no need for subclasses to override or call
     * this method.
     *
     * @param e
     *        the {@link ModifyEvent} (never <code>null</code>)
     */
    protected void onTextModified(final ModifyEvent e) {
        final Combo subject = (Combo) e.widget;
        validate(subject.getText());
    }

    /**
     * Called by subclasses inside the subclass constructor to perform the
     * initial validation of the subject. Normally, there is no need for
     * subclasses to override.
     */
    protected void validate() {
        final Combo subject = getComboControlSubject();
        final String text = subject.getText();
        validate(text);
    }

    /**
     * Called to validate the specified {@link String} value. The
     * {@link IValidity} of this {@link Validator} will be updated to reflect
     * the validation result. Normally, there is no need for subclasses to
     * override or call this method.
     *
     * @param text
     *        the {@link String} value (never <code>null</code>)
     */
    protected void validate(final String text) {
        final IValidity validity = computeValidity(text);
        setValidity(validity);
    }

    /**
     * Called to compute an {@link IValidity} for the specified {@link String}
     * value. Subclasses must override to perform subclass-specific validation.
     * If needed, subclasses can obtain the {@link Combo} control subject by
     * calling {@link #getTextControlSubject()}.
     *
     * @param text
     *        the {@link String} value to validate (never <code>null</code>)
     * @return an {@link IValidity} that represents the validation of the
     *         specified value (must not be <code>null</code>)
     */
    protected abstract IValidity computeValidity(String text);
}
