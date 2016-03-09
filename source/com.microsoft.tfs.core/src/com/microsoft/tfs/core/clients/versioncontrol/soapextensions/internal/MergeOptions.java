// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.internal.wrappers.FlagSetWrapper;

import ms.tfs.versioncontrol.clientservices._03._MergeOptions;
import ms.tfs.versioncontrol.clientservices._03._MergeOptions._MergeOptions_Flag;

/**
 * Merge options for a TFS 2005 or TFS 2008 server. {@link MergeFlags}
 * supersedes this type.
 */
public final class MergeOptions extends FlagSetWrapper {
    /**
     * Extends {@link _MergeOptions_Flag} to provide some extra values that
     * simply aren't defined in the WSDL (but TFS accepts them).
     */
    final static class _MergeOptions_Flag_Extended extends _MergeOptions_Flag {
        public static final _MergeOptions_Flag SILENT = new _MergeOptions_Flag_Extended("Silent"); //$NON-NLS-1$
        public static final _MergeOptions_Flag NO_IMPLICIT_BASELESS =
            new _MergeOptions_Flag_Extended("NoImplicitBaseless"); //$NON-NLS-1$

        private _MergeOptions_Flag_Extended(final String name) {
            super(name);
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
     * Wraps the web service flag type as an {@link Option}.
     */
    public final static class Option extends FlagWrapper {
        private Option(final _MergeOptions_Flag flag) {
            super(flag);
        }
    }

    public static final Option NONE = new Option(_MergeOptions_Flag.None);
    public static final Option FORCE_MERGE = new Option(_MergeOptions_Flag.ForceMerge);
    public static final Option BASELESS = new Option(_MergeOptions_Flag.Baseless);
    public static final Option NO_MERGE = new Option(_MergeOptions_Flag.NoMerge);
    public static final Option ALWAYS_ACCEPT_MINE = new Option(_MergeOptions_Flag.AlwaysAcceptMine);

    /*
     * These values are not in the web service definition, but are known by TFS
     * 2008 all the same.
     */

    /**
     * @since TFS 2008
     */
    public static final Option SILENT = new Option(_MergeOptions_Flag_Extended.SILENT);

    /**
     * @since TFS 2008
     */
    public static final Option NO_IMPLICIT_BASELESS = new Option(_MergeOptions_Flag_Extended.NO_IMPLICIT_BASELESS);

    /**
     * Constructs an {@link MergeOptions} with no options initially set.
     */
    public MergeOptions() {
        super(new _MergeOptions());
    }

    /**
     * Constructs an {@link MergeOptions} that wraps the given
     * {@link _MergeOptions}.
     *
     * @param mergeOptions
     *        the inner {@link _MergeOptions} (must not be <code>null</code>)
     */
    public MergeOptions(final _MergeOptions mergeOptions) {
        super(mergeOptions);
    }

    /**
     * Constructs a {@link MergeOptions} with the given single option initially
     * set.
     */
    public MergeOptions(final Option value) {
        this();
        add(value);
    }

    /**
     * Constructs a {@link MergeOptions} with the given options initially set.
     */
    public MergeOptions(final Option[] values) {
        this();
        addAll(values);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _MergeOptions getWebServiceObject() {
        return (_MergeOptions) webServiceObject;
    }

    /*
     * Overrides.
     */

    public void add(final Option flagWrapper) {
        super.add(flagWrapper);
    }

    public void addAll(final MergeOptions flagSetWrapper) {
        super.addAll(flagSetWrapper);
    }

    public void addAll(final Option[] flagWrappers) {
        super.addAll(flagWrappers);
    }

    public boolean contains(final Option flagWrapper) {
        return super.contains(flagWrapper);
    }

    public boolean containsAll(final MergeOptions flagSetWrapper) {
        return super.containsAll(flagSetWrapper);
    }

    public boolean containsAll(final Option[] flagWrappers) {
        return super.containsAll(flagWrappers);
    }

    public boolean containsAny(final MergeOptions flagSetWrapper) {
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

    public boolean removeAll(final MergeOptions flagSetWrapper) {
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
