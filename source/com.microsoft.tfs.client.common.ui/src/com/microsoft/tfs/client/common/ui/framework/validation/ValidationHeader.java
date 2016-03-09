// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.valid.AbstractValidatorBinding;
import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.ValidatorBinding;

public class ValidationHeader extends Composite {
    private static Image getErrorImage() {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
    }

    private static Image getWarningImage() {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    }

    private final Label imageLabel;
    private final Label textLabel;
    private final ValidatorBinding validatorBinding;

    public ValidationHeader(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        imageLabel = new Label(this, SWT.NONE);

        textLabel = new Label(this, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(textLabel);

        validatorBinding = new AbstractValidatorBinding() {
            @Override
            protected void update(final IValidity validity) {
                if (validity.getSeverity() == Severity.OK) {
                    setVisible(false);
                } else {
                    String text = ""; //$NON-NLS-1$

                    final IValidationMessage message = validity.getFirstMessage();
                    if (message != null) {
                        text = message.getMessage();
                        if (text == null) {
                            text = ""; //$NON-NLS-1$
                        }
                    }

                    textLabel.setText(text);
                    imageLabel.setImage(
                        validity.getSeverity() == Severity.WARNING ? getWarningImage() : getErrorImage());
                    setVisible(true);
                }
            }
        };
    }

    public ValidatorBinding getValidatorBinding() {
        return validatorBinding;
    }
}
