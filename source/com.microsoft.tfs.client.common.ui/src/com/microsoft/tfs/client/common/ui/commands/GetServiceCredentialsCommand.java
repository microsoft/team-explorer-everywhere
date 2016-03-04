// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.net.URI;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.FederatedAuthenticationHelpers;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Gets an OAuth WRAP access token for the username, password, and domain
 * credentials in the given {@link Credentials}.
 *
 * @threadsafety unknown
 */
public class GetServiceCredentialsCommand extends TFSCommand {
    private static final Log log = LogFactory.getLog(GetServiceCredentialsCommand.class);

    private final URI serverURI;
    private final UsernamePasswordCredentials credentials;
    private final URI acsIssuerURI;
    private final String acsAuthRealm;

    private String wrapAccessToken;

    /**
     * Creates a {@link GetServiceCredentialsCommand} that reads simple
     * credentials from the given {@link Profile}.
     *
     * @param profile
     *        the {@link Profile} whose credentials will be resolved into a
     *        service credentials token (must not be <code>null</code>)
     * @param acsIssuerURI
     *        the URI of the ACS issuer host that can transform username,
     *        domain, and password into service credentials; this value can be
     *        found in a header when TFS rejects the initial authentication
     *        attempt with a 302 (must not be <code>null</code>)
     */
    public GetServiceCredentialsCommand(
        final URI serverURI,
        final Credentials credentials,
        final URI acsIssuerURI,
        final String acsAuthRealm) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$
        Check.notNull(acsIssuerURI, "acsIssuerURI"); //$NON-NLS-1$
        Check.notNull(acsAuthRealm, "acsAuthRealm"); //$NON-NLS-1$
        Check.isTrue(
            credentials instanceof UsernamePasswordCredentials,
            "credentials instanceof UsernamePasswordCredentials"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = (UsernamePasswordCredentials) credentials;
        this.acsIssuerURI = acsIssuerURI;
        this.acsAuthRealm = acsAuthRealm;
    }

    @Override
    public String getName() {
        return MessageFormat.format(
            Messages.getString("GetServiceCredentialsCommand.CommandNameFormat"), //$NON-NLS-1$
            credentials.getUsername());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("GetServiceCredentialsCommand.CommandErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            Messages.getString("GetServiceCredentialsCommand.CommandNameFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            credentials.getUsername());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String message = getName();

        log.info(message);

        final HTTPClientFactory httpClientFactory = new UIClientConnectionAdvisor().getHTTPClientFactory(
            new ConnectionInstanceData(serverURI, new AtomicReference<Credentials>(credentials), GUID.newGUID()));

        final String token = FederatedAuthenticationHelpers.getWRAPAccessToken(
            httpClientFactory,
            acsIssuerURI,
            acsAuthRealm,
            credentials.getUsername(),
            credentials.getPassword());

        if (token == null) {
            return new Status(
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("GetServiceCredentialsCommand.CouldNotGetAccessTokenFromURIFormat"), //$NON-NLS-1$
                    acsIssuerURI,
                    acsAuthRealm),
                null);
        }

        this.wrapAccessToken = token;
        return Status.OK_STATUS;
    }

    public String getWRAPAccessToken() {
        return this.wrapAccessToken;
    }
}
