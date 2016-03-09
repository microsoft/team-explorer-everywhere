// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.wit.SaveWorkItemCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.events.QueryItemEventArg;
import com.microsoft.tfs.client.common.ui.wit.qe.QueryEditor;
import com.microsoft.tfs.client.common.ui.wit.query.QueryDocumentEditorInput;
import com.microsoft.tfs.client.common.ui.wit.results.QueryResultsEditor;
import com.microsoft.tfs.client.common.wit.QueryDocumentService;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocumentSaveListener;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleUtil;

@SuppressWarnings("restriction")
public class WorkItemHelpers {
    public static void openNewQuery(final TeamExplorerContext context) {
        final QueryDocumentService service = context.getServer().getQueryDocumentService();
        final ProjectInfo projectInfo = context.getCurrentProjectInfo();

        final QueryDocument document = service.createNewQueryDocument(projectInfo.getName(), QueryScope.PRIVATE);

        document.addSaveListener(new QueryDocumentSaveListener() {
            @Override
            public void onQueryDocumentSaved(final QueryDocument queryDocument) {
                final QueryHierarchy queryHierarchy = context.getCurrentProject().getQueryHierarchy();
                final QueryItem savedToParent = queryHierarchy.find(queryDocument.getParentGUID());

                if (savedToParent != null) {
                    final QueryItemEventArg arg = new QueryItemEventArg(savedToParent);
                    context.getEvents().notifyListener(TeamExplorerEvents.QUERY_ITEM_UPDATED, arg);
                }
            }
        });

        QueryEditor.openEditor(context.getServer(), document);
    }

    public static void runQuery(
        final Shell shell,
        final TFSServer server,
        final Project project,
        final StoredQuery storedQuery) {
        if (!storedQuery.isParsable()) {
            MessageDialog.openError(
                shell,
                Messages.getString("TeamExplorerWitQueryDefinitionNode.NotSupportedDialogTitle"), //$NON-NLS-1$
                Messages.getString("TeamExplorerWitQueryDefinitionNode.NotSupportedDialogText")); //$NON-NLS-1$

            return;
        }

        QueryResultsEditor.openEditor(server, project, storedQuery);
    }

    public static StoredQuery createStoredQueryFromDefinition(final QueryDefinition queryDefinition) {
        return new StoredQueryImpl(
            queryDefinition.getID(),
            queryDefinition.getName(),
            queryDefinition.getQueryText(),
            queryDefinition.isPersonal() ? QueryScope.PRIVATE : QueryScope.PUBLIC,
            queryDefinition.getProject().getID(),
            (ProjectImpl) queryDefinition.getProject(),
            queryDefinition.isDeleted(),
            queryDefinition.getProject().getWITContext());
    }

    /**
     * Closes all WIT editors open with the given {@link QueryDefinition}. Must
     * be called on the UI thread.
     *
     * @param queryDefinition
     *        The {@link QueryDefinition} to close editors for (must not be
     *        <code>null</code>).
     */
    public static void closeEditors(final QueryDefinition queryDefinition) {
        Check.notNull(queryDefinition, "queryDefinition"); //$NON-NLS-1$

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final List<IEditorReference> editorReferencesToCloseList = new ArrayList<IEditorReference>();

        final IEditorReference[] references = page.getEditorReferences();
        for (int i = 0; i < references.length; i++) {
            final IEditorPart editorPart = references[i].getEditor(false);

            if (editorPart != null) {
                final IEditorInput editorInput = editorPart.getEditorInput();

                if (editorInput instanceof QueryDocumentEditorInput) {
                    final QueryDocumentEditorInput queryInput = (QueryDocumentEditorInput) editorInput;

                    if (queryDefinition.getID().equals(queryInput.getQueryDocument().getGUID())) {
                        editorReferencesToCloseList.add(references[i]);
                    }
                }
            }
        }

        final IEditorReference[] referencesToClose =
            editorReferencesToCloseList.toArray(new IEditorReference[editorReferencesToCloseList.size()]);

        page.closeEditors(referencesToClose, false);
    }

    public static WorkItem[] workItemCheckinInfosToWorkItems(final WorkItemCheckinInfo[] workItemInfos) {
        final WorkItem[] workItems = new WorkItem[workItemInfos.length];
        for (int i = 0; i < workItemInfos.length; i++) {
            workItems[i] = workItemInfos[i].getWorkItem();
        }
        return workItems;
    }

    public static boolean anyWorkItemsAreInvalid(final WorkItem[] workItems) {
        for (final WorkItem workItem : workItems) {
            if (!workItem.isValid()) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyWorkItemsAreDirty(final WorkItem[] workItems) {
        for (final WorkItem workItem : workItems) {
            if (workItem.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public static IStatus syncWorkItemsToLatest(final Shell shell, final WorkItem[] workItems) {
        final TFSCommand command = new TFSCommand() {
            @Override
            public String getName() {
                return Messages.getString("WorkItemsCheckinControl.SyncWorkItemsForCheckin"); //$NON-NLS-1$
            }

            @Override
            public String getErrorDescription() {
                return Messages.getString("WorkItemsCheckinControl.ErrorSyncWorkItems"); //$NON-NLS-1$
            }

            @Override
            public String getLoggingDescription() {
                return Messages.getString("WorkItemsCheckinControl.SyncWorkItemsForCheckin", LocaleUtil.ROOT); //$NON-NLS-1$
            }

            @Override
            public boolean isCancellable() {
                return true;
            }

            @Override
            protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
                progressMonitor.beginTask(
                    Messages.getString("WorkItemsCheckinControl.SyncWorkItemsForCheckin"), //$NON-NLS-1$
                    workItems.length);
                for (final WorkItem workItem : workItems) {
                    if (!workItem.isDirty()) {
                        workItem.syncToLatest();
                    }
                    progressMonitor.worked(1);

                    if (progressMonitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                }
                return Status.OK_STATUS;
            }
        };

        return UICommandExecutorFactory.newUICommandExecutor(shell).execute(command);
    }

    public static boolean saveAllDirtyWorkItems(final Shell shell, final WorkItem[] workItems) {
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);

        for (final WorkItem workItem : workItems) {
            if (workItem.isDirty()) {
                final SaveWorkItemCommand command = new SaveWorkItemCommand(workItem);
                final IStatus status = executor.execute(command);

                if (!status.isOK()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getWorkItemDocumentName(
        final WorkItemType workItemType,
        final int workItemID,
        final int tempID) {
        /*
         * Internationalization note: format the IDs as strings instead of
         * passing numbers to MessageFormat, which always inserts group
         * separators for the locale (for example, "Work item 1,234").
         */
        if (workItemID == 0) {
            final String messageFormat = Messages.getString("WorkItemEditorInput.NewWitNameAndNumFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, workItemType.getName(), Integer.toString(tempID));
            return message;
        } else {
            final String messageFormat = Messages.getString("WorkItemEditorInput.ExistingWitNameAndIdFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, workItemType.getName(), Integer.toString(workItemID));
            return message;
        }
    }

    public static Image getImageForWorkItemQueryType(final ImageHelper imageHelper, final QueryType queryType) {
        if (QueryType.LIST.equals(queryType)) {
            return imageHelper.getImage("images/wit/query_type_flat.gif"); //$NON-NLS-1$
        } else if (QueryType.TREE.equals(queryType)) {
            return imageHelper.getImage("images/wit/query_type_tree.gif"); //$NON-NLS-1$
        } else if (QueryType.ONE_HOP.equals(queryType)) {
            return imageHelper.getImage("images/wit/query_type_onehop.gif"); //$NON-NLS-1$
        }

        return imageHelper.getImage("images/wit/query_type_flat_error.gif"); //$NON-NLS-1$
    }

    public static Image getImageForWorkItemQueryFolder(final ImageHelper imageHelper, final QueryFolder queryFolder) {
        /*
         * Deleted item, is not in the tree anymore (has no parent.) The label
         * decorator can call this method during the refresh where we're being
         * removed, thus handle it gracefully.
         */
        if (queryFolder.getParent() == null) {
            return null;
        }

        if (GUID.EMPTY.equals(new GUID(queryFolder.getParent().getID()))) {
            // This is a top level "Team Queries" / "My Queries" folder
            if (queryFolder.isPersonal()) {
                return imageHelper.getImage("images/wit/query_group_my.gif"); //$NON-NLS-1$
            }
            return imageHelper.getImage("images/wit/query_group_team.gif"); //$NON-NLS-1$
        }

        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    }

    public static void showWorkItemDoesNotExistError(final Shell shell, final int workItemID) {
        final String messageFormat = Messages.getString("GoToWorkItemAction.ErrorDialogTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(workItemID));
        MessageDialog.openError(shell, Messages.getString("GoToWorkItemAction.ErrorDialogTitle"), message); //$NON-NLS-1$
    }

    public static QueryFolder getMyQueriesFolder(final Project project) {
        if (project == null) {
            return null;
        }

        final QueryHierarchy queryHierarchy = project.getQueryHierarchy();
        if (queryHierarchy == null) {
            return null;
        }

        final QueryItem[] roots = queryHierarchy.getItems();
        if (roots == null) {
            return null;
        }

        for (int i = 0; i < roots.length; i++) {
            if (roots[i] instanceof QueryFolder && roots[i].isPersonal()) {
                return (QueryFolder) roots[i];
            }
        }

        return null;
    }

    public static String getCurrentTeamName() {
        final TeamExplorerContext teamContext = new TeamExplorerContext(null);
        final TeamConfiguration teamConfiguration = teamContext.getCurrentTeam();

        if (teamConfiguration == null) {
            return null;
        } else {
            return teamConfiguration.getTeamName();
        }
    }
}
