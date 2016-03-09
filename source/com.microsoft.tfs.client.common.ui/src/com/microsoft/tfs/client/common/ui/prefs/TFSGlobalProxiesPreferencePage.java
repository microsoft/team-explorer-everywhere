// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.connect.TFSGlobalProxiesControl;
import com.microsoft.tfs.client.common.ui.controls.connect.TFSGlobalProxiesControl.TFSGlobalProxySettings;
import com.microsoft.tfs.client.common.ui.framework.validation.PreferencePageValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

public class TFSGlobalProxiesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    public static final String TFS_GLOBAL_PROXIES_CONTROL_ID = "TFSGlobalProxiesPreferencePage.proxiesControl"; //$NON-NLS-1$

    public static final String HTTP_PROXY_PREFIX = "httpProxy."; //$NON-NLS-1$
    public static final String TFS_PROXY_PREFIX = "tfsProxy."; //$NON-NLS-1$

    public static final String HTTP_PROXY_ENABLED = HTTP_PROXY_PREFIX + "enable"; //$NON-NLS-1$
    public static final String HTTP_PROXY_URL = HTTP_PROXY_PREFIX + "url"; //$NON-NLS-1$
    public static final String HTTP_PROXY_DEFAULT_CREDENTIALS = HTTP_PROXY_PREFIX + "defaultCredentials"; //$NON-NLS-1$
    public static final String HTTP_PROXY_USERNAME = HTTP_PROXY_PREFIX + "username"; //$NON-NLS-1$
    public static final String HTTP_PROXY_DOMAIN = HTTP_PROXY_PREFIX + "domain"; //$NON-NLS-1$
    public static final String HTTP_PROXY_PASSWORD = HTTP_PROXY_PREFIX + "password"; //$NON-NLS-1$
    public static final String TFS_PROXY_ENABLED = TFS_PROXY_PREFIX + "enable"; //$NON-NLS-1$
    public static final String TFS_PROXY_URL = TFS_PROXY_PREFIX + "url"; //$NON-NLS-1$

    private TFSGlobalProxiesControl control;

    @Override
    protected Control createContents(final Composite parent) {
        control = new TFSGlobalProxiesControl(parent, SWT.NONE, getContainer());
        AutomationIDHelper.setWidgetID(control, TFS_GLOBAL_PROXIES_CONTROL_ID);

        new PreferencePageValidatorBinding(this).bind(control);

        control.setSettings(getCurrentSettings());

        return control;
    }

    @Override
    protected void performDefaults() {
        control.setSettings(getDefaultSettings());
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        setSettings(control.getSettings());
        return super.performOk();
    }

    private void setSettings(final TFSGlobalProxySettings settings) {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final boolean useHttpProxy = settings.isUseHTTPProxy();
        String httpProxyUrl = settings.getHTTPProxyURL();
        final boolean useHttpProxyDefaultCredentials = settings.isUseHTTPProxyDefaultCredentials();
        String httpProxyUsername = settings.getHTTPProxyUsername();
        String httpProxyPassword = settings.getHTTPProxyPassword();
        final boolean useTfProxy = settings.isUseTFProxy();
        String tfProxyUrl = settings.getTFProxyURL();

        httpProxyUrl = normalizeStringForPreferences(httpProxyUrl);
        httpProxyUsername = normalizeStringForPreferences(httpProxyUsername);
        httpProxyPassword = normalizeStringForPreferences(httpProxyPassword);
        tfProxyUrl = normalizeStringForPreferences(tfProxyUrl);

        preferences.setValue(HTTP_PROXY_ENABLED, useHttpProxy);
        preferences.setValue(HTTP_PROXY_URL, httpProxyUrl);
        preferences.setValue(HTTP_PROXY_DEFAULT_CREDENTIALS, useHttpProxyDefaultCredentials);
        preferences.setValue(HTTP_PROXY_USERNAME, httpProxyUsername);
        preferences.setValue(HTTP_PROXY_PASSWORD, httpProxyPassword);
        preferences.setValue(TFS_PROXY_ENABLED, useTfProxy);
        preferences.setValue(TFS_PROXY_URL, tfProxyUrl);
    }

    private String normalizeStringForPreferences(final String input) {
        return input == null ? "" : input; //$NON-NLS-1$
    }

    private TFSGlobalProxySettings getCurrentSettings() {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final boolean useHttpProxy = preferences.getBoolean(HTTP_PROXY_ENABLED);
        final String httpProxyUrl = preferences.getString(HTTP_PROXY_URL);
        final boolean useHttpProxyDefaultCredentials = preferences.getBoolean(HTTP_PROXY_DEFAULT_CREDENTIALS);
        final String httpProxyUsername = preferences.getString(HTTP_PROXY_USERNAME);
        final String httpProxyDomain = preferences.getString(HTTP_PROXY_DOMAIN);
        final String httpProxyPassword = preferences.getString(HTTP_PROXY_PASSWORD);
        final boolean useTfProxy = preferences.getBoolean(TFS_PROXY_ENABLED);
        final String tfProxyUrl = preferences.getString(TFS_PROXY_URL);

        return new TFSGlobalProxySettings(
            useHttpProxy,
            httpProxyUrl,
            useHttpProxyDefaultCredentials,
            httpProxyUsername,
            httpProxyDomain,
            httpProxyPassword,
            useTfProxy,
            tfProxyUrl);
    }

    private TFSGlobalProxySettings getDefaultSettings() {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();

        final boolean useHttpProxy = preferences.getDefaultBoolean(HTTP_PROXY_ENABLED);
        final String httpProxyUrl = preferences.getDefaultString(HTTP_PROXY_URL);
        final boolean useHttpProxyDefaultCredentials = preferences.getDefaultBoolean(HTTP_PROXY_DEFAULT_CREDENTIALS);
        final String httpProxyUsername = preferences.getDefaultString(HTTP_PROXY_USERNAME);
        final String httpProxyDomain = preferences.getDefaultString(HTTP_PROXY_DOMAIN);
        final String httpProxyPassword = preferences.getDefaultString(HTTP_PROXY_PASSWORD);
        final boolean useTfProxy = preferences.getDefaultBoolean(TFS_PROXY_ENABLED);
        final String tfProxyUrl = preferences.getDefaultString(TFS_PROXY_URL);

        return new TFSGlobalProxySettings(
            useHttpProxy,
            httpProxyUrl,
            useHttpProxyDefaultCredentials,
            httpProxyUsername,
            httpProxyDomain,
            httpProxyPassword,
            useTfProxy,
            tfProxyUrl);
    }

    @Override
    public void init(final IWorkbench workbench) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        CodeMarkerDispatch.dispatch(
            visible ? BasePreferencePage.CODEMARKER_VISIBLE_TRUE : BasePreferencePage.CODEMARKER_VISIBLE_FALSE);
    }
}
