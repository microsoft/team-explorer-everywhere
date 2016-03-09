// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._03._ContinuousIntegrationType;
import ms.tfs.build.buildservice._03._ContinuousIntegrationType._ContinuousIntegrationType_Flag;

/**
 * Describes the continuous integration type.
 *
 * @see IBuildDefinition
 *
 * @since TEE-SDK-10.1
 */
public class ContinuousIntegrationType extends BitField {
    public static final ContinuousIntegrationType NONE =
        new ContinuousIntegrationType(0, _ContinuousIntegrationType_Flag.None.toString());
    public static final ContinuousIntegrationType INDIVIDUAL =
        new ContinuousIntegrationType(2, _ContinuousIntegrationType_Flag.Individual.toString());
    public static final ContinuousIntegrationType BATCH =
        new ContinuousIntegrationType(4, _ContinuousIntegrationType_Flag.Batch.toString());
    public static final ContinuousIntegrationType SCHEDULE =
        new ContinuousIntegrationType(8, _ContinuousIntegrationType_Flag.Schedule.toString());
    public static final ContinuousIntegrationType SCHEDULE_FORCED =
        new ContinuousIntegrationType(16, _ContinuousIntegrationType_Flag.ScheduleForced.toString());
    public static final ContinuousIntegrationType GATED =
        new ContinuousIntegrationType(32, _ContinuousIntegrationType_Flag.Gated.toString());
    public static final ContinuousIntegrationType ALL =
        new ContinuousIntegrationType(62, _ContinuousIntegrationType_Flag.All.toString());

    private ContinuousIntegrationType(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    /**
     * "Special" private constructor which uses the given flags but does not
     * register any name for it. Overridden to provide the derived type for
     * {@link #combine(ContinuousIntegrationType)}, etc.
     */
    private ContinuousIntegrationType(final int flags) {
        super(flags);
    }

    public ContinuousIntegrationType(final _ContinuousIntegrationType type) {
        this(webServiceObjectToFlags(type));
    }

    /**
     * Determines the correct flags value that represents the individual flag
     * types contained by the given web service object (a flag set).
     *
     * @param webServiceObject
     *        the web service object flag set (must not be <code>null</code>)
     * @return the flags value appropriate for the given flag set
     */
    private static int webServiceObjectToFlags(final _ContinuousIntegrationType webServiceObject) {
        Check.notNull(webServiceObject, "ContinuousIntegrationType"); //$NON-NLS-1$

        /*
         * The web service type is a flag set. Get all the strings from the
         * flags contained in it, and convert those values to one integer.
         */

        final List strings = new ArrayList();
        final _ContinuousIntegrationType_Flag[] flags = webServiceObject.getFlags();

        for (int i = 0; i < flags.length; i++) {
            strings.add(flags[i].toString());
        }

        return fromStringValues(
            (String[]) strings.toArray(new String[strings.size()]),
            ContinuousIntegrationType.class);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ContinuousIntegrationType getWebServiceObject() {
        return new _ContinuousIntegrationType(toStringValues());
    }

    /*
     * BitField Overrides.
     */
    public static ContinuousIntegrationType combine(final ContinuousIntegrationType[] changeTypes) {
        return new ContinuousIntegrationType(BitField.combine(changeTypes));
    }

    public boolean containsAll(final ContinuousIntegrationType other) {
        return containsAllInternal(other);
    }

    public boolean contains(final ContinuousIntegrationType other) {
        return containsInternal(other);
    }

    public boolean containsAny(final ContinuousIntegrationType other) {
        return containsAnyInternal(other);
    }

    public ContinuousIntegrationType remove(final ContinuousIntegrationType other) {
        return new ContinuousIntegrationType(removeInternal(other));
    }

    public ContinuousIntegrationType retain(final ContinuousIntegrationType other) {
        return new ContinuousIntegrationType(retainInternal(other));
    }

    public ContinuousIntegrationType combine(final ContinuousIntegrationType other) {
        return new ContinuousIntegrationType(combineInternal(other));
    }

}
