// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.eula;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.util.EULAText;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class EULAControl extends BaseControl {
    public static final String ACCEPT_CHECKBOX_ID = "EulaControl.acceptButton"; //$NON-NLS-1$
    public static final String EULA_TEXTBOX_ID = "EulaControl.eulaText"; //$NON-NLS-1$

    private final Button acceptButton;

    private boolean accepted = false;

    private final SingleListenerFacade listeners = new SingleListenerFacade(EULAControlAcceptedListener.class);

    public EULAControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        final Label descriptionText = new Label(this, SWT.WRAP | SWT.READ_ONLY);
        descriptionText.setText(Messages.getString("EulaControl.DescriptionText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(descriptionText);

        final Text eulaText = new Text(this, SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        AutomationIDHelper.setWidgetID(eulaText, EULA_TEXTBOX_ID);
        eulaText.setText(EULAText.getEULAText());
        eulaText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        eulaText.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().applyTo(eulaText);

        final Font font = JFaceResources.getTextFont();

        if (font != null) {
            eulaText.setFont(font);
        }

        ControlSize.setCharHeightHint(eulaText, 10);
        ControlSize.setCharWidthHint(eulaText, 80);

        // TODO: This is a work around for a linux layout problem. We may have
        // to revisit this and other issues with the wizard when we update for
        // localization.
        ControlSize.setCharHeightHint(descriptionText, 3);
        ControlSize.setCharWidthHint(descriptionText, 80);

        acceptButton = new Button(this, SWT.CHECK);
        AutomationIDHelper.setWidgetID(acceptButton, ACCEPT_CHECKBOX_ID);
        acceptButton.setText(Messages.getString("EulaControl.AcceptButtonText")); //$NON-NLS-1$
        acceptButton.setSelection(accepted);
        acceptButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                accepted = acceptButton.getSelection();

                notifyListeners();
            }
        });
        GridDataBuilder.newInstance().hGrab().applyTo(acceptButton);
    }

    public void setAccepted(final boolean accepted) {
        this.accepted = accepted;

        if (acceptButton != null && !acceptButton.isDisposed()) {
            acceptButton.setSelection(accepted);
        }
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setReadonly(final boolean readonly) {
        acceptButton.setEnabled(!readonly);
    }

    public void addAcceptedListener(final EULAControlAcceptedListener listener) {
        listeners.addListener(listener);
    }

    public void removeAcceptedListener(final EULAControlAcceptedListener listener) {
        listeners.removeListener(listener);
    }

    private void notifyListeners() {
        ((EULAControlAcceptedListener) listeners.getListener()).eulaAccepted(new EULAControlAcceptedEvent(accepted));
    }

    public interface EULAControlAcceptedListener {
        public void eulaAccepted(EULAControlAcceptedEvent event);
    }

    public final class EULAControlAcceptedEvent {
        private final boolean accepted;

        public EULAControlAcceptedEvent(final boolean accepted) {
            this.accepted = accepted;
        }

        public boolean isAccepted() {
            return accepted;
        }
    }
}
