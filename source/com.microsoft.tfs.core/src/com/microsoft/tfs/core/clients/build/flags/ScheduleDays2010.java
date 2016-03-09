// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.FlagSetWrapper;

import ms.tfs.build.buildservice._03._ScheduleDays;
import ms.tfs.build.buildservice._03._ScheduleDays._ScheduleDays_Flag;

/**
 * Describes the schedule days.
 *
 * @since TEE-SDK-10.1
 */
public final class ScheduleDays2010 extends FlagSetWrapper {
    /**
     * Wraps the web service flag type as a {@link ScheduleDays2010.Day}.
     */
    public final static class Day extends FlagWrapper {
        private Day(final _ScheduleDays_Flag flag) {
            super(flag);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj);
        }
    }

    /*
     * "None" is a special value, and is not public. Simply create or make this
     * object empty.
     */

    public static final Day MONDAY = new Day(_ScheduleDays_Flag.Monday);
    public static final Day TUESDAY = new Day(_ScheduleDays_Flag.Tuesday);
    public static final Day WEDNESDAY = new Day(_ScheduleDays_Flag.Wednesday);
    public static final Day THURSDAY = new Day(_ScheduleDays_Flag.Thursday);
    public static final Day FRIDAY = new Day(_ScheduleDays_Flag.Friday);
    public static final Day SATURDAY = new Day(_ScheduleDays_Flag.Saturday);
    public static final Day SUNDAY = new Day(_ScheduleDays_Flag.Sunday);

    /*
     * "All" is a special value that is equivalent to all the known values.
     */
    public static final Day[] ALL = new Day[] {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    };

    /**
     * Constructs a {@link ScheduleDays2010} with no days initially set.
     */
    public ScheduleDays2010() {
        super(new _ScheduleDays());
    }

    /**
     * Constructs a {@link ScheduleDays2010} that wraps the given
     * {@link _ScheduleDays}.
     *
     * @param scheduleDays
     *        the inner {@link _ScheduleDays} (must not be <code>null</code>)
     */
    public ScheduleDays2010(final _ScheduleDays scheduleDays) {
        super(scheduleDays);

        /*
         * Convert the special states into only flags and combinations that are
         * publicaly accessible from this class.
         */
        if (scheduleDays.contains(_ScheduleDays_Flag.None)) {
            scheduleDays.clear();
        } else if (scheduleDays.contains(_ScheduleDays_Flag.All)) {
            scheduleDays.clear();
            scheduleDays.add(_ScheduleDays_Flag.Monday);
            scheduleDays.add(_ScheduleDays_Flag.Tuesday);
            scheduleDays.add(_ScheduleDays_Flag.Wednesday);
            scheduleDays.add(_ScheduleDays_Flag.Thursday);
            scheduleDays.add(_ScheduleDays_Flag.Friday);
            scheduleDays.add(_ScheduleDays_Flag.Saturday);
            scheduleDays.add(_ScheduleDays_Flag.Sunday);
        }
    }

    /**
     * Constructs a {@link ScheduleDays2010} with the given single day initially
     * set.
     */
    public ScheduleDays2010(final Day value) {
        this();
        add(value);
    }

    /**
     * Constructs a {@link ScheduleDays2010} with the given days initially set.
     */
    public ScheduleDays2010(final Day[] values) {
        this();
        addAll(values);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ScheduleDays getWebServiceObject() {
        /*
         * Handle the web service object's special states. Return new web
         * service objects if needed.
         *
         * Watch out for calls to this method from elsewhere in this class (you
         * might get an object that isn't a valid wrapped web service object for
         * this class!).
         */

        /*
         * Empty means None.
         */
        if (super.size() == 0) {
            return new _ScheduleDays(new _ScheduleDays_Flag[] {
                _ScheduleDays_Flag.None
            });
        }

        /*
         * We never store None or All in our web service object, so a full count
         * means all the normal types.
         */
        if (super.size() == 7) {
            return new _ScheduleDays(new _ScheduleDays_Flag[] {
                _ScheduleDays_Flag.All,
            });
        }

        return (_ScheduleDays) webServiceObject;
    }

    /*
     * Overrides.
     */

    public void add(final Day flagWrapper) {
        super.add(flagWrapper);
    }

    public void addAll(final ScheduleDays2010 flagSetWrapper) {
        super.addAll(flagSetWrapper);
    }

    public void addAll(final Day[] flagWrappers) {
        super.addAll(flagWrappers);
    }

    public boolean contains(final Day flagWrapper) {
        return super.contains(flagWrapper);
    }

    public boolean containsAll(final Day[] flagWrappers) {
        return super.containsAll(flagWrappers);
    }

    public boolean containsAny(final Day[] flagWrappers) {
        return super.containsAny(flagWrappers);
    }

    public boolean containsAll(final ScheduleDays2010 flagSetWrapper) {
        return super.containsAll(flagSetWrapper);
    }

    public boolean containsAny(final ScheduleDays2010 flagSetWrapper) {
        return super.containsAny(flagSetWrapper);
    }

    public boolean containsOnly(final Day flagWrapper) {
        return super.containsOnly(flagWrapper);
    }

    public boolean remove(final Day flagWrapper) {
        return super.remove(flagWrapper);
    }

    public boolean removeAll(final Day[] flagWrappers) {
        return super.removeAll(flagWrappers);
    }

    public boolean removeAll(final ScheduleDays2010 flagSetWrapper) {
        return super.removeAll(flagSetWrapper);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }
}
