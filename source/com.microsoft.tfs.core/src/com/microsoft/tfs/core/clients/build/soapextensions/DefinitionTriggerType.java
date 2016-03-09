// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._DefinitionTriggerType;
import ms.tfs.build.buildservice._04._DefinitionTriggerType._DefinitionTriggerType_Flag;

public class DefinitionTriggerType extends BitField {
    private static final long serialVersionUID = 5016152375999783846L;

    public static final DefinitionTriggerType NONE =
        new DefinitionTriggerType(0, _DefinitionTriggerType_Flag.None.toString());
    public static final DefinitionTriggerType CONTINUOUS_INTEGRATION =
        new DefinitionTriggerType(2, _DefinitionTriggerType_Flag.ContinuousIntegration.toString());
    public static final DefinitionTriggerType BATCHED_CONTINUOUS_INTEGRATION =
        new DefinitionTriggerType(4, _DefinitionTriggerType_Flag.BatchedContinuousIntegration.toString());
    public static final DefinitionTriggerType SCHEDULE =
        new DefinitionTriggerType(8, _DefinitionTriggerType_Flag.Schedule.toString());
    public static final DefinitionTriggerType SCHEDULE_FORCED =
        new DefinitionTriggerType(16, _DefinitionTriggerType_Flag.ScheduleForced.toString());
    public static final DefinitionTriggerType GATED_CHECKIN =
        new DefinitionTriggerType(32, _DefinitionTriggerType_Flag.GatedCheckIn.toString());
    public static final DefinitionTriggerType BATCHED_GATED_CHECKIN =
        new DefinitionTriggerType(64, _DefinitionTriggerType_Flag.BatchedGatedCheckIn.toString());
    public static final DefinitionTriggerType ALL =
        new DefinitionTriggerType(126, _DefinitionTriggerType_Flag.All.toString());

    private DefinitionTriggerType(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    /**
     * "Special" private constructor which uses the given flags but does not
     * register any name for it. Overridden to provide the derived type for
     * {@link #combine(ContinuousIntegrationType)}, etc.
     */
    private DefinitionTriggerType(final int flags) {
        super(flags);
    }

    public DefinitionTriggerType(final _DefinitionTriggerType type) {
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
    private static int webServiceObjectToFlags(final _DefinitionTriggerType webServiceObject) {
        Check.notNull(webServiceObject, "ContinuousIntegrationType"); //$NON-NLS-1$

        /*
         * The web service type is a flag set. Get all the strings from the
         * flags contained in it, and convert those values to one integer.
         */

        final List<String> strings = new ArrayList<String>();
        final _DefinitionTriggerType_Flag[] flags = webServiceObject.getFlags();

        for (int i = 0; i < flags.length; i++) {
            strings.add(flags[i].toString());
        }

        return fromStringValues(strings.toArray(new String[strings.size()]), DefinitionTriggerType.class);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _DefinitionTriggerType getWebServiceObject() {
        return new _DefinitionTriggerType(toStringValues());
    }

    /*
     * BitField Overrides.
     */
    public static DefinitionTriggerType combine(final DefinitionTriggerType[] other) {
        return new DefinitionTriggerType(BitField.combine(other));
    }

    public boolean containsAll(final DefinitionTriggerType other) {
        return containsAllInternal(other);
    }

    public boolean contains(final DefinitionTriggerType other) {
        return containsInternal(other);
    }

    public boolean containsAny(final DefinitionTriggerType other) {
        return containsAnyInternal(other);
    }

    public DefinitionTriggerType remove(final DefinitionTriggerType other) {
        return new DefinitionTriggerType(removeInternal(other));
    }

    public DefinitionTriggerType retain(final DefinitionTriggerType other) {
        return new DefinitionTriggerType(retainInternal(other));
    }

    public DefinitionTriggerType combine(final DefinitionTriggerType other) {
        return new DefinitionTriggerType(combineInternal(other));
    }

}
