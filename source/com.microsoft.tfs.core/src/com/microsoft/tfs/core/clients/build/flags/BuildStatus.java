// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.util.BitField;

import ms.tfs.build.buildservice._04._BuildStatus;
import ms.tfs.build.buildservice._04._BuildStatus._BuildStatus_Flag;

/**
 * This enumeration represents the status of builds and build steps.
 *
 *
 * @since TEE-SDK-10.1
 */
public class BuildStatus extends BitField implements Comparable {

    public static final BuildStatus NONE = new BuildStatus(0, _BuildStatus_Flag.None);
    public static final BuildStatus IN_PROGRESS = new BuildStatus(1, _BuildStatus_Flag.InProgress);
    public static final BuildStatus SUCCEEDED = new BuildStatus(2, _BuildStatus_Flag.Succeeded);
    public static final BuildStatus PARTIALLY_SUCCEEDED = new BuildStatus(4, _BuildStatus_Flag.PartiallySucceeded);
    public static final BuildStatus FAILED = new BuildStatus(8, _BuildStatus_Flag.Failed);
    public static final BuildStatus STOPPED = new BuildStatus(16, _BuildStatus_Flag.Stopped);
    public static final BuildStatus NOT_STARTED = new BuildStatus(32, _BuildStatus_Flag.NotStarted);
    public static final BuildStatus ALL = new BuildStatus(63, _BuildStatus_Flag.All);

    private BuildStatus(final int flags, final _BuildStatus_Flag flag) {
        super(flags);
        registerStringValue(getClass(), flags, flag.toString());
    }

    private BuildStatus(final int flags) {
        super(flags);
    }

    public _BuildStatus getWebServiceObject() {
        return new _BuildStatus(toFullStringValues());
    }

    public static BuildStatus fromWebServiceObject(final _BuildStatus buildStatus) {
        if (buildStatus == null) {
            return null;
        }

        return new BuildStatus(webServiceObjectToFlags(buildStatus));
    }

    private static int webServiceObjectToFlags(final _BuildStatus buildStatus) {
        final _BuildStatus_Flag[] flagArray = buildStatus.getFlags();
        final String[] flagStrings = new String[flagArray.length];
        for (int i = 0; i < flagArray.length; i++) {
            flagStrings[i] = flagArray[i].toString();
        }
        return fromStringValues(flagStrings, BuildStatus.class);
    }

    // -- Common Strongly types BitField methods.

    public static BuildStatus combine(final BuildStatus[] buildStatus) {
        return new BuildStatus(BitField.combine(buildStatus));
    }

    public boolean containsAll(final BuildStatus other) {
        return containsAllInternal(other);
    }

    public boolean contains(final BuildStatus other) {
        return containsInternal(other);
    }

    public boolean containsAny(final BuildStatus other) {
        return containsAnyInternal(other);
    }

    public BuildStatus remove(final BuildStatus other) {
        return new BuildStatus(removeInternal(other));
    }

    public BuildStatus retain(final BuildStatus other) {
        return new BuildStatus(retainInternal(other));
    }

    public BuildStatus combine(final BuildStatus other) {
        return new BuildStatus(combineInternal(other));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Object o) {
        final int thisSortPosition = getSortPosition(this);
        final int otherSortposition = getSortPosition((BuildStatus) o);

        return thisSortPosition - otherSortposition;
    }

    /**
     * Return the sort order of the build status in the same order used by the
     * Microsoft API.
     *
     * See: Microsoft.TeamFoundation.Build.Controls.ImageHelper.GetImageIndex(
     * BuildStatus)
     */
    private static final int getSortPosition(final BuildStatus buildStatus) {
        if (buildStatus.contains(BuildStatus.IN_PROGRESS)) {
            return 1;
        }
        if (buildStatus.contains(BuildStatus.SUCCEEDED)) {
            return 2;
        }
        if (buildStatus.contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
            return 5;
        }
        if (buildStatus.contains(BuildStatus.FAILED)) {
            return 3;
        }
        if (buildStatus.contains(BuildStatus.STOPPED)) {
            return 4;
        }
        if (buildStatus.contains(BuildStatus.NOT_STARTED)) {
            return 0;
        }
        return 0;
    }

}
