// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.controls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.eclipse.ui.Messages;

public class ProjectTable extends TableControl {
    public final static int LOCAL_PATH = (1 << 31);
    public final static int SERVER_PATH = (1 << 30);

    private final static int ALL_TABLE_STYLE = (LOCAL_PATH);

    private final int style;

    public ProjectTable(final Composite parent, final int style) {
        super(parent, (style & ~(ALL_TABLE_STYLE)), IProject.class, null);

        this.style = (style & ALL_TABLE_STYLE);

        setupTable(true, false, getColumnData());
        setUseViewerDefaults();
    }

    private TableColumnData[] getColumnData() {
        float projectWidth = 0.20F, localWidth = 0.40F, serverWidth = 0.40F;

        /* Rejiggle widths */
        if ((style & LOCAL_PATH) == 0 && (style & SERVER_PATH) == SERVER_PATH) {
            projectWidth += 0.20F;
            serverWidth += 0.20F;
        } else if ((style & LOCAL_PATH) == LOCAL_PATH && (style & SERVER_PATH) == 0) {
            projectWidth += 0.20F;
            localWidth += 0.20F;
        } else if ((style & LOCAL_PATH) == 0 && (style & SERVER_PATH) == 0) {
            projectWidth += 0.80F;
        }

        final List columnDataList = new ArrayList();

        columnDataList.add(
            new TableColumnData(Messages.getString("ProjectTable.ColumnNameProject"), 25, projectWidth, "project")); //$NON-NLS-1$ //$NON-NLS-2$

        if ((style & LOCAL_PATH) == LOCAL_PATH) {
            columnDataList.add(
                new TableColumnData(Messages.getString("ProjectTable.ColumnNameLocalPath"), 100, localWidth, "local")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if ((style & SERVER_PATH) == SERVER_PATH) {
            columnDataList.add(new TableColumnData(
                Messages.getString("ProjectTable.ColumnNameServerPath"), //$NON-NLS-1$
                100,
                serverWidth,
                "server")); //$NON-NLS-1$
        }

        return (TableColumnData[]) columnDataList.toArray(new TableColumnData[columnDataList.size()]);
    }

    public void setProjects(final IProject[] projects) {
        setElements(projects);
    }

    @Override
    protected String getColumnText(final Object element, final String propertyName) {
        final IProject project = (IProject) element;

        if (propertyName.equals("project")) //$NON-NLS-1$
        {
            return project.getName();
        } else if (propertyName.equals("local")) //$NON-NLS-1$
        {
            return project.getLocation().toOSString();
        } else if (propertyName.equals("server")) //$NON-NLS-1$
        {

        }

        return ""; //$NON-NLS-1$
    }
}
