// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.image.SystemImageHelper;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;

public class ConnectionErrorControl extends BaseControl {
    private final Label imageLabel; /* The image label */
    private final Label messageLabel; /* The error message */

    /* The server URI that failed */
    private URI serverURI;

    /* The exception from the failure. (Used if errorMessage is null) */
    private Exception failure;

    /* Any additional message */
    private String message;

    /* The icon to use */
    private int imageId = SWT.ICON_WARNING;
    private Image image;

    public ConnectionErrorControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout controlLayout = new GridLayout(2, false);
        controlLayout.verticalSpacing = getVerticalSpacing();
        controlLayout.horizontalSpacing = getHorizontalSpacing() * 3;
        controlLayout.marginWidth = 0;
        controlLayout.marginHeight = 0;
        setLayout(controlLayout);

        imageLabel = new Label(this, SWT.NONE);
        imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

        messageLabel = new Label(this, SWT.WRAP);

        final GridData messageLabelData = new GridData(SWT.FILL, SWT.BEGINNING, true, true);
        messageLabelData.grabExcessHorizontalSpace = true;
        messageLabelData.grabExcessVerticalSpace = true;
        messageLabel.setLayoutData(messageLabelData);

        computeMessage();
    }

    public void setMessageWidthHint(final int wHint) {
        ((GridData) messageLabel.getLayoutData()).widthHint = wHint;
    }

    public void setServerURI(final URI serverURI) {
        this.serverURI = serverURI;
        computeMessage();
    }

    public void setException(final Exception failure) {
        this.failure = failure;
        computeMessage();
    }

    public Exception getException() {
        return failure;
    }

    public void setMessage(final String message) {
        this.message = message;
        computeMessage();
    }

    public String getMessage() {
        return message;
    }

    public void setImageID(final int imageId) {
        this.imageId = imageId;
        computeMessage();
    }

    public int getImageID() {
        return imageId;
    }

    private void computeMessage() {
        /* Setup the image */
        image = SystemImageHelper.getSystemImage(getShell(), imageId);
        image.setBackground(imageLabel.getBackground());
        imageLabel.setImage(image);

        final StringBuffer fullMsg = new StringBuffer();

        /* Setup the message */
        if (failure != null) {
            String headerMessage;

            if (serverURI == null) {
                headerMessage = Messages.getString("ConnectionErrorControl.CouldNotConnectTfs"); //$NON-NLS-1$
            } else {
                headerMessage = MessageFormat.format(
                    Messages.getString("ConnectionErrorControl.CouldNotConnectTfsWithServerURIFormat"), //$NON-NLS-1$
                    serverURI.toString());
            }

            fullMsg.append(headerMessage);
            if (getExceptionMessage() != null) {
                fullMsg.append("\n"); //$NON-NLS-1$
                fullMsg.append(getExceptionMessage());
                fullMsg.append("."); //$NON-NLS-1$
            }

            if (message != null) {
                fullMsg.append("\n\n"); //$NON-NLS-1$
            }
        }

        if (message != null) {
            fullMsg.append(message);
        }

        messageLabel.setText(fullMsg.toString());
    }

    private String getExceptionMessage() {
        if (failure == null) {
            return null;
        } else if (failure instanceof UnauthorizedException) {
            return Messages.getString("ConnectionErrorControl.UserNameNotAccepted"); //$NON-NLS-1$
        } else if (failure instanceof SocketTimeoutException) {
            return Messages.getString("ConnectionErrorControl.ConnectionTimedOut"); //$NON-NLS-1$
        } else {
            return failure.getMessage();
        }
    }
}
