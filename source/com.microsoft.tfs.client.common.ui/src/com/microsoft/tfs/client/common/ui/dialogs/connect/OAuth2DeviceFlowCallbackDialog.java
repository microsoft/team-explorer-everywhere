// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.alm.auth.oauth.DeviceFlowResponse;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class OAuth2DeviceFlowCallbackDialog extends BaseDialog {
    private static final Log log = LogFactory.getLog(OAuth2DeviceFlowCallbackDialog.class);

    private final DeviceFlowResponse response;
    private final FormToolkit toolkit;

    public OAuth2DeviceFlowCallbackDialog(final Shell parentShell, final DeviceFlowResponse response) {
        super(parentShell);

        this.response = response;
        this.toolkit = new FormToolkit(parentShell.getDisplay());
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAddToDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(3, false);

        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label verificationUrlLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("OAuth2DeviceFlowCallbackDialog.DeviceLoginUrlText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(verificationUrlLabel);

        final String urlLinkText = this.response.getVerificationUri().toString();
        final Hyperlink verificationUrlLink =
            this.toolkit.createHyperlink(dialogArea, urlLinkText, SWT.WRAP | SWT.CENTER);
        GridDataBuilder.newInstance().hSpan(layout).hAlignCenter().applyTo(verificationUrlLink);

        verificationUrlLink.setUnderlined(false);
        verificationUrlLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.getLabel()));
                } catch (PartInitException pie) {
                    log.error("Failed to open the verification url in browser.", pie); //$NON-NLS-1$
                } catch (MalformedURLException mue) {
                    log.error("Failed to open the verification url in browser: " + e.getLabel(), mue); //$NON-NLS-1$
                }
            }
        });

        final Label codeNameLabel = SWTUtil.createLabel(
            dialogArea,
            MessageFormat.format(
                Messages.getString("OAuth2DeviceFlowCallbackDialog.DeviceUserCodeTextFormat"), //$NON-NLS-1$
                this.response.getExpiresIn() / 60));
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(codeNameLabel);

        final Text deviceCodeText = new Text(dialogArea, SWT.READ_ONLY | SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(layout).hFill().applyTo(deviceCodeText);
        deviceCodeText.setText(this.response.getUserCode());
        deviceCodeText.setFocus();

        final Label finishFlowLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("OAuth2DeviceFlowCallbackDialog.SelectOkText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(finishFlowLabel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        /*
         * If users clicked on OK, it means they should have completed auth flow
         * on the browser. The dialog closes and control flow gets back to the
         * OAuth library. Since version 0.4.0 of the OAuth2 library, the token
         * polling loop always executes at least one time, and checks for
         * expiration time after that. We don't need a second attempt in the UI
         * plug-in. Either the token is available or we get an error explaining
         * the reason for operation rejection, e.g. CANCELLED, TIMEOUT,
         * MISSING_PERMISSIONS. Thus we set the ExpiresAt property to the
         * current time to prevent retry attempts in the OAuth2 library's
         * polling loop.
         */
        this.response.getExpiresAt().setTime(new Date());
        super.okPressed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void cancelPressed() {
        this.response.requestCancel();
        super.cancelPressed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("OAuth2DeviceFlowCallbackDialog.DialogTitle"); //$NON-NLS-1$
    }
}
