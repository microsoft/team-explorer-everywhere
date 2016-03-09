// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.wit.QueryWorkItemsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.wit.WorkItemCheckinTable;
import com.microsoft.tfs.client.common.ui.controls.wit.WorkItemQueryControl;
import com.microsoft.tfs.client.common.ui.controls.wit.WorkItemQueryControl.QuerySelectedListener;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemActions;
import com.microsoft.tfs.core.clients.workitem.internal.AccessDeniedWorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;

@SuppressWarnings("restriction")
public class WorkItemsCheckinControl extends AbstractCheckinSubControl {
    public static final CodeMarker WI_UPDATE_COMMAND_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.controls.vc.checkin.WorkItemsCheckinControl#afterWorkItemUpdateCommand"); //$NON-NLS-1$

    public static final String WORKITEMS_TABLE_ID = "WorkItemsCheckinControl.workItemTable"; //$NON-NLS-1$

    private final CheckinControlOptions options;

    private WorkItemQueryControl queryControl; /* may be null */
    private final WorkItemCheckinTable workItemTable;

    private TFSRepository repository;

    private static final Log log = LogFactory.getLog(WorkItemsCheckinControl.class);

    /* Fields always added to queries */
    private static final String[] EXTRA_QUERY_FIELDS = new String[] {
        CoreFieldReferenceNames.STATE,
        CoreFieldReferenceNames.TITLE
    };

    protected WorkItemsCheckinControl(final Composite parent, final int style, final CheckinControlOptions options) {
        super(parent, style, Messages.getString("WorkItemsCheckinControl.Title"), CheckinSubControlType.WORK_ITEMS); //$NON-NLS-1$

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        Check.notNull(options, "options"); //$NON-NLS-1$
        this.options = new CheckinControlOptions(options);

        if (this.options.isWorkItemSearchEnabled()) {
            /* Stuff in a composite to add margin */
            final Composite searchComposite = new Composite(this, SWT.NONE);

            final GridLayout searchLayout = new GridLayout();
            searchLayout.horizontalSpacing = 0;
            searchLayout.verticalSpacing = 0;

            if (options.isForDialog()) {
                searchLayout.marginWidth = 0;
                searchLayout.marginHeight = 0;
            } else {
                searchLayout.marginWidth = getHorizontalMargin();
                searchLayout.marginHeight = 0;

                /* Use reflection to add a bit of top margin. Aesthetic only. */
                try {
                    final Field marginTopField = GridLayout.class.getDeclaredField("marginTop"); //$NON-NLS-1$

                    if (marginTopField != null) {
                        marginTopField.set(searchLayout, new Integer(getVerticalMargin()));
                    }
                } catch (final Exception e) {
                    /* Ignore, aesthetic only. */
                }
            }

            searchComposite.setLayout(searchLayout);

            queryControl = new WorkItemQueryControl(searchComposite, SWT.NONE);

            queryControl.addQuerySelectedListener(new QuerySelectedListener() {
                @Override
                public void querySelected(final Query query) {
                    updateWorkItems(query);
                }
            });
            queryControl.setSuppressInitialPopulation(!options.getWorkItemInitialQuery());

            GridDataBuilder.newInstance().hFill().hGrab().applyTo(queryControl);

            GridDataBuilder.newInstance().hFill().hGrab().applyTo(searchComposite);
        }

        int workItemTableStyle = SWT.MULTI | SWT.FULL_SELECTION;
        workItemTableStyle |= (this.options.isWorkItemReadOnly() ? SWT.READ_ONLY : SWT.CHECK);
        workItemTableStyle |= (this.options.getWorkItemShowAction()) ? WorkItemCheckinTable.CHECKIN_ACTION : 0;

        workItemTable = new WorkItemCheckinTable(this, workItemTableStyle);
        AutomationIDHelper.setWidgetID(workItemTable, WORKITEMS_TABLE_ID);
        GridDataBuilder.newInstance().fill().grab().applyTo(workItemTable);

        setSelectionProvider(workItemTable);

        workItemTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final WorkItemCheckinInfo[] selection = workItemTable.getSelectedWorkItems();

                if (selection.length == 1) {
                    openWorkItem(selection[0]);
                }
            }
        });
    }

    private void openWorkItem(final WorkItemCheckinInfo workItem) {
        if (repository == null) {
            return;
        }

        /* TODO: change when TFSServer <-> TFSRepository get coupled */
        final TFSServer server = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getServer(
            repository.getVersionControlClient().getConnection());

        /*
         * If we're in a modal context, we need to open the work item in an
         * external browser. Otherwise, open it with the preferred work item
         * editor.
         */
        if (options.isForDialog()) {
            WorkItemEditorHelper.openEditor(
                server,
                workItem.getWorkItem(),
                WorkItemEditorHelper.EXTERNAL_WEB_ACCESS_EDITOR_ID);
        } else {
            WorkItemEditorHelper.openEditor(server, workItem.getWorkItem());
        }
    }

    @Override
    public void addContributions(final IContributionManager contributionManager, final String groupName) {
    }

    @Override
    public void removeContributions(final IContributionManager contributionManager, final String groupname) {
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;

        if (workItemTable != null) {
            workItemTable.setWorkItems(new WorkItem[0]);
            workItemTable.setEnabled(repository != null);
        }

        if (queryControl != null) {
            queryControl.setRepository(repository);
        }
    }

    public WorkItemQueryControl getWorkItemQueryControl() {
        return queryControl;
    }

    public WorkItemCheckinTable getWorkItemTable() {
        return workItemTable;
    }

    private void updateWorkItems(final Query query) {
        if (query == null) {
            return;
        }

        for (int i = 0; i < EXTRA_QUERY_FIELDS.length; i++) {
            query.getDisplayFieldList().add(EXTRA_QUERY_FIELDS[i]);
        }

        final WorkItemCheckinInfo[] originallySelected = workItemTable.getSelectedWorkItems();
        final WorkItemCheckinInfo[] originallyChecked = workItemTable.getCheckedWorkItems();

        queryControl.setEnabled(false);
        workItemTable.setEnabled(false);

        final WorkItemUpdateCommand updateCommand =
            new WorkItemUpdateCommand(query, originallySelected, originallyChecked);
        UICommandExecutorFactory.newUIJobCommandExecutor(getShell()).execute(updateCommand);
    }

    /**
     * Call this method before evaluating the {@link PendingCheckin} to ensure
     * the subcontrol is in a consistent state. Raises errors to the user to
     * correct some problems.
     *
     * @return true if the subcontrol is in a valid state for checkin to
     *         proceed, false if it is not
     */
    public boolean validateForCheckin() {
        final WorkItemCheckinInfo[] workItemInfos = workItemTable.getSelectedWorkItems();
        final WorkItem[] workItems = WorkItemHelpers.workItemCheckinInfosToWorkItems(workItemInfos);

        if (workItems.length == 0) {
            return true;
        }

        final IStatus status = WorkItemHelpers.syncWorkItemsToLatest(getShell(), workItems);
        if (!status.isOK()) {
            return false;
        }

        if (WorkItemHelpers.anyWorkItemsAreInvalid(workItems)) {
            final String title = Messages.getString("WorkItemsCheckinControl.ValidationErrorDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("WorkItemsCheckinControl.ValidationErrorDialogText"); //$NON-NLS-1$

            MessageDialog.openError(getShell(), title, message);
            return false;
        }

        if (WorkItemHelpers.anyWorkItemsAreDirty(workItems)) {
            final String title = Messages.getString("WorkItemsCheckinControl.SaveDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("WorkItemsCheckinControl.SaveDialogText"); //$NON-NLS-1$

            if (MessageDialog.openQuestion(getShell(), title, message) == false) {
                return false;
            }

            if (!WorkItemHelpers.saveAllDirtyWorkItems(getShell(), workItems)) {
                return false;
            }
        }

        // None of the associated work items are dirty at this point.
        for (final WorkItemCheckinInfo workItemInfo : workItemInfos) {
            if (!validateTransition(workItemInfo)) {
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
            MessageDialog.openError(
                getShell(),
                Messages.getString("WorkItemsCheckinControl.WorkItemValidationDialogTitle"), //$NON-NLS-1$
                Messages.getString("WorkItemsCheckinControl.WorkItemValidationDialogText")); //$NON-NLS-1$
            return false;
        } catch (final Exception ex) {
            log.warn("error testing transition for work item during check-in", ex); //$NON-NLS-1$

            final String title = Messages.getString("WorkItemsCheckinControl.WorkItemValidationDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("WorkItemsCheckinControl.WorkItemDialogTextFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, Integer.toString(workItem.getFields().getID()), ex.getMessage());

            MessageDialog.openError(getShell(), title, message);
            return false;
        } finally {
            workItem.reset();
        }
    }

    public void afterCheckin() {
        workItemTable.setCheckedWorkItems(new WorkItemCheckinInfo[0]);
        workItemTable.clearActions();
    }

    protected static int[] getIDsForWorkItemCheckinInfos(final WorkItemCheckinInfo[] workItemInfos) {
        Check.notNull(workItemInfos, "workItemInfos"); //$NON-NLS-1$

        final int[] ret = new int[workItemInfos.length];

        for (int i = 0; i < workItemInfos.length; i++) {
            ret[i] = workItemInfos[i].getWorkItem().getID();
        }

        return ret;
    }

    protected static int[] getIDsForWorkItems(final WorkItem[] workItems) {
        Check.notNull(workItems, "workItems"); //$NON-NLS-1$

        final int[] ret = new int[workItems.length];

        for (int i = 0; i < workItems.length; i++) {
            ret[i] = workItems[i].getID();
        }

        return ret;
    }

    private class WorkItemUpdateCommand extends TFSCommand {
        private final Query query;
        private final WorkItemCheckinInfo[] selectedWorkItems;
        private final WorkItemCheckinInfo[] checkedWorkItems;

        public WorkItemUpdateCommand(
            final Query query,
            final WorkItemCheckinInfo[] selectedWorkItems,
            final WorkItemCheckinInfo[] checkedWorkItems) {
            this.query = query;
            this.selectedWorkItems = selectedWorkItems;
            this.checkedWorkItems = checkedWorkItems;
        }

        @Override
        public String getName() {
            return (Messages.getString("WorkItemsCheckinControl.CommandText")); //$NON-NLS-1$
        }

        @Override
        public String getErrorDescription() {
            return (Messages.getString("WorkItemsCheckinControl.CommandErrorText")); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return null;
        }

        @Override
        @SuppressWarnings("restriction")
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            progressMonitor.beginTask(Messages.getString("WorkItemsCheckinControl.QueryingItemsProgressText"), 1); //$NON-NLS-1$
            final QueryWorkItemsCommand subCommand = new QueryWorkItemsCommand(query);
            progressMonitor.worked(1);

            final List<WorkItemCheckinInfo> itemInfos = new ArrayList<WorkItemCheckinInfo>();
            final List<WorkItemCheckinInfo> newlySelected = new ArrayList<WorkItemCheckinInfo>();
            final List<WorkItemCheckinInfo> newlyChecked = new ArrayList<WorkItemCheckinInfo>();

            IStatus subStatus = Status.OK_STATUS;

            try {
                subStatus = new CommandExecutor(progressMonitor).execute(subCommand);

                if (subStatus.isOK() && subCommand.getWorkItems() != null) {
                    final WorkItem[] items = subCommand.getWorkItems();

                    for (int i = 0; i < items.length; i++) {
                        if (items[i] instanceof AccessDeniedWorkItemImpl) {
                            continue;
                        }

                        final WorkItemCheckinInfo info = new WorkItemCheckinInfo(items[i]);

                        itemInfos.add(info);

                        for (int j = 0; j < selectedWorkItems.length; j++) {
                            if (selectedWorkItems[j].getWorkItem().getFields().getID() == items[i].getFields().getID()) {
                                newlySelected.add(info);
                            }
                        }

                        for (int j = 0; j < checkedWorkItems.length; j++) {
                            if (checkedWorkItems[j].getWorkItem().getFields().getID() == items[i].getFields().getID()) {
                                /*
                                 * Set the action to the previously selected
                                 * action UNLESS the work item checkin info no
                                 * longer supports that action type. (Example:
                                 * you had set the action to a work item to
                                 * "resolve", then did a refresh and the work
                                 * item has been resolved outside this process.
                                 * We now cannot resolve this work item, so we
                                 * downgrade to associate which is always
                                 * allowed.
                                 */
                                if (info.isActionSupported(checkedWorkItems[j].getAction())) {
                                    info.setAction(checkedWorkItems[j].getAction());
                                } else {
                                    info.setAction(CheckinWorkItemAction.ASSOCIATE);
                                }

                                newlyChecked.add(info);
                            }
                        }
                    }
                }
            } finally {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        if (!queryControl.isDisposed()) {
                            queryControl.setEnabled(true);
                        }

                        if (workItemTable.isDisposed()) {
                            return;
                        }

                        /*
                         * Setting work items on the table can now throw an
                         * exception.
                         */
                        try {
                            workItemTable.setEnabled(true);
                            workItemTable.setWorkItems(itemInfos.toArray(new WorkItemCheckinInfo[itemInfos.size()]));
                            workItemTable.setCheckedWorkItems(
                                newlyChecked.toArray(new WorkItemCheckinInfo[newlyChecked.size()]));
                            workItemTable.setSelectedWorkItems(
                                newlySelected.toArray(new WorkItemCheckinInfo[newlySelected.size()]));
                        } catch (final Exception e) {
                            MessageDialog.openError(
                                getShell(),
                                Messages.getString("WorkItemsCheckinControl.ErrorDialogText"), //$NON-NLS-1$
                                e.getLocalizedMessage());
                            return;
                        }

                        CodeMarkerDispatch.dispatch(WI_UPDATE_COMMAND_COMPLETE);
                    }
                });
            }

            return subStatus;
        }
    };
}
