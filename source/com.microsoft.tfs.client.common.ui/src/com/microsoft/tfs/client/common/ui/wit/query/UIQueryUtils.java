// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.query;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.internal.AccessDeniedWorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortField;

public class UIQueryUtils {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public static boolean verifyAccessToWorkItem(final WorkItem workItem) {
        if (workItem == null || workItem instanceof AccessDeniedWorkItemImpl) {
            final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            MessageBoxHelpers.errorMessageBox(
                shell,
                Messages.getString("UiQueryUtils.NoAccessDialogTitle"), //$NON-NLS-1$
                Messages.getString("UiQueryUtils.NoAccessDialogText")); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    public static void generateDebugInfo(final QueryDocument queryDocument, final StringBuffer buffer) {
        final ResultOptions resultOptions = queryDocument.getResultOptions();

        buffer.append("QueryDocument"); //$NON-NLS-1$
        if (queryDocument.getGUID() != null) {
            buffer.append(" guid=[" + queryDocument.getGUID() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (queryDocument.getFile() != null) {
            buffer.append(" file=[" + queryDocument.getFile().getAbsolutePath() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            buffer.append(" (new)"); //$NON-NLS-1$
        }
        buffer.append(" dirty=[" + queryDocument.isDirty() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(" name=[" + queryDocument.getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(" scope=[" + queryDocument.getQueryScope() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(NEWLINE);
        buffer.append("filter expression: [" + queryDocument.getFilterExpression() + "]").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$

        buffer.append(NEWLINE);

        buffer.append("display fields: " + resultOptions.getDisplayFields().getCount()).append(NEWLINE); //$NON-NLS-1$
        for (int i = 0; i < resultOptions.getDisplayFields().getCount(); i++) {
            final DisplayField displayField = resultOptions.getDisplayFields().get(i);
            buffer.append(displayField.getFieldName() + "," + displayField.getWidth()).append(NEWLINE); //$NON-NLS-1$
        }

        buffer.append(NEWLINE);

        buffer.append("sort fields: " + resultOptions.getSortFields().getCount()).append(NEWLINE); //$NON-NLS-1$
        for (int i = 0; i < resultOptions.getSortFields().getCount(); i++) {
            final SortField sortField = resultOptions.getSortFields().get(i);
            buffer.append(sortField.getFieldName() + "," + (sortField.isAscending() ? "ASC" : "DESC")).append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        buffer.append(NEWLINE);

        buffer.append("WIQL:").append(NEWLINE); //$NON-NLS-1$
        try {
            final String wiql = queryDocument.getQueryText();
            buffer.append(wiql).append(NEWLINE);
        } catch (final Exception ex) {
            buffer.append("unable to generate WIQL: " + ex.getMessage()).append(NEWLINE); //$NON-NLS-1$
        }
    }
}
