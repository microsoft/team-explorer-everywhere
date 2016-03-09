// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.util.tracker.ServiceTracker;

import com.microsoft.tfs.client.common.config.CommonClientConnectionAdvisor;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.DefaultWebServiceFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;

public class UIClientConnectionAdvisor extends CommonClientConnectionAdvisor {
    /**
     * Creates a {@link UIClientConnectionAdvisor} that uses the current default
     * {@link Locale} and {@link TimeZone} for all
     * {@link ConnectionInstanceData}s.
     */
    public UIClientConnectionAdvisor() {
        super(Locale.getDefault(), TimeZone.getDefault());
    }

    private static final Log log = LogFactory.getLog(UIClientConnectionAdvisor.class);

    @Override
    public HTTPClientFactory getHTTPClientFactory(final ConnectionInstanceData instanceData) {
        final ServiceTracker proxyServiceTracker = TFSCommonUIClientPlugin.getDefault().getProxyServiceTracker();

        if (proxyServiceTracker.getService() != null) {
            if (log.isDebugEnabled()) {
                log.debug("IProxyService is available, returning an EclipseHttpClientFactory"); //$NON-NLS-1$
            }

            return new ProxyServiceHTTPClientFactory(instanceData, proxyServiceTracker);
        }

        if (log.isDebugEnabled()) {
            log.debug("IProxyService is not available, returning a LegacyHttpClientFactory"); //$NON-NLS-1$
        }

        return new LegacyHTTPClientFactory(instanceData);
    }

    @Override
    public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(final ConnectionInstanceData instanceData) {
        return new EclipseTFProxyServerSettingsFactory(instanceData);
    }

    @Override
    public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
        /*
         * Handle federated authentication with a GUI.
         */
        return new DefaultWebServiceFactory(
            getLocale(instanceData),
            new UITransportRequestHandler(
                instanceData,
                (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
    }
}
