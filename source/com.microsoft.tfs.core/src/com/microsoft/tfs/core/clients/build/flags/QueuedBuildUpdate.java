// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._QueuedBuildUpdate;
import ms.tfs.build.buildservice._04._QueuedBuildUpdate._QueuedBuildUpdate_Flag;

@SuppressWarnings("serial")
public class QueuedBuildUpdate extends BitField {
    public static QueuedBuildUpdate NONE = new QueuedBuildUpdate(0, _QueuedBuildUpdate_Flag.None);
    public static QueuedBuildUpdate PRIORITY = new QueuedBuildUpdate(1, _QueuedBuildUpdate_Flag.Priority);
    public static QueuedBuildUpdate POSTPONED = new QueuedBuildUpdate(2, _QueuedBuildUpdate_Flag.Postponed);
    public static QueuedBuildUpdate RETRY = new QueuedBuildUpdate(4, _QueuedBuildUpdate_Flag.Retry);
    public static QueuedBuildUpdate BATCHID = new QueuedBuildUpdate(8, _QueuedBuildUpdate_Flag.BatchId);
    public static QueuedBuildUpdate REQUEUE = new QueuedBuildUpdate(16, _QueuedBuildUpdate_Flag.Requeue);

    private QueuedBuildUpdate(final int flags, final _QueuedBuildUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueuedBuildUpdate(final int flags) {
        super(flags);
    }

    public _QueuedBuildUpdate getWebServiceObject() {
        return new _QueuedBuildUpdate(toFullStringValues());
    }

    public static QueuedBuildUpdate fromWebServiceObject(final _QueuedBuildUpdate value) {
        if (value == null) {
            return null;
        }
        return new QueuedBuildUpdate(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _QueuedBuildUpdate value) {
        final _QueuedBuildUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueuedBuildUpdate.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueuedBuildUpdate combine(final QueuedBuildUpdate[] buildReason) {
        return new QueuedBuildUpdate(BitField.combine(buildReason));
    }

    public boolean containsAll(final QueuedBuildUpdate other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueuedBuildUpdate other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueuedBuildUpdate other) {
        return containsAnyInternal(other);
    }

    public QueuedBuildUpdate remove(final QueuedBuildUpdate other) {
        return new QueuedBuildUpdate(removeInternal(other));
    }

    public QueuedBuildUpdate retain(final QueuedBuildUpdate other) {
        return new QueuedBuildUpdate(retainInternal(other));
    }

    public QueuedBuildUpdate combine(final QueuedBuildUpdate other) {
        return new QueuedBuildUpdate(combineInternal(other));
    }
}
