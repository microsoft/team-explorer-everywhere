// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

/**
 * @deprecated to be removed
 */
@Deprecated
public class MultipleValidatableControl extends Composite implements Validatable {
    private final MultiValidator multiValidator;

    public MultipleValidatableControl(final Composite parent, final int style) {
        super(parent, style);
        multiValidator = new MultiValidator(this);
    }

    @Override
    public Validator getValidator() {
        return multiValidator;
    }

    protected final MultiValidator getMultiValidator() {
        return multiValidator;
    }
}
