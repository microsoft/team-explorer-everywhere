// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.util.Locale;

import org.eclipse.swt.graphics.Point;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;
import com.microsoft.tfs.client.common.ui.framework.dialog.DialogSettingsHelper;
import com.microsoft.tfs.client.common.ui.framework.dialog.StoredDialogStatistics;

public class DialogStatisticsDataProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final StoredDialogStatistics[] stats = DialogSettingsHelper.getStatistics();

        final TabularData table = new TabularData(new String[] {
            Messages.getString("DialogStatisticsDataProvider.ColumnNameKeys", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameViews", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameSince", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameOrigin", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameSize", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameAvgOpenTime", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameAccOpenTime", locale), //$NON-NLS-1$
            Messages.getString("DialogStatisticsDataProvider.ColumnNameTimeCounts", locale) //$NON-NLS-1$
        });

        for (int i = 0; i < stats.length; i++) {
            final Point origin = (stats[i].getOrigin() != null) ? stats[i].getOrigin() : new Point(-1, -1);
            final Point size = (stats[i].getSize() != null) ? stats[i].getSize() : new Point(-1, -1);

            final Row row = new Row(new Object[] {
                stats[i].getSettingsKey(),
                String.valueOf(stats[i].getViews()),
                stats[i].getSince(),
                "(" + origin.x + "," + origin.y + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "(" + size.x + "," + size.y + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                String.valueOf(stats[i].getAverageOpenTimeMs()),
                String.valueOf(stats[i].getAccumulatedOpenTimeMs()),
                String.valueOf(stats[i].getTimeCounts())
            });

            table.addRow(row);
        }

        return table;
    }
}
