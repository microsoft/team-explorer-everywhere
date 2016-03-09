// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._BuildAgentUpdate;
import ms.tfs.build.buildservice._03._BuildAgentUpdate._BuildAgentUpdate_Flag;

public class BuildAgentUpdate2010 extends BitField {
    public static final BuildAgentUpdate2010 NONE = new BuildAgentUpdate2010(0, _BuildAgentUpdate_Flag.None);
    public static final BuildAgentUpdate2010 NAME = new BuildAgentUpdate2010(1, _BuildAgentUpdate_Flag.Name);
    public static final BuildAgentUpdate2010 DESCRIPTION =
        new BuildAgentUpdate2010(2, _BuildAgentUpdate_Flag.Description);
    public static final BuildAgentUpdate2010 CONTROLLER_URI =
        new BuildAgentUpdate2010(4, _BuildAgentUpdate_Flag.ControllerUri);
    public static final BuildAgentUpdate2010 BUILD_DIRECTORY =
        new BuildAgentUpdate2010(8, _BuildAgentUpdate_Flag.BuildDirectory);
    public static final BuildAgentUpdate2010 STATUS = new BuildAgentUpdate2010(16, _BuildAgentUpdate_Flag.Status);
    public static final BuildAgentUpdate2010 STATUS_MESSAGE =
        new BuildAgentUpdate2010(32, _BuildAgentUpdate_Flag.StatusMessage);
    public static final BuildAgentUpdate2010 TAGS = new BuildAgentUpdate2010(64, _BuildAgentUpdate_Flag.Tags);
    public static final BuildAgentUpdate2010 ENABLED = new BuildAgentUpdate2010(128, _BuildAgentUpdate_Flag.Enabled);

    private BuildAgentUpdate2010(final int flags, final _BuildAgentUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildAgentUpdate2010(final int flags) {
        super(flags);
    }

    public _BuildAgentUpdate getWebServiceObject() {
        return new _BuildAgentUpdate(toFullStringValues());
    }

    public static BuildAgentUpdate2010 fromWebServiceObject(final _BuildAgentUpdate value) {
        return new BuildAgentUpdate2010(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _BuildAgentUpdate value) {
        final _BuildAgentUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildAgentUpdate2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildAgentUpdate2010 combine(final BuildAgentUpdate2010[] value) {
        return new BuildAgentUpdate2010(BitField.combine(value));
    }

    public boolean containsAll(final BuildAgentUpdate2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildAgentUpdate2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildAgentUpdate2010 other) {
        return containsAnyInternal(other);
    }

    public BuildAgentUpdate2010 remove(final BuildAgentUpdate2010 other) {
        return new BuildAgentUpdate2010(removeInternal(other));
    }

    public BuildAgentUpdate2010 retain(final BuildAgentUpdate2010 other) {
        return new BuildAgentUpdate2010(retainInternal(other));
    }

    public BuildAgentUpdate2010 combine(final BuildAgentUpdate2010 other) {
        return new BuildAgentUpdate2010(combineInternal(other));
    }
}
