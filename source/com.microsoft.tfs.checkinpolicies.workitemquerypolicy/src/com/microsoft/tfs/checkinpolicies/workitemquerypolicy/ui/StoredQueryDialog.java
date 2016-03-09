// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.workitemquerypolicy.ui;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.checkinpolicies.workitemquerypolicy.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.util.Check;

public class StoredQueryDialog extends BaseDialog {
    private StoredQueryTable table;

    private final StoredQuery[] initialQueries;
    private final StoredQuery initiallySelected;

    private StoredQuery selectedQuery;

    public StoredQueryDialog(
        final Shell parentShell,
        final StoredQuery[] initialQueries,
        final StoredQuery initiallySelected) {
        super(parentShell);

        Check.notNull(initialQueries, "queries"); //$NON-NLS-1$

        this.initiallySelected = initiallySelected;

        // Take a copy of the passed initial queries for thread safety
        if (initialQueries == null) {
            this.initialQueries = null;
        } else {
            this.initialQueries = new StoredQuery[initialQueries.length];
            System.arraycopy(initialQueries, 0, this.initialQueries, 0, initialQueries.length);
        }
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = SWTUtil.fillLayout(dialogArea);
        layout.marginHeight = 10;
        layout.marginWidth = 10;

        table = new StoredQueryTable(dialogArea, SWT.NORMAL, "workItemQueryPolicy"); //$NON-NLS-1$
        table.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                selectedQuery = table.getSelectedStoredQuery();
            }
        });

        table.setStoredQueries(initialQueries);

        if (initiallySelected != null) {
            table.setSelectedStoredQuery(initiallySelected);
        }
    }

    @Override
    protected Point defaultComputeInitialSize() {
        return new Point(400, 400);
    }

    public StoredQuery getSelectedQuery() {
        return selectedQuery;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("StoredQueryDialog.DialogTitle"); //$NON-NLS-1$
    }
}
