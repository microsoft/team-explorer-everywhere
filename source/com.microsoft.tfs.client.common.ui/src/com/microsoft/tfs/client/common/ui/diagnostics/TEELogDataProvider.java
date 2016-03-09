// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.client.common.logging.TELogFileName;
import com.microsoft.tfs.client.common.logging.TELogUtils;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.DiagnosticLocale;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;

public class TEELogDataProvider implements DataProvider, AvailableCallback, PopulateCallback {
    private TabularData table;
    private TabularData tableNOLOC;

    private Map<File, TELogFileName> createNameMap(final File[] logFiles) {
        final Map<File, TELogFileName> map = new HashMap<File, TELogFileName>();

        for (int i = 0; i < logFiles.length; i++) {
            final TELogFileName logFileName = TELogUtils.getLogFileName(logFiles[i]);

            if (logFileName != null) {
                map.put(logFiles[i], logFileName);
            }
        }

        return map;
    }

    private List<File> prune(final File[] input, final Map<File, TELogFileName> nameMap) {
        final List<File> results = new ArrayList<File>();

        for (int i = 0; i < input.length; i++) {
            if (nameMap.containsKey(input[i])) {
                results.add(input[i]);
            }
        }

        return results;
    }

    @Override
    public void populate() throws Exception {
        table = null;

        final FilesystemPersistenceStore logLocation = TELogUtils.getTeamExplorerLogsLocation();
        final File[] logFileArray = TELogUtils.getAllLogFilesInLocation(logLocation.getStoreFile());

        if (logFileArray.length == 0) {
            return;
        }

        final Map<File, TELogFileName> nameMap = createNameMap(logFileArray);

        final List<File> logFiles = prune(logFileArray, nameMap);

        Collections.sort(logFiles, new Comparator<File>() {
            @Override
            public int compare(final File file1, final File file2) {
                final TELogFileName name1 = nameMap.get(file1);
                final TELogFileName name2 = nameMap.get(file2);
                return name1.compareTo(name2);
            }
        });

        final Set<String> logTypes = new HashSet<String>();
        for (final Iterator<TELogFileName> it = nameMap.values().iterator(); it.hasNext();) {
            final TELogFileName logFileName = it.next();
            logTypes.add(logFileName.getLogType());
        }

        final Set<File> inUseLogs = new HashSet<File>();
        for (final Iterator<String> it = logTypes.iterator(); it.hasNext();) {
            final String logType = it.next();
            final File inUseLogFile = TELogUtils.getInUseLogFileForLogType(logType);

            if (inUseLogFile != null) {
                inUseLogs.add(inUseLogFile);
            }
        }

        table = createTable(DiagnosticLocale.USER_LOCALE, nameMap, logFiles, inUseLogs);
        tableNOLOC = createTable(DiagnosticLocale.SUPPORT_LOCALE, nameMap, logFiles, inUseLogs);
    }

    private TabularData createTable(
        final Locale locale,
        final Map<File, TELogFileName> nameMap,
        final List<File> logFiles,
        final Set<File> inUseLogs) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("TEELogDataProvider.ColumnNameInUse", locale), //$NON-NLS-1$
            Messages.getString("TEELogDataProvider.ColumnNameLogType", locale), //$NON-NLS-1$
            Messages.getString("TEELogDataProvider.ColumnNameApplication", locale), //$NON-NLS-1$
            Messages.getString("TEELogDataProvider.ColumnNameDate", locale), //$NON-NLS-1$
            Messages.getString("TEELogDataProvider.ColumnNameSize", locale), //$NON-NLS-1$
            Messages.getString("TEELogDataProvider.ColumnNameLastModified", locale) //$NON-NLS-1$
        });

        for (final Iterator<File> it = logFiles.iterator(); it.hasNext();) {
            final File logFile = it.next();
            final TELogFileName logFileName = nameMap.get(logFile);
            final boolean inUse = inUseLogs.contains(logFile);
            final long length = logFile.length();

            if (!inUse && length == 0) {
                continue;
            }

            table.addRow(new Row(new Object[] {
                (inUse ? "*" : ""), //$NON-NLS-1$ //$NON-NLS-2$
                logFileName.getLogType(),
                logFileName.getApplication(),
                logFileName.getDate(),
                String.valueOf(length),
                new Date(logFile.lastModified())
            }, logFile));
        }

        table.setSorted(true);

        return table;
    }

    @Override
    public Object getData() {
        return table;
    }

    @Override
    public Object getDataNOLOC() {
        return tableNOLOC;
    }

    @Override
    public boolean isAvailable() {
        return table != null && tableNOLOC != null;
    }
}
