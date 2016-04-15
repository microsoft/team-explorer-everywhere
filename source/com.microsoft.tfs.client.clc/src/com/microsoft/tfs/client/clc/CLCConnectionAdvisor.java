// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.options.shared.OptionLogin;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.common.config.CommonClientConnectionAdvisor;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.DefaultWebServiceFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.util.Check;

/**
 * Extends {@link DefaultConnectionAdvisor} to provide a
 * {@link CLCHTTPClientFactory}, which can use environment variables to cofigure
 * some HTTP settings.
 *
 * @threadsafety thread-safe
 */
public class CLCConnectionAdvisor extends CommonClientConnectionAdvisor {
    private final Command command;

    /**
     * Creates a {@link CLCConnectionAdvisor} that uses the current default
     * {@link Locale} and {@link TimeZone} for all
     * {@link ConnectionInstanceData}s.
     *
     * @param command
     *        the {@link Command} being run (must not be <code>null</code>)
     */
    public CLCConnectionAdvisor(final Command command) {
        super(Locale.getDefault(), TimeZone.getDefault());

        Check.notNull(command, "command"); //$NON-NLS-1$
        this.command = command;
    }

    @Override
    public HTTPClientFactory getHTTPClientFactory(final ConnectionInstanceData instanceData) {
        /*
         * The CLC client factory uses environment variables to configure the
         * global proxy URL, however we need to provide the CredentialsManager.
         */
        final CredentialsManager credentialsManager = CredentialsManagerFactory.getCredentialsManager(
            getPersistenceStoreProvider(instanceData),
            command.usePersistanceCredentialsManager());

        return new CLCHTTPClientFactory(instanceData, credentialsManager);
    }

    @Override
    public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
        /*
         * Handle authentication with interactive prompts.
         */
        return new DefaultWebServiceFactory(
            getLocale(instanceData),
            new CLCTransportRequestHandler(
                instanceData,
                (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData),
                command.getDisplay(),
                command.getInput(),
                command.findOptionType(OptionLogin.class) != null,
                command.persistCredentials(),
                command.usePersistanceCredentialsManager(),
                command.findOptionType(OptionNoPrompt.class) == null));
    }

    @Override
    public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(final ConnectionInstanceData instanceData) {
        return new CLCTFProxyServerSettingsFactory(instanceData, command);
    }
}
