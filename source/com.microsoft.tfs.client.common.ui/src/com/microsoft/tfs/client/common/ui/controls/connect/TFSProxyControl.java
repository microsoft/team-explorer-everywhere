// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.Alignable;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.validation.ErrorLabel;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

public class TFSProxyControl extends BaseControl implements Validatable, Alignable {
    public static final String USE_TF_PROXY_BUTTON_ID = "TFSProxyControl.useTfProxyButton"; //$NON-NLS-1$
    public static final String TF_PROXY_URL_TEXT_ID = "TFSProxyControl.tfProxyUrlText"; //$NON-NLS-1$

    private static final int URL_TEXT_WIDTH_CHARS = 50;

    private final Button useTfProxyButton;
    private final Label tfProxyUrlLabel;
    private final Text tfProxyUrlText;
    private final ErrorLabel tfProxyUrlErrorLabel;
    private final Label tfProxyUrlExampleLabel;
    private final AbstractValidator tfProxyUrlValidator;

    private boolean useTfProxy;
    private String tfProxyUrl;

    private boolean ignoreTfProxyUrlModifyEvent;

    private final MultiValidator multiValidator = new MultiValidator(this);

    public TFSProxyControl(final Composite parent, final int style) {
        super(parent, style);

        final boolean rightAlign = WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA);
        final int labelHorizontalAlignment = rightAlign ? SWT.RIGHT : SWT.LEFT;

        final GridLayout layout = new GridLayout(4, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        useTfProxyButton =
            SWTUtil.createButton(this, SWT.CHECK, Messages.getString("TFSProxyControl.UseTfsProxyButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(useTfProxyButton, USE_TF_PROXY_BUTTON_ID);
        useTfProxyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                TFSProxyControl.this.onUseTFProxyButtonSelected(e);
            }
        });
        GridDataBuilder.newInstance().hSpan(layout).applyTo(useTfProxyButton);

        SWTUtil.createHorizontalGridLayoutSpacer(this, 25, 1);

        tfProxyUrlLabel = SWTUtil.createLabel(this, Messages.getString("TFSProxyControl.TfProxyUrlLabelText")); //$NON-NLS-1$
        tfProxyUrlLabel.setAlignment(labelHorizontalAlignment);
        GridDataBuilder.newInstance().hAlign(labelHorizontalAlignment).applyTo(tfProxyUrlLabel);

        tfProxyUrlText = new Text(this, SWT.BORDER);
        AutomationIDHelper.setWidgetID(tfProxyUrlText, TF_PROXY_URL_TEXT_ID);
        SWTUtil.setSelectAllOnFocusGained(tfProxyUrlText);
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(tfProxyUrlText, URL_TEXT_WIDTH_CHARS).applyTo(
            tfProxyUrlText);
        tfProxyUrlText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                TFSProxyControl.this.onTFProxyURLTextModified(e);
            }
        });

        tfProxyUrlErrorLabel = new ErrorLabel(this, SWT.NONE);

        tfProxyUrlValidator =
            new TextControlURLValidator(
                tfProxyUrlText,
                Messages.getString("TFSProxyControl.TfProxyValidatorText"), //$NON-NLS-1$
                false);
        tfProxyUrlErrorLabel.getValidatorBinding().bind(tfProxyUrlValidator);
        multiValidator.addValidator(tfProxyUrlValidator);

        SWTUtil.createHorizontalGridLayoutSpacer(this, 2);

        tfProxyUrlExampleLabel = SWTUtil.createLabel(this, "Example: http://tfsproxy.example.com:8081"); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hSpan(2).hIndent(getHorizontalSpacing()).applyTo(tfProxyUrlExampleLabel);

        updateEnablementAndValidation();
    }

    public void setUseTFProxyButtonText(final String text) {
        useTfProxyButton.setText(text);
    }

    @Override
    public Point getPreferredAlignSize() {
        return tfProxyUrlLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    @Override
    public void setAlignSize(final Point size) {
        final int widthHint = size.x;

        final Point preferredSize = getPreferredAlignSize();

        if (preferredSize.x < widthHint) {
            ControlSize.setSizeHints(tfProxyUrlLabel, widthHint, SWT.DEFAULT);
        }
    }

    public GridLayout getGridLayout() {
        return (GridLayout) getLayout();
    }

    public void resetValues() {
        setValues(false, null);
    }

    public void setValues(final boolean useTfProxy, String tfProxyUrl) {
        this.useTfProxy = useTfProxy;
        useTfProxyButton.setSelection(useTfProxy);

        if (tfProxyUrl != null) {
            tfProxyUrl = tfProxyUrl.trim();
            if (tfProxyUrl.length() == 0) {
                tfProxyUrl = null;
            }
        }
        this.tfProxyUrl = tfProxyUrl;
        ignoreTfProxyUrlModifyEvent = true;
        tfProxyUrlText.setText(tfProxyUrl == null ? "" : tfProxyUrl); //$NON-NLS-1$
        ignoreTfProxyUrlModifyEvent = false;

        updateEnablementAndValidation();
    }

    public boolean isUseTFProxy() {
        return useTfProxy;
    }

    public String getTFProxyURL() {
        return tfProxyUrl;
    }

    public boolean setFocusToFirstError() {
        return !tfProxyUrlValidator.getValidity().isValid() && tfProxyUrlText.setFocus();
    }

    @Override
    public boolean setFocus() {
        return setFocusToFirstError() || super.setFocus();
    }

    @Override
    public Validator getValidator() {
        return multiValidator;
    }

    private void onUseTFProxyButtonSelected(final SelectionEvent e) {
        useTfProxy = useTfProxyButton.getSelection();

        updateEnablementAndValidation();

        if (useTfProxy) {
            tfProxyUrlText.setFocus();
        }
    }

    private void onTFProxyURLTextModified(final ModifyEvent e) {
        if (ignoreTfProxyUrlModifyEvent) {
            return;
        }

        final String text = tfProxyUrlText.getText().trim();
        tfProxyUrl = text.length() > 0 ? text : null;
    }

    private void updateEnablementAndValidation() {
        tfProxyUrlLabel.setEnabled(useTfProxy);
        tfProxyUrlText.setEnabled(useTfProxy);
        tfProxyUrlExampleLabel.setEnabled(useTfProxy);

        if (useTfProxy) {
            tfProxyUrlValidator.resumeValidation();
        } else {
            tfProxyUrlValidator.suspendValidation(true);
        }
    }
}
