// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._QueuedBuildUpdate;
import ms.tfs.build.buildservice._03._QueuedBuildUpdate._QueuedBuildUpdate_Flag;

@SuppressWarnings("serial")
public class QueuedBuildUpdate2010 extends BitField {
    public static QueuedBuildUpdate2010 NONE = new QueuedBuildUpdate2010(0, _QueuedBuildUpdate_Flag.None);
    public static QueuedBuildUpdate2010 PRIORITY = new QueuedBuildUpdate2010(1, _QueuedBuildUpdate_Flag.Priority);
    public static QueuedBuildUpdate2010 POSTPONED = new QueuedBuildUpdate2010(2, _QueuedBuildUpdate_Flag.Postponed);

    private QueuedBuildUpdate2010(final int flags, final _QueuedBuildUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueuedBuildUpdate2010(final int flags) {
        super(flags);
    }

    public _QueuedBuildUpdate getWebServiceObject() {
        return new _QueuedBuildUpdate(toFullStringValues());
    }

    public static QueuedBuildUpdate2010 fromWebServiceObject(final _QueuedBuildUpdate value) {
        if (value == null) {
            return null;
        }
        return new QueuedBuildUpdate2010(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _QueuedBuildUpdate value) {
        final _QueuedBuildUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueuedBuildUpdate2010.class);
    }

    public static QueuedBuildUpdate2010 combine(final QueuedBuildUpdate2010[] buildReason) {
        return new QueuedBuildUpdate2010(BitField.combine(buildReason));
    }

    public boolean containsAll(final QueuedBuildUpdate2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueuedBuildUpdate2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueuedBuildUpdate2010 other) {
        return containsAnyInternal(other);
    }

    public QueuedBuildUpdate2010 remove(final QueuedBuildUpdate2010 other) {
        return new QueuedBuildUpdate2010(removeInternal(other));
    }

    public QueuedBuildUpdate2010 retain(final QueuedBuildUpdate2010 other) {
        return new QueuedBuildUpdate2010(retainInternal(other));
    }

    public QueuedBuildUpdate2010 combine(final QueuedBuildUpdate2010 other) {
        return new QueuedBuildUpdate2010(combineInternal(other));
    }
}
