// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.internal.wrappers.FlagSetWrapper;

import ms.tfs.versioncontrol.clientservices._03._CheckinOptions;
import ms.tfs.versioncontrol.clientservices._03._CheckinOptions._CheckinOptions_Flag;

/**
 * Check-in options for a TFS 2005 or TFS 2008 server. {@link CheckinFlags}
 * supersedes this type.
 */
public final class CheckinOptions extends FlagSetWrapper {
    /**
     * Wraps the web service flag type as an {@link Option}.
     */
    public final static class Option extends FlagWrapper {
        private Option(final _CheckinOptions_Flag flag) {
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

    /**
     * No options desired.
     */
    public static final Option NONE = new Option(_CheckinOptions_Flag.None);

    /**
     * TODO
     */
    public static final Option VALIDATE_CHECKIN_OWNER = new Option(_CheckinOptions_Flag.ValidateCheckinOwner);

    /**
     * TODO
     */
    public static final Option SUPPRESS_EVENT = new Option(_CheckinOptions_Flag.SuppressEvent);

    /**
     * Constructs an {@link CheckinOptions} with no types initially set.
     */
    public CheckinOptions() {
        super(new _CheckinOptions());
    }

    /**
     * Constructs an {@link CheckinOptions} that wraps the given
     * {@link _CheckinOptions}.
     *
     * @param checkinOptions
     *        the inner {@link _CheckinOptions} (must not be <code>null</code>)
     */
    public CheckinOptions(final _CheckinOptions checkinOptions) {
        super(checkinOptions);
    }

    /**
     * Constructs a {@link CheckinOptions} with the given single option
     * initially set.
     */
    public CheckinOptions(final Option value) {
        this();
        add(value);
    }

    /**
     * Constructs a {@link CheckinOptions} with the given options initially set.
     */
    public CheckinOptions(final Option[] values) {
        this();
        addAll(values);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinOptions getWebServiceObject() {
        return (_CheckinOptions) webServiceObject;
    }

    /*
     * Overrides.
     */

    public void add(final Option flagWrapper) {
        super.add(flagWrapper);
    }

    public void addAll(final CheckinOptions flagSetWrapper) {
        super.addAll(flagSetWrapper);
    }

    public void addAll(final Option[] flagWrappers) {
        super.addAll(flagWrappers);
    }

    public boolean contains(final Option flagWrapper) {
        return super.contains(flagWrapper);
    }

    public boolean containsAll(final CheckinOptions flagSetWrapper) {
        return super.containsAll(flagSetWrapper);
    }

    public boolean containsAll(final Option[] flagWrappers) {
        return super.containsAll(flagWrappers);
    }

    public boolean containsAny(final CheckinOptions flagSetWrapper) {
        return super.containsAny(flagSetWrapper);
    }

    public boolean containsAny(final Option[] flagWrappers) {
        return super.containsAny(flagWrappers);
    }

    public boolean containsOnly(final Option flagWrapper) {
        return super.containsOnly(flagWrapper);
    }

    public boolean remove(final Option flagWrapper) {
        return super.remove(flagWrapper);
    }

    public boolean removeAll(final CheckinOptions flagSetWrapper) {
        return super.removeAll(flagSetWrapper);
    }

    public boolean removeAll(final Option[] flagWrappers) {
        return super.removeAll(flagWrappers);
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
