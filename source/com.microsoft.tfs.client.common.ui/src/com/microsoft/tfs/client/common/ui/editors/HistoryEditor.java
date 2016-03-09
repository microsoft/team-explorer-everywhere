// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.editors;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.RepositoryAction;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemNode;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickEvent;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.DoubleClickListener;
import com.microsoft.tfs.client.common.ui.controls.vc.history.HistoryCombinedControl;
import com.microsoft.tfs.client.common.ui.controls.vc.history.HistoryInput;
import com.microsoft.tfs.client.common.ui.controls.vc.history.IHistoryControl;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.GetTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.RollbackTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class HistoryEditor extends EditorPart {
    public static final String ID = "com.microsoft.tfs.client.common.ui.editors.HistoryEditor"; //$NON-NLS-1$

    public static final CodeMarker HISTORY_EDITOR_LOADED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.editors.HistoryEditor#editorLoaded"); //$NON-NLS-1$

    private final DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    private TFSRepository repository;
    private final HistoryEditorRepositoryListener repositoryManagerListener = new HistoryEditorRepositoryListener();

    private Text locationText;
    private HistoryCombinedControl historyControl;

    private IAction refreshAction;
    private DetailsAction detailsAction;
    private ViewChangesetAction viewAction;
    private CompareAction compareAction;
    private GetThisVersionAction getThisVersionAction;
    private RollbackAction rollbackAction;
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private final SingleListenerFacade selectionChangedListeners =
        new SingleListenerFacade(ISelectionChangedListener.class);

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        /* Should never be called as isSaveAllowed should return false. */
        throw new RuntimeException("Saving history results is not implemented."); //$NON-NLS-1$
    }

    @Override
    public void doSaveAs() {
        /* Should never be called as isSaveAllowed should return false. */
        throw new RuntimeException("Saving history results is not implemented."); //$NON-NLS-1$
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void createPartControl(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        /* Compute metrics in pixels */
        final GC gc = new GC(composite);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        final int horizontalSpacing =
            Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING);
        final int verticalSpacing = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);
        final int marginWidth = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
        final int marginHeight = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN);

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = horizontalSpacing;
        layout.verticalSpacing = verticalSpacing;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        final Composite locationComposite = new Composite(composite, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(locationComposite);

        final GridLayout locationLayout = new GridLayout(3, false);
        locationLayout.horizontalSpacing = horizontalSpacing;
        locationLayout.verticalSpacing = verticalSpacing;
        locationLayout.marginWidth = marginWidth;
        locationLayout.marginHeight = marginHeight;
        locationComposite.setLayout(locationLayout);

        final Label locationLabel = new Label(locationComposite, SWT.NONE);
        locationLabel.setText(Messages.getString("HistoryEditor.SourceLocationLabel")); //$NON-NLS-1$

        locationText = new Text(locationComposite, SWT.BORDER | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(locationText);

        final Button refreshButton = new Button(locationComposite, SWT.PUSH);

        /* Icons in buttons look good on Windows, strange elsewhere. */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            refreshButton.setImage(
                imageHelper.getImage(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_REFRESH)));
        }

        refreshButton.setText(Messages.getString("HistoryEditor.RefreshActionText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(refreshButton);

        historyControl = new HistoryCombinedControl(composite, SWT.FULL_SELECTION | SWT.MULTI);
        GridDataBuilder.newInstance().grab().fill().hSpan(3).applyTo(historyControl);

        historyControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                updateStatusLine((IStructuredSelection) event.getSelection());
            }
        });

        getSite().setSelectionProvider(historyControl);

        createActions();

        // getEditorSite().getActionBars().setGlobalActionHandler(
        // ActionFactory.COPY.getId(),
        // historyControl.getCopyAction());
        // getEditorSite().getActionBars().updateActionBars();
        refreshButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                refresh();

            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                refresh();
            }
        });

        setPartName(Messages.getString("HistoryEditor.PartName")); //$NON-NLS-1$
        contributeActions();

        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().addListener(
            repositoryManagerListener);
    }

    @Override
    public void dispose() {
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().removeListener(
            repositoryManagerListener);
        imageHelper.dispose();
    }

    public void run() {
        final HistoryInput historyInput = (HistoryInput) getEditorInput();

        if (historyInput == null) {
            repository = null;

            refreshAction.setEnabled(false);
            detailsAction.setEnabled(false);
            viewAction.setEnabled(false);
            compareAction.setEnabled(false);
            getThisVersionAction.setEnabled(false);
            rollbackAction.setEnabled(false);

            historyControl.setInput(null);
            locationText.setText(""); //$NON-NLS-1$
            setPartName(Messages.getString("HistoryEditor.PartName")); //$NON-NLS-1$

            return;
        }

        /*
         * We hook up repository listeners to know when the repository goes
         * offline. Update those if necessary.
         */
        repository = historyInput.getRepository();

        refreshAction.setEnabled(true);

        detailsAction.setEnabled(true);
        detailsAction.setRepository(repository);

        viewAction.setEnabled(true);
        viewAction.setRepository(repository);
        viewAction.setHistoryInput(historyInput);

        compareAction.setEnabled(true);
        compareAction.setRepository(repository);
        compareAction.setHistoryInput(historyInput);

        getThisVersionAction.setEnabled(true);
        getThisVersionAction.setRepository(repository);
        getThisVersionAction.setHistoryInput(historyInput);

        rollbackAction.setEnabled(true);
        rollbackAction.setRepository(repository);
        rollbackAction.setHistoryInput(historyInput);

        final String sourceLocation =
            ServerPath.isServerPath(historyInput.getHistoryItem()) ? historyInput.getHistoryItem()
                : repository.getWorkspace().getMappedServerPath(historyInput.getHistoryItem());

        historyControl.setInput(historyInput);
        locationText.setText(sourceLocation);

        final String messageFormat = Messages.getString("HistoryEditor.PartNameFormat"); //$NON-NLS-1$
        final String message =
            MessageFormat.format(messageFormat, ServerPath.getFileName(historyInput.getHistoryItem()));
        setPartName(message);

        CodeMarkerDispatch.dispatch(HISTORY_EDITOR_LOADED);
    }

    public void refresh() {
        historyControl.refresh();
    }

    private void updateStatusLine(final IStructuredSelection selection) {
        final String message = getStatusLineMessage(selection);
        getEditorSite().getActionBars().getStatusLineManager().setMessage(message);
    }

    private String getStatusLineMessage(final IStructuredSelection selection) {
        if (selection.size() == 1) {
            final Changeset changeset = (Changeset) selection.getFirstElement();
            final int id = changeset.getChangesetID();
            final int count = changeset.getChanges() != null ? changeset.getChanges().length : 0;
            final String owner = changeset.getOwnerDisplayName();
            final String date = dateFormat.format(changeset.getDate().getTime());

            if (count == 1) {
                final String messageFormat = Messages.getString("HistoryEditor.SingleSelectOneChangeStatusFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, Integer.toString(id), owner, date);

            } else {
                final String messageFormat = Messages.getString("HistoryEditor.SingleSelectMultiChangeStatusFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, Integer.toString(id), count, owner, date);
            }
        }

        if (selection.size() > 1) {
            final String messageFormat = Messages.getString("HistoryEditor.MultiSelectStatusFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, selection.size());
        }

        return null;
    }

    private void createActions() {
        refreshAction = new RefreshAction();

        detailsAction = new DetailsAction(historyControl, getEditorSite(), historyControl);

        viewAction = new ViewChangesetAction(historyControl, getEditorSite());

        compareAction = new CompareAction(historyControl, historyControl.getShell());

        getThisVersionAction = new GetThisVersionAction(historyControl, historyControl);

        rollbackAction = new RollbackAction(historyControl, historyControl.getShell());
    }

    private void contributeActions() {
        // final IToolBarManager toolBarManager =
        // getEditorSite().getActionBars().getToolBarManager();
        // toolBarManager.add(viewAction);
        // toolBarManager.add(compareAction);
        // toolBarManager.add(new Separator());
        // toolBarManager.add(refreshAction);
        // toolBarManager.add(new
        // Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        historyControl.registerContextMenu(getSite());
        historyControl.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final String groupId = StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS;

                manager.appendToGroup(groupId, compareAction);
                manager.appendToGroup(groupId, viewAction);
                manager.appendToGroup(groupId, detailsAction);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, getThisVersionAction);
                manager.appendToGroup(groupId, rollbackAction);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, refreshAction);
            }
        });

        historyControl.addDoubleClickListener(new DoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                detailsAction.run();
            }
        });
    }

    @Override
    public void setFocus() {
        historyControl.setFocus();
    }

    private class RefreshAction extends Action {
        public RefreshAction() {
            setText(Messages.getString("HistoryEditor.RefreshActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryEditor.RefreshActionTooltip")); //$NON-NLS-1$
            setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_REFRESH));
            setEnabled(false);
        }

        @Override
        public void run() {
            refresh();
        }
    }

    private static class DetailsAction extends RepositoryAction {
        private final IEditorSite site;
        private final IHistoryControl control;

        public DetailsAction(
            final ISelectionProvider selectionProvider,
            final IEditorSite site,
            final IHistoryControl control) {
            super(selectionProvider, null);

            this.site = site;
            this.control = control;

            setText(Messages.getString("HistoryEditor.DetailsActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryEditor.DetailsActionTooltip")); //$NON-NLS-1$
            setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_DETAILS));
        }

        @Override
        protected void doRun(final TFSRepository repository) {
            /*
             * TODO maybe support showing in a view instead of dialog?
             */
            // final String uiType =
            // TFSCommonUiPreferences.getHistoryDetailsUIType();
            //
            // if (TFSCommonUiPreferences.UI_TYPE_VIEW.equals(uiType))
            // {
            // ChangesetDetailsView changesetDetailsView;
            // try
            // {
            // changesetDetailsView = (ChangesetDetailsView)
            // site.getPage().showView(ChangesetDetailsView.ID);
            // }
            // catch (final PartInitException e)
            // {
            // throw new RuntimeException(e);
            // }
            //
            // changesetDetailsView.setChangeset(fullChangeset, repository);
            // }
            final Changeset changeset = (Changeset) getSelectionFirstElement();
            final int changesetID = changeset.getChangesetID();

            final ViewChangesetDetailsTask task =
                new ViewChangesetDetailsTask(site.getShell(), repository, changesetID);

            task.run();

            if (task.wasChangesetUpdated()) {
                control.refresh();
            }
        }

        @Override
        protected boolean computeEnablement(final IStructuredSelection selection) {
            return selection.size() == 1;
        }
    }

    private static class ViewChangesetAction extends RepositoryAction {
        private final IEditorSite site;
        private HistoryInput historyInput;

        public ViewChangesetAction(final ISelectionProvider selectionProvider, final IEditorSite site) {
            super(selectionProvider, null);

            this.site = site;

            setText(Messages.getString("HistoryEditor.ViewActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryEditor.ViewActionTooltip")); //$NON-NLS-1$
            setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_VIEW));
        }

        public void setHistoryInput(final HistoryInput historyInput) {
            this.historyInput = historyInput;
        }

        @Override
        protected void doRun(final TFSRepository repository) {
            final Changeset changeset = (Changeset) getSelectionFirstElement();

            final Item item = changeset.getChanges()[0].getItem();

            final String localFileName = ServerPath.getFileName(item.getServerItem());

            final String localItem =
                item.downloadFileToTempLocation(repository.getVersionControlClient(), localFileName).getAbsolutePath();

            ViewFileHelper.viewLocalFileOrFolder(localItem, site.getPage(), false);
        }

        @Override
        protected boolean computeEnablement(final IStructuredSelection selection) {
            if (selection.size() != 1) {
                return false;
            }

            if (historyInput == null || !historyInput.isSingleItem()) {
                return false;
            }

            final Changeset changeset = (Changeset) selection.getFirstElement();

            final Change[] changes = changeset.getChanges();

            if (changes == null || changes.length == 0) {
                return false;
            }

            return changes[0].getItem().getItemType() != ItemType.FOLDER;
        }
    }

    private static class GetThisVersionAction extends RepositoryAction {
        private HistoryInput historyInput;
        private final IHistoryControl control;

        public GetThisVersionAction(final ISelectionProvider selectionProvider, final IHistoryControl control) {
            super(selectionProvider, null);

            this.control = control;

            setText(Messages.getString("HistoryEditor.GetThisVersionActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryEditor.GetThisVersionTooltip")); //$NON-NLS-1$
        }

        public void setHistoryInput(final HistoryInput historyInput) {
            this.historyInput = historyInput;
        }

        @Override
        protected void doRun(final TFSRepository repository) {
            final Changeset changeset = (Changeset) getSelectionFirstElement();

            /*
             * Must use the current selection's item path for the get, so we can
             * support getting items before a rename (path in selection differs
             * from the history input's path) in item mode TFS 2005/2008
             * servers. TFS 2010 servers will disable the get menu action for
             * items before a rename or merge (when the path is different), but
             * this method would work there, too.
             */
            if (changeset != null) {
                final int changesetId = changeset.getChangesetID();
                final VersionSpec versionSpec = new ChangesetVersionSpec(changesetId);

                /*
                 * If this is a single item history, pull the item path from the
                 * changeset item. If this is a recursive history, the
                 * individual changeset item will not have a path, so use the
                 * path of the item for which history is being displayed.
                 */
                String getPath = null;
                if (historyInput.isSingleItem()) {
                    final Change[] changes = changeset.getChanges();
                    if (changes != null && changes.length > 0) {
                        final Item item = changes[0].getItem();
                        if (item != null) {
                            getPath = item.getServerItem();
                        }
                    }
                } else {
                    getPath = historyInput.getHistoryItem();
                }

                if (getPath != null) {
                    final RecursionType recursionType =
                        historyInput.isSingleItem() ? RecursionType.NONE : RecursionType.FULL;

                    final GetRequest getRequest = new GetRequest(new ItemSpec(getPath, recursionType), versionSpec);

                    new GetTask(control.getShell(), getRepository(), new GetRequest[] {
                        getRequest
                    }, GetOptions.NONE).run();
                }
            }
        }

        @Override
        protected boolean computeEnablement(final IStructuredSelection selection) {
            if (selection.size() != 1) {
                return false;
            }

            // For VS 2010 the content provider is a TreeContentProvider. Only
            // enable this action for history items at the root level. For
            // versions prior to 2010 the condition for enablement conditions
            // are unchanged.
            boolean isRootItemSelected = true;
            if (selection instanceof ITreeSelection) {
                final ITreeSelection treeSelection = (ITreeSelection) selection;
                final TreePath[] treePaths = treeSelection.getPaths();
                if (treePaths.length == 1) {
                    isRootItemSelected = treePaths[0].getSegmentCount() == 1;
                }
            }

            final String item = historyInput.getHistoryItem();
            return !ServerPath.isServerPath(item) && isRootItemSelected;
        }
    }

    private static class CompareAction extends RepositoryAction {
        private HistoryInput historyInput;
        private final Shell shell;

        public CompareAction(final ISelectionProvider selectionProvider, final Shell shell) {
            super(selectionProvider, null);

            Check.notNull(shell, "shell"); //$NON-NLS-1$
            this.shell = shell;

            setText(Messages.getString("HistoryEditor.CompareActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryEditor.CompareActionTooltip")); //$NON-NLS-1$
            setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));
        }

        public void setHistoryInput(final HistoryInput historyInput) {
            this.historyInput = historyInput;
        }

        @Override
        protected void doRun(final TFSRepository repository) {
            if (getSelectionSize() == 2) {
                final Object[] selectedElements = selectionToArray();

                final Item firstItem = ((Changeset) selectedElements[0]).getChanges()[0].getItem();
                final Item secondItem = ((Changeset) selectedElements[1]).getChanges()[0].getItem();

                // TODO: Microsoft's client checks the deletion IDs here (see
                // ControlHistory#ShowDifference())

                final Compare compare = new Compare();

                compare.setModified(new TFSItemNode(firstItem, repository.getVersionControlClient()));
                compare.setOriginal(new TFSItemNode(secondItem, repository.getVersionControlClient()));

                compare.addComparator(TFSItemContentComparator.INSTANCE);

                compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(shell));
                compare.open();
            } else {
                final String localPath = historyInput.getHistoryItem();
                if (!new File(localPath).exists()) {
                    // show error dialog
                    return;
                }

                final Changeset changeset = (Changeset) getSelectionFirstElement();
                final Item item = changeset.getChanges()[0].getItem();

                // TODO: Microsoft's client checks the deletion ID here (see
                // ControlHistory#ShowDifference())

                final Compare compare = new Compare();

                compare.setModifiedLocalPath(localPath, ResourceType.FILE);
                compare.setOriginal(new TFSItemNode(item, repository.getVersionControlClient()));

                compare.addComparator(TFSItemContentComparator.INSTANCE);

                compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(shell));
                compare.open();
            }
        }

        @Override
        protected boolean computeEnablement(final IStructuredSelection selection) {
            if (selection.size() != 1 && selection.size() != 2) {
                return false;
            }

            if (historyInput == null || !historyInput.isSingleItem()) {
                return false;
            }

            for (final Iterator it = selection.iterator(); it.hasNext();) {
                final Changeset changeset = (Changeset) it.next();
                final Change[] changes = changeset.getChanges();

                if (changes == null
                    || changes.length == 0
                    || changes[0].getItem().getItemType() == ItemType.FOLDER
                    || containsSymlinkChange(changes[0])) {
                    return false;
                }
            }

            if (selection.size() == 1) {
                return !ServerPath.isServerPath(historyInput.getHistoryItem())
                    && !isSymbolicLink(historyInput.getHistoryItem());
            }

            return true;
        }
    }

    private static class RollbackAction extends RepositoryAction {
        private HistoryInput historyInput;
        private final Shell shell;

        public RollbackAction(final ISelectionProvider selectionProvider, final Shell shell) {
            super(selectionProvider, null);

            Check.notNull(shell, "shell"); //$NON-NLS-1$
            this.shell = shell;

            setText(Messages.getString("HistoryEditor.RollbackSingleName")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryEditor.RollbackSingleTooltip")); //$NON-NLS-1$
        }

        public void setHistoryInput(final HistoryInput historyInput) {
            this.historyInput = historyInput;
        }

        @Override
        protected void doRun(final TFSRepository repository) {
            final Changeset[] selectedElements = (Changeset[]) selectionToArray(Changeset.class, true);

            int minChangesetID = Integer.MAX_VALUE;
            int maxChangesetID = Integer.MIN_VALUE;

            for (final Changeset changeset : selectedElements) {
                minChangesetID = Math.min(minChangesetID, changeset.getChangesetID());
                maxChangesetID = Math.max(maxChangesetID, changeset.getChangesetID());
            }

            String itemPath = historyInput.getHistoryItem();
            if (!ServerPath.isServerPath(itemPath)) {
                itemPath = repository.getWorkspace().getMappedServerPath(itemPath);
            }

            RollbackTask rollbackTask;

            if (minChangesetID == maxChangesetID) {
                // Single selection
                final VersionSpec version = VersionSpec.parseSingleVersionFromSpec(
                    String.valueOf(minChangesetID),
                    VersionControlConstants.AUTHENTICATED_USER);

                rollbackTask = new RollbackTask(shell, repository, null, version);
            } else {
                // Multi selection
                final VersionSpec versionFrom = VersionSpec.parseSingleVersionFromSpec(
                    String.valueOf(minChangesetID),
                    VersionControlConstants.AUTHENTICATED_USER);

                final VersionSpec versionTo = VersionSpec.parseSingleVersionFromSpec(
                    String.valueOf(maxChangesetID),
                    VersionControlConstants.AUTHENTICATED_USER);

                rollbackTask = new RollbackTask(shell, repository, itemPath, versionFrom, versionTo);
            }

            rollbackTask.run();
        }

        @Override
        protected boolean computeEnablement(final IStructuredSelection selection) {
            if (selection.size() > 1) {
                setText(Messages.getString("HistoryEditor.RollbackMultipleName")); //$NON-NLS-1$
                setToolTipText(Messages.getString("HistoryEditor.RollbackMultipleTooltip")); //$NON-NLS-1$
            } else {
                setText(Messages.getString("HistoryEditor.RollbackSingleName")); //$NON-NLS-1$
                setToolTipText(Messages.getString("HistoryEditor.RollbackSingleTooltip")); //$NON-NLS-1$
            }

            if (selection instanceof ITreeSelection) {
                final ITreeSelection treeSelection = (ITreeSelection) selection;
                final TreePath[] treePaths = treeSelection.getPaths();
                for (final TreePath path : treePaths) {
                    if (path.getSegmentCount() > 1) {
                        return false;
                    }
                }
            }

            final TFSRepository repository = historyInput.getRepository();
            if (repository == null) {
                return false;
            }

            if (repository.getVersionControlClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
                return false;
            }

            final String itemPath = historyInput.getHistoryItem();
            if (itemPath == null) {
                return false;
            }

            if (ServerPath.isServerPath(itemPath)) {
                return repository.getWorkspace().isServerPathMapped(itemPath);
            } else {
                return repository.getWorkspace().isLocalPathMapped(itemPath);
            }
        }
    }

    private final class HistoryEditorRepositoryListener extends RepositoryManagerAdapter {
        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository() != repository) {
                        final IWorkbenchPage page =
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        page.closeEditor(HistoryEditor.this, false);
                    }
                }
            });
        }
    }
}
