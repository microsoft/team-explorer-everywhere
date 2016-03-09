// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.AbstractValidatorBinding;
import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorBinding;

/**
 * {@link DialogPageValidatorBinding} is a {@link ValidatorBinding}
 * implementation that binds a {@link Validator} to a {@link DialogPage}. The
 * dialog page's completion and error message is bound to the {@link Validator}
 * 's validation state. Dialog pages are the superclass of preference pages and
 * wizard pages. Therefore {@link DialogPageValidatorBinding} is the superclass
 * of {@link WizardPageValidatorBinding} and
 * {@link PreferencePageValidatorBinding}.
 *
 *
 * @see ValidatorBinding
 * @see Validator
 * @see DialogPage
 */
public class DialogPageValidatorBinding extends AbstractValidatorBinding {
    private final DialogPage dialogPage;

    private boolean firstTime = false;

    /**
     * Creates a new {@link DialogPageValidatorBinding} that binds the specified
     * {@link DialogPage}'s error message to a {@link Validator}'s validation
     * state.
     *
     * @param dialogPage
     *        the {@link DialogPage} to bind (must not be <code>null</code>)
     */
    public DialogPageValidatorBinding(final DialogPage dialogPage) {
        Check.notNull(dialogPage, "dialogPage"); //$NON-NLS-1$

        this.dialogPage = dialogPage;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.AbstractValidatorBinding#update(com.
     * microsoft .tfs.util.valid.IValidity)
     */
    @Override
    protected void update(final IValidity validity) {
        if (!firstTime && validity != null && validity.getSeverity() != Severity.OK) {
            final IValidationMessage message = validity.getFirstMessage();
            if (message != null && message.getMessage() != null) {
                final int type =
                    message.getSeverity() == Severity.WARNING ? IMessageProvider.WARNING : IMessageProvider.ERROR;
                dialogPage.setMessage(message.getMessage(), type);
                return;
            }
        }

        dialogPage.setMessage(null);
        return;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.valid.AbstractValidatorBinding#bind(com.microsoft
     * .tfs.util.valid.Validator)
     */
    @Override
    public void bind(final Validator newValidator) {
        firstTime = true;

        // super.bind() calls update()
        super.bind(newValidator);

        firstTime = false;
    }
}
