// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.controls;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;

public class EditorStatusControl extends Composite {
    private final CLabel label;

    private String statusMessage;
    private Image statusImage;

    private boolean disconnected = false;

    public EditorStatusControl(final Composite parent, final int style) {
        super(parent, style);

        final Color backgroundColor = getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);

        final FillLayout layout = new FillLayout();
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        setLayout(layout);

        setBackground(backgroundColor);

        label = new CLabel(this, SWT.LEFT);
        label.setBackground(backgroundColor);
    }

    public void setStatus(final String message) {
        setStatus(message, true);
    }

    public void setStatus(final String message, final boolean valid) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) {
                    return;
                }

                statusMessage = message;

                if (valid) {
                    statusImage = null;
                } else {
                    statusImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
                }

                refresh();
            }
        };

        if (getDisplay().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            UIHelpers.runOnUIThread(getDisplay(), true, r);
        }
    }

    private void refresh() {
        String text;
        if (disconnected) {
            final String textFormat = Messages.getString("EditorStatusControl.DisconnectedStatusFormat"); //$NON-NLS-1$
            text = MessageFormat.format(textFormat, statusMessage);
        } else {
            text = statusMessage;
        }

        label.setText(text);
        label.setImage(statusImage);

        label.getParent().layout();
    }

    public void setDisconnected(final boolean disconnected) {
        this.disconnected = disconnected;
        refresh();
    }
}
