// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.text.MessageFormat;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.QueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.views.ITeamExplorerView;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.util.Check;

public abstract class TeamExplorerWITBaseAction extends ObjectActionDelegate {
    private final static Log log = LogFactory.getLog(TeamExplorerWITBaseAction.class);

    protected QueryItem selectedQueryItem;

    public TeamExplorerContext getContext() {
        final IWorkbenchPart part = getTargetPart();
        Check.isTrue(part instanceof ITeamExplorerView);
        return ((ITeamExplorerView) part).getContext();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
     * .IAction, org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled()) {
            action.setEnabled(
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer() != null);
        }

        final Object element = getSelectionFirstElement();

        if (element instanceof QueryItem) {
            selectedQueryItem = (QueryItem) element;
        } else if (element instanceof QueryFavoriteItem) {
            selectedQueryItem = ((QueryFavoriteItem) element).getQueryDefinition();
        } else {
            selectedQueryItem = null;
            if (element != null) {
                log.error("Team Explorer Action contributed to non-Team Explorer node"); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void run(final IAction action) {
        try {
            ClientTelemetryHelper.sendRunActionEvent(this);
            doRun(action);
        } catch (final Throwable t) {
            final String className = this.getClass().getName();
            final String messageFormat = Messages.getString("TeamExplorerBaseAction.ErrorRunningActionFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, className);

            log.error(message, t);

            ErrorDialog.openError(
                getShell(),
                Messages.getString("TeamExplorerBaseAction.ErrorDialogTitle"), //$NON-NLS-1$
                message,
                new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, null, t));
        }
    }

    protected abstract void doRun(IAction action);

    protected QueryHierarchy getQueryHierarchy() {
        Check.notNull(selectedQueryItem, "selectedQueryItem"); //$NON-NLS-1$
        Check.notNull(selectedQueryItem.getProject(), "selectedQueryItem.getProject()"); //$NON-NLS-1$
        Check.notNull(
            selectedQueryItem.getProject().getQueryHierarchy(),
            "selectedQueryItem.getProject().getQueryHierarchy()"); //$NON-NLS-1$

        return selectedQueryItem.getProject().getQueryHierarchy();
    }

    protected boolean selectionContainsRootFolder() {
        final IStructuredSelection selection = getStructuredSelection();

        if (selection != null) {
            Iterator i;

            for (i = getStructuredSelection().iterator(); i.hasNext();) {
                final QueryItem queryItem = (QueryItem) i.next();

                if (queryItem instanceof QueryFolder) {
                    final QueryFolder queryFolder = (QueryFolder) queryItem;
                    if (queryFolder.getParent() instanceof QueryHierarchy) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
