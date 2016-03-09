// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.logging;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;
import com.microsoft.tfs.util.FileLastModifiedComparator;

/**
 * Static utility methods related to Team Explorer-specific logging policy.
 */
public class TELogUtils {
    private static final Map logTypesToInUseLogFiles = new HashMap();

    /**
     * Creates a File object appropriate for use for a log file of the given
     * type.
     *
     * @param logType
     *        the log type
     * @param location
     *        the location of the log file
     * @param setInUse
     *        true to call setInUseLogFileForLogType with the log file
     * @return a file object representing the log file
     */
    public static File createLogFileObject(final String logType, final File location, final boolean setInUse) {
        final TELogFileName logFileName = new TELogFileName(logType);

        final File logFile = logFileName.createFileDescriptor(location);

        if (setInUse) {
            TELogUtils.setInUseLogFileForLogType(logType, logFile);
        }

        return logFile;
    }

    /**
     * Obtains the {@link PersistenceStore} where all Team Explorer log files
     * should be stored. This {@link PersistenceStore} object is cached
     * internally.
     *
     * @return the {@link PersistenceStore} described above
     */
    public synchronized static VersionedVendorFilesystemPersistenceStore getTeamExplorerLogsLocation() {
        return (VersionedVendorFilesystemPersistenceStore) DefaultPersistenceStoreProvider.INSTANCE.getLogPersistenceStore();
    }

    public static TELogFileName getLogFileName(final File logFile) {
        return TELogFileName.parse(logFile.getName());
    }

    /**
     * Returns all currently existing log files of a given log type residing in
     * a given directory. The result array is sorted by the last modified date
     * of the files. If the sortAscending option is true, earlier last modified
     * dates come first.
     *
     * @param logType
     *        the log type
     * @param location
     *        the directory
     * @param sortAscending
     *        true to sort last modified dates in ascending order
     * @return matched Files
     */
    public static File[] getAllLogFilesForLogType(
        final String logType,
        final File location,
        final boolean sortAscending) {
        final File[] files = location.listFiles(TELogFileName.getFilterForLogFilesOfTypeForCurrentApplication(logType));

        final FileLastModifiedComparator c = new FileLastModifiedComparator();
        c.setAscending(sortAscending);
        Arrays.sort(files, c);

        return files;
    }

    /**
     * Obtains all of the log file currently existing in the given location.
     *
     * @param location
     *        the location to search
     * @return an unsorted array of log files
     */
    public static File[] getAllLogFilesInLocation(final File location) {
        return location.listFiles(TELogFileName.getFilterForAllLogFiles());
    }

    /**
     * Marks the specified file as being the currently in-use log file for the
     * given log type.
     *
     * @param logType
     *        the log type
     * @param logFile
     *        the in-use log file
     */
    static void setInUseLogFileForLogType(final String logType, final File logFile) {
        synchronized (logTypesToInUseLogFiles) {
            logTypesToInUseLogFiles.put(logType, logFile);
        }
    }

    /**
     * Obtains the currently in-use log file, if any, of the given log type.
     * This will only return a result if there has been a previous call to
     * setInUseLogFileForLogType for the same type.
     *
     * @param logType
     *        the log type
     * @return the in-use log file, or null if there is none
     */
    public static File getInUseLogFileForLogType(final String logType) {
        synchronized (logTypesToInUseLogFiles) {
            return (File) logTypesToInUseLogFiles.get(logType);
        }
    }
}
