// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.preference.PreferencePage;

import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorBinding;

/**
 * {@link PreferencePageValidatorBinding} is a {@link ValidatorBinding}
 * implementation that binds a {@link Validator} to a {@link PreferencePage}.
 * The wizard page's valid state and error message is bound to the
 * {@link Validator}'s validation state.
 *
 * @see ValidatorBinding
 * @see Validator
 * @see PreferencePage
 */
public class PreferencePageValidatorBinding extends DialogPageValidatorBinding {
    private final PreferencePage preferencePage;

    /**
     * Creates a new {@link PreferencePageValidatorBinding} that binds the
     * specified {@link PreferencePage}'s valid state and error message to a
     * {@link Validator}'s validation state.
     *
     * @param preferencePage
     *        the {@link PreferencePage} to bind (must not be <code>null</code>)
     */
    public PreferencePageValidatorBinding(final PreferencePage preferencePage) {
        super(preferencePage);
        this.preferencePage = preferencePage;
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
        preferencePage.setValid(validity == null || validity.isValid());
        super.update(validity);
    }
}
