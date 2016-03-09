// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.generic;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.ImageButton;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;

public class MacMountBusyDialog extends BaseDialog {
    private final String serverName;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
    private Font serverLabelFont;

    public MacMountBusyDialog(final Shell parent, final String serverName) {
        super(parent);

        setOptionResizable(false);
        setOptionPersistGeometry(false);
        setOptionIncludeDefaultButtons(false);
        setReturnCode(IDialogConstants.CANCEL_ID);

        this.serverName = serverName;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("MacMountBusyDialog.ConnectingDialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final Color backgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

        final FormLayout dialogLayout = new FormLayout();
        dialogLayout.marginHeight = 0;
        dialogLayout.marginWidth = 0;
        dialogLayout.spacing = 0;

        dialogArea.setBackground(backgroundColor);
        dialogArea.setLayout(dialogLayout);

        final Composite imageComposite = new Composite(dialogArea, SWT.NONE);
        imageComposite.setSize(29, 31);
        imageComposite.setBackgroundImage(imageHelper.getImage("images/generic/macmount/network_icon_mac.gif")); //$NON-NLS-1$

        final FormData imageCompositeData = new FormData();
        imageCompositeData.left = new FormAttachment(0, 13);
        imageCompositeData.top = new FormAttachment(0, 14);
        imageCompositeData.width = 29;
        imageCompositeData.height = 31;
        imageComposite.setLayoutData(imageCompositeData);

        final Label serverLabel = new Label(dialogArea, SWT.NONE);
        serverLabel.setBackground(backgroundColor);
        serverLabel.setText(
            MessageFormat.format(Messages.getString("MacMountBusyDialog.ConnectingFormat"), serverName)); //$NON-NLS-1$

        // twiddle the font to match the Finder font default
        final FontData[] fontData = serverLabel.getFont().getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(10);
        }
        serverLabelFont = new Font(serverLabel.getFont().getDevice(), fontData);
        serverLabel.setFont(serverLabelFont);

        final FormData serverLabelData = new FormData();
        serverLabelData.left = new FormAttachment(imageComposite, 17, SWT.RIGHT);
        serverLabelData.top = new FormAttachment(0, 7);
        serverLabel.setLayoutData(serverLabelData);

        final ProgressBar progressBar = new ProgressBar(dialogArea, SWT.INDETERMINATE);
        progressBar.setSize(288, 10);

        final FormData progressBarData = new FormData();
        progressBarData.left = new FormAttachment(serverLabel, -1, SWT.LEFT);
        progressBarData.top = new FormAttachment(serverLabel, 7, SWT.BOTTOM);
        progressBarData.width = 288;
        progressBar.setLayoutData(progressBarData);

        final ImageButton closeButton = new ImageButton(dialogArea, SWT.NONE);
        closeButton.setBackground(backgroundColor);
        closeButton.setEnabledImage(imageHelper.getImage("images/generic/macmount/close_button_mac.gif")); //$NON-NLS-1$
        closeButton.setHoverImage(imageHelper.getImage("images/generic/macmount/close_button_hover_mac.gif")); //$NON-NLS-1$
        closeButton.setDepressedImage(imageHelper.getImage("images/generic/macmount/close_button_depressed_mac.gif")); //$NON-NLS-1$
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                close();
            }
        });

        final FormData closeButtonData = new FormData();
        closeButtonData.left = new FormAttachment(progressBar, 7, SWT.RIGHT);
        closeButtonData.top = new FormAttachment(0, 25);
        closeButtonData.right = new FormAttachment(100, -15);
        closeButtonData.width = 14;
        closeButtonData.height = 14;
        closeButton.setLayoutData(closeButtonData);

        final Composite fillerComposite = new Composite(dialogArea, SWT.NONE);
        fillerComposite.setBackground(backgroundColor);

        final FormData fillerCompositeData = new FormData();
        fillerCompositeData.left = new FormAttachment(0, 0);
        fillerCompositeData.top = new FormAttachment(progressBar, 0, SWT.BOTTOM);
        fillerCompositeData.right = new FormAttachment(100, 0);
        fillerCompositeData.bottom = new FormAttachment(100, 0);
        fillerCompositeData.height = 21;
        fillerComposite.setLayoutData(fillerCompositeData);
    }

    @Override
    protected Control createButtonBar(final Composite parent) {
        return null;
    }

    @Override
    protected void hookDialogAboutToClose() {
        if (serverLabelFont != null && !serverLabelFont.isDisposed()) {
            serverLabelFont.dispose();
        }

        if (imageHelper != null) {
            imageHelper.dispose();
        }
    }
}
