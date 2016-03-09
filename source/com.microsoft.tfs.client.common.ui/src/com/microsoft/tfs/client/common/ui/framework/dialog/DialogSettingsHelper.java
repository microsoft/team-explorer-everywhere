// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.dialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

/**
 * This class is modeled after
 * org.eclipse.debug.internal.ui.DialogSettingsHelper.
 */
public class DialogSettingsHelper {
    private static final Log log = LogFactory.getLog(DialogSettingsHelper.class);

    private static final String DIALOG_ORIGIN_X = "DIALOG_ORIGIN_X"; //$NON-NLS-1$
    private static final String DIALOG_ORIGIN_Y = "DIALOG_ORIGIN_Y"; //$NON-NLS-1$
    private static final String DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$
    private static final String DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$
    private static final String SINCE = "SINCE"; //$NON-NLS-1$
    private static final String VIEWS = "VIEWS"; //$NON-NLS-1$
    private static final String ACCUMULATED_OPEN_TIME = "ACCUMULATED_OPEN_TIME"; //$NON-NLS-1$
    private static final String TIME_COUNTS = "TIME_COUNTS"; //$NON-NLS-1$

    private static final String DATE_FORMAT = "MM/dd/yy HH:mm:ss"; //$NON-NLS-1$
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public static void persistShellGeometry(final Shell shell, final String dialogSettingsKey) {
        final Point shellLocation = shell.getLocation();
        final Point shellSize = shell.getSize();
        final IDialogSettings settings = getDialogSettings(dialogSettingsKey);
        settings.put(DIALOG_ORIGIN_X, shellLocation.x);
        settings.put(DIALOG_ORIGIN_Y, shellLocation.y);
        settings.put(DIALOG_WIDTH, shellSize.x);
        settings.put(DIALOG_HEIGHT, shellSize.y);
    }

    public static String getDebugInfo(final String dialogSettingsKey) {
        final IDialogSettings settings = getDialogSettings(dialogSettingsKey);

        long averageOpenTime = -1;
        long timeCounts = 0;

        try {
            timeCounts = settings.getInt(TIME_COUNTS);
            averageOpenTime = settings.getLong(ACCUMULATED_OPEN_TIME) / timeCounts;
        } catch (final NumberFormatException ex) {
            averageOpenTime = -1;
        }

        return

        "stored origin (" //$NON-NLS-1$
            + settings.get(DIALOG_ORIGIN_X)
            + "," //$NON-NLS-1$
            + settings.get(DIALOG_ORIGIN_Y)
            + ")" //$NON-NLS-1$
            + NEWLINE
            + "stored size (" //$NON-NLS-1$
            + settings.get(DIALOG_WIDTH)
            + "," //$NON-NLS-1$
            + settings.get(DIALOG_HEIGHT)
            + ")" //$NON-NLS-1$
            + NEWLINE
            + "views: " //$NON-NLS-1$
            + settings.get(VIEWS)
            + NEWLINE
            +

        ((averageOpenTime != -1) ? "average display time: " //$NON-NLS-1$
            + averageOpenTime
            + " ms (" //$NON-NLS-1$
            + timeCounts
            + " records)" //$NON-NLS-1$
            + NEWLINE : "") //$NON-NLS-1$
            + "since " //$NON-NLS-1$
            + settings.get(SINCE);
    }

    private static StoredDialogStatistics createStatistics(final IDialogSettings settings) {
        final String settingsKey = settings.getName();

        Point origin = null;
        try {
            origin = new Point(settings.getInt(DIALOG_ORIGIN_X), settings.getInt(DIALOG_ORIGIN_Y));
        } catch (final NumberFormatException ex) {

        }

        Point size = null;
        try {
            size = new Point(settings.getInt(DIALOG_WIDTH), settings.getInt(DIALOG_HEIGHT));
        } catch (final NumberFormatException ex) {

        }

        Date since = null;
        final String sinceValue = settings.get(SINCE);
        if (sinceValue != null) {
            try {
                final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                since = formatter.parse(sinceValue);
            } catch (final ParseException ex) {

            }
        }

        int views = -1;
        long accumulatedOpenTimeMs = -1;
        int timeCounts = -1;

        try {
            views = settings.getInt(VIEWS);
        } catch (final NumberFormatException ex) {

        }

        try {
            accumulatedOpenTimeMs = settings.getLong(ACCUMULATED_OPEN_TIME);
        } catch (final NumberFormatException ex) {

        }

        try {
            timeCounts = settings.getInt(TIME_COUNTS);
        } catch (final NumberFormatException ex) {

        }

        long averageOpenTimeMs = -1;

        if (accumulatedOpenTimeMs != -1 && timeCounts != -1) {
            averageOpenTimeMs = accumulatedOpenTimeMs / timeCounts;
        }

        return new StoredDialogStatistics(
            settingsKey,
            origin,
            size,
            since,
            views,
            accumulatedOpenTimeMs,
            timeCounts,
            averageOpenTimeMs);
    }

    public static void recordDialogOpened(final String dialogSettingsKey) {
        final IDialogSettings settings = getDialogSettings(dialogSettingsKey);
        int views = 0;
        try {
            views = settings.getInt(VIEWS);
        } catch (final NumberFormatException ex) {
            // ignore, leave views set to 0
        }
        settings.put(VIEWS, views + 1);
    }

    public static void recordDialogClosed(final String dialogSettingsKey, final long elapsedTimeOpen) {
        final IDialogSettings settings = getDialogSettings(dialogSettingsKey);

        /*
         * Add to accumulated open time
         */
        long accumulatedOpenTime = 0;
        try {
            accumulatedOpenTime = settings.getLong(ACCUMULATED_OPEN_TIME);
        } catch (final NumberFormatException ex) {
            // ignore
        }
        settings.put(ACCUMULATED_OPEN_TIME, accumulatedOpenTime + elapsedTimeOpen);

        /*
         * Add to time counts
         */
        int timeCounts = 0;
        try {
            timeCounts = settings.getInt(TIME_COUNTS);
        } catch (final NumberFormatException ex) {
            // ignore
        }
        settings.put(TIME_COUNTS, timeCounts + 1);
    }

    private static IDialogSettings getDialogSettings(final String dialogSettingsKey) {
        final IDialogSettings settings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(dialogSettingsKey);
        if (section == null) {
            section = settings.addNewSection(dialogSettingsKey);
            final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            section.put(SINCE, formatter.format(new Date()));
        }

        return section;
    }

    public static StoredDialogStatistics[] getStatistics() {
        final IDialogSettings settings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        final IDialogSettings[] sections = settings.getSections();
        final List statistics = new ArrayList();
        if (sections != null) {
            for (int i = 0; i < sections.length; i++) {
                statistics.add(createStatistics(sections[i]));
            }
        }
        return (StoredDialogStatistics[]) statistics.toArray(new StoredDialogStatistics[] {});
    }

    public static Point getInitialSize(final String dialogSettingsKey, final Point initialSize) {
        return getInitialSize(dialogSettingsKey, initialSize, false);
    }

    public static Point getInitialSize(
        final String dialogSettingsKey,
        final Point initialSize,
        final boolean enforceMinimum) {
        final IDialogSettings settings = getDialogSettings(dialogSettingsKey);
        try {
            int x, y;
            x = settings.getInt(DIALOG_WIDTH);
            y = settings.getInt(DIALOG_HEIGHT);

            if (log.isTraceEnabled()) {
                log.trace("using saved initial size for [" + dialogSettingsKey + "]: " + x + "," + y); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            /*
             * If we wanted the computed initial size to always be a minimum
             * size (overriding the persisted size if the persisted size is
             * smaller), we return the maximum of persisted or initial.
             */
            if (enforceMinimum) {
                return new Point(Math.max(x, initialSize.x), Math.max(y, initialSize.y));
            } else {
                return new Point(x, y);
            }
        } catch (final NumberFormatException e) {
        }

        if (log.isTraceEnabled()) {
            log.trace("no saved initial size for [" //$NON-NLS-1$
                + dialogSettingsKey
                + "] - using default initial size of: " //$NON-NLS-1$
                + initialSize.x
                + "," //$NON-NLS-1$
                + initialSize.y);
        }

        return initialSize;
    }

    public static Point getInitialLocation(final String dialogSettingsKey, final Point initialLocation) {
        final IDialogSettings settings = getDialogSettings(dialogSettingsKey);
        try {
            final int x = settings.getInt(DIALOG_ORIGIN_X);
            final int y = settings.getInt(DIALOG_ORIGIN_Y);
            return new Point(x, y);
        } catch (final NumberFormatException e) {
        }
        return initialLocation;
    }
}
