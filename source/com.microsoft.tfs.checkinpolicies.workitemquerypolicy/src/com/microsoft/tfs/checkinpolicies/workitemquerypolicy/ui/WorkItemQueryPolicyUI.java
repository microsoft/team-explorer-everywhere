// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.workitemquerypolicy.ui;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.UIPlugin;

import com.microsoft.tfs.checkinpolicies.workitemquerypolicy.Activator;
import com.microsoft.tfs.checkinpolicies.workitemquerypolicy.Messages;
import com.microsoft.tfs.checkinpolicies.workitemquerypolicy.WorkItemQueryPolicy;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.wit.dialogs.SelectQueryItemDialog;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.clients.versioncontrol.TeamProject;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.util.GUID;

public class WorkItemQueryPolicyUI extends WorkItemQueryPolicy {
    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        final Shell shell = (Shell) policyEditArgs.getContext().getProperty(PolicyContextKeys.SWT_SHELL);

        if (shell == null) {
            return false;
        }

        final TeamProject teamProject = policyEditArgs.getTeamProject();

        final WorkItemClient wic = teamProject.getVersionControlClient().getConnection().getWorkItemClient();

        final Project correctWorkItemProject = wic.getProjects().get(teamProject.getName());

        if (correctWorkItemProject == null) {
            final String messageFormat = Messages.getString("WorkItemQueryPolicyUI.ProjectMismatchFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, teamProject.getName());

            MessageBoxHelpers.errorMessageBox(
                shell,
                Messages.getString("WorkItemQueryPolicyUI.ErrorMessageTitle"), //$NON-NLS-1$
                message);
            UIPlugin.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, 0, message, null));
            return false;
        }

        final QueryHierarchy queryHierarchy = correctWorkItemProject.getQueryHierarchy();

        final GUID existingQueryGUID = getQueryGUID();
        final QueryItem existingQueryItem = (existingQueryGUID == null) ? null : queryHierarchy.find(existingQueryGUID);

        final SelectQueryItemDialog queryDefinitionDialog =
            new SelectQueryItemDialog(shell, wic.getProjects().getProjects(), new String[] {
                correctWorkItemProject.getName()
        }, existingQueryItem, QueryItemType.QUERY_DEFINITION);

        if (queryDefinitionDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        final QueryItem queryItem = queryDefinitionDialog.getSelectedQueryItem();

        if (queryItem == null || !(queryItem instanceof QueryDefinition)) {
            return false;
        }

        wic.getStoredQuery(queryItem.getID());

        setQueryGUID(queryItem.getID());

        return true;
    }
}
