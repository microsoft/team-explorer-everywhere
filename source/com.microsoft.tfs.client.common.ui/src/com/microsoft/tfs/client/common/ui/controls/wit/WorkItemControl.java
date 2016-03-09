// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.commands.wit.GetWorkItemsForChangesetCommand;
import com.microsoft.tfs.client.common.commands.wit.QueryWorkItemsCommand;
import com.microsoft.tfs.client.common.commands.wit.RefreshStoredQueriesCommand;
import com.microsoft.tfs.client.common.commands.wit.SaveWorkItemCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.ServerManagerAdapter;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemActions;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.util.Check;

/**
 * @deprecated
 */
@Deprecated
public class WorkItemControl extends Composite {
    private static final Log log = LogFactory.getLog(WorkItemControl.class);
    private static final String[] EXTRA_FIELDS = new String[] {
        CoreFieldReferenceNames.STATE,
        CoreFieldReferenceNames.TITLE
    };
    private static final int MAX_RECENT_QUERIES = 10;
    private static final String VIEW_STATE_KEY = "work-item-control"; //$NON-NLS-1$

    private final WorkItemList workItemList;
    private final WorkItemQueryControl queryControl;

    private final TFSServer server;
    private boolean serverOnline = true;

    private final boolean forHistoryDetails;
    private Query lastQuery;

    private ServerManagerListener serverManagerListener;

    private boolean firstTimeSetVisible = true;

    public WorkItemControl(
        final Composite parent,
        final int style,
        final TFSServer server,
        final TFSRepository repository,
        final boolean forHistoryDetails,
        final boolean suppressInitialQuery) {
        super(parent, style);

        Check.notNull(server, "server"); //$NON-NLS-1$

        this.server = server;
        this.forHistoryDetails = forHistoryDetails;

        final GridLayout layout = new GridLayout(1, false);
        setLayout(layout);

        queryControl = new WorkItemQueryControl(this, SWT.NONE, MAX_RECENT_QUERIES, VIEW_STATE_KEY);
        queryControl.setRepository(repository);
        queryControl.addQuerySelectedListener(new WorkItemQueryControl.QuerySelectedListener() {
            @Override
            public void querySelected(final Query query) {
                lastQuery = query;
                updateWorkItems(query);
            }
        });
        queryControl.setSuppressInitialPopulation(suppressInitialQuery);

        workItemList = new WorkItemList(this, SWT.NONE, server, forHistoryDetails);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        workItemList.setLayoutData(gd);

        if (forHistoryDetails) {
            queryControl.setEnabled(false);
        } else {
            serverManagerListener = new ServerManagerAdapter() {
                @Override
                public void onServerAdded(final ServerManagerEvent event) {
                    if (event.getServer().equals(WorkItemControl.this.server)) {
                        serverOnline = true;
                        ensurePopulated(true);
                    }
                }

                @Override
                public void onServerRemoved(final ServerManagerEvent event) {
                    if (event.getServer().equals(WorkItemControl.this.server)) {
                        serverOnline = false;
                        ensurePopulated(true);
                    }
                }
            };

            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().addListener(
                serverManagerListener);

            addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().removeListener(
                        serverManagerListener);
                }
            });
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        if (visible) {
            if (firstTimeSetVisible) {
                workItemList.resetLayout();
                firstTimeSetVisible = false;
            }
            ensurePopulated(false);
        }
    }

    public boolean validateForCheckin() {
        final WorkItemCheckinInfo[] workItems = getSelectedWorkItems();

        boolean dirty = false;

        /*
         * Check each work item. If the work item is dirty, record that we have
         * at least one dirty work item. If the work item is invalid, show an
         * error message and invalidate the checkin.
         */
        for (int i = 0; i < workItems.length; i++) {
            dirty = dirty | workItems[i].getWorkItem().isDirty();
            if (!workItems[i].getWorkItem().isValid()) {
                final String title = Messages.getString("WorkItemControl.InvalidItemDialogTitle"); //$NON-NLS-1$
                final String message = Messages.getString("WorkItemControl.InvalidItemDialogText"); //$NON-NLS-1$
                MessageBoxHelpers.errorMessageBox(getShell(), title, message);
                return false;
            }
        }

        if (dirty) {
            final String title = Messages.getString("WorkItemControl.SaveFailedDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("WorkItemControl.SaveFailedDialogText"); //$NON-NLS-1$

            /*
             * We have at least one dirty work item. Prompt the user to save all
             * dirty work items. If they decline, invalidate the checkin.
             */
            if (!MessageBoxHelpers.dialogConfirmPrompt(getShell(), title, message)) {
                return false;
            }

            /*
             * The user accepted the offer to save all dirty work items. Save
             * each one. If the save fails with an exception, show an error
             * message and invalidate the checkin.
             */
            final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());
            for (int i = 0; i < workItems.length; i++) {
                if (workItems[i].getWorkItem().isDirty()) {
                    final SaveWorkItemCommand command = new SaveWorkItemCommand(workItems[i].getWorkItem());
                    final IStatus status = executor.execute(command);

                    if (!status.isOK()) {
                        return false;
                    }
                }
            }
        }

        /*
         * Note: at this point in the method none of the work items should be
         * dirty.
         */

        for (int i = 0; i < workItems.length; i++) {
            if (!validateTransition(workItems[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean validateTransition(final WorkItemCheckinInfo workItemCheckinInfo) {
        if (!(CheckinWorkItemAction.RESOLVE == workItemCheckinInfo.getAction())) {
            return true;
        }

        final WorkItem workItem = workItemCheckinInfo.getWorkItem();

        try {
            workItem.open();
            final String nextState = workItem.getNextState(WorkItemActions.VS_CHECKIN);
            if (nextState == null) {
                return true;
            }
            workItem.getFields().getField(CoreFieldReferenceNames.STATE).setValue(nextState);
            if (workItem.isValid()) {
                return true;
            }

            /*
             * This isn't a very good error message, but it's what Visual Studio
             * uses.
             */
            final String title = Messages.getString("WorkItemControl.InvalidItemDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("WorkItemControl.InvalidItemDialog2Text"); //$NON-NLS-1$

            MessageBoxHelpers.errorMessageBox(getShell(), title, message);
            return false;
        } catch (final Exception ex) {
            log.warn("error testing transition for work item during check-in", ex); //$NON-NLS-1$

            final String title = Messages.getString("WorkItemControl.SaveFailedDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("WorkItemControl.SaveFailedDialog3TextFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, Integer.toString(workItem.getFields().getID()), ex.getMessage());

            MessageBoxHelpers.errorMessageBox(getShell(), title, message);
            return false;
        } finally {
            workItem.reset();
        }

    }

    public WorkItemCheckinInfo[] getSelectedWorkItems() {
        /*
         * Note: the main work done by this method involves in obtaining
         * references to workitems that are checked in the pending changes work
         * item control and are open in an editor.
         *
         * This is done so that changes made to the work items as a result of a
         * checkin are made to the work item object that is already open in an
         * editor. This gives us a chance to do some edge case handling (for
         * example, dirty and invalid work items in an editor) as well as update
         * all parts of the GUI that are displaying data for the work item.
         */

        final WorkItemCheckinInfo[] originalWorkItems =
            (WorkItemCheckinInfo[]) workItemList.getSelectedWorkItems().toArray(new WorkItemCheckinInfo[] {});

        final WorkItemCheckinInfo[] newWorkItems = new WorkItemCheckinInfo[originalWorkItems.length];

        final Map idToWorkItemsInEditors = new HashMap();

        for (int i = 0; i < originalWorkItems.length; i++) {
            final WorkItem workItem = (WorkItem) idToWorkItemsInEditors.get(
                new Integer(originalWorkItems[i].getWorkItem().getFields().getID()));

            if (workItem != null) {
                newWorkItems[i] = new WorkItemCheckinInfo(workItem, originalWorkItems[i].getAction());
            } else {
                newWorkItems[i] = originalWorkItems[i];
            }
        }

        return newWorkItems;
    }

    public void refreshResults() {
        if (lastQuery != null) {
            updateWorkItems(lastQuery);
        }
    }

    private void ensurePopulated(final boolean reset) {
        if (forHistoryDetails) {
            return;
        }

        if (!reset && queryControl.isPopulated()) {
            return;
        }

        if (!serverOnline) {
            return;
        }

        final ProjectCollection projects = server.getConnection().getWorkItemClient().getProjects();

        final RefreshStoredQueriesCommand refreshCommand = new RefreshStoredQueriesCommand(server, projects);
        final IStatus refreshStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(refreshCommand);

        if (!refreshStatus.isOK()) {
            return;
        }

        queryControl.populate(projects, getPendingChangeProject());
    }

    /**
     * Guess the name of the project you are interested in my looking at the
     * first item in the pending changes window and using the project from it.
     *
     * @return guessed name of project that you are interested in.
     */
    private String getPendingChangeProject() {
        if (getParent().getParent() instanceof SourceFilesCheckinControl) {
            final ChangeItem[] pendingChanges =
                ((SourceFilesCheckinControl) getParent().getParent()).getChangesTable().getSelectedChangeItems();

            if (pendingChanges != null && pendingChanges.length > 0) {
                // Pick the first file and get the project name from it.
                final String serverPath = pendingChanges[0].getServerItem();
                if (serverPath != null && serverPath.length() > 0) {
                    return ServerPath.getTeamProjectName(serverPath);
                }
            }
        }

        return null;
    }

    private void updateWorkItems(final Query query) {
        if (query == null) {
            return;
        }

        for (int i = 0; i < EXTRA_FIELDS.length; i++) {
            query.getDisplayFieldList().add(EXTRA_FIELDS[i]);
        }

        final QueryWorkItemsCommand queryCommand = new QueryWorkItemsCommand(query);
        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return;
        }

        populateWorkItemList(queryCommand.getWorkItems());
    }

    private void populateWorkItemList(final WorkItem[] workItems) {
        final List workItemInfos = new ArrayList();

        for (int i = 0; i < workItems.length; i++) {
            workItemInfos.add(new WorkItemCheckinInfo(workItems[i]));
        }

        workItemList.setWorkItemCheckinInfos(workItemInfos);
    }

    public void setChangesetID(final int changesetID) {
        final GetWorkItemsForChangesetCommand getCommand = new GetWorkItemsForChangesetCommand(server, changesetID);
        getCommand.setExtraFieldNames(EXTRA_FIELDS);
        final IStatus getStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(getCommand);

        if (!getStatus.isOK()) {
            return;
        }

        populateWorkItemList(getCommand.getWorkItems());
    }

    public WorkItemList getWorkItemList() {
        return workItemList;
    }
}
