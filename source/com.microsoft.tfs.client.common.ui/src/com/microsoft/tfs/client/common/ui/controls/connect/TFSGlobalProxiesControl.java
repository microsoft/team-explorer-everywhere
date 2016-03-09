// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.text.MessageFormat;

import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.preferences.PreferenceLinkAreaHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.Alignable;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

public class TFSGlobalProxiesControl extends Composite implements Validatable {
    public final static String HTTP_PROXY_CONTROL = "TFSGlobalProxiesControl.httpProxyControl"; //$NON-NLS-1$
    public final static String TFS_PROXY_CONTROL = "TFSGlobalProxiesControl.tfsProxyControl"; //$NON-NLS-1$

    private final HTTPProxyControl httpProxyControl;
    private final TFSProxyControl tfProxyControl;
    private final MultiValidator multiValidator = new MultiValidator(this);

    public TFSGlobalProxiesControl(
        final Composite parent,
        final int style,
        final IPreferencePageContainer preferencePageContainer) {
        super(parent, style);

        final int verticalSectionSpacing = ControlSize.convertCharHeightToPixels(this, 1) * 2;

        final GridLayout layout = SWTUtil.gridLayout(this, 2);
        layout.verticalSpacing = verticalSectionSpacing;

        SWTUtil.createLabel(this, getDisplay().getSystemImage(SWT.ICON_INFORMATION));

        final String messageFormat = Messages.getString("TFSGlobalProxiesControl.SummaryLabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, ProductInformation.getCurrent().toString());
        final Label label = SWTUtil.createLabel(this, SWT.WRAP, message);

        GridDataBuilder.newInstance().hGrab().hFill().wCHint(label, 80).applyTo(label);

        final boolean showHttpProxyControls =
            TFSCommonUIClientPlugin.getDefault().getProxyServiceTracker().getService() == null;

        if (showHttpProxyControls) {
            httpProxyControl = new HTTPProxyControl(this, SWT.NONE);
            AutomationIDHelper.setWidgetID(httpProxyControl, HTTP_PROXY_CONTROL);
            httpProxyControl.getGridLayout().marginHeight = 0;
            httpProxyControl.getGridLayout().marginWidth = 0;
            multiValidator.addValidatable(httpProxyControl);
            GridDataBuilder.newInstance().hGrab().hFill().hSpan(layout).applyTo(httpProxyControl);
        } else {
            httpProxyControl = null;
        }

        tfProxyControl = new TFSProxyControl(this, SWT.NONE);
        AutomationIDHelper.setWidgetID(tfProxyControl, TFS_PROXY_CONTROL);
        tfProxyControl.getGridLayout().marginHeight = 0;
        tfProxyControl.getGridLayout().marginWidth = 0;
        multiValidator.addValidatable(tfProxyControl);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(layout).applyTo(tfProxyControl);

        if (httpProxyControl != null) {
            ControlSize.align(new Alignable[] {
                httpProxyControl,
                tfProxyControl
            });
        }

        if (!showHttpProxyControls) {
            final Control linkControl =
                PreferenceLinkAreaHelper.createPreferenceLinkArea(
                    this,
                    SWT.NONE,
                    "org.eclipse.ui.net.NetPreferences", //$NON-NLS-1$
                    Messages.getString("TFSGlobalProxiesControl.ClickHereToConfigureHttp"), //$NON-NLS-1$
                    preferencePageContainer,
                    null);

            GridDataBuilder.newInstance().hGrab().hFill().hSpan(layout).applyTo(linkControl);
        }
    }

    public void resetSettings() {
        setSettings(null);
    }

    public void setSettings(final TFSGlobalProxySettings data) {
        if (httpProxyControl != null) {
            final boolean useHttpProxy = data == null ? false : data.isUseHTTPProxy();
            final String httpProxyUrl = data == null ? null : data.getHTTPProxyURL().trim();
            final boolean useHttpProxyDefaultCredentials =
                data == null ? true : data.isUseHTTPProxyDefaultCredentials();
            final String httpProxyUsername = data == null ? null : data.getHTTPProxyUsername().trim();
            final String httpProxyDomain = data == null ? null : data.getHTTPProxyDomain().trim();
            final String httpProxyPassword = data == null ? null : data.getHTTPProxyPassword().trim();

            httpProxyControl.setValues(
                useHttpProxy,
                httpProxyUrl,
                useHttpProxyDefaultCredentials,
                httpProxyUsername,
                httpProxyDomain,
                httpProxyPassword);
        }

        final boolean useTfProxy = data == null ? false : data.isUseTFProxy();
        final String tfProxyUrl = data == null ? null : data.getTFProxyURL().trim();

        tfProxyControl.setValues(useTfProxy, tfProxyUrl);
    }

    public TFSGlobalProxySettings getSettings() {
        if (httpProxyControl == null) {
            return new TFSGlobalProxySettings(
                false,
                null,
                false,
                null,
                null,
                null,
                tfProxyControl.isUseTFProxy(),
                tfProxyControl.getTFProxyURL());
        }

        return new TFSGlobalProxySettings(
            httpProxyControl.isUseHTTPProxy(),
            httpProxyControl.getHTTPProxyURL(),
            httpProxyControl.isUseHTTPProxyDefaultCredentials(),
            httpProxyControl.getHTTPProxyUsername(),
            httpProxyControl.getHTTPProxyDomain(),
            httpProxyControl.getHTTPProxyPassword(),
            tfProxyControl.isUseTFProxy(),
            tfProxyControl.getTFProxyURL());
    }

    @Override
    public Validator getValidator() {
        return multiValidator;
    }

    public static class TFSGlobalProxySettings {
        private final boolean useHttpProxy;
        private final String httpProxyUrl;
        private final boolean useHttpProxyDefaultCredentials;
        private final String httpProxyUsername;
        private final String httpProxyDomain;
        private final String httpProxyPassword;

        private final boolean useTfProxy;
        private final String tfProxyUrl;

        public TFSGlobalProxySettings(
            final boolean useHttpProxy,
            final String httpProxyUrl,
            final boolean useHttpProxyDefaultCredentials,
            final String httpProxyUsername,
            final String httpProxyDomain,
            final String httpProxyPassword,
            final boolean useTfProxy,
            final String tfProxyUrl) {
            this.useHttpProxy = useHttpProxy;
            this.httpProxyUrl = httpProxyUrl;
            this.useHttpProxyDefaultCredentials = useHttpProxyDefaultCredentials;
            this.httpProxyUsername = httpProxyUsername;
            this.httpProxyDomain = httpProxyDomain;
            this.httpProxyPassword = httpProxyPassword;
            this.useTfProxy = useTfProxy;
            this.tfProxyUrl = tfProxyUrl;
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

        public boolean isUseTFProxy() {
            return useTfProxy;
        }

        public String getTFProxyURL() {
            return tfProxyUrl;
        }
    }
}
