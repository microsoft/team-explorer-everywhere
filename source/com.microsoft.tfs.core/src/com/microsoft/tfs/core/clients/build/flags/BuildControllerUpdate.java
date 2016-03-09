// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._BuildControllerUpdate;
import ms.tfs.build.buildservice._04._BuildControllerUpdate._BuildControllerUpdate_Flag;

@SuppressWarnings("serial")
public class BuildControllerUpdate extends BitField {
    public static BuildControllerUpdate NONE = new BuildControllerUpdate(0, _BuildControllerUpdate_Flag.None);
    public static BuildControllerUpdate NAME = new BuildControllerUpdate(1, _BuildControllerUpdate_Flag.Name);
    public static BuildControllerUpdate DESCRIPTION =
        new BuildControllerUpdate(2, _BuildControllerUpdate_Flag.Description);
    public static BuildControllerUpdate CUSTOM_ASSEMBLY_PATH =
        new BuildControllerUpdate(4, _BuildControllerUpdate_Flag.CustomAssemblyPath);
    public static BuildControllerUpdate MAX_CONCURRENT_BUILDS =
        new BuildControllerUpdate(8, _BuildControllerUpdate_Flag.MaxConcurrentBuilds);
    public static BuildControllerUpdate STATUS = new BuildControllerUpdate(16, _BuildControllerUpdate_Flag.Status);
    public static BuildControllerUpdate STATUS_MESSAGE =
        new BuildControllerUpdate(32, _BuildControllerUpdate_Flag.StatusMessage);
    public static BuildControllerUpdate ENABLED = new BuildControllerUpdate(64, _BuildControllerUpdate_Flag.Enabled);
    public static BuildControllerUpdate ATTACHED_PROPERTIES =
        new BuildControllerUpdate(64, _BuildControllerUpdate_Flag.AttachedProperties);

    private BuildControllerUpdate(final int flags, final _BuildControllerUpdate_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildControllerUpdate(final int flags) {
        super(flags);
    }

    public _BuildControllerUpdate getWebServiceObject() {
        return new _BuildControllerUpdate(toFullStringValues());
    }

    public static BuildControllerUpdate fromWebServiceObject(final _BuildControllerUpdate value) {
        return new BuildControllerUpdate(webServiceObjectToFlags(value));
    }

    private static int webServiceObjectToFlags(final _BuildControllerUpdate value) {
        final _BuildControllerUpdate_Flag[] flagArray = value.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildControllerUpdate.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildControllerUpdate combine(final BuildControllerUpdate[] value) {
        return new BuildControllerUpdate(BitField.combine(value));
    }

    public boolean containsAll(final BuildControllerUpdate other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildControllerUpdate other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildControllerUpdate other) {
        return containsAnyInternal(other);
    }

    public BuildControllerUpdate remove(final BuildControllerUpdate other) {
        return new BuildControllerUpdate(removeInternal(other));
    }

    public BuildControllerUpdate retain(final BuildControllerUpdate other) {
        return new BuildControllerUpdate(retainInternal(other));
    }

    public BuildControllerUpdate combine(final BuildControllerUpdate other) {
        return new BuildControllerUpdate(combineInternal(other));
    }

}
