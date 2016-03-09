// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.swt.widgets.Button;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.AbstractValidatorBinding;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorBinding;

/**
 * {@link ButtonValidatorBinding} is a {@link ValidatorBinding} implementation
 * that binds a {@link Validator} to a {@link Button}. The button's enablement
 * is bound to the {@link Validator}'s validation state.
 *
 * @see ValidatorBinding
 * @see Validator
 * @see Button
 */
public class ButtonValidatorBinding extends AbstractValidatorBinding {
    private final Button button;

    /**
     * Creates a new {@link ButtonValidatorBinding} that binds the specified
     * {@link Button}'s enablement to a {@link Validator}'s validation state.
     *
     * @param button
     *        the {@link Button} to bind (must not be <code>null</code>)
     */
    public ButtonValidatorBinding(final Button button) {
        Check.notNull(button, "button"); //$NON-NLS-1$

        this.button = button;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.AbstractValidatorBinding#update(com.
     * microsoft .tfs.util.valid.IValidity)
     */
    @Override
    protected void update(final IValidity validity) {
        button.setEnabled(validity == null || validity.isValid());
    }
}
