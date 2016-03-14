// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.FindInSourceControlQuery;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.QueryPendingSetsCommand;
import com.microsoft.tfs.client.common.ui.controls.vc.FindInSourceControlPendingChangesTable;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.CheckinAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.CheckoutAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.CopyToClipboardAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.HistoryAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.PropertiesAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions.UndoAction;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.OpenInSourceControlExplorerAction;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class FindInSourceControlEditor extends EditorPart {
    public static final String ID =
        "com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor"; //$NON-NLS-1$
    private TFSRepository repository;

    private Label resultsLabel;

    private Composite tableComposite;
    private StackLayout tableStack;
    private FindInSourceControlPendingChangesTable pendingChangesTable;
    private MenuManager menuManager;

    private CheckoutAction checkoutAction;
    private CheckinAction checkinAction;
    private CopyToClipboardAction copyAction;
    private UndoAction undoAction;
    private HistoryAction historyAction;
    private OpenInSourceControlExplorerAction openInSCEAction;
    private PropertiesAction propertiesAction;
    private static final Log log = LogFactory.getLog(FindInSourceControlEditor.class);

    private final FindInSCEEditorRepositoryListener repositoryManagerListener = new FindInSCEEditorRepositoryListener();

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
        throw new RuntimeException("Saving find results is not implemented."); //$NON-NLS-1$
    }

    @Override
    public void doSaveAs() {
        /* Should never be called as isSaveAllowed should return false. */
        throw new RuntimeException("Saving find results is not implemented."); //$NON-NLS-1$
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void setInput(final IEditorInput input) {
        super.setInput(input);
    }

    @Override
    public void createPartControl(final Composite parent) {
        final IEditorInput editorInput = getEditorInput();
        repository = getEditorInput().getRepository();

        setPartName(editorInput.getName());
        setTitleToolTip(editorInput.getToolTipText());

        final Composite partComposite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        partComposite.setLayout(layout);

        final Composite labelComposite = new Composite(partComposite, SWT.NONE);

        /* Compute metrics in pixels */
        final GC gc = new GC(labelComposite);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        final FillLayout labelCompositeLayout = new FillLayout();
        labelCompositeLayout.marginWidth =
            Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN) / 2;
        labelCompositeLayout.marginHeight =
            Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN) / 2;

        labelComposite.setLayout(labelCompositeLayout);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(labelComposite);

        resultsLabel = new Label(labelComposite, SWT.NONE);

        tableComposite = new Composite(partComposite, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().applyTo(tableComposite);

        tableStack = new StackLayout();
        tableComposite.setLayout(tableStack);

        pendingChangesTable =
            new FindInSourceControlPendingChangesTable(tableComposite, SWT.FULL_SELECTION | SWT.MULTI);

        createActions();

        menuManager = new MenuManager("menu"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillContextMenu(manager);
            }
        });

        pendingChangesTable.getTable().setMenu(menuManager.createContextMenu(pendingChangesTable));

        /*
         * Proxy selection changes in both tables to listeners.
         */
        final ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                ((ISelectionChangedListener) selectionChangedListeners.getListener()).selectionChanged(event);
            }
        };

        pendingChangesTable.addSelectionChangedListener(selectionChangedListener);

        pendingChangesTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                openInSCEAction.run();
            }

        });

        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().addListener(
            repositoryManagerListener);

    }

    @Override
    public void dispose() {
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().removeListener(
            repositoryManagerListener);

    }

    private void createActions() {
        checkoutAction = new CheckoutAction(this);
        checkoutAction.setText(Messages.getString("FindInSourceControlEditor.CheckoutActionText")); //$NON-NLS-1$

        checkinAction = new CheckinAction(this);
        checkinAction.setText(Messages.getString("FindInSourceControlEditor.CheckinActionText")); //$NON-NLS-1$

        copyAction = new CopyToClipboardAction(this);
        copyAction.setText(Messages.getString("FindInSourceControlEditor.CopyToClipboardActionText")); //$NON-NLS-1$

        undoAction = new UndoAction(this);
        undoAction.setText(Messages.getString("FindInSourceControlEditor.UndoActionText")); //$NON-NLS-1$

        historyAction = new HistoryAction(this);
        historyAction.setText(Messages.getString("FindInSourceControlEditor.HistoryActionText")); //$NON-NLS-1$

        openInSCEAction = new OpenInSourceControlExplorerAction(this);
        openInSCEAction.setText(Messages.getString("FindInSourceControlEditor.OpenInSCEActionText")); //$NON-NLS-1$

        propertiesAction = new PropertiesAction(this);
        propertiesAction.setText(Messages.getString("FindInSourceControlEditor.PropertiesActionText")); //$NON-NLS-1$
    }

    private void fillContextMenu(final IMenuManager manager) {
        final TypedServerItem[] serverItems = getSelectedServerItems();

        if (serverItems != null) {
            final boolean showStatus = getEditorInput().getQuery().showStatus();

            if (showStatus) {
                manager.add(checkoutAction);
                manager.add(checkinAction);
                manager.add(undoAction);
                manager.add(new Separator());
            }
            manager.add(historyAction);
            manager.add(openInSCEAction);
            manager.add(propertiesAction);
            manager.add(copyAction);
        }
    }

    @Override
    public void setFocus() {
    }

    @Override
    public FindInSourceControlEditorInput getEditorInput() {
        if (super.getEditorInput() != null && (super.getEditorInput() instanceof FindInSourceControlEditorInput)) {
            return (FindInSourceControlEditorInput) super.getEditorInput();
        }

        return null;
    }

    public void run() {
        final FindInSourceControlEditorInput input = getEditorInput();

        if (resultsLabel == null
            || resultsLabel.isDisposed()
            || tableComposite == null
            || tableComposite.isDisposed()) {
            return;
        }

        if (input == null) {
            return;
        }

        if (repository != input.getRepository()) {
            log.error("Connection has changed unexpectedly."); //$NON-NLS-1$
            return;
        }

        final FindInSourceControlQuery query = input.getQuery();

        resultsLabel.setText(Messages.getString("FindInSourceControlEditor.ResultsLabelFinding")); //$NON-NLS-1$

        pendingChangesTable.setItems(null);
        pendingChangesTable.setPendingChangesMap(null, query.showStatus());
        pendingChangesTable.setEnabled(false);

        tableStack.topControl = pendingChangesTable;
        tableComposite.layout();

        final Job queryJob;

        if (query.isCheckedOut()) {
            queryJob = new FindInSourceControlPendingChangesJob(repository, query);
        } else {
            queryJob = new FindInSourceControlItemsJob(repository, query);
        }

        queryJob.schedule();
    }

    public TypedServerItem[] getSelectedServerItems() {
        if (pendingChangesTable == null || pendingChangesTable.isDisposed()) {
            return null;
        }

        final ArrayList<TypedServerItem> typedServerItems = new ArrayList<TypedServerItem>();

        final String[] serverItems = pendingChangesTable.getSelectedServerItems();

        for (final String serverItem : serverItems) {
            final ItemType itemType = pendingChangesTable.getSelectedItemType(serverItem);

            if (serverItem == null || itemType == null) {
                continue;
            }

            final ServerItemType type = ItemType.FOLDER.equals(itemType) ? ServerItemType.FOLDER : ServerItemType.FILE;

            typedServerItems.add(new TypedServerItem(serverItem, type));
        }

        return typedServerItems.toArray(new TypedServerItem[typedServerItems.size()]);
    }

    public int getSelectedItemsCount() {
        final TypedServerItem[] serverItems = getSelectedServerItems();
        return (serverItems == null ? 0 : serverItems.length);

    }

    public boolean isSingleItemSelected() {
        return (getSelectedItemsCount() == 1);
    }

    public Map<PendingChange, PendingSet> getSelectedPendingChanges() {
        if (pendingChangesTable == null || pendingChangesTable.isDisposed()) {
            return null;
        }

        return pendingChangesTable.getSelectedPendingChanges();
    }

    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListeners.addListener(listener);
    }

    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListeners.removeListener(listener);
    }

    private abstract class FindInSourceControlJob extends Job {
        protected final TFSRepository repository;
        protected final FindInSourceControlQuery query;

        public FindInSourceControlJob(final TFSRepository repository, final FindInSourceControlQuery query) {
            super(Messages.getString("FindInSourceControlEditor.FindInSourceControlJobName")); //$NON-NLS-1$

            Check.notNull(repository, "repository"); //$NON-NLS-1$
            Check.notNull(query, "query"); //$NON-NLS-1$

            this.repository = repository;
            this.query = query;
        }

        @Override
        protected final IStatus run(final IProgressMonitor monitor) {
            final ICommand[] queryCommands = getQueryCommands();
            IStatus queryStatus = Status.OK_STATUS;

            for (final ICommand command : queryCommands) {
                // If the previous command succeeded then execute the current
                // command
                if (queryStatus.isOK()) {
                    queryStatus = new CommandExecutor().execute(command);
                }
            }

            final Runnable resultsRunnable;
            if (queryStatus.isOK()) {
                resultsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        updateResults(queryCommands);
                    }
                };
            } else {
                final String queryOutputMessage = queryStatus.getMessage();
                final Throwable queryException = queryStatus.getException();
                resultsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (resultsLabel == null || resultsLabel.isDisposed()) {
                            return;
                        }

                        resultsLabel.setText(
                            MessageFormat.format(
                                Messages.getString("FindInSourceControlEditor.ResultsLabelErrorFormat"), //$NON-NLS-1$
                                queryOutputMessage));
                        log.error(MessageFormat.format("Error: {0}", queryOutputMessage), queryException);//$NON-NLS-1$
                    }
                };
            }

            UIHelpers.runOnUIThread(true, resultsRunnable);

            return queryStatus;
        }

        protected abstract ICommand[] getQueryCommands();

        protected abstract void updateResults(final ICommand[] queryCommand);

        public String getQueryPath() {
            final String queryPath;

            if (query.getWildcard() == null) {
                queryPath = query.getServerPath();
            } else {
                queryPath = ServerPath.combine(query.getServerPath(), query.getWildcard());
            }

            return queryPath;
        }

        public RecursionType getRecursionType() {
            return query.isRecursive() ? RecursionType.FULL : RecursionType.NONE;
        }
    }

    private class FindInSourceControlPendingChangesJob extends FindInSourceControlJob {
        public FindInSourceControlPendingChangesJob(
            final TFSRepository repository,
            final FindInSourceControlQuery query) {
            super(repository, query);
        }

        @Override
        protected ICommand[] getQueryCommands() {
            return new ICommand[] {
                new QueryPendingSetsCommand(repository, new ItemSpec[] {
                    new ItemSpec(getQueryPath(), getRecursionType())
                }, query.getCheckedOutUser())
            };
        }

        @Override
        protected void updateResults(final ICommand[] queryCommands) {

            if (resultsLabel == null
                || resultsLabel.isDisposed()
                || pendingChangesTable == null
                || pendingChangesTable.isDisposed()) {
                return;
            }

            final PendingSet[] pendingSets = ((QueryPendingSetsCommand) queryCommands[0]).getPendingSets();

            /*
             * Use a tree map to collate server paths by the server's locale so
             * that we can display all pending changes for an item.
             *
             * This differs from Visual Studio which displays only a single
             * pending change per server item.
             */
            final Map<String, List<PendingSet>> pathToPendingSet = new TreeMap<String, List<PendingSet>>(
                repository.getVersionControlClient().getConnection().getCaseInsensitiveCollator());

            for (final PendingSet pendingSet : pendingSets) {
                for (final PendingChange pendingChange : pendingSet.getPendingChanges()) {
                    List<PendingSet> pendingSetList = pathToPendingSet.get(pendingChange.getServerItem());

                    if (pendingSetList == null) {
                        pendingSetList = new ArrayList<PendingSet>();
                        pathToPendingSet.put(pendingChange.getServerItem(), pendingSetList);
                    }

                    pendingSetList.add(pendingSet);

                }
            }

            if (!query.isCheckedOut()) {
                resultsLabel.setText(
                    MessageFormat.format(
                        Messages.getString("FindInSourceControlEditor.ResultsWildcardFormat"), //$NON-NLS-1$
                        pathToPendingSet.keySet().size(),
                        getQueryPath(),
                        query.isRecursive() ? StringUtil.EMPTY
                            : Messages.getString("FindInSourceControlEditor.ResultsQueryLabelOneLevel"))); //$NON-NLS-1$
            } else if (StringUtil.isNullOrEmpty(query.getCheckedOutUser())) {
                resultsLabel.setText(
                    MessageFormat.format(
                        Messages.getString("FindInSourceControlEditor.ResultsCheckedOutFormat"), //$NON-NLS-1$
                        pathToPendingSet.keySet().size(),
                        getQueryPath()));
            } else {
                resultsLabel.setText(
                    MessageFormat.format(
                        Messages.getString("FindInSourceControlEditor.ResultsCheckedOutUserFormat"), //$NON-NLS-1$
                        pathToPendingSet.keySet().size(),
                        getQueryPath(),
                        query.getCheckedOutUser()));
            }

            pendingChangesTable.setRepository(repository);
            pendingChangesTable.setPendingChangesMap(pathToPendingSet, query.showStatus());
            pendingChangesTable.setEnabled(true);

        }
    }

    private class FindInSourceControlItemsJob extends FindInSourceControlJob {
        public FindInSourceControlItemsJob(final TFSRepository repository, final FindInSourceControlQuery query) {
            super(repository, query);
        }

        @Override
        protected ICommand[] getQueryCommands() {
            final ArrayList<ICommand> commands = new ArrayList<ICommand>();

            // The command to retrieve the items
            commands.add(new QueryItemsCommand(
                repository,
                new ItemSpec[] {
                    new ItemSpec(getQueryPath(), getRecursionType())
            },
                LatestVersionSpec.INSTANCE,
                DeletedState.NON_DELETED,
                ItemType.ANY,
                GetItemsOptions.UNSORTED.combine(GetItemsOptions.INCLUDE_BRANCH_INFO)));
            if (query.showStatus()) {
                // The command that retrieves the pending changes of the items
                commands.add(new QueryPendingSetsCommand(repository, new ItemSpec[] {
                    new ItemSpec(getQueryPath(), getRecursionType())
                }, query.getCheckedOutUser()));
            }

            return commands.toArray(new ICommand[commands.size()]);

        }

        @Override
        protected void updateResults(final ICommand[] queryCommands) {

            if (resultsLabel == null
                || resultsLabel.isDisposed()
                || pendingChangesTable == null
                || pendingChangesTable.isDisposed()) {
                return;
            }

            final Map<String, List<PendingSet>> pathToPendingSet = new TreeMap<String, List<PendingSet>>(
                repository.getVersionControlClient().getConnection().getCaseInsensitiveCollator());

            if (query.showStatus()) {
                final PendingSet[] pendingSets = ((QueryPendingSetsCommand) queryCommands[1]).getPendingSets();

                /*
                 * Use a tree map to collate server paths by the server's locale
                 * so that we can display all pending changes for an item.
                 *
                 * This differs from Visual Studio which displays only a single
                 * pending change per server item.
                 */

                for (final PendingSet pendingSet : pendingSets) {
                    for (final PendingChange pendingChange : pendingSet.getPendingChanges()) {
                        List<PendingSet> pendingSetList = pathToPendingSet.get(pendingChange.getServerItem());

                        if (pendingSetList == null) {
                            pendingSetList = new ArrayList<PendingSet>();
                            pathToPendingSet.put(pendingChange.getServerItem(), pendingSetList);
                        }

                        pendingSetList.add(pendingSet);

                    }
                }
            }

            final Item[] items = ((QueryItemsCommand) queryCommands[0]).getItemSets()[0].getItems();
            final int itemsCount = items.length == 0 ? pathToPendingSet.keySet().size() : items.length;

            if (!query.isCheckedOut()) {
                resultsLabel.setText(
                    MessageFormat.format(
                        Messages.getString("FindInSourceControlEditor.ResultsWildcardFormat"), //$NON-NLS-1$
                        itemsCount,
                        getQueryPath(),
                        query.isRecursive() ? StringUtil.EMPTY
                            : Messages.getString("FindInSourceControlEditor.ResultsQueryLabelOneLevel"))); //$NON-NLS-1$
            } else if (StringUtil.isNullOrEmpty(query.getCheckedOutUser())) {
                resultsLabel.setText(
                    MessageFormat.format(
                        Messages.getString("FindInSourceControlEditor.ResultsCheckedOutFormat"), //$NON-NLS-1$
                        itemsCount,
                        getQueryPath()));
            } else {
                resultsLabel.setText(
                    MessageFormat.format(
                        Messages.getString("FindInSourceControlEditor.ResultsCheckedOutUserFormat"), //$NON-NLS-1$
                        itemsCount,
                        getQueryPath(),
                        query.getCheckedOutUser()));
            }
            final Map<String, Item> itemsMap = new HashMap<String, Item>();
            getEditorInput().getRepository().refresh(false);
            for (final Item item : items) {
                final PendingChange change =
                    getEditorInput().getRepository().getPendingChangeCache().getRenamePendingChangeByServerPath(
                        item.getServerItem());

                // Use the new name in case of a rename pending change
                if (change != null) {
                    itemsMap.put(change.getServerItem(), item);
                } else {
                    itemsMap.put(item.getServerItem(), item);
                }
            }

            pendingChangesTable.setRepository(repository);
            pendingChangesTable.setItems(itemsMap);
            pendingChangesTable.setPendingChangesMap(pathToPendingSet, query.showStatus());
            pendingChangesTable.setEnabled(true);
        }

    }

    private final class FindInSCEEditorRepositoryListener extends RepositoryManagerAdapter {
        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            if (TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository() != repository) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        final IWorkbenchPage page =
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        page.closeEditor(FindInSourceControlEditor.this, false);

                    }
                });
            }
        }
    }
}
