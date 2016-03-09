// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.framework.command.JobCommandExecutor;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.RepositoryManagerListener;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.TFSRepositoryUpdatedListener;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheEvent;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheListener;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.file.FileControl;
import com.microsoft.tfs.client.common.ui.controls.vc.folder.FolderControl;
import com.microsoft.tfs.client.common.ui.controls.vc.folder.FolderControlRefreshSupport;
import com.microsoft.tfs.client.common.ui.controls.vc.folder.FolderControlViewState;
import com.microsoft.tfs.client.common.ui.editors.ConnectionSpecificPart;
import com.microsoft.tfs.client.common.ui.editors.SourceControlListener;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFile;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.TFSVersionControlExplorerPlugin;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.client.common.util.CoreAffectedFileCollector;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Editor to browse and manipulate the TFS version control tree.
 */
public class VersionControlEditor extends EditorPart implements ConnectionSpecificPart, ISelectionProvider {
    public static final String ID = "com.microsoft.tfs.client.common.ui.vcexplorer.VersionControlEditor"; //$NON-NLS-1$

    private FolderControl folderControl;
    private final FolderControlViewState folderControlViewState = new FolderControlViewState();
    private FileControl fileControl;
    private RepositoryManagerListener repositoryManagerListener;
    private IPropertyChangeListener propertyChangeListener;

    private final CoreListener coreListener = new CoreListener();
    private final PendingChangeListener pendingChangeListener = new PendingChangeListener();
    private final TFSRepositoryUpdatedListener repositoryUpdatedListener =
        new VersionControlEditorRepositoryUpdatedListener();
    private SourceControlListenerImpl sourceControlListener;

    private final Object repositoryLock = new Object();
    private TFSRepository repository;

    static VersionControlEditor instance;

    private static final Log log = LogFactory.getLog(VersionControlEditor.class);

    /* Code markers */
    public static final CodeMarker CODEMARKER_FILE_REFRESH_START = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor#fileRefreshStart"); //$NON-NLS-1$
    public static final CodeMarker CODEMARKER_FILE_REFRESH_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor#fileRefreshComplete"); //$NON-NLS-1$

    /**
     *
     */
    public VersionControlEditor() {
        super();
        instance = this;

    }

    public static VersionControlEditor getCurrent() {
        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        // DO NOTHING - should never be called.
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        // DO NOTHING - should never be called.
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
     * org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        if (!(input instanceof VersionControlEditorInput)) {
            throw new PartInitException("Invalid Input: Must be VersionControlEditorInput"); //$NON-NLS-1$
        }
        setSite(site);
        setInput(input);
        sourceControlListener = new SourceControlListenerImpl(this);
        TFSCommonUIClientPlugin.getDefault().addSourceControlListener(sourceControlListener);
    }

    /**
     * The version control editor is never dirty.
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * The version control editor never needs to be saved.
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.
     * widgets .Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        final VersionControlEditorInput editorInput = (VersionControlEditorInput) getEditorInput();

        setPartName(editorInput.getName());
        setTitleToolTip(editorInput.getToolTipText());

        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FillLayout());

        final SashForm sashForm = new SashForm(composite, SWT.HORIZONTAL);

        folderControl = new FolderControl(sashForm, SWT.NONE, false, true, isShowDeletedItems());
        fileControl = new FileControl(sashForm, SWT.NONE, isShowDeletedItems());

        sashForm.setWeights(new int[] {
            25,
            75
        });

        getSite().setSelectionProvider(this);

        /* Get the current repository and hook up a change listener */
        repositoryManagerListener = new RepositoryManagerAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
                setRepository(event.getRepository());
                initializeControls();
            }
        };
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepositoryAndAddListener(
                repositoryManagerListener);

        // Detect if logged on
        if (repository != null) {
            // If we are - initialize
            setRepository(repository);
            initializeControls();
        }

        fileControl.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final Object element = selection.getFirstElement();
                if (element instanceof TFSFolder) {
                    final IStructuredSelection currentSelection = (IStructuredSelection) folderControl.getSelection();
                    final TFSFolder currentlySelectedFolder = (TFSFolder) currentSelection.getFirstElement();
                    folderControl.getTreeViewer().setExpandedState(currentlySelectedFolder, true);
                    folderControl.setSelection(new StructuredSelection(element));
                } else if (element instanceof TFSFile) {
                    final Shell shell = Display.getDefault().getActiveShell();
                    if (shell != null) {
                        /*
                         * TODO can we detect if we're inModalContext and pass
                         * the correct value?
                         */
                        ViewFileHelper.viewTFSFile(
                            getRepository(),
                            (TFSFile) element,
                            TFSVersionControlExplorerPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(),
                            false);
                    }
                }
            }
        });

        folderControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                handleFolderSelection(event.getSelection());
            }

        });

        propertyChangeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (UIPreferenceConstants.SHOW_DELETED_ITEMS.equals(event.getProperty())) {
                    final boolean showDeletedItems = isShowDeletedItems();
                    fileControl.setShowDeletedItems(showDeletedItems);
                    folderControl.setShowDeletedItems(showDeletedItems);
                    refresh(false);
                }
            }
        };
        TFSCommonUIClientPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);

        registerMenus("VersionControl.FileControl", fileControl.getTableViewer().getControl(), fileControl); //$NON-NLS-1$
        registerMenus("VersionControl.FolderControl", folderControl.getTreeViewer().getControl(), folderControl); //$NON-NLS-1$

        /*
         * Use reflection to set up key binding context service. Doesn't exist >
         * 3.1.
         */
        try {
            final Class contextServiceClass = Class.forName("org.eclipse.ui.contexts.IContextService"); //$NON-NLS-1$

            if (contextServiceClass != null) {
                final Method getServiceMethod = getSite().getClass().getMethod("getService", new Class[] //$NON-NLS-1$
                {
                    Class.class
                });
                final Object contextSupport = getServiceMethod.invoke(getSite(), new Object[] {
                    contextServiceClass
                });

                if (contextSupport != null) {
                    final Method activateContextMethod = contextServiceClass.getMethod("activateContext", new Class[] //$NON-NLS-1$
                    {
                        String.class
                    });

                    activateContextMethod.invoke(contextSupport, new Object[] {
                        "com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.bindingContext" //$NON-NLS-1$
                    });
                }
            }
        } catch (final Exception e) {
            log.warn("Could not setup key binding context", e); //$NON-NLS-1$
        }
    }

    private boolean isShowDeletedItems() {
        final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
        return preferences.getBoolean(UIPreferenceConstants.SHOW_DELETED_ITEMS);
    }

    private void handleFolderSelection(final ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_FILE_REFRESH_START);

        final IStructuredSelection iss = (IStructuredSelection) selection;
        final TFSRepository repository = getRepository();

        if (iss.size() != 1) {
            fileControl.clear();
        } else if (repository != null) {
            final TFSFolder folder = (TFSFolder) iss.getFirstElement();
            fileControl.refresh(folder);
            try {
                fileControl.setSelectedServerPath(repository, folder.getFullPath());
            } catch (final ServerPathFormatException e) {
                final String messageFormat = "Server path incorrect: {0}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, folder.getFullPath());
                log.error(message, e);
            }
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_FILE_REFRESH_COMPLETE);
    }

    /**
     * Create the menu extension points to allow plugins (including this one) to
     * add menu items.
     */
    private void registerMenus(final String menuId, final Control control, final ISelectionProvider selectionProvider) {
        final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(false);
        menuMgr.add(new Separator("group0")); //$NON-NLS-1$
        menuMgr.add(new Separator("group1")); //$NON-NLS-1$
        menuMgr.add(new Separator("group2")); //$NON-NLS-1$
        menuMgr.add(new Separator("group3")); //$NON-NLS-1$
        menuMgr.add(new Separator("group4")); //$NON-NLS-1$
        menuMgr.add(new Separator("group5")); //$NON-NLS-1$
        menuMgr.add(new Separator("group6")); //$NON-NLS-1$
        menuMgr.add(new Separator("group7")); //$NON-NLS-1$
        menuMgr.add(new Separator("group8")); //$NON-NLS-1$
        menuMgr.add(new Separator("group9")); //$NON-NLS-1$

        menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        final Menu menu = menuMgr.createContextMenu(control);
        control.setMenu(menu);
        getSite().registerContextMenu(menuId, menuMgr, selectionProvider);
    }

    /**
     * Initialize the editor. Called when first created or when logged in to
     * server.
     */
    private void init(final IMemento memento) {
        folderControl.populateTreeUsingServerRoot();
        if (memento != null) {
            folderControlViewState.setSavedState(memento);
        }

        final ServerItemPath[] expanded = folderControlViewState.getPreviouslyExpandedPaths();
        if (expanded != null && expanded.length > 0) {
            folderControl.setExpandedItems(expanded);
        } else {
            folderControl.expandToLevel(TFSItemFactory.getRoot(), 1);
        }

        final ServerItemPath selected = folderControlViewState.getPreviouslySelectedPath();
        if (selected != null) {
            folderControl.setSelectedItem(selected);
        } else {
            folderControl.setSelectedItem(ServerItemPath.ROOT);
        }
        handleFolderSelection(folderControl.getSelection());

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        folderControl.setFocus();
    }

    public void refresh(final boolean refreshWorkspace) {
        refresh(refreshWorkspace, true);
    }

    /**
     *
     * @param refreshWorkspace
     *        <code>true</code> to refresh the workspace's working folder
     *        mappings ({@link Workspace#refresh()}
     * @param tfvc
     *        <code>true</code> if source control is tfvc, false otherwise
     */
    public void refresh(final boolean refreshWorkspace, final boolean tfvc) {
        if (repository == null || !ConnectionHelper.isConnected(repository.getConnection())) {
            return;
        }
        if (!tfvc) {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            page.closeEditor(this, false);
            return;
        }

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (folderControl == null || folderControl.isDisposed() || repository == null) {
                    return;
                }

                final FolderControlRefreshSupport refreshSupport =
                    new FolderControlRefreshSupport(folderControl, repository);

                /*
                 * Clear the pending changes cache held by the tree view, so it
                 * requeries the server for other changes found in extended
                 * items. This only happens here (manual refreshes).
                 */
                fileControl.clearPendingChangesCache();

                /* Refresh the workspace's working folder mappings. */
                if (refreshWorkspace) {
                    repository.getWorkspace().refresh();
                }

                refreshSupport.refresh();
                handleFolderSelection(folderControl.getSelection());
            }
        });
    }

    public void setRepository(final TFSRepository repository) {
        synchronized (repositoryLock) {
            if (repository != null && repository.equals(this.repository)) {
                return;
            }

            if (this.repository != null) {
                coreListener.removeRepository(this.repository);
                this.repository.getPendingChangeCache().removeListener(pendingChangeListener);
                this.repository.removeRepositoryUpdatedListener(repositoryUpdatedListener);
            }

            this.repository = repository;

            /* Connected to a server */
            if (repository != null) {
                coreListener.addRepository(repository);
                repository.getPendingChangeCache().addListener(pendingChangeListener);
                repository.addRepositoryUpdatedListener(repositoryUpdatedListener);
            }
        }

        if (repository != null) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (!fileControl.isDisposed()) {
                        fileControl.setEnabled(true);
                    }

                    if (!folderControl.isDisposed()) {
                        if (ConnectionHelper.isConnected(repository.getConnection())) {
                            folderControlViewState.setCurrentlySelectedItem(folderControl.getSelectedItem());
                            folderControlViewState.setCurrentlyExpandedPaths(folderControl.getExpandedElements());
                        }
                        folderControl.setEnabled(true);
                    }
                }
            });
        }
        /* Not connected */
        else {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (folderControl.isDisposed() == false) {
                        folderControl.setEnabled(false);
                        folderControl.getTreeViewer().setInput(null);
                    }

                    if (fileControl.isDisposed() == false) {
                        fileControl.setEnabled(false);
                        fileControl.getTableViewer().setInput(null);
                        fileControl.setStatusNotConnected();
                    }
                }
            });
        }
    }

    public TFSRepository getRepository() {
        synchronized (repositoryLock) {
            return repository;
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        setRepository(null);

        // Stop this editor listening for events.
        if (repositoryManagerListener != null) {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().removeListener(
                repositoryManagerListener);
        }

        if (propertyChangeListener != null) {
            TFSCommonUIClientPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
                propertyChangeListener);
        }

        if (sourceControlListener != null) {
            TFSCommonUIClientPlugin.getDefault().removeSourceControlListener(sourceControlListener);
        }
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        folderControl.addSelectionChangedListener(listener);
        fileControl.addSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return (fileControl.isFocusControl()
            && fileControl.getSelection() != null
            && !fileControl.getSelection().isEmpty()) ? fileControl.getSelection() : folderControl.getSelection();
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        folderControl.removeSelectionChangedListener(listener);
        fileControl.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        throw new RuntimeException("Setting selection of Version Control Editor is not yet supported"); //$NON-NLS-1$
    }

    public void setSelectedFolder(final ServerItemPath path) {
        folderControl.setSelectedItem(path);
    }

    public void setSelectedFile(final ServerItemPath path) {
        setSelectedFolder(path.getParent());

        fileControl.setSelectedFilename(path.getFilePart());
    }

    @Override
    public boolean closeOnConnectionChange() {
        // This windows listens for server connection changes and refreshes
        // accordingly,
        // therefore it should not close if the user re-connects to a different
        // server.
        return false;
    }

    public void saveState(final IMemento memento) {
        if (!folderControl.isDisposed()) {
            folderControlViewState.setCurrentlySelectedItem(folderControl.getSelectedItem());
            folderControlViewState.setCurrentlyExpandedPaths(folderControl.getExpandedElements());
            folderControlViewState.populateWithStateToSave(memento);
        }
    }

    /**
     * Exposed only for testing.
     */
    public FileControl getFileControl() {
        return fileControl;
    }

    /**
     * Exposed only for testing.
     */
    public FolderControl getFolderControl() {
        return folderControl;
    }

    private class CoreListener extends CoreAffectedFileCollector {
        public CoreListener() {
            /*
             * Don't pay attention to new and undone pending change events.
             * Those are covered by the pending change cache listener.
             */
            super(EventType.ALL.remove(new EventType[] {
                EventType.NEW_PENDING_CHANGE,
                EventType.UNDONE_PENDING_CHANGE
            }));
        }

        @Override
        protected void filesChanged(final Set fileSet) {
            refresh(false);
        }
    }

    public class VersionControlEditorRepositoryUpdatedListener implements TFSRepositoryUpdatedListener {
        @Override
        public void onRepositoryUpdated() {
            refresh(false);
        }

        @Override
        public void onFolderContentChanged(final int changesetID) {
            refresh(false);
        }

        @Override
        public void onGetCompletedEvent(final WorkspaceEventSource source) {
            refresh(false);
        }

        @Override
        public void onLocalWorkspaceScan(final WorkspaceEventSource source) {
            /*
             * Ignore these when they're internal, because we'll get pending
             * change events with more specific information.
             */
            if (source != WorkspaceEventSource.INTERNAL) {
                refresh(false);
            }
        }
    }

    private class PendingChangeListener implements PendingChangeCacheListener {
        private final Object lock = new Object();
        int defer = 0;
        boolean newChanges = false;

        private void refreshOrDefer() {
            synchronized (lock) {
                if (defer > 0) {
                    newChanges = true;
                    return;
                }
            }

            refresh(false);
        }

        @Override
        public void onBeforeUpdatePendingChanges(final PendingChangeCacheEvent event) {
            synchronized (lock) {
                defer++;
            }
        }

        @Override
        public void onAfterUpdatePendingChanges(
            final PendingChangeCacheEvent event,
            final boolean modifiedDuringOperation) {
            synchronized (lock) {
                defer--;

                if (defer == 0 && newChanges == true) {
                    newChanges = false;
                    refresh(false);
                }
            }
        }

        @Override
        public void onPendingChangeAdded(final PendingChangeCacheEvent event) {
            refreshOrDefer();
        }

        @Override
        public void onPendingChangeModified(final PendingChangeCacheEvent event) {
            refreshOrDefer();
        }

        @Override
        public void onPendingChangeRemoved(final PendingChangeCacheEvent event) {
            refreshOrDefer();
        }

        @Override
        public void onPendingChangesCleared(final PendingChangeCacheEvent event) {
            refreshOrDefer();
        }
    }

    private void initializeControls() {
        if (getRepository() != null && getRepository().getConnection() != null) {
            new JobCommandExecutor().execute(
                new VersionControlEditorInitializeCommand(getRepository().getConnection()));
        }
    }

    private class VersionControlEditorInitializeCommand extends TFSConnectedCommand {
        public VersionControlEditorInitializeCommand(final TFSConnection connection) {
            super(connection);
        }

        @Override
        public String getName() {
            return Messages.getString("VersionControlEditor.VersionControlEditorInitializeCommandText"); //$NON-NLS-1$
        }

        @Override
        public String getErrorDescription() {
            return Messages.getString("VersionControlEditor.VersionControlEditorInitializeCommandError"); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return ("Initializing TFVC Source Control Explorer"); //$NON-NLS-1$
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            progressMonitor.beginTask(
                Messages.getString("VersionControlEditor.VersionControlEditorInitializeCommandProgressTitle"), //$NON-NLS-1$
                IProgressMonitor.UNKNOWN);
            try {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (VersionControlEditor.this.getEditorInput() == null) {
                            VersionControlEditor.this.init(null);
                        } else {
                            VersionControlEditor.this.init(((VersionControlEditorInput) getEditorInput()).getMemento());
                        }
                    }
                });
            } finally {
                progressMonitor.done();
            }

            return Status.OK_STATUS;
        }
    }

    public class SourceControlListenerImpl implements SourceControlListener {
        private final VersionControlEditor editor;

        public SourceControlListenerImpl(final VersionControlEditor editor) {
            this.editor = editor;
        }

        @Override
        public void onSourceControlChanged(final boolean tfvc) {
            editor.refresh(false, tfvc);

        }
    }
}
