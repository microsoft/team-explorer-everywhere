// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;

public class ColumnOptionsAction extends Action {
    private QueryEditor editor;
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public ColumnOptionsAction() {
        this(null);
    }

    public ColumnOptionsAction(final QueryEditor editor) {
        this.editor = editor;

        setToolTipText(Messages.getString("ColumnOptionsAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(imageHelper.getImageDescriptor("images/wit/column_options.gif")); //$NON-NLS-1$
    }

    public void setActiveEditor(final QueryEditor editor) {
        this.editor = editor;
    }

    @Override
    public void run() {
        if (editor == null) {
            return;
        }

        final QueryDocument queryDocument = editor.getQueryDocument();

        final ResultOptions resultOptions = new ResultOptions(queryDocument.getResultOptions(), true, queryDocument);

        final ResultOptionsDialog dialog = new ResultOptionsDialog(
            editor.getSite().getShell(),
            queryDocument.getWorkItemClient().getFieldDefinitions(),
            resultOptions);

        if (IDialogConstants.OK_ID == dialog.open()) {
            editor.getQueryDocument().setResultOptions(resultOptions);
        }
    }
}
