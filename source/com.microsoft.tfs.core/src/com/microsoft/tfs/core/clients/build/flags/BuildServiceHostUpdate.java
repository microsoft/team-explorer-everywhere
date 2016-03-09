// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._BuildServiceHostUpdate;
import ms.tfs.build.buildservice._04._BuildServiceHostUpdate._BuildServiceHostUpdate_Flag;

@SuppressWarnings("serial")
public class BuildServiceHostUpdate extends BitField {
    public static final BuildServiceHostUpdate NONE = new BuildServiceHostUpdate(0, _BuildServiceHostUpdate_Flag.None);
    public static final BuildServiceHostUpdate NAME = new BuildServiceHostUpdate(1, _BuildServiceHostUpdate_Flag.Name);
    public static final BuildServiceHostUpdate BASE_URI =
        new BuildServiceHostUpdate(2, _BuildServiceHostUpdate_Flag.BaseUrl);
    public static final BuildServiceHostUpdate REQUIRE_CLIENT_CERTIFICATE =
        new BuildServiceHostUpdate(4, _BuildServiceHostUpdate_Flag.RequireClientCertificates);

    private BuildServiceHostUpdate(final int flags, final _BuildServiceHostUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildServiceHostUpdate(final int flags) {
        super(flags);
    }

    public _BuildServiceHostUpdate getWebServiceObject() {
        return new _BuildServiceHostUpdate(toFullStringValues());
    }

    public static BuildServiceHostUpdate fromWebServiceObject(final _BuildServiceHostUpdate value) {
        return new BuildServiceHostUpdate(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _BuildServiceHostUpdate value) {
        final _BuildServiceHostUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildServiceHostUpdate.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildServiceHostUpdate combine(final BuildServiceHostUpdate[] value) {
        return new BuildServiceHostUpdate(BitField.combine(value));
    }

    public boolean containsAll(final BuildServiceHostUpdate other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildServiceHostUpdate other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildServiceHostUpdate other) {
        return containsAnyInternal(other);
    }

    public BuildServiceHostUpdate remove(final BuildServiceHostUpdate other) {
        return new BuildServiceHostUpdate(removeInternal(other));
    }

    public BuildServiceHostUpdate retain(final BuildServiceHostUpdate other) {
        return new BuildServiceHostUpdate(retainInternal(other));
    }

    public BuildServiceHostUpdate combine(final BuildServiceHostUpdate other) {
        return new BuildServiceHostUpdate(combineInternal(other));
    }

}
