// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

public abstract class FormPart extends Composite implements Validatable {
    private final MultiValidator validator;

    protected FormPart(final Composite parent, final int style) {
        super(parent, style);

        validator = new MultiValidator(this);
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    protected final void addValidator(final Validator v) {
        validator.addValidator(v);
    }

    protected final void addValidatable(final Validatable v) {
        validator.addValidatable(v);
    }

    protected final void removeValidator(final Validator v) {
        validator.removeValidator(v);
    }

    protected final void removeValidatable(final Validatable v) {
        validator.removeValidatable(v);
    }

    protected final Composite createControlsArea(final boolean useGroupBox, final String groupBoxName) {
        Composite composite;

        if (useGroupBox) {
            setLayout(new FillLayout());
            final Group group = new Group(this, SWT.NONE);
            group.setText(groupBoxName);
            composite = group;
        } else {
            composite = this;
        }

        final GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        return composite;
    }
}
