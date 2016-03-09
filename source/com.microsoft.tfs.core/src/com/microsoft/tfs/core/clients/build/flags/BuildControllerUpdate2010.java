// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._BuildControllerUpdate;
import ms.tfs.build.buildservice._03._BuildControllerUpdate._BuildControllerUpdate_Flag;

@SuppressWarnings("serial")
public class BuildControllerUpdate2010 extends BitField {
    public static BuildControllerUpdate2010 NONE = new BuildControllerUpdate2010(0, _BuildControllerUpdate_Flag.None);
    public static BuildControllerUpdate2010 NAME = new BuildControllerUpdate2010(1, _BuildControllerUpdate_Flag.Name);
    public static BuildControllerUpdate2010 DESCRIPTION =
        new BuildControllerUpdate2010(2, _BuildControllerUpdate_Flag.Description);
    public static BuildControllerUpdate2010 CUSTOM_ASSEMBLY_PATH =
        new BuildControllerUpdate2010(4, _BuildControllerUpdate_Flag.CustomAssemblyPath);
    public static BuildControllerUpdate2010 MAX_CONCURRENT_BUILDS =
        new BuildControllerUpdate2010(8, _BuildControllerUpdate_Flag.MaxConcurrentBuilds);
    public static BuildControllerUpdate2010 STATUS =
        new BuildControllerUpdate2010(16, _BuildControllerUpdate_Flag.Status);
    public static BuildControllerUpdate2010 STATUS_MESSAGE =
        new BuildControllerUpdate2010(32, _BuildControllerUpdate_Flag.StatusMessage);
    public static BuildControllerUpdate2010 ENABLED =
        new BuildControllerUpdate2010(64, _BuildControllerUpdate_Flag.Enabled);

    private BuildControllerUpdate2010(final int flags, final _BuildControllerUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildControllerUpdate2010(final int flags) {
        super(flags);
    }

    public _BuildControllerUpdate getWebServiceObject() {
        return new _BuildControllerUpdate(toFullStringValues());
    }

    public static BuildControllerUpdate2010 fromWebServiceObject(final _BuildControllerUpdate value) {
        return new BuildControllerUpdate2010(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _BuildControllerUpdate value) {
        final _BuildControllerUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildControllerUpdate2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildControllerUpdate2010 combine(final BuildControllerUpdate2010[] value) {
        return new BuildControllerUpdate2010(BitField.combine(value));
    }

    public boolean containsAll(final BuildControllerUpdate2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildControllerUpdate2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildControllerUpdate2010 other) {
        return containsAnyInternal(other);
    }

    public BuildControllerUpdate2010 remove(final BuildControllerUpdate2010 other) {
        return new BuildControllerUpdate2010(removeInternal(other));
    }

    public BuildControllerUpdate2010 retain(final BuildControllerUpdate2010 other) {
        return new BuildControllerUpdate2010(retainInternal(other));
    }

    public BuildControllerUpdate2010 combine(final BuildControllerUpdate2010 other) {
        return new BuildControllerUpdate2010(combineInternal(other));
    }

}
