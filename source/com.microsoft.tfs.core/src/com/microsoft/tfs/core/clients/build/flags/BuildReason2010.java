// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._BuildReason;
import ms.tfs.build.buildservice._03._BuildReason._BuildReason_Flag;

/**
 * Describes the reason for the build.
 *
 *
 * @since TEE-SDK-10.1
 */
public class BuildReason2010 extends BitField {
    public static final BuildReason2010 NONE = new BuildReason2010(0, _BuildReason_Flag.None);
    public static final BuildReason2010 MANUAL = new BuildReason2010(1, _BuildReason_Flag.Manual);
    public static final BuildReason2010 INDIVIDUAL_CI = new BuildReason2010(2, _BuildReason_Flag.IndividualCI);
    public static final BuildReason2010 BATCHED_CI = new BuildReason2010(4, _BuildReason_Flag.BatchedCI);
    public static final BuildReason2010 SCHEDULE = new BuildReason2010(8, _BuildReason_Flag.Schedule);
    public static final BuildReason2010 SCHEDULE_FORCED = new BuildReason2010(18, _BuildReason_Flag.ScheduleForced);
    public static final BuildReason2010 USER_CREATED = new BuildReason2010(32, _BuildReason_Flag.UserCreated);
    public static final BuildReason2010 VALIDATE_SHELVESET =
        new BuildReason2010(64, _BuildReason_Flag.ValidateShelveset);
    public static final BuildReason2010 CHECK_IN_SHELVESET =
        new BuildReason2010(128, _BuildReason_Flag.CheckInShelveset);
    public static final BuildReason2010 TRIGGERED = new BuildReason2010(191, _BuildReason_Flag.Triggered);
    public static final BuildReason2010 ALL = new BuildReason2010(255, _BuildReason_Flag.All);

    private BuildReason2010(final int flags, final _BuildReason_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildReason2010(final int flags) {
        super(flags);
    }

    public _BuildReason getWebServiceObject() {
        return new _BuildReason(toFullStringValues());
    }

    public static BuildReason2010 fromWebServiceObject(final _BuildReason buildReason) {
        return new BuildReason2010(webServiceObjectToFlags(buildReason));
    }

    private static int webServiceObjectToFlags(final _BuildReason buildReason) {
        final _BuildReason_Flag[] flagArray = buildReason.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildReason2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildReason2010 combine(final BuildReason2010[] buildReason) {
        return new BuildReason2010(BitField.combine(buildReason));
    }

    public boolean containsAll(final BuildReason2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildReason2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildReason2010 other) {
        return containsAnyInternal(other);
    }

    public BuildReason2010 remove(final BuildReason2010 other) {
        return new BuildReason2010(removeInternal(other));
    }

    public BuildReason2010 retain(final BuildReason2010 other) {
        return new BuildReason2010(retainInternal(other));
    }

    public BuildReason2010 combine(final BuildReason2010 other) {
        return new BuildReason2010(combineInternal(other));
    }

}
