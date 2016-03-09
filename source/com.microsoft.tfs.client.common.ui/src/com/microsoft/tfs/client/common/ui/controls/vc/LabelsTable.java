// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.DateFormat;
import java.util.Comparator;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.NewlineUtils;

public class LabelsTable extends TableControl {
    private static final String DATE_COLUMN_ID = "comment"; //$NON-NLS-1$
    private static final String COMMENT_COLUMN_ID = "comment"; //$NON-NLS-1$
    private static final String OWNER_COLUMN_ID = "owner"; //$NON-NLS-1$
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$
    private final DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    public LabelsTable(final Composite parent, final TFSRepository repository, final int style) {
        this(parent, repository, style, null);
    }

    public LabelsTable(
        final Composite parent,
        final TFSRepository repository,
        final int style,
        final String viewDataKey) {
        super(parent, style, VersionControlLabel.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("LabelsTable.ColumnHeaderNameText"), 100, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("LabelsTable.ColumnHeaderOwnerText"), 100, OWNER_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("LabelsTable.ColumnHeaderDateText"), 100, COMMENT_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("LabelsTable.ColumnHeaderCommentText"), 200, DATE_COLUMN_ID) //$NON-NLS-1$
        };
        setupTable(true, false, columnData);

        setUseViewerDefaults();

        final TableViewerSorter sorter = (TableViewerSorter) getViewer().getSorter();

        sorter.setComparator(DATE_COLUMN_ID, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                return ((VersionControlLabel) o1).getDate().compareTo(((VersionControlLabel) o2).getDate());
            }
        });

        Check.notNull(repository, "repository"); //$NON-NLS-1$
    }

    public void setLabels(final VersionControlLabel[] labels) {
        setElements(labels);
    }

    public VersionControlLabel[] getLabels() {
        return (VersionControlLabel[]) getElements();
    }

    public void setSelectedLabels(final VersionControlLabel[] labels) {
        setSelectedElements(labels);
    }

    public void setSelectedLabel(final VersionControlLabel label) {
        setSelectedElement(label);
    }

    public VersionControlLabel[] getSelectedLabels() {
        return (VersionControlLabel[]) getSelectedElements();
    }

    public VersionControlLabel getSelectedLabel() {
        return (VersionControlLabel) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final VersionControlLabel label = (VersionControlLabel) element;

        switch (columnIndex) {
            case 0:
                return label.getName();

            case 1:
                return label.getOwnerDisplayName();
            case 2:
                return dateFormat.format(label.getDate().getTime());
            case 3:
                return NewlineUtils.stripNewlines(label.getComment());

            default:
                return ""; //$NON-NLS-1$
        }
    }
}
