// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._DeleteOptions;
import ms.tfs.build.buildservice._04._DeleteOptions._DeleteOptions_Flag;

/**
 * Options for delete.
 *
 * @since TEE-SDK-10.1
 */
public class DeleteOptions extends BitField {

    public static final DeleteOptions NONE = new DeleteOptions(0, _DeleteOptions_Flag.None);
    public static final DeleteOptions DROP_LOCATION = new DeleteOptions(1, _DeleteOptions_Flag.DropLocation);
    public static final DeleteOptions TEST_RESULTS = new DeleteOptions(2, _DeleteOptions_Flag.TestResults);
    public static final DeleteOptions LABEL = new DeleteOptions(4, _DeleteOptions_Flag.Label);
    public static final DeleteOptions DETAILS = new DeleteOptions(8, _DeleteOptions_Flag.Details);
    public static final DeleteOptions SYMBOLS = new DeleteOptions(16, _DeleteOptions_Flag.Symbols);
    public static final DeleteOptions ALL = new DeleteOptions(31, _DeleteOptions_Flag.All);

    private DeleteOptions(final int flags, final _DeleteOptions_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private DeleteOptions(final int flags) {
        super(flags);
    }

    public _DeleteOptions getWebServiceObject() {
        return new _DeleteOptions(toFullStringValues());
    }

    public static DeleteOptions fromWebServiceObject(final _DeleteOptions deleteOptions) {
        if (deleteOptions == null) {
            return null;
        }
        return new DeleteOptions(webServiceObjectToFlags(deleteOptions));
    }

    private static int webServiceObjectToFlags(final _DeleteOptions deleteOptions) {
        final _DeleteOptions_Flag[] flagArray = deleteOptions.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, DeleteOptions.class);
    }

    // -- Common Strongly types BitField methods.

    public static DeleteOptions combine(final DeleteOptions[] deleteOptions) {
        return new DeleteOptions(BitField.combine(deleteOptions));
    }

    public boolean containsAll(final DeleteOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final DeleteOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final DeleteOptions other) {
        return containsAnyInternal(other);
    }

    public DeleteOptions remove(final DeleteOptions other) {
        return new DeleteOptions(removeInternal(other));
    }

    public DeleteOptions retain(final DeleteOptions other) {
        return new DeleteOptions(retainInternal(other));
    }

    public DeleteOptions combine(final DeleteOptions other) {
        return new DeleteOptions(combineInternal(other));
    }

}
