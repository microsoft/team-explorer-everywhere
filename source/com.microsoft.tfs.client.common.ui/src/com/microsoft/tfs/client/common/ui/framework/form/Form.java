// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.form;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ValidationHeader;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

public class Form extends Composite implements Validatable {
    private final MultiValidator validator;

    public Form(final Composite parent, final int style, final boolean useValidationHeader) {
        super(parent, style);

        validator = new MultiValidator(this);

        final GridLayout layout = new GridLayout();
        setLayout(layout);
        layout.marginHeight = 0;
        layout.marginWidth = 0;

        if (useValidationHeader) {
            final ValidationHeader validationHeader = new ValidationHeader(this, SWT.NONE);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(validationHeader);
            validationHeader.getValidatorBinding().bind(validator);
        }
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    protected final MultiValidator getMultiValidator() {
        return validator;
    }

    protected final void addPart(final FormPart part, final boolean stretchHorizontal, final boolean stretchVertical) {
        validator.addValidatable(part);

        final int horizontalAlignment = stretchHorizontal ? SWT.FILL : SWT.BEGINNING;
        final int verticalAlignment = stretchVertical ? SWT.FILL : SWT.CENTER;

        final GridData gd = new GridData(horizontalAlignment, verticalAlignment, stretchHorizontal, stretchVertical);
        part.setLayoutData(gd);
    }
}
