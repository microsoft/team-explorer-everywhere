// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import com.microsoft.tfs.core.util.CredentialsUtils;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.util.valid.AbstractValidator;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

public class HTTPProxyControl extends BaseControl implements Validatable, Alignable {
    public static final String USE_HTTP_PROXY_BUTTON_ID = "HTTPProxyControl.useHttpProxyButton"; //$NON-NLS-1$
    public static final String HTTP_PROXY_URL_TEXT_ID = "HTTPProxyControl.httpProxyUrlText"; //$NON-NLS-1$
    public static final String AUTHENTICATION_COMBO_ID = "HTTPProxyControl.httpProxyAuthenticationCombo"; //$NON-NLS-1$
    public static final String USERNAME_TEXT_ID = "HTTPProxyControl.httpProxyUsernameText"; //$NON-NLS-1$
    public static final String DOMAIN_TEXT_ID = "HTTPProxyControl.httpProxyDomainText"; //$NON-NLS-1$
    public static final String PASSWORD_TEXT_ID = "HTTPProxyControl.httpProxyPasswordText"; //$NON-NLS-1$

    private static final int URL_TEXT_WIDTH_CHARS = 50;
    private static final int CREDENTIALS_TEXT_WIDTH_CHARS = 25;

    private final Button useHttpProxyButton;
    private final Label httpProxyUrlLabel;
    private final Text httpProxyUrlText;
    private final ErrorLabel httpProxyUrlErrorLabel;
    private final MultiValidator httpProxyUrlValidator;
    private final AbstractValidator httpProxyUrlForPasswordValidator;
    private final Label httpProxyUrlExampleLabel;

    private Control httpProxyAuthenticationSpacer;
    private final Combo httpProxyAuthenticationCombo;
    private final Label httpProxyAuthenticationLabel;

    private final Label httpProxyUsernameLabel;
    private final Text httpProxyUsernameText;
    private final ErrorLabel httpProxyUsernameErrorLabel;
    private final AbstractValidator httpProxyUsernameValidator;
    private final Label httpProxyDomainLabel;
    private final Text httpProxyDomainText;
    private final Label httpProxyPasswordLabel;
    private final Text httpProxyPasswordText;

    private boolean useHttpProxy;
    private String httpProxyUrl;
    private boolean useHttpProxyDefaultCredentials;
    private String httpProxyUsername;
    private String httpProxyDomain;
    private String httpProxyPassword;

    private boolean ignoreHttpProxyUrlModifyEvent;
    private boolean ignoreHttpProxyAuthenticationChangedEvent;
    private boolean ignoreHttpProxyUsernameModifyEvent;
    private boolean ignoreHttpProxyDomainModifyEvent;
    private boolean ignoreHttpProxyPasswordModifyEvent;

    private final boolean supportsDefaultCredentials = CredentialsUtils.supportsDefaultCredentials();
    private final boolean supportsSpecifiedCredentials = CredentialsUtils.supportsSpecifiedCredentials();

    private final MultiValidator multiValidator = new MultiValidator(this);

    public HTTPProxyControl(final Composite parent, final int style) {
        super(parent, style);

        final boolean rightAlign = WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA);
        final int labelHorizontalAlignment = rightAlign ? SWT.RIGHT : SWT.LEFT;
        final int verticalSectionSpacing = ControlSize.convertCharHeightToPixels(this, 1);

        final GridLayout layout = new GridLayout(4, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        useHttpProxyButton =
            SWTUtil.createButton(this, SWT.CHECK, Messages.getString("HTTPProxyControl.UseHttpProxyButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(useHttpProxyButton, USE_HTTP_PROXY_BUTTON_ID);
        useHttpProxyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                HTTPProxyControl.this.onUseHTTPProxyButtonSelected(e);
            }
        });
        GridDataBuilder.newInstance().hSpan(layout).applyTo(useHttpProxyButton);

        SWTUtil.createHorizontalGridLayoutSpacer(this, 25, 1);

        httpProxyUrlLabel = SWTUtil.createLabel(this, Messages.getString("HTTPProxyControl.UrlButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlign(labelHorizontalAlignment).applyTo(httpProxyUrlLabel);

        httpProxyUrlText = new Text(this, SWT.BORDER);
        AutomationIDHelper.setWidgetID(httpProxyUrlText, HTTP_PROXY_URL_TEXT_ID);
        SWTUtil.setSelectAllOnFocusGained(httpProxyUrlText);
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(httpProxyUrlText, URL_TEXT_WIDTH_CHARS).applyTo(
            httpProxyUrlText);
        httpProxyUrlText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                HTTPProxyControl.this.onHTTPProxyURLTextModified(e);
            }
        });

        httpProxyUrlErrorLabel = new ErrorLabel(this, SWT.NONE);

        /*
         * Use a multi-validator so that the password url-requirement validator
         * can add its validation into this for decoration.
         */
        httpProxyUrlValidator = new MultiValidator(httpProxyUrlText);
        httpProxyUrlValidator.addValidator(
            new TextControlURLValidator(
                httpProxyUrlText,
                Messages.getString("HTTPProxyControl.UrlValidatorText"), //$NON-NLS-1$
                false));

        httpProxyUrlErrorLabel.getValidatorBinding().bind(httpProxyUrlValidator);
        multiValidator.addValidator(httpProxyUrlValidator);

        SWTUtil.createHorizontalGridLayoutSpacer(this, 2);

        httpProxyUrlExampleLabel =
            SWTUtil.createLabel(this, Messages.getString("HTTPProxyControl.ProxyUrlExampleLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hSpan(2).hIndent(getHorizontalSpacing()).applyTo(
            httpProxyUrlExampleLabel);

        if (supportsDefaultCredentials) {
            useHttpProxyDefaultCredentials = true;
        }

        if (supportsDefaultCredentials && supportsSpecifiedCredentials) {
            httpProxyAuthenticationSpacer = SWTUtil.createHorizontalGridLayoutSpacer(this, 2);

            httpProxyAuthenticationCombo = new Combo(this, SWT.READ_ONLY);
            AutomationIDHelper.setWidgetID(httpProxyAuthenticationCombo, AUTHENTICATION_COMBO_ID);
            httpProxyAuthenticationCombo.add(Messages.getString("HTTPProxyControl.AuthCurrentUserChoice")); //$NON-NLS-1$
            httpProxyAuthenticationCombo.add(Messages.getString("HTTPProxyControl.AuthWithTheseCredentialsChoice")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(httpProxyAuthenticationCombo);
            httpProxyAuthenticationCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    HTTPProxyControl.this.onHTTPProxyAuthenticationChanged(e);
                }
            });

            SWTUtil.createHorizontalGridLayoutSpacer(this, 1);
        } else {
            httpProxyAuthenticationCombo = null;
        }

        if (supportsDefaultCredentials && !supportsSpecifiedCredentials) {
            httpProxyAuthenticationSpacer = SWTUtil.createHorizontalGridLayoutSpacer(this, 2);

            httpProxyAuthenticationLabel = new Label(this, SWT.NONE);
            httpProxyAuthenticationLabel.setText(
                MessageFormat.format(
                    Messages.getString("HTTPProxyControl.AuthenticateAsDomainUserFormat"), //$NON-NLS-1$
                    UserNameUtil.getCurrentUserDomain(),
                    UserNameUtil.getCurrentUserName()));
            GridDataBuilder.newInstance().hFill().hSpan(2).hIndent(getHorizontalSpacing()).applyTo(
                httpProxyAuthenticationLabel);

            SWTUtil.createHorizontalGridLayoutSpacer(this, 1);
        } else {
            httpProxyAuthenticationLabel = null;
        }

        if (supportsSpecifiedCredentials) {
            final Control httpProxyUsernameSpacer = SWTUtil.createHorizontalGridLayoutSpacer(this, 1);

            if (httpProxyAuthenticationSpacer == null) {
                httpProxyAuthenticationSpacer = httpProxyUsernameSpacer;
            }

            httpProxyUsernameLabel =
                SWTUtil.createLabel(this, Messages.getString("HTTPProxyControl.UsernameLabelText")); //$NON-NLS-1$

            GridDataBuilder.newInstance().hAlign(labelHorizontalAlignment).applyTo(httpProxyUsernameLabel);

            httpProxyUsernameText = new Text(this, SWT.BORDER);
            AutomationIDHelper.setWidgetID(httpProxyUsernameText, USERNAME_TEXT_ID);
            SWTUtil.setSelectAllOnFocusGained(httpProxyUsernameText);
            GridDataBuilder.newInstance().hGrab().hFill().wCHint(
                httpProxyUsernameText,
                CREDENTIALS_TEXT_WIDTH_CHARS).applyTo(httpProxyUsernameText);
            httpProxyUsernameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    HTTPProxyControl.this.onHTTPProxyUsernameTextModified(e);
                }
            });

            httpProxyUsernameErrorLabel = new ErrorLabel(this, SWT.NONE);

            SWTUtil.createHorizontalGridLayoutSpacer(this, 1);

            httpProxyDomainLabel = SWTUtil.createLabel(this, Messages.getString("HTTPProxyControl.DomainLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlign(labelHorizontalAlignment).applyTo(httpProxyDomainLabel);

            httpProxyDomainText = new Text(this, SWT.BORDER);
            AutomationIDHelper.setWidgetID(httpProxyDomainText, DOMAIN_TEXT_ID);
            SWTUtil.setSelectAllOnFocusGained(httpProxyDomainText);
            GridDataBuilder.newInstance().hGrab().hFill().wCHint(
                httpProxyDomainText,
                CREDENTIALS_TEXT_WIDTH_CHARS).applyTo(httpProxyDomainText);
            httpProxyDomainText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    HTTPProxyControl.this.onHTTPProxyDomainTextModified(e);
                }
            });

            SWTUtil.createHorizontalGridLayoutSpacer(this, 1);

            SWTUtil.createHorizontalGridLayoutSpacer(this, 1);

            httpProxyPasswordLabel =
                SWTUtil.createLabel(this, Messages.getString("HTTPProxyControl.PasswordLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlign(labelHorizontalAlignment).applyTo(httpProxyPasswordLabel);

            httpProxyPasswordText = new Text(this, SWT.BORDER | SWT.PASSWORD);
            AutomationIDHelper.setWidgetID(httpProxyPasswordText, PASSWORD_TEXT_ID);
            SWTUtil.setSelectAllOnFocusGained(httpProxyPasswordText);
            GridDataBuilder.newInstance().hGrab().hFill().wCHint(
                httpProxyPasswordText,
                CREDENTIALS_TEXT_WIDTH_CHARS).applyTo(httpProxyPasswordText);
            httpProxyPasswordText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    HTTPProxyControl.this.onHTTPProxyPasswordTextModified(e);
                }
            });

            /*
             * Add a validator such that the URL and username texts are required
             * when specifying an HTTP proxy password.
             */
            httpProxyUsernameValidator = new TextControlRequiredWhenOtherTextSpecifiedValidator(
                httpProxyUsernameText,
                httpProxyPasswordText,
                Messages.getString("HTTPProxyControl.ProxyPasswordValidatorText")); //$NON-NLS-1$

            httpProxyUsernameErrorLabel.getValidatorBinding().bind(httpProxyUsernameValidator);
            multiValidator.addValidator(httpProxyUsernameValidator);

            httpProxyUrlForPasswordValidator = new TextControlRequiredWhenOtherTextSpecifiedValidator(
                httpProxyUrlText,
                httpProxyPasswordText,
                Messages.getString("HTTPProxyControl.ProxyPasswordValidatorText")); //$NON-NLS-1$
            httpProxyUrlValidator.addValidator(httpProxyUrlForPasswordValidator);
        } else {
            httpProxyUrlForPasswordValidator = null;
            httpProxyUsernameLabel = null;
            httpProxyUsernameText = null;
            httpProxyUsernameErrorLabel = null;
            httpProxyUsernameValidator = null;
            httpProxyDomainLabel = null;
            httpProxyDomainText = null;
            httpProxyPasswordLabel = null;
            httpProxyPasswordText = null;
        }

        if (supportsDefaultCredentials && supportsSpecifiedCredentials) {
            SWTUtil.addGridLayoutVerticalIndent(new Control[] {
                httpProxyAuthenticationSpacer,
                httpProxyAuthenticationCombo
            }, verticalSectionSpacing);
        } else if (supportsDefaultCredentials) {
            SWTUtil.addGridLayoutVerticalIndent(new Control[] {
                httpProxyAuthenticationSpacer,
                httpProxyAuthenticationLabel
            }, verticalSectionSpacing);
        } else if (supportsSpecifiedCredentials) {
            SWTUtil.addGridLayoutVerticalIndent(new Control[] {
                httpProxyAuthenticationSpacer,
                httpProxyUsernameLabel,
                httpProxyUsernameText,
                httpProxyUsernameErrorLabel
            }, verticalSectionSpacing);
        }

        updateEnablementAndValidation();
    }

    public void setUseHTTPProxyButtonText(final String text) {
        useHttpProxyButton.setText(text);
    }

    @Override
    public Point getPreferredAlignSize() {
        Control[] alignControls;

        if (supportsSpecifiedCredentials) {
            alignControls = new Control[] {
                httpProxyUrlLabel,
                httpProxyUsernameLabel,
                httpProxyPasswordLabel
            };
        } else {
            alignControls = new Control[] {
                httpProxyUrlLabel
            };
        }

        return ControlSize.maxSize(alignControls);
    }

    @Override
    public void setAlignSize(final Point size) {
        final int widthHint = size.x;

        Label[] alignLabels;

        if (supportsSpecifiedCredentials) {
            alignLabels = new Label[] {
                httpProxyUrlLabel,
                httpProxyUsernameLabel,
                httpProxyDomainLabel,
                httpProxyPasswordLabel
            };
        } else {
            alignLabels = new Label[] {
                httpProxyUrlLabel
            };
        }

        final Point preferredSize = getPreferredAlignSize();

        for (int i = 0; i < alignLabels.length; i++) {
            if (preferredSize.x < widthHint) {
                ControlSize.setSizeHints(alignLabels[i], widthHint, SWT.DEFAULT);
            }
        }
    }

    public GridLayout getGridLayout() {
        return (GridLayout) getLayout();
    }

    public void resetValues() {
        setValues(false, null, false, null, null, null);
    }

    public void setValues(
        final boolean useHttpProxy,
        String httpProxyUrl,
        final boolean useHttpProxyDefaultCredentials,
        String httpProxyUsername,
        String httpProxyDomain,
        String httpProxyPassword) {
        this.useHttpProxy = useHttpProxy;
        useHttpProxyButton.setSelection(useHttpProxy);

        if (httpProxyUrl != null) {
            httpProxyUrl = httpProxyUrl.trim();
            if (httpProxyUrl.length() == 0) {
                httpProxyUrl = null;
            }
        }
        this.httpProxyUrl = httpProxyUrl;
        ignoreHttpProxyUrlModifyEvent = true;
        httpProxyUrlText.setText(httpProxyUrl == null ? "" : httpProxyUrl); //$NON-NLS-1$
        ignoreHttpProxyUrlModifyEvent = false;

        this.useHttpProxyDefaultCredentials = (useHttpProxyDefaultCredentials && supportsDefaultCredentials);

        if (httpProxyAuthenticationCombo != null) {
            httpProxyAuthenticationCombo.select(useHttpProxyDefaultCredentials ? 0 : 1);
        }

        if (supportsSpecifiedCredentials) {
            if (httpProxyUsername != null) {
                httpProxyUsername = httpProxyUsername.trim();

                if (httpProxyUsername.length() == 0) {
                    httpProxyUsername = null;
                }
            }
            this.httpProxyUsername = httpProxyUsername;
            ignoreHttpProxyUsernameModifyEvent = true;
            httpProxyUsernameText.setText(httpProxyUsername == null ? "" : httpProxyUsername); //$NON-NLS-1$
            ignoreHttpProxyUsernameModifyEvent = false;

            if (httpProxyDomain != null) {
                httpProxyDomain = httpProxyDomain.trim();

                if (httpProxyDomain.length() == 0) {
                    httpProxyDomain = null;
                }
            }
            this.httpProxyDomain = httpProxyDomain;
            ignoreHttpProxyDomainModifyEvent = true;
            httpProxyDomainText.setText(httpProxyDomain == null ? "" : httpProxyDomain); //$NON-NLS-1$
            ignoreHttpProxyDomainModifyEvent = false;

            if (httpProxyPassword != null) {
                httpProxyPassword = httpProxyPassword.trim();

                if (httpProxyPassword.length() == 0) {
                    httpProxyPassword = null;
                }
            }
            this.httpProxyPassword = httpProxyPassword;
            ignoreHttpProxyPasswordModifyEvent = true;
            httpProxyPasswordText.setText(httpProxyPassword == null ? "" : httpProxyPassword); //$NON-NLS-1$
            ignoreHttpProxyPasswordModifyEvent = false;
        }

        updateEnablementAndValidation();
    }

    public boolean isUseHTTPProxy() {
        return useHttpProxy;
    }

    public String getHTTPProxyURL() {
        return httpProxyUrl;
    }

    public boolean isUseHTTPProxyDefaultCredentials() {
        return useHttpProxyDefaultCredentials;
    }

    public String getHTTPProxyUsername() {
        return httpProxyUsername;
    }

    public String getHTTPProxyDomain() {
        return httpProxyDomain;
    }

    public String getHTTPProxyPassword() {
        return httpProxyPassword;
    }

    public boolean setFocusToFirstError() {
        if (httpProxyUrlValidator.getValidity().isValid() == false) {
            return httpProxyUrlText.setFocus();
        }

        else if (supportsSpecifiedCredentials && httpProxyUsernameValidator.getValidity().isValid() == false) {
            return httpProxyUsernameText.setFocus();
        }

        return false;
    }

    @Override
    public boolean setFocus() {
        return setFocusToFirstError() || super.setFocus();
    }

    @Override
    public Validator getValidator() {
        return multiValidator;
    }

    private void onUseHTTPProxyButtonSelected(final SelectionEvent e) {
        useHttpProxy = useHttpProxyButton.getSelection();

        updateEnablementAndValidation();

        if (useHttpProxy) {
            httpProxyUrlText.setFocus();
        }
    }

    private void onHTTPProxyURLTextModified(final ModifyEvent e) {
        if (ignoreHttpProxyUrlModifyEvent) {
            return;
        }

        final String text = httpProxyUrlText.getText().trim();
        httpProxyUrl = text.length() > 0 ? text : null;
    }

    protected void onHTTPProxyAuthenticationChanged(final SelectionEvent e) {
        if (ignoreHttpProxyAuthenticationChangedEvent) {
            return;
        }

        useHttpProxyDefaultCredentials = (httpProxyAuthenticationCombo.getSelectionIndex() == 0);
        updateEnablementAndValidation();
    }

    private void onHTTPProxyPasswordTextModified(final ModifyEvent e) {
        if (ignoreHttpProxyPasswordModifyEvent) {
            return;
        }

        final String text = httpProxyPasswordText.getText().trim();
        httpProxyPassword = text.length() > 0 ? text : null;
    }

    private void onHTTPProxyDomainTextModified(final ModifyEvent e) {
        if (ignoreHttpProxyDomainModifyEvent) {
            return;
        }

        final String text = httpProxyDomainText.getText().trim();
        httpProxyDomain = text.length() > 0 ? text : null;
    }

    private void onHTTPProxyUsernameTextModified(final ModifyEvent e) {
        if (ignoreHttpProxyUsernameModifyEvent) {
            return;
        }

        final String text = httpProxyUsernameText.getText().trim();
        httpProxyUsername = text.length() > 0 ? text : null;
    }

    private void updateEnablementAndValidation() {
        httpProxyUrlLabel.setEnabled(useHttpProxy);
        httpProxyUrlText.setEnabled(useHttpProxy);
        httpProxyUrlExampleLabel.setEnabled(useHttpProxy);

        if (httpProxyAuthenticationCombo != null) {
            httpProxyAuthenticationCombo.setEnabled(useHttpProxy);

            ignoreHttpProxyAuthenticationChangedEvent = true;
            httpProxyAuthenticationCombo.select(useHttpProxyDefaultCredentials ? 0 : 1);
            ignoreHttpProxyAuthenticationChangedEvent = false;
        }

        if (supportsDefaultCredentials && supportsSpecifiedCredentials && useHttpProxyDefaultCredentials) {
            ignoreHttpProxyUsernameModifyEvent = true;
            ignoreHttpProxyDomainModifyEvent = true;
            ignoreHttpProxyPasswordModifyEvent = true;

            httpProxyUrlValidator.suspendValidation(true);
            httpProxyUrlForPasswordValidator.suspendValidation(true);
            httpProxyUsernameValidator.suspendValidation(true);

            httpProxyUsernameText.setText(UserNameUtil.getCurrentUserName());
            httpProxyDomainText.setText(UserNameUtil.getCurrentUserDomain());
            httpProxyPasswordText.setText("********"); //$NON-NLS-1$

            httpProxyUsernameLabel.setEnabled(false);
            httpProxyUsernameText.setEnabled(false);
            httpProxyDomainLabel.setEnabled(false);
            httpProxyDomainText.setEnabled(false);
            httpProxyPasswordLabel.setEnabled(false);
            httpProxyPasswordText.setEnabled(false);

            ignoreHttpProxyUsernameModifyEvent = false;
            ignoreHttpProxyDomainModifyEvent = false;
            ignoreHttpProxyPasswordModifyEvent = false;
        } else if (supportsSpecifiedCredentials) {
            httpProxyUsernameText.setText(httpProxyUsername != null ? httpProxyUsername : ""); //$NON-NLS-1$
            httpProxyDomainText.setText(httpProxyDomain != null ? httpProxyDomain : ""); //$NON-NLS-1$
            httpProxyPasswordText.setText(httpProxyPassword != null ? httpProxyPassword : ""); //$NON-NLS-1$

            httpProxyUsernameLabel.setEnabled(useHttpProxy);
            httpProxyUsernameText.setEnabled(useHttpProxy);
            httpProxyDomainLabel.setEnabled(useHttpProxy);
            httpProxyDomainText.setEnabled(useHttpProxy);
            httpProxyPasswordLabel.setEnabled(useHttpProxy);
            httpProxyPasswordText.setEnabled(useHttpProxy);
        }

        if (useHttpProxy) {
            httpProxyUrlValidator.resumeValidation();

            if (supportsSpecifiedCredentials && !useHttpProxyDefaultCredentials) {
                httpProxyUrlForPasswordValidator.resumeValidation();
                httpProxyUsernameValidator.resumeValidation();
            }
        } else {
            httpProxyUrlValidator.suspendValidation(true);

            if (supportsSpecifiedCredentials) {
                httpProxyUrlForPasswordValidator.suspendValidation(true);
                httpProxyUsernameValidator.suspendValidation(true);
            }
        }
    }
}
