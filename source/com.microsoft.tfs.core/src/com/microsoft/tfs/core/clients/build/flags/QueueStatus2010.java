// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._QueueStatus;
import ms.tfs.build.buildservice._03._QueueStatus._QueueStatus_Flag;

/**
 * Describes the status of the queue item.
 *
 * @since TEE-SDK-10.1
 */
public final class QueueStatus2010 extends BitField implements Comparable {
    public static final QueueStatus2010 NONE = new QueueStatus2010(0, _QueueStatus_Flag.None);
    public static final QueueStatus2010 IN_PROGRESS = new QueueStatus2010(1, _QueueStatus_Flag.InProgress);
    public static final QueueStatus2010 QUEUED = new QueueStatus2010(2, _QueueStatus_Flag.Queued);
    public static final QueueStatus2010 POSTPONED = new QueueStatus2010(4, _QueueStatus_Flag.Postponed);
    public static final QueueStatus2010 COMPLETED = new QueueStatus2010(8, _QueueStatus_Flag.Completed);
    public static final QueueStatus2010 CANCELED = new QueueStatus2010(16, _QueueStatus_Flag.Canceled);
    public static final QueueStatus2010 ALL = new QueueStatus2010(31, _QueueStatus_Flag.All);

    private QueueStatus2010(final int flags, final _QueueStatus_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueueStatus2010(final int flags) {
        super(flags);
    }

    public _QueueStatus getWebServiceObject() {
        return new _QueueStatus(toFullStringValues());
    }

    public static QueueStatus2010 fromWebServiceObject(final _QueueStatus queueStatus) {
        if (queueStatus == null) {
            return QueueStatus2010.NONE;
        }

        return new QueueStatus2010(webServiceObjectToFlags(queueStatus));
    }

    private static int webServiceObjectToFlags(final _QueueStatus queueStatus) {
        final _QueueStatus_Flag[] flagArray = queueStatus.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueueStatus2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueueStatus2010 combine(final QueueStatus2010[] queueStatus) {
        return new QueueStatus2010(BitField.combine(queueStatus));
    }

    public boolean containsAll(final QueueStatus2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueueStatus2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueueStatus2010 other) {
        return containsAnyInternal(other);
    }

    public QueueStatus2010 remove(final QueueStatus2010 other) {
        return new QueueStatus2010(removeInternal(other));
    }

    public QueueStatus2010 retain(final QueueStatus2010 other) {
        return new QueueStatus2010(retainInternal(other));
    }

    public QueueStatus2010 combine(final QueueStatus2010 other) {
        return new QueueStatus2010(combineInternal(other));
    }

    /*
     *
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Object o) {
        final int thisSortPos = getSortPosition(this);
        final int otherSortPos = getSortPosition((QueueStatus2010) o);
        return thisSortPos - otherSortPos;
    }

    private int getSortPosition(final QueueStatus2010 status) {
        // Sort ordering determined from
        // Microsoft.TeamFoundation.Build.Controls.ImageHelper.GetImageIndex(QueueStatus)
        if (status.contains(QueueStatus2010.QUEUED)) {
            return 0;
        }
        if (status.contains(QueueStatus2010.IN_PROGRESS)) {
            return 2;
        }
        if (status.contains(QueueStatus2010.POSTPONED)) {
            return 1;
        }
        if (status.contains(QueueStatus2010.COMPLETED)) {
            return 3;
        }
        if (status.contains(QueueStatus2010.CANCELED)) {
            return 4;
        }
        return 0;
    }
}
