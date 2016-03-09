// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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

public class ErrorLabel extends Composite {
    private final Label label;
    private final Image errorImage;
    private final Image warningImage;
    private final ValidatorBinding validatorBinding;

    private Point computedSize;

    public ErrorLabel(final Composite parent, final int style) {
        super(parent, style);

        label = new Label(this, SWT.NONE);
        GridDataBuilder.newInstance().hHint(16).wHint(16).applyTo(label);

        errorImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
        warningImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);

        validatorBinding = new AbstractValidatorBinding() {
            @Override
            protected void update(final IValidity validity) {
                if (validity.getSeverity() == Severity.OK) {
                    clearErrorState();
                } else {
                    final IValidationMessage validationMessage = validity.getFirstMessage();
                    if (validationMessage == null) {
                        setErrorState(validity.getSeverity(), null);
                    } else {
                        setErrorState(validationMessage.getSeverity(), validationMessage.getMessage());
                    }
                }
            }
        };

        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(final ControlEvent e) {
                ErrorLabel.this.controlResized(e);
            }
        });
    }

    public ValidatorBinding getValidatorBinding() {
        return validatorBinding;
    }

    public void clearErrorState() {
        setErrorState(Severity.OK, null);
    }

    public void setErrorState(final Severity severity, final String message) {
        if (severity == Severity.ERROR) {
            label.setVisible(true);
            label.setImage(errorImage);

            if (message != null) {
                label.setToolTipText(message);
            } else {
                label.setToolTipText(null);
            }
        } else if (severity == Severity.WARNING) {
            label.setVisible(true);
            label.setImage(warningImage);

            if (message != null) {
                label.setToolTipText(message);
            } else {
                label.setToolTipText(null);
            }
        } else {
            label.setVisible(false);
            label.setToolTipText(null);
        }
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        if (computedSize == null) {
            final Rectangle r = errorImage.getBounds().union(warningImage.getBounds());
            computedSize = Geometry.getSize(r);
        }

        return computedSize;
    }

    private void controlResized(final ControlEvent e) {
        label.setBounds(getClientArea());
    }
}
