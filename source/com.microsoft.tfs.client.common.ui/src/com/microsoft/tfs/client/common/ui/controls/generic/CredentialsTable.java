// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.credentials.CachedCredentials;

/**
 *
 *
 * @threadsafety unknown
 */
public class CredentialsTable extends TableControl {
    private static final String SERVER_URI_COLUMN_ID = "serverURI"; //$NON-NLS-1$
    private static final String USERNAME_COLUMN_ID = "username"; //$NON-NLS-1$
    private static final String PASSWORD_COLUMN_ID = "password"; //$NON-NLS-1$

    public CredentialsTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public CredentialsTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, CachedCredentials.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("CredentialsTable.ColumnHeaderServerURI"), //$NON-NLS-1$
                100,
                0.34F,
                SERVER_URI_COLUMN_ID),
            new TableColumnData(
                Messages.getString("CredentialsTable.ColumnHeaderUsername"), //$NON-NLS-1$
                100,
                0.33F,
                USERNAME_COLUMN_ID),
            new TableColumnData(
                Messages.getString("CredentialsTable.ColumnHeaderPassword"), //$NON-NLS-1$
                200,
                0.33F,
                PASSWORD_COLUMN_ID)
        };

        setupTable(true, true, columnData);

        setUseViewerDefaults();
        setEnableTooltips(true);
    }

    public void setCredentials(final CachedCredentials[] credentials) {
        setElements(credentials);
    }

    public CachedCredentials[] getCredentials() {
        return (CachedCredentials[]) getElements();
    }

    public CachedCredentials getSelectedCredentials() {
        return (CachedCredentials) getSelectedElement();
    }

    public void setSelectedCredentials(final CachedCredentials newCredentials) {
        setSelectedElement(newCredentials);
    }

    public void addSelectionListener(final ISelectionChangedListener listener) {
        getViewer().addSelectionChangedListener(listener);
    }

    public void removeSelectionListener(final ISelectionChangedListener listener) {
        getViewer().removeSelectionChangedListener(listener);
    }

    @Override
    public String getColumnText(final Object element, final String columnPropertyName) {
        final CachedCredentials credentials = (CachedCredentials) element;

        if (SERVER_URI_COLUMN_ID.equals(columnPropertyName)) {
            return credentials.getURI().toASCIIString();
        } else if (USERNAME_COLUMN_ID.equals(columnPropertyName)) {
            return credentials.getUsername();
        } else if (PASSWORD_COLUMN_ID.equals(columnPropertyName)) {
            if (credentials.getPassword() != null && credentials.getPassword().length() > 0) {
                return "**********"; //$NON-NLS-1$
            } else {
                return ""; //$NON-NLS-1$
            }
        }

        return null;
    }
}
