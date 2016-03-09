// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.enums;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;

/**
 * Typsesafe enum representing the valid date filters that may be applied to a
 * build query.
 */
public class DateFilter {

    public static final DateFilter TODAY = new DateFilter(Messages.getString("DateFilter.Today"), 0); //$NON-NLS-1$
    public static final DateFilter LAST_24_HOURS = new DateFilter(Messages.getString("DateFilter.Last24Hours"), 1); //$NON-NLS-1$
    public static final DateFilter LAST_48_HOURS = new DateFilter(Messages.getString("DateFilter.Last48Hours"), 2); //$NON-NLS-1$
    public static final DateFilter LAST_7_DAYS = new DateFilter(Messages.getString("DateFilter.Last7Days"), 7); //$NON-NLS-1$
    public static final DateFilter LAST_14_DAYS = new DateFilter(Messages.getString("DateFilter.Last14Days"), 14); //$NON-NLS-1$
    public static final DateFilter LAST_28_DAYS = new DateFilter(Messages.getString("DateFilter.Last28Days"), 28); //$NON-NLS-1$
    public static final DateFilter ALL = new DateFilter(Messages.getString("DateFilter.All"), -1); //$NON-NLS-1$

    private final String displayText;
    private final int daysAgo;

    private DateFilter(final String displayText, final int daysAgo) {
        this.displayText = displayText;
        this.daysAgo = daysAgo;
    }

    /**
     * @return the daysAgo
     */
    public int getDaysAgo() {
        return daysAgo;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return displayText.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DateFilter)) {
            return false;
        }
        final DateFilter other = (DateFilter) obj;
        if (displayText == null) {
            if (other.displayText != null) {
                return false;
            }
        } else if (!displayText.equals(other.displayText)) {
            return false;
        }
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return displayText;
    }

    public static DateFilter fromDisplayText(final String displayText) {
        if (DateFilter.TODAY.toString().equals(displayText)) {
            return DateFilter.TODAY;
        }
        if (DateFilter.LAST_24_HOURS.toString().equals(displayText)) {
            return DateFilter.LAST_24_HOURS;
        }
        if (DateFilter.LAST_48_HOURS.toString().equals(displayText)) {
            return DateFilter.LAST_48_HOURS;
        }
        if (DateFilter.LAST_7_DAYS.toString().equals(displayText)) {
            return DateFilter.LAST_7_DAYS;
        }
        if (DateFilter.LAST_14_DAYS.toString().equals(displayText)) {
            return DateFilter.LAST_14_DAYS;
        }
        if (DateFilter.LAST_28_DAYS.toString().equals(displayText)) {
            return DateFilter.LAST_28_DAYS;
        }
        return DateFilter.ALL;
    }

}
