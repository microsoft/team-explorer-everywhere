// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;
import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class ConnectionDataProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final RepositoryManager repositoryManager =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();

        final TFSRepository[] repositories = repositoryManager.getRepositories();

        if (repositories.length == 0) {
            return Messages.getString("ConnectionDataProvider.NoActiveConnections", locale); //$NON-NLS-1$
        } else if (repositories.length == 1) {
            return getSingleRepositoryData(repositories[0], locale);
        } else {
            return getMultipleRepositoryData(repositories, locale);
        }

    }

    private final Object getSingleRepositoryData(final TFSRepository repository, final Locale locale) {
        final Properties properties = new Properties();

        final VersionControlClient vcClient = repository.getVersionControlClient();
        final Workspace workspace = repository.getWorkspace();

        properties.setProperty(
            Messages.getString("ConnectionDataProvider.ColumnNameUrl", locale), //$NON-NLS-1$
            vcClient.getConnection().getBaseURI().toString());

        String httpProxyUrl = ""; //$NON-NLS-1$
        if (vcClient.getConnection().getHTTPClient().getHostConfiguration().getProxyHost() != null) {
            if (vcClient.getConnection().getHTTPClient().getHostConfiguration().getProxyPort() == -1) {
                httpProxyUrl = vcClient.getConnection().getHTTPClient().getHostConfiguration().getProxyHost();
            } else {
                httpProxyUrl = MessageFormat.format(
                    "{0}:{1}", //$NON-NLS-1$
                    vcClient.getConnection().getHTTPClient().getHostConfiguration().getProxyHost(),
                    Integer.toString(vcClient.getConnection().getHTTPClient().getHostConfiguration().getProxyPort()));
            }
        }

        properties.setProperty(
            Messages.getString("ConnectionDataProvider.ColumnNameHTTPProxy", locale), //$NON-NLS-1$
            httpProxyUrl);

        properties.setProperty(
            Messages.getString("ConnectionDataProvider.ColumnNameGuid", locale), //$NON-NLS-1$
            vcClient.getServerGUID().toString());

        properties.setProperty(
            Messages.getString("ConnectionDataProvider.ColumnNameWorkspace", locale), //$NON-NLS-1$
            workspace.getName());

        properties.setProperty(
            Messages.getString("ConnectionDataProvider.ColumnNameUser", locale), //$NON-NLS-1$
            vcClient.getConnection().getAuthorizedIdentity().getDisplayName());

        final TFProxyServerSettings tfProxyServerSettings = vcClient.getConnection().getTFProxyServerSettings();
        String tfProxyUrl = ""; //$NON-NLS-1$

        if (tfProxyServerSettings.isAvailable()) {
            tfProxyUrl = tfProxyServerSettings.getURL().toString();
        }

        properties.setProperty(
            Messages.getString("ConnectionDataProvider.ColumnNameTFSProxy", locale), //$NON-NLS-1$
            tfProxyUrl);

        return properties;
    }

    private final Object getMultipleRepositoryData(final TFSRepository[] repositories, final Locale locale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("ConnectionDataProvider.ColumnNameUrl", locale), //$NON-NLS-1$
            Messages.getString("ConnectionDataProvider.ColumnNameGuid", locale), //$NON-NLS-1$
            Messages.getString("ConnectionDataProvider.ColumnNameWorkspace", locale), //$NON-NLS-1$
            Messages.getString("ConnectionDataProvider.ColumnNameUser", locale), //$NON-NLS-1$
            Messages.getString("ConnectionDataProvider.ColumnNameTFSProxy", locale) //$NON-NLS-1$
        });

        for (int i = 0; i < repositories.length; i++) {
            final VersionControlClient vcClient = repositories[i].getVersionControlClient();
            final Workspace workspace = repositories[i].getWorkspace();

            final TFProxyServerSettings tfProxyServerSettings = vcClient.getConnection().getTFProxyServerSettings();
            String tfProxyUrl = null;

            if (tfProxyServerSettings.isAvailable()) {
                tfProxyUrl = tfProxyServerSettings.getURL().toString();
            }

            final Row row = new Row(new String[] {
                vcClient.getConnection().getBaseURI().toString(),
                vcClient.getServerGUID().toString(),
                workspace.getName(),
                vcClient.getConnection().getAuthorizedIdentity().getDisplayName(),
                tfProxyUrl
            });

            table.addRow(row);
        }

        return table;
    }
}
