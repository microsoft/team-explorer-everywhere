// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.enums;

import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

/**
 * Typesafe enum representing the valid Queue status filters.
 */
public class QueueStatusFilter {

    public static final QueueStatusFilter ALL = new QueueStatusFilter("<Any Status>", 0, QueueStatus.ALL); //$NON-NLS-1$
    public static final QueueStatusFilter IN_PROGRESS =
        new QueueStatusFilter("In Progress", 1, QueueStatus.IN_PROGRESS); //$NON-NLS-1$
    public static final QueueStatusFilter QUEUED = new QueueStatusFilter("Queued", 2, QueueStatus.QUEUED); //$NON-NLS-1$
    public static final QueueStatusFilter POSTPONED = new QueueStatusFilter("Postponed", 3, QueueStatus.POSTPONED); //$NON-NLS-1$

    private static final QueueStatusFilter[] filters = new QueueStatusFilter[] {
        ALL,
        IN_PROGRESS,
        QUEUED,
        POSTPONED
    };

    private final String displayText;
    private final int value;
    private final QueueStatus queueStatus;

    private QueueStatusFilter(final String displayText, final int value, final QueueStatus queueStatus) {
        this.displayText = displayText;
        this.value = value;
        this.queueStatus = queueStatus;
    }

    /**
     * @return the displayText
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
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
        if (!(obj instanceof QueueStatusFilter)) {
            return false;
        }
        final QueueStatusFilter other = (QueueStatusFilter) obj;
        if (displayText == null) {
            if (other.displayText != null) {
                return false;
            }
        } else if (!displayText.equals(other.displayText)) {
            return false;
        }
        return true;
    }

    public static QueueStatusFilter[] getAllQueueStatusFilters() {
        return filters.clone();
    }

    /**
     * @return the queueStatus
     */
    public QueueStatus getQueueStatus() {
        return queueStatus;
    }

}
