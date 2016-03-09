// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._QueueStatus;
import ms.tfs.build.buildservice._04._QueueStatus._QueueStatus_Flag;

@SuppressWarnings("serial")
public final class QueueStatus extends BitField implements Comparable<QueueStatus> {
    public static final QueueStatus NONE = new QueueStatus(0, _QueueStatus_Flag.None);
    public static final QueueStatus IN_PROGRESS = new QueueStatus(1, _QueueStatus_Flag.InProgress);
    public static final QueueStatus RETRY = new QueueStatus(2, _QueueStatus_Flag.Retry);
    public static final QueueStatus QUEUED = new QueueStatus(4, _QueueStatus_Flag.Queued);
    public static final QueueStatus POSTPONED = new QueueStatus(8, _QueueStatus_Flag.Postponed);
    public static final QueueStatus COMPLETED = new QueueStatus(16, _QueueStatus_Flag.Completed);
    public static final QueueStatus CANCELED = new QueueStatus(32, _QueueStatus_Flag.Canceled);
    public static final QueueStatus ALL = new QueueStatus(63, _QueueStatus_Flag.All);

    private QueueStatus(final int flags, final _QueueStatus_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueueStatus(final int flags) {
        super(flags);
    }

    public _QueueStatus getWebServiceObject() {
        return new _QueueStatus(toFullStringValues());
    }

    public static QueueStatus fromWebServiceObject(final _QueueStatus queueStatus) {
        if (queueStatus == null) {
            return QueueStatus.NONE;
        }

        return new QueueStatus(webServiceObjectToFlags(queueStatus));
    }

    private static int webServiceObjectToFlags(final _QueueStatus queueStatus) {
        final _QueueStatus_Flag[] flagArray = queueStatus.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueueStatus.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueueStatus combine(final QueueStatus[] queueStatus) {
        return new QueueStatus(BitField.combine(queueStatus));
    }

    public boolean containsAll(final QueueStatus other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueueStatus other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueueStatus other) {
        return containsAnyInternal(other);
    }

    public QueueStatus remove(final QueueStatus other) {
        return new QueueStatus(removeInternal(other));
    }

    public QueueStatus retain(final QueueStatus other) {
        return new QueueStatus(retainInternal(other));
    }

    public QueueStatus combine(final QueueStatus other) {
        return new QueueStatus(combineInternal(other));
    }

    @Override
    public int compareTo(final QueueStatus other) {
        final int thisSortPos = getSortPosition(this);
        final int otherSortPos = getSortPosition(other);
        return thisSortPos - otherSortPos;
    }

    private int getSortPosition(final QueueStatus status) {
        // Sort ordering determined from
        // Microsoft.TeamFoundation.Build.Controls.ImageHelper.GetImageIndex(QueueStatus)
        if (status.contains(QueueStatus.QUEUED)) {
            return 0;
        }
        if (status.contains(QueueStatus.IN_PROGRESS)) {
            return 2;
        }
        if (status.contains(QueueStatus.POSTPONED)) {
            return 1;
        }
        if (status.contains(QueueStatus.COMPLETED)) {
            return 3;
        }
        if (status.contains(QueueStatus.CANCELED)) {
            return 4;
        }
        return 0;
    }
}
