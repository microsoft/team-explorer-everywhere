// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.wizard.WizardPage;

import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorBinding;

/**
 * {@link WizardPageValidatorBinding} is a {@link ValidatorBinding}
 * implementation that binds a {@link Validator} to a {@link WizardPage}. The
 * wizard page's completion and error message is bound to the {@link Validator}
 * 's validation state.
 *
 * @see ValidatorBinding
 * @see Validator
 * @see WizardPage
 */
public class WizardPageValidatorBinding extends DialogPageValidatorBinding {
    private final WizardPage wizardPage;

    /**
     * Creates a new {@link WizardPageValidatorBinding} that binds the specified
     * {@link WizardPage}'s completion and error message to a {@link Validator}
     * 's validation state.
     *
     * @param wizardPage
     *        the {@link WizardPage} to bind (must not be <code>null</code>)
     */
    public WizardPageValidatorBinding(final WizardPage wizardPage) {
        super(wizardPage);
        this.wizardPage = wizardPage;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.valid.
     * DialogPageValidatorBinding
     * #update(com.microsoft.tfs.util.valid.IValidity)
     */
    @Override
    protected void update(final IValidity validity) {
        wizardPage.setPageComplete(validity == null || validity.isValid());
        super.update(validity);
    }
}
