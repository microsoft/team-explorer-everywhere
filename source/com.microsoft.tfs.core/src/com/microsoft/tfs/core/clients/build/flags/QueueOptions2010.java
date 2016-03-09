// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._QueueOptions;
import ms.tfs.build.buildservice._03._QueueOptions._QueueOptions_Flag;

/**
 * Describes options for a queue.
 *
 * @since TEE-SDK-10.1
 */
public final class QueueOptions2010 extends BitField {
    public static final QueueOptions2010 NONE = new QueueOptions2010(0, _QueueOptions_Flag.None);
    public static final QueueOptions2010 PREVIEW = new QueueOptions2010(1, _QueueOptions_Flag.Preview);

    private QueueOptions2010(final int flags, final _QueueOptions_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueueOptions2010(final int flags) {
        super(flags);
    }

    public _QueueOptions getWebServiceObject() {
        return new _QueueOptions(toFullStringValues());
    }

    public static QueueOptions2010 fromWebServiceObject(final _QueueOptions queueOptions) {
        if (queueOptions == null) {
            return null;
        }
        return new QueueOptions2010(webServiceObjectToFlags(queueOptions));
    }

    private static int webServiceObjectToFlags(final _QueueOptions queueOptions) {
        final _QueueOptions_Flag[] flagArray = queueOptions.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueueOptions2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueueOptions2010 combine(final QueueOptions2010[] queueOptions) {
        return new QueueOptions2010(BitField.combine(queueOptions));
    }

    public boolean containsAll(final QueueOptions2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueueOptions2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueueOptions2010 other) {
        return containsAnyInternal(other);
    }

    public QueueOptions2010 remove(final QueueOptions2010 other) {
        return new QueueOptions2010(removeInternal(other));
    }

    public QueueOptions2010 retain(final QueueOptions2010 other) {
        return new QueueOptions2010(retainInternal(other));
    }

    public QueueOptions2010 combine(final QueueOptions2010 other) {
        return new QueueOptions2010(combineInternal(other));
    }
}
