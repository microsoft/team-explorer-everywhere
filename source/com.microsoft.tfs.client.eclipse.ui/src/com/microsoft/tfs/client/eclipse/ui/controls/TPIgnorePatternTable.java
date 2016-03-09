// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.controls;

import java.util.Comparator;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.eclipse.ui.Messages;

public class TPIgnorePatternTable extends TableControl {
    public TPIgnorePatternTable(final Composite parent, final int style) {
        super(parent, style, Pattern.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("TPIgnorePatternsTable.ColumnHeaderPattern"), 100, 1.0f, null), //$NON-NLS-1$
        };

        setupTable(true, true, columnData);

        setUseViewerDefaults();
        setEnableTooltips(false);

        final TableViewerSorter sorter = (TableViewerSorter) getViewer().getSorter();
        sorter.setComparator(0, new Comparator<Pattern>() {
            @Override
            public int compare(final Pattern o1, final Pattern o2) {
                return o1.pattern().compareTo(o2.pattern());
            }
        });
    }

    public void setPatterns(final Pattern[] patterns) {
        setElements(patterns);
    }

    public Pattern[] getPatterns() {
        return (Pattern[]) getElements();
    }

    public void setSelectedPatterns(final Pattern[] patterns) {
        setSelectedElements(patterns);
    }

    public void setSelectedPattern(final Pattern pattern) {
        setSelectedElement(pattern);
    }

    public Pattern[] getSelectedPatterns() {
        return (Pattern[]) getSelectedElements();
    }

    public Pattern getSelectedPattern() {
        return (Pattern) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        return ((Pattern) element).pattern();
    }

    public void setCheckedPatterns(final Pattern[] patterns) {
        setCheckedElements(patterns);
    }

    public Pattern[] getCheckedPattern() {
        return (Pattern[]) getCheckedElements();
    }

}
