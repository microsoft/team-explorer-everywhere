// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.query;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.ServerManagerAdapter;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.wit.QueryDocumentService;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocumentDirtyListener;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocumentSaveListener;
import com.microsoft.tfs.core.clients.workitem.query.ResultOptionsColumnWidthPersistence;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.util.MementoRepository;

public abstract class BaseQueryDocumentEditor extends EditorPart {
    private TFSServer server;
    private QueryDocument queryDocument;
    private QueryDocumentSaveListener saveListener;
    private QueryDocumentEditorEnabledChangedListener enabledChangedListener;

    private ServerManagerListener serverManagerListener;

    public TFSServer getServer() {
        return server;
    }

    public boolean isConnected() {
        final ServerManager serverManager = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager();
        final TFSServer defaultServer = serverManager.getDefaultServer();
        return defaultServer == server;
    }

    public void setEnabledChangedListener(final QueryDocumentEditorEnabledChangedListener listener) {
        enabledChangedListener = listener;
    }

    public void fireEnabledChanged(final boolean enabled) {
        if (enabledChangedListener != null) {
            enabledChangedListener.onEnabledChanged(this, enabled);
        }
    }

    public QueryDocument getQueryDocument() {
        return queryDocument;
    }

    public void onQueryDocumentRenamed(final String newName) {
        queryDocument.updateName(newName);
        setTitleAndTooltip();
    }

    @Override
    public void init(final IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof IPathEditorInput) {
            final IPathEditorInput pathEditorInput = (IPathEditorInput) input;
            final File file = pathEditorInput.getPath().toFile();
            input = createQueryDocumentEditorInputForFile(file);
        }

        try {
            /*
             * Use reflection to build the input from a file.
             * FileStoreEditorInput is new for Eclipse 3.3.
             */
            final Class fileStoreEditorInputClass = Class.forName("org.eclipse.ui.ide.FileStoreEditorInput"); //$NON-NLS-1$

            if (fileStoreEditorInputClass.isInstance(input)) {
                final Method getUriMethod = fileStoreEditorInputClass.getMethod("getURI", new Class[0]); //$NON-NLS-1$
                final URI fileInputUri = (URI) getUriMethod.invoke(input, new Object[0]);

                final File file = new File(fileInputUri);
                input = createQueryDocumentEditorInputForFile(file);
            }
        } catch (final PartInitException e) {
            /* Bubble these up, not reflection errors */
            throw e;
        } catch (final Exception e) {
            /* Supress these (Reflection errors) */
        }

        if (!(input instanceof QueryDocumentEditorInput)) {
            final String messageFormat = "attempt to open editor [{0}] with invalid editor input: {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getID(), input);
            throw new PartInitException(message);
        }

        final QueryDocumentEditorInput queryDocumentEditorInput = (QueryDocumentEditorInput) input;

        if (!getID().equals(queryDocumentEditorInput.getTargetEditorID())) {
            final String messageFormat = "attempt to open editor [{0}] with different target id: [{1}]"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, getID(), queryDocumentEditorInput.getTargetEditorID());
            throw new PartInitException(message);
        }

        server = queryDocumentEditorInput.getServer();
        queryDocument = queryDocumentEditorInput.getQueryDocument();

        setSite(site);
        setInput(input);

        server.getQueryDocumentService().incrementReferences(queryDocument);

        saveListener = new QueryDocumentSaveListener() {
            @Override
            public void onQueryDocumentSaved(final QueryDocument queryDocument) {
                setTitleAndTooltip();
            }
        };

        queryDocument.addSaveListener(saveListener);
    }

    private QueryDocumentEditorInput createQueryDocumentEditorInputForFile(final File file) throws PartInitException {
        final TFSServer defaultServer =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();
        if (defaultServer == null) {
            throw new PartInitException(Messages.getString("BaseQueryDocumentEditor.NotConnectedToTFS")); //$NON-NLS-1$
        }

        try {
            final QueryDocumentService documentService = defaultServer.getQueryDocumentService();
            final QueryDocument queryDocument = documentService.getQueryDocumentForFile(file);
            return new QueryDocumentEditorInput(defaultServer, queryDocument, getID());
        } catch (final RuntimeException e) {
            throw new PartInitException(e.getMessage());
        }
    }

    private void setTitleAndTooltip() {
        final IEditorInput editorInput = getEditorInput();

        setPartName(editorInput.getName() + " " + getTitleSuffix()); //$NON-NLS-1$
        setTitleToolTip(editorInput.getToolTipText());
    }

    @Override
    public void dispose() {
        if (queryDocument != null) {
            queryDocument.removeSaveListener(saveListener);

            if (server != null) {
                final int refCount = server.getQueryDocumentService().decrementReferences(queryDocument);

                if (refCount <= 0) {
                    onLastReferenceClosing(queryDocument);
                }
            }
        }

        if (serverManagerListener != null) {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().removeListener(
                serverManagerListener);
            serverManagerListener = null;
        }

        super.dispose();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        setTitleAndTooltip();
        doCreatePartControl(parent, queryDocument);

        queryDocument.addDirtyListener(new DirtyListener());

        serverManagerListener = new ServerManagerAdapter() {
            @Override
            public void onServerAdded(final ServerManagerEvent event) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        if (event.getServer() == server) {
                            setDisconnected(false);
                        }

                        /*
                         * See if we've just reconnected - the server objects
                         * will differ, but they may refer to the same endpoint
                         * w/ the same authentication. If so, swap in the new
                         * connection and reenable this control.
                         */
                        else if (event.getServer().connectionsEquivalent(server)) {
                            server = event.getServer();
                            setDisconnected(false);
                        }
                    }
                });
            }

            @Override
            public void onServerRemoved(final ServerManagerEvent event) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        if (event.getServer() == server) {
                            setDisconnected(true);
                        }
                    }
                });
            }
        };
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().addListener(serverManagerListener);
    }

    protected void onLastReferenceClosing(final QueryDocument queryDocument) {
        if (queryDocument.isDirty()) {
            queryDocument.reset();
        }
        if (server != null) {
            ResultOptionsColumnWidthPersistence.persist(
                new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getCachePersistenceStore()),
                queryDocument);
        }
    }

    @Override
    public boolean isDirty() {
        return getQueryDocument().isDirty();
    }

    protected abstract String getID();

    protected abstract String getTitleSuffix();

    protected abstract void doCreatePartControl(Composite parent, QueryDocument queryDocument);

    public abstract void setDisconnected(boolean disconnected);

    public abstract void setEnabled(boolean enabled);

    private class DirtyListener implements QueryDocumentDirtyListener {
        @Override
        public void dirtyStateChanged(final QueryDocument queryDocument) {
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }
}
