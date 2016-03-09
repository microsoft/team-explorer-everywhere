// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._QueueOptions;
import ms.tfs.build.buildservice._04._QueueOptions._QueueOptions_Flag;

/**
 * Describes options for a queue.
 *
 * @since TEE-SDK-10.1
 */
@SuppressWarnings("serial")
public final class QueueOptions extends BitField {
    public static final QueueOptions NONE = new QueueOptions(0, _QueueOptions_Flag.None);
    public static final QueueOptions PREVIEW = new QueueOptions(1, _QueueOptions_Flag.Preview);

    private QueueOptions(final int flags, final _QueueOptions_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private QueueOptions(final int flags) {
        super(flags);
    }

    public _QueueOptions getWebServiceObject() {
        return new _QueueOptions(toFullStringValues());
    }

    public static QueueOptions fromWebServiceObject(final _QueueOptions queueOptions) {
        if (queueOptions == null) {
            return null;
        }
        return new QueueOptions(webServiceObjectToFlags(queueOptions));
    }

    private static int webServiceObjectToFlags(final _QueueOptions queueOptions) {
        final _QueueOptions_Flag[] flagArray = queueOptions.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, QueueOptions.class);
    }

    // -- Common Strongly types BitField methods.

    public static QueueOptions combine(final QueueOptions[] queueOptions) {
        return new QueueOptions(BitField.combine(queueOptions));
    }

    public boolean containsAll(final QueueOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final QueueOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final QueueOptions other) {
        return containsAnyInternal(other);
    }

    public QueueOptions remove(final QueueOptions other) {
        return new QueueOptions(removeInternal(other));
    }

    public QueueOptions retain(final QueueOptions other) {
        return new QueueOptions(retainInternal(other));
    }

    public QueueOptions combine(final QueueOptions other) {
        return new QueueOptions(combineInternal(other));
    }
}
