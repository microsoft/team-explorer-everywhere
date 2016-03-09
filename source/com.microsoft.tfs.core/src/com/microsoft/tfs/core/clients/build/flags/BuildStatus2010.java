// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._03._BuildStatus;
import ms.tfs.build.buildservice._03._BuildStatus._BuildStatus_Flag;

/**
 * This enumeration represents the status of builds and build steps.
 *
 *
 * @since TEE-SDK-10.1
 */
public class BuildStatus2010 extends BitField implements Comparable {

    public static final BuildStatus2010 NONE = new BuildStatus2010(0, _BuildStatus_Flag.None);
    public static final BuildStatus2010 IN_PROGRESS = new BuildStatus2010(1, _BuildStatus_Flag.InProgress);
    public static final BuildStatus2010 SUCCEEDED = new BuildStatus2010(2, _BuildStatus_Flag.Succeeded);
    public static final BuildStatus2010 PARTIALLY_SUCCEEDED =
        new BuildStatus2010(4, _BuildStatus_Flag.PartiallySucceeded);
    public static final BuildStatus2010 FAILED = new BuildStatus2010(8, _BuildStatus_Flag.Failed);
    public static final BuildStatus2010 STOPPED = new BuildStatus2010(16, _BuildStatus_Flag.Stopped);
    public static final BuildStatus2010 NOT_STARTED = new BuildStatus2010(32, _BuildStatus_Flag.NotStarted);
    public static final BuildStatus2010 ALL = new BuildStatus2010(63, _BuildStatus_Flag.All);

    private BuildStatus2010(final int flags, final _BuildStatus_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildStatus2010(final int flags) {
        super(flags);
    }

    public _BuildStatus getWebServiceObject() {
        return new _BuildStatus(toFullStringValues());
    }

    public static BuildStatus2010 fromWebServiceObject(final _BuildStatus buildStatus) {
        if (buildStatus == null) {
            return null;
        }

        return new BuildStatus2010(webServiceObjectToFlags(buildStatus));
    }

    private static int webServiceObjectToFlags(final _BuildStatus buildStatus) {
        final _BuildStatus_Flag[] flagArray = buildStatus.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildStatus2010.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildStatus2010 combine(final BuildStatus2010[] buildStatus) {
        return new BuildStatus2010(BitField.combine(buildStatus));
    }

    public boolean containsAll(final BuildStatus2010 other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildStatus2010 other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildStatus2010 other) {
        return containsAnyInternal(other);
    }

    public BuildStatus2010 remove(final BuildStatus2010 other) {
        return new BuildStatus2010(removeInternal(other));
    }

    public BuildStatus2010 retain(final BuildStatus2010 other) {
        return new BuildStatus2010(retainInternal(other));
    }

    public BuildStatus2010 combine(final BuildStatus2010 other) {
        return new BuildStatus2010(combineInternal(other));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Object o) {
        final int thisSortPosition = getSortPosition(this);
        final int otherSortposition = getSortPosition((BuildStatus2010) o);

        return thisSortPosition - otherSortposition;
    }

    /**
     * Return the sort order of the build status in the same order used by the
     * Microsoft API.
     *
     * See: Microsoft.TeamFoundation.Build.Controls.ImageHelper.GetImageIndex(
     * BuildStatus)
     */
    private static final int getSortPosition(final BuildStatus2010 buildStatus) {
        if (buildStatus.contains(BuildStatus2010.IN_PROGRESS)) {
            return 1;
        }
        if (buildStatus.contains(BuildStatus2010.SUCCEEDED)) {
            return 2;
        }
        if (buildStatus.contains(BuildStatus2010.PARTIALLY_SUCCEEDED)) {
            return 5;
        }
        if (buildStatus.contains(BuildStatus2010.FAILED)) {
            return 3;
        }
        if (buildStatus.contains(BuildStatus2010.STOPPED)) {
            return 4;
        }
        if (buildStatus.contains(BuildStatus2010.NOT_STARTED)) {
            return 0;
        }
        return 0;
    }

}
