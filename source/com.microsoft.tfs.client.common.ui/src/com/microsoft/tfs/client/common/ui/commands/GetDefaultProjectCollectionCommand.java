// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.catalog.TeamProjectCollectionInfo;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.configuration.QueryProjectCollectionsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.helpers.UIConnectionPersistence;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Gets the "default" TFS project collection (typically the last used project
 * collection) for the given configuration server.
 *
 * If there is not a previously-used TFS workspace (or it cannot be realized),
 * then the first project collection will be used. If there are no project
 * collections, then an error will be returned.
 *
 * If the given connection is a {@link TFSTeamProjectCollection} instead of a
 * {@link TFSConfigurationServer}, it is assumed that this is a 2008 connection
 * and the project collection given will be returned back.
 *
 * The project collection connected will NOT be registered in the server
 * manager.
 *
 * @threadsafety unknown
 */
public class GetDefaultProjectCollectionCommand extends TFSCommand {
    private final TFSConnection connnection;

    private TFSTeamProjectCollection projectCollection;

    public GetDefaultProjectCollectionCommand(final TFSConnection connnection) {
        Check.notNull(connnection, "connnection"); //$NON-NLS-1$

        this.connnection = connnection;
    }

    @Override
    public String getName() {
        return Messages.getString("GetDefaultProjectCollectionCommand.Name"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("GetDefaultProjectCollectionCommand.ErrorDescription"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("GetDefaultProjectCollectionCommand.Name", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        /*
         * 2008 does not have configuration servers: simply return the given
         * project collection back.
         */
        if (connnection instanceof TFSTeamProjectCollection) {
            projectCollection = (TFSTeamProjectCollection) connnection;
            return Status.OK_STATUS;
        }

        final TFSConfigurationServer configurationServer = (TFSConfigurationServer) this.connnection;

        GUID collectionID = UIConnectionPersistence.getInstance().getLastUsedProjectCollection(configurationServer);

        /*
         * If there is not a persisted collection id, query the server for the
         * collections.
         */
        if (collectionID == null) {
            final QueryProjectCollectionsCommand queryCommand = new QueryProjectCollectionsCommand(configurationServer);
            final IStatus queryStatus = new CommandExecutor().execute(queryCommand);

            if (!queryStatus.isOK()) {
                return queryStatus;
            }

            final TeamProjectCollectionInfo[] collectionInfo = queryCommand.getProjectCollections();

            if (collectionInfo == null || collectionInfo.length == 0) {
                return new Status(
                    IStatus.ERROR,
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("GetDefaultProjectCollectionCommand.NoProjectCollections"), //$NON-NLS-1$
                    null);
            }

            collectionID = collectionInfo[0].getIdentifier();
        }

        projectCollection = configurationServer.getTeamProjectCollection(collectionID);

        if (projectCollection == null) {
            return new Status(
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    "Could not open default team project collection {0}", //$NON-NLS-1$
                    collectionID.getGUIDString()),
                null);
        }

        return Status.OK_STATUS;
    }

    public TFSTeamProjectCollection getConnection() {
        return projectCollection;
    }
}
