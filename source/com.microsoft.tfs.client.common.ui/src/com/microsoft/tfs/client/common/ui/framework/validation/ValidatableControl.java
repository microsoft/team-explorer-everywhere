// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorHelper;

/**
 * @deprecated to be removed
 */
@Deprecated
public class ValidatableControl extends Composite implements Validatable {
    private final ValidatorHelper validator;

    public ValidatableControl(final Composite parent, final int style) {
        super(parent, style);
        validator = new ValidatorHelper(this);
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    protected final ValidatorHelper getValidatorHelper() {
        return validator;
    }
}