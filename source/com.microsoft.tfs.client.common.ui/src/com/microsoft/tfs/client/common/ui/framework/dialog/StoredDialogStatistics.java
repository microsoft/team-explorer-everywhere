// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.dialog;

import java.util.Date;

import org.eclipse.swt.graphics.Point;

public class StoredDialogStatistics implements Comparable {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private final String settingsKey;
    private final Point origin;
    private final Point size;
    private final Date since;
    private final int views;
    private final long accumulatedOpenTimeMs;
    private final int timeCounts;
    private final long averageOpenTimeMs;

    public StoredDialogStatistics(
        final String settingsKey,
        final Point origin,
        final Point size,
        final Date since,
        final int views,
        final long accumulatedOpenTimeMs,
        final int timeCounts,
        final long averageOpenTimeMs) {
        this.settingsKey = settingsKey;
        this.origin = origin;
        this.size = size;
        this.since = since;
        this.views = views;
        this.accumulatedOpenTimeMs = accumulatedOpenTimeMs;
        this.timeCounts = timeCounts;
        this.averageOpenTimeMs = averageOpenTimeMs;
    }

    @Override
    public int compareTo(final Object o) {
        final StoredDialogStatistics other = (StoredDialogStatistics) o;
        return settingsKey.compareTo(other.settingsKey);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        sb.append("key: " + settingsKey); //$NON-NLS-1$
        sb.append(", " + views + (views == 1 ? " view" : " views")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        sb.append(" since: " + (since == null ? "(not available)" : since.toString())).append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("origin: " + (origin == null ? "(not available)" : "(" + origin.x + "," + origin.y + ")")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        sb.append(", size: " + (size == null ? "(not available)" : "(" + size.x + "," + size.y + ")")).append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        sb.append("average open time: " + averageOpenTimeMs); //$NON-NLS-1$
        sb.append(" (accumulated: " + accumulatedOpenTimeMs); //$NON-NLS-1$
        sb.append(", timeCounts: " + timeCounts + ")").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$

        return sb.toString();
    }

    public long getAccumulatedOpenTimeMs() {
        return accumulatedOpenTimeMs;
    }

    public long getAverageOpenTimeMs() {
        return averageOpenTimeMs;
    }

    public Point getOrigin() {
        return origin;
    }

    public String getSettingsKey() {
        return settingsKey;
    }

    public Date getSince() {
        return since;
    }

    public Point getSize() {
        return size;
    }

    public int getTimeCounts() {
        return timeCounts;
    }

    public int getViews() {
        return views;
    }
}
