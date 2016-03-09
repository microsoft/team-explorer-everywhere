// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.productplugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.autoconnect.AutoConnector;
import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.ui.helpers.FileViewer;
import com.microsoft.tfs.client.common.ui.wizard.teamprojectwizard.ITeamProjectWizard;
import com.microsoft.tfs.core.util.MementoRepository;

/**
 * Represents a UI product plug-in that can influence connection management and
 * preference storage.
 *
 * @threadsafety unknown
 */
public interface TFSProductPlugin {
    /**
     * @return this plugin's auto connector.
     */
    AutoConnector getAutoConnector();

    /**
     * @return this plugin's preference store.
     * @see AbstractUIPlugin#getPreferenceStore()
     */
    IPreferenceStore getPreferenceStore();

    /**
     * @return the {@link ServerManager} this product uses
     */
    ServerManager getServerManager();

    /**
     * @return the {@link RepositoryManager} this product uses
     */
    RepositoryManager getRepositoryManager();

    /**
     * @return an array of {@link TFSRepository}s that are immutable and cannot
     *         be removed (for the Eclipse plugin, this is the current
     *         repository that projects are bound to)
     */
    TFSRepository[] getImmutableRepositories();

    /**
     * @return the {@link ConnectionConflictHandler} this product uses
     */
    ConnectionConflictHandler getConnectionConflictHandler();

    /**
     * @return the preference storage key prefix to use for this product when
     *         lower layers (common client, core) store memento preferences via
     *         {@link MementoRepository}
     */
    String getMementoPreferenceKeyPrefix();

    /**
     * @return the team project wizard for this product (never <code>null</code>
     *         )
     */
    ITeamProjectWizard getTeamProjectWizard();

    /**
     * @return the file viewer implementation for this product, or
     *         <code>null</code> if no special implementation is available
     */
    FileViewer getFileViewer();
}
