// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.WIQDocument;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.util.StringUtil;

public class SaveWIQLAction extends TeamExplorerWITBaseAction {
    @Override
    protected void doRun(final IAction action) {
        final QueryDefinition queryDefinition = (QueryDefinition) selectedQueryItem;
        final StoredQuery query = WorkItemHelpers.createStoredQueryFromDefinition(queryDefinition);

        final FileDialog dlg = new FileDialog(getShell(), SWT.SAVE);
        dlg.setFilterNames(new String[] {
            "*.wiq" //$NON-NLS-1$
        });
        dlg.setFilterExtensions(new String[] {
            "*.wiq" //$NON-NLS-1$
        });

        dlg.setText(Messages.getString("SaveWIQLAction.FileDialogTitle")); //$NON-NLS-1$
        dlg.setFileName(query.getName() + ".wiq"); //$NON-NLS-1$

        final String saveFileName = dlg.open();
        if (saveFileName == null) {
            return;
        }

        final String queryText = query.getQueryText();
        final String teamName;
        if (StringUtil.isNullOrEmpty(queryText) || !queryText.contains(WIQLOperators.MACRO_CURRENT_ITERATION)) {
            teamName = null;
        } else {
            teamName = WorkItemHelpers.getCurrentTeamName();
        }

        final WIQDocument wiqDocument = new WIQDocument(
            queryText,
            getContext().getServer().getConnection().getBaseURI().toString(),
            query.getProject().getName(),
            teamName);

        wiqDocument.save(new File(saveFileName));
    }
}
