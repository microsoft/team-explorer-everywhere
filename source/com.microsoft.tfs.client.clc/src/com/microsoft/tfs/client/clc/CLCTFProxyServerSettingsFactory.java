// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionProxy;
import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettings;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettingsFactory;
import com.microsoft.tfs.util.Check;

/**
 * A CLC TF proxy settings factory that looks for the proxy option on the
 * command line. The super class handles environment variables and other
 * settings.
 *
 * @threadsafety unknown
 */
public class CLCTFProxyServerSettingsFactory extends DefaultTFProxyServerSettingsFactory {
    private static final Log log = LogFactory.getLog(CLCTFProxyServerSettingsFactory.class);

    private final Command command;

    public CLCTFProxyServerSettingsFactory(final ConnectionInstanceData connectionInstanceData, final Command command) {
        super(connectionInstanceData);

        Check.notNull(command, "command"); //$NON-NLS-1$
        this.command = command;
    }

    @Override
    public TFProxyServerSettings newProxyServerSettings() {
        final Option o = command.findOptionType(OptionProxy.class);
        if (o != null) {
            final String uri = ((OptionProxy) o).getURI().toString();

            log.debug(MessageFormat.format("TF download option found, using proxy {0}", uri)); //$NON-NLS-1$
            return new DefaultTFProxyServerSettings(uri);
        }

        return super.newProxyServerSettings();
    }

    @Override
    public void dispose(final TFProxyServerSettings proxyServerSettings) {
        /*
         * Let super dispose if we didn't use our own implementation because of
         * the option.
         */
        if (command.findOptionType(OptionProxy.class) == null) {
            super.dispose(proxyServerSettings);
        }
    }
}
