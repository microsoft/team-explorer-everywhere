// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._DeleteOptions;
import ms.tfs.build.buildservice._03._DeleteOptions._DeleteOptions_Flag;

/**
 * Options for delete.
 *
 * @since TEE-SDK-10.1
 */
public class DeleteOptions2010 extends BitField {

    public static final DeleteOptions2010 NONE = new DeleteOptions2010(0, _DeleteOptions_Flag.None);
    public static final DeleteOptions2010 DROP_LOCATION = new DeleteOptions2010(1, _DeleteOptions_Flag.DropLocation);
    public static final DeleteOptions2010 TEST_RESULTS = new DeleteOptions2010(2, _DeleteOptions_Flag.TestResults);
    public static final DeleteOptions2010 LABEL = new DeleteOptions2010(4, _DeleteOptions_Flag.Label);
    public static final DeleteOptions2010 DETAILS = new DeleteOptions2010(8, _DeleteOptions_Flag.Details);
    public static final DeleteOptions2010 SYMBOLS = new DeleteOptions2010(16, _DeleteOptions_Flag.Symbols);
    public static final DeleteOptions2010 ALL = new DeleteOptions2010(31, _DeleteOptions_Flag.All);

    private DeleteOptions2010(final int flags, final _DeleteOptions_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private DeleteOptions2010(final int flags) {
        super(flags);
    }

    public _DeleteOptions getWebServiceObject() {
        return new _DeleteOptions(toFullStringValues());
    }

    public static DeleteOptions2010 fromWebServiceObject(final _DeleteOptions deleteOptions) {
        if (deleteOptions == null) {
            return null;
        }
        return new DeleteOptions2010(webServiceObjectToFlags(deleteOptions));
    }

    private static int webServiceObjectToFlags(final _DeleteOptions deleteOptions) {
        final _DeleteOptions_Flag[] flagArray = deleteOptions.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, DeleteOptions2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static DeleteOptions2010 combine(final DeleteOptions2010[] deleteOptions) {
        return new DeleteOptions2010(BitField.combine(deleteOptions));
    }

    public boolean containsAll(final DeleteOptions2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final DeleteOptions2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final DeleteOptions2010 other) {
        return containsAnyInternal(other);
    }

    public DeleteOptions2010 remove(final DeleteOptions2010 other) {
        return new DeleteOptions2010(removeInternal(other));
    }

    public DeleteOptions2010 retain(final DeleteOptions2010 other) {
        return new DeleteOptions2010(retainInternal(other));
    }

    public DeleteOptions2010 combine(final DeleteOptions2010 other) {
        return new DeleteOptions2010(combineInternal(other));
    }

}
