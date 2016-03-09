// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.connectionconflict;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter.TFSEditorSaveableType;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.connectionconflict.EclipseConnectionConflictHandler;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;

/**
 * A connection conflict handler for the Eclipse Plug-in. Allows users to turn
 * {@link IProject}s offline to connect to a different server / workspace.
 *
 * @threadsafety unknown
 */
public class EclipseUIConnectionConflictHandler extends EclipseConnectionConflictHandler {
    /**
     * Allows the user to resolve a server conflict, caused by trying to connect
     * to a server that is not the currently-connected (default) server.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean resolveServerConflict() {
        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

        /*
         * If no projects are currently managed by TFS, allow this connection to
         * proceed. (Examine all non-connecting projects -- we may be called
         * during a connection and want to ignore ourselves.)
         */
        final IProject[] projects = projectManager.getProjectsOfStatus(ProjectRepositoryStatus.ONLINE);

        if (projects.length == 0) {
            if (promptForSave() == false) {
                return false;
            }

            disconnectDefaultServer();
            return true;
        }

        /*
         * There are managed projects, we need to prompt to disconnect them.
         */

        if (promptForDisconnect() == false) {
            return false;
        }

        /* Prompt to save dirty editors */
        if (promptForSave() == false) {
            return false;
        }

        /*
         * Disconnect these projects from their repository - this will bring the
         * projects offline (but will not persist offline state.) This is done
         * so that we can close the repository gracefully, then close the
         * projects in the background. If the project close fails, the projects
         * will still be in a sane (disconnected) state.
         */

        TFSEclipseClientPlugin.getDefault().getProjectManager().disconnect(projects, false);

        disconnectDefaultServer();

        return true;
    }

    /**
     * Allows the user to resolve a repository conflict, caused by trying to
     * connect to a repository that is not the currently-connected (default)
     * repository.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean resolveRepositoryConflict() {
        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

        /*
         * If no projects are currently managed by TFS, allow this connection to
         * proceed. (Examine all non-connecting projects -- we may be called
         * during a connection and want to ignore ourselves.)
         */
        final IProject[] projects = projectManager.getProjectsOfStatus(ProjectRepositoryStatus.ONLINE);

        if (projects.length == 0) {
            if (promptForSave() == false) {
                return false;
            }

            disconnectDefaultRepository();
            return true;
        }

        /*
         * There are managed projects, we need to prompt to disconnect them.
         */

        if (promptForDisconnect() == false) {
            return false;
        }

        /* Prompt to save dirty editors */
        if (promptForSave() == false) {
            return false;
        }

        /*
         * Disconnect these projects from their repository - this will bring the
         * projects offline (but will not persist offline state.) This is done
         * so that we can close the repository gracefully, then close the
         * projects in the background. If the project close fails, the projects
         * will still be in a sane (disconnected) state.
         *
         * Do not disconnect the server with the repositories.
         */
        TFSEclipseClientPlugin.getDefault().getProjectManager().disconnect(projects, false);

        disconnectDefaultRepository();

        return true;
    }

    private boolean promptForSave() {
        /* Synchronize for visibility */
        final Object cancelLock = new Object();
        final boolean[] cancel = new boolean[1];

        UIHelpers.runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                synchronized (cancelLock) {
                    cancel[0] =
                        EditorHelper.saveAllDirtyEditors(new TFSEditorSaveableFilter(TFSEditorSaveableType.ALL));
                }
            }
        });

        synchronized (cancelLock) {
            return cancel[0];
        }
    }

    private boolean promptForDisconnect() {
        final Shell parentShell = ShellUtils.getWorkbenchShell();

        /* Synchronize for visibility */
        final Object retryLock = new Object();
        final boolean[] retry = new boolean[1];

        UIHelpers.runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                synchronized (retryLock) {
                    retry[0] = MessageDialog.openQuestion(
                        parentShell,
                        Messages.getString("EclipseUIConnectionConflictHandler.DisconnectProjectsPromptTitle"), //$NON-NLS-1$
                        Messages.getString("EclipseUIConnectionConflictHandler.DisconnectProjectsPromptMessage")); //$NON-NLS-1$
                }
            }
        });

        synchronized (retryLock) {
            return retry[0];
        }
    }

    private void disconnectDefaultServer() {
        final ServerManager serverManager = TFSEclipseClientUIPlugin.getDefault().getServerManager();
        final RepositoryManager repositoryManager = TFSEclipseClientUIPlugin.getDefault().getRepositoryManager();

        /* Remove the current repository and server from the manager. */
        if (repositoryManager.getDefaultRepository() != null) {
            repositoryManager.getDefaultRepository().close();
            repositoryManager.removeRepository(repositoryManager.getDefaultRepository());
        }

        if (serverManager.getDefaultServer() != null) {
            serverManager.getDefaultServer().close();
            serverManager.removeServer(serverManager.getDefaultServer());
        }
    }

    private void disconnectDefaultRepository() {
        final RepositoryManager repositoryManager = TFSEclipseClientUIPlugin.getDefault().getRepositoryManager();

        /* Remove the current repository and server from the manager. */
        if (repositoryManager.getDefaultRepository() != null) {
            repositoryManager.getDefaultRepository().close();
            repositoryManager.removeRepository(repositoryManager.getDefaultRepository());
        }
    }

    /**
     * Notifies the user that he/she is trying to connect to a server that is
     * not the currently-connected (default) server.
     */
    @Override
    public void notifyServerConflict() {
        notifyConflict();
    }

    /**
     * Notifies the user that he/she is trying to connect to a repository that
     * is not the currently-connected (default) repository.
     */
    @Override
    public void notifyRepositoryConflict() {
        notifyConflict();
    }

    private void notifyConflict() {
        final Shell parentShell = ShellUtils.getWorkbenchShell();

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                MessageDialog.openWarning(
                    parentShell,
                    Messages.getString("EclipseConnectionConflictHandler.ConnectionExistsDialogTitle"), //$NON-NLS-1$
                    Messages.getString("EclipseConnectionConflictHandler.ConnectionExistsDialogText")); //$NON-NLS-1$
            }
        });
    }
}
