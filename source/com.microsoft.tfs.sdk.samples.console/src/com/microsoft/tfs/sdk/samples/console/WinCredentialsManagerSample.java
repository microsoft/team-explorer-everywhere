// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import java.net.URI;
import java.net.URISyntaxException;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.internal.WinCredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 * This sample demonstrates reading credentials from Window Credentials Manager
 * and using those credentials to connect to TFS. It also demonstrates saving
 * credentials in Windows Credentials Manager.
 */
public class WinCredentialsManagerSample {
    public static void main(final String[] args) throws InterruptedException {

        // This sample only works on Windows
        if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return;
        }

        // Read credentials from Windows Credentials Manager
        final Credentials credentials = createsCredentials();
        if (credentials == null) {
            return;
        }

        URI httpProxyURI = null;

        if (!StringUtil.isNullOrEmpty(ConsoleSettings.HTTP_PROXY_URL)) {
            try {
                httpProxyURI = new URI(ConsoleSettings.HTTP_PROXY_URL);
            } catch (final URISyntaxException e) {
                // Do Nothing
            }
        }

        final ConsoleSamplesConnectionAdvisor advisor = new ConsoleSamplesConnectionAdvisor(httpProxyURI);

        /*
         * Connect using credentials retrieved from Windows Credentials Manager
         */
        final TFSTeamProjectCollection tpc =
            new TFSTeamProjectCollection(URIUtils.newURI(ConsoleSettings.COLLECTION_URL), credentials, advisor);

        System.out.println("Authorized User: " + tpc.getAuthorizedTFSUser().getUsername()); //$NON-NLS-1$

    }

    /**
     * Reads credentials from Windows Credentials Manager if available
     * otherwise, reads credentials from user settings and stores them in
     * Windows credentials manager (In case the user sets SAVE_CREDENTIALS)
     *
     */
    private static Credentials createsCredentials() {
        Credentials credentials = null;

        final WinCredentialsManager credentialsManager = new WinCredentialsManager();
        URI serverURI = null;
        try {
            serverURI = new URI(ConsoleSettings.SERVER_URL);
        } catch (final URISyntaxException e) {
            // Do Nothing
        }

        CachedCredentials cachedCredentials = credentialsManager.getCredentials(serverURI);

        if (cachedCredentials != null) {
            credentials =
                new UsernamePasswordCredentials(cachedCredentials.getUsername(), cachedCredentials.getPassword());
        } else if (!StringUtil.isNullOrEmpty(ConsoleSettings.USERNAME)) {
            credentials = new UsernamePasswordCredentials(ConsoleSettings.USERNAME, ConsoleSettings.PASSWORD);
            if (ConsoleSettings.SAVE_CREDENTIALS && credentialsManager.canWrite()) {
                cachedCredentials = new CachedCredentials(serverURI, credentials);
                credentialsManager.setCredentials(cachedCredentials);
            }
        }

        return credentials;
    }

}
