// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.CheckinOptions;
import com.microsoft.tfs.util.BitField;

/**
 * {@link CheckinFlags} is used to control the options of a check-in operation.
 * These flags were introduced in 2010, superseding {@link CheckinOptions} in
 * this API.
 *
 * @since TEE-SDK-10.1
 */
public class CheckinFlags extends BitField {
    /**
     * No check-in flags. The default check in options will be used.
     */
    public static final CheckinFlags NONE = new CheckinFlags(0, "None"); //$NON-NLS-1$

    /**
     * Make sure that the identity specified as the check-in owner refers to a
     * valid user
     */
    public static final CheckinFlags VALIDATE_CHECK_IN_OWNER = new CheckinFlags(1, "ValidateCheckInOwner"); //$NON-NLS-1$

    /**
     * Suppress events for this check-in
     */
    public static final CheckinFlags SUPPRESS_EVENT = new CheckinFlags(2, "SuppressEvent"); //$NON-NLS-1$

    /**
     * Delete the shelveset after a successful submission into the repository
     */
    public static final CheckinFlags DELETE_SHELVESET = new CheckinFlags(4, "DeleteShelveset"); //$NON-NLS-1$

    /**
     * Bypass gated check-in validation for this check-in
     */
    public static final CheckinFlags OVERRIDE_GATED_CHECK_IN = new CheckinFlags(8, "OverrideGatedCheckIn"); //$NON-NLS-1$

    /**
     * Automatically queue the build for gated check-in validation if possible
     */
    public static final CheckinFlags QUEUE_BUILD_FOR_GATED_CHECK_IN = new CheckinFlags(16, "QueueBuildForGatedCheckIn"); //$NON-NLS-1$

    /**
     * The server will permit a call to CheckIn with a null or empty list of
     * items to check in, (check-in /all) even if there is a change with the
     * edit bit set.
     */
    public static final CheckinFlags ALL_CONTENT_UPLOADED = new CheckinFlags(32, "AllContentUploaded"); //$NON-NLS-1$

    /**
     * The server will permit items which have a pending edit to be checked in
     * even if the content hasn't changed instead of undoing the change
     * (default)
     */
    public static final CheckinFlags ALLOW_UNCHANGED_CONTENT = new CheckinFlags(64, "AllowUnchangedContent"); //$NON-NLS-1$

    /**
     * The client will not attempt to automatically resolve conflicts arising
     * from the checkin. This flag is only honored on TFS2012 servers.
     */
    public static final CheckinFlags NO_AUTO_RESOLVE = new CheckinFlags(128, "NoAutoResolve"); //$NON-NLS-1$

    public static CheckinFlags combine(final CheckinFlags[] values) {
        return new CheckinFlags(BitField.combine(values));
    }

    private CheckinFlags(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private CheckinFlags(final int flags) {
        super(flags);
    }

    /**
     * Converts this {@link CheckinFlags} to the legacy (TFS 2008 and previous)
     * {@link CheckinOptions} type for use with the older web service.
     *
     * @return a {@link CheckinOptions} with the options set which correspond to
     *         supported {@link CheckinFlags} set in this object
     */
    public CheckinOptions toCheckinOptions() {
        final CheckinOptions ret = new CheckinOptions();

        if (contains(CheckinFlags.VALIDATE_CHECK_IN_OWNER)) {
            ret.add(CheckinOptions.VALIDATE_CHECKIN_OWNER);
        }

        if (contains(CheckinFlags.SUPPRESS_EVENT)) {
            ret.add(CheckinOptions.SUPPRESS_EVENT);
        }

        return ret;
    }

    public boolean containsAll(final CheckinFlags other) {
        return containsAllInternal(other);
    }

    public boolean contains(final CheckinFlags other) {
        return containsInternal(other);
    }

    public boolean containsAny(final CheckinFlags other) {
        return containsAnyInternal(other);
    }

    public CheckinFlags remove(final CheckinFlags other) {
        return new CheckinFlags(removeInternal(other));
    }

    public CheckinFlags retain(final CheckinFlags other) {
        return new CheckinFlags(retainInternal(other));
    }

    public CheckinFlags combine(final CheckinFlags other) {
        return new CheckinFlags(combineInternal(other));
    }

}
