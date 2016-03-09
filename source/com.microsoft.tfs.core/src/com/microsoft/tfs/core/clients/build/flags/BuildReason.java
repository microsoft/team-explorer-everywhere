// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._BuildReason;
import ms.tfs.build.buildservice._04._BuildReason._BuildReason_Flag;

/**
 * Describes the reason for the build.
 *
 *
 * @since TEE-SDK-10.1
 */
public class BuildReason extends BitField {
    public static final BuildReason NONE = new BuildReason(0, _BuildReason_Flag.None);
    public static final BuildReason MANUAL = new BuildReason(1, _BuildReason_Flag.Manual);
    public static final BuildReason INDIVIDUAL_CI = new BuildReason(2, _BuildReason_Flag.IndividualCI);
    public static final BuildReason BATCHED_CI = new BuildReason(4, _BuildReason_Flag.BatchedCI);
    public static final BuildReason SCHEDULE = new BuildReason(8, _BuildReason_Flag.Schedule);
    public static final BuildReason SCHEDULE_FORCED = new BuildReason(18, _BuildReason_Flag.ScheduleForced);
    public static final BuildReason USER_CREATED = new BuildReason(32, _BuildReason_Flag.UserCreated);
    public static final BuildReason VALIDATE_SHELVESET = new BuildReason(64, _BuildReason_Flag.ValidateShelveset);
    public static final BuildReason CHECK_IN_SHELVESET = new BuildReason(128, _BuildReason_Flag.CheckInShelveset);
    public static final BuildReason TRIGGERED = new BuildReason(191, _BuildReason_Flag.Triggered);
    public static final BuildReason ALL = new BuildReason(255, _BuildReason_Flag.All);

    private BuildReason(final int flags, final _BuildReason_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildReason(final int flags) {
        super(flags);
    }

    public _BuildReason getWebServiceObject() {
        return new _BuildReason(toFullStringValues());
    }

    public static BuildReason fromWebServiceObject(final _BuildReason buildReason) {
        return new BuildReason(webServiceObjectToFlags(buildReason));
    }

    private static int webServiceObjectToFlags(final _BuildReason buildReason) {
        final _BuildReason_Flag[] flagArray = buildReason.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildReason.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildReason combine(final BuildReason[] buildReason) {
        return new BuildReason(BitField.combine(buildReason));
    }

    public boolean containsAll(final BuildReason other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildReason other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildReason other) {
        return containsAnyInternal(other);
    }

    public BuildReason remove(final BuildReason other) {
        return new BuildReason(removeInternal(other));
    }

    public BuildReason retain(final BuildReason other) {
        return new BuildReason(retainInternal(other));
    }

    public BuildReason combine(final BuildReason other) {
        return new BuildReason(combineInternal(other));
    }

}
