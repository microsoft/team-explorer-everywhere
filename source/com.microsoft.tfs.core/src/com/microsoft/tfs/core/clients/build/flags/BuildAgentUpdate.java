// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._BuildAgentUpdate;
import ms.tfs.build.buildservice._04._BuildAgentUpdate._BuildAgentUpdate_Flag;

@SuppressWarnings("serial")
public class BuildAgentUpdate extends BitField {
    public static final BuildAgentUpdate NONE = new BuildAgentUpdate(0, _BuildAgentUpdate_Flag.None);
    public static final BuildAgentUpdate NAME = new BuildAgentUpdate(1, _BuildAgentUpdate_Flag.Name);
    public static final BuildAgentUpdate DESCRIPTION = new BuildAgentUpdate(2, _BuildAgentUpdate_Flag.Description);
    public static final BuildAgentUpdate CONTROLLER_URI = new BuildAgentUpdate(4, _BuildAgentUpdate_Flag.ControllerUri);
    public static final BuildAgentUpdate BUILD_DIRECTORY =
        new BuildAgentUpdate(8, _BuildAgentUpdate_Flag.BuildDirectory);
    public static final BuildAgentUpdate STATUS = new BuildAgentUpdate(16, _BuildAgentUpdate_Flag.Status);
    public static final BuildAgentUpdate STATUS_MESSAGE =
        new BuildAgentUpdate(32, _BuildAgentUpdate_Flag.StatusMessage);
    public static final BuildAgentUpdate TAGS = new BuildAgentUpdate(64, _BuildAgentUpdate_Flag.Tags);
    public static final BuildAgentUpdate ENABLED = new BuildAgentUpdate(128, _BuildAgentUpdate_Flag.Enabled);
    public static final BuildAgentUpdate ATTACHED_PROPERTIES =
        new BuildAgentUpdate(128, _BuildAgentUpdate_Flag.AttachedProperties);

    private BuildAgentUpdate(final int flags, final _BuildAgentUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildAgentUpdate(final int flags) {
        super(flags);
    }

    public _BuildAgentUpdate getWebServiceObject() {
        return new _BuildAgentUpdate(toFullStringValues());
    }

    public static BuildAgentUpdate fromWebServiceObject(final _BuildAgentUpdate value) {
        return new BuildAgentUpdate(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _BuildAgentUpdate value) {
        final _BuildAgentUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildAgentUpdate.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildAgentUpdate combine(final BuildAgentUpdate[] value) {
        return new BuildAgentUpdate(BitField.combine(value));
    }

    public boolean containsAll(final BuildAgentUpdate other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildAgentUpdate other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildAgentUpdate other) {
        return containsAnyInternal(other);
    }

    public BuildAgentUpdate remove(final BuildAgentUpdate other) {
        return new BuildAgentUpdate(removeInternal(other));
    }

    public BuildAgentUpdate retain(final BuildAgentUpdate other) {
        return new BuildAgentUpdate(retainInternal(other));
    }

    public BuildAgentUpdate combine(final BuildAgentUpdate other) {
        return new BuildAgentUpdate(combineInternal(other));
    }
}
