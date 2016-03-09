// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.logging;

import java.io.File;
import java.io.FileFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TELogFileName implements Comparable {
    private static final String LOGFILE_DATE_FORMAT = "yyyy.MM.dd-HH.mm.ss"; //$NON-NLS-1$

    public static final String LOGFILE_EXTENSION = ".log"; //$NON-NLS-1$

    private final String logType;
    private final String application;
    private final Date date;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(LOGFILE_DATE_FORMAT);

    public static FileFilter getFilterForAllLogFiles() {
        return new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(LOGFILE_EXTENSION);
            }
        };
    }

    public static FileFilter getFilterForLogFilesOfTypeForCurrentApplication(final String logType) {
        final String prefix = logType + "-" + ApplicationIdentifier.getApplication(); //$NON-NLS-1$

        return new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isFile()
                    && pathname.getName().startsWith(prefix)
                    && pathname.getName().endsWith(LOGFILE_EXTENSION);
            }
        };
    }

    public static TELogFileName parse(final String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }

        final String[] sections = name.split("-"); //$NON-NLS-1$

        if (sections.length < 4) {
            return null;
        }

        final String sDate = sections[sections.length - 2] + "-" + sections[sections.length - 1]; //$NON-NLS-1$
        final SimpleDateFormat dateFormat = new SimpleDateFormat(LOGFILE_DATE_FORMAT);
        Date date;
        try {
            date = dateFormat.parse(sDate);
        } catch (final ParseException e) {
            return null;
        }

        final String application = sections[sections.length - 3];

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < (sections.length - 3); i++) {
            sb.append(sections[i]);
            if (i < (sections.length - 4)) {
                sb.append("-"); //$NON-NLS-1$
            }
        }

        final String logType = sb.toString();

        return new TELogFileName(logType, application, date);
    }

    public TELogFileName(final String logType) {
        this(logType, ApplicationIdentifier.getApplication(), new Date());
    }

    private TELogFileName(final String logType, final String application, final Date date) {
        this.logType = logType;
        this.application = application;
        this.date = date;
    }

    @Override
    public int compareTo(final Object o) {
        final TELogFileName other = (TELogFileName) o;

        int c = logType.compareTo(other.logType);
        if (c == 0) {
            c = application.compareTo(other.application);
            if (c == 0) {
                c = other.date.compareTo(date);
            }
        }

        return c;
    }

    public String getFileName() {
        return logType + "-" + application + "-" + dateFormat.format(date) + LOGFILE_EXTENSION; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public File createFileDescriptor(final File location) {
        return new File(location, getFileName());
    }

    public String getApplication() {
        return application;
    }

    public Date getDate() {
        return date;
    }

    public String getLogType() {
        return logType;
    }
}
