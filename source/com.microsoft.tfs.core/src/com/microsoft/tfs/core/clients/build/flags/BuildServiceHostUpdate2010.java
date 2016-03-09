// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._BuildServiceHostUpdate;
import ms.tfs.build.buildservice._03._BuildServiceHostUpdate._BuildServiceHostUpdate_Flag;

public class BuildServiceHostUpdate2010 extends BitField {
    public static final BuildServiceHostUpdate2010 NONE =
        new BuildServiceHostUpdate2010(0, _BuildServiceHostUpdate_Flag.None);
    public static final BuildServiceHostUpdate2010 NAME =
        new BuildServiceHostUpdate2010(1, _BuildServiceHostUpdate_Flag.Name);
    public static final BuildServiceHostUpdate2010 BASE_URI =
        new BuildServiceHostUpdate2010(2, _BuildServiceHostUpdate_Flag.BaseUrl);
    public static final BuildServiceHostUpdate2010 REQUIRE_CLIENT_CERTIFICATE =
        new BuildServiceHostUpdate2010(4, _BuildServiceHostUpdate_Flag.RequireClientCertificates);

    private BuildServiceHostUpdate2010(final int flags, final _BuildServiceHostUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildServiceHostUpdate2010(final int flags) {
        super(flags);
    }

    public _BuildServiceHostUpdate getWebServiceObject() {
        return new _BuildServiceHostUpdate(toFullStringValues());
    }

    public static BuildServiceHostUpdate2010 fromWebServiceObject(final _BuildServiceHostUpdate value) {
        return new BuildServiceHostUpdate2010(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _BuildServiceHostUpdate value) {
        final _BuildServiceHostUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildServiceHostUpdate2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildServiceHostUpdate2010 combine(final BuildServiceHostUpdate2010[] value) {
        return new BuildServiceHostUpdate2010(BitField.combine(value));
    }

    public boolean containsAll(final BuildServiceHostUpdate2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildServiceHostUpdate2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildServiceHostUpdate2010 other) {
        return containsAnyInternal(other);
    }

    public BuildServiceHostUpdate2010 remove(final BuildServiceHostUpdate2010 other) {
        return new BuildServiceHostUpdate2010(removeInternal(other));
    }

    public BuildServiceHostUpdate2010 retain(final BuildServiceHostUpdate2010 other) {
        return new BuildServiceHostUpdate2010(retainInternal(other));
    }

    public BuildServiceHostUpdate2010 combine(final BuildServiceHostUpdate2010 other) {
        return new BuildServiceHostUpdate2010(combineInternal(other));
    }

}
