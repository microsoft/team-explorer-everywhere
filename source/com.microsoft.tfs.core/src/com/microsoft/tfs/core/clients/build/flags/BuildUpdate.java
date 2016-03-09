// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.FlagSetWrapper;

import ms.tfs.build.buildservice._04._BuildUpdate;
import ms.tfs.build.buildservice._04._BuildUpdate._BuildUpdate_Flag;

public class BuildUpdate extends FlagSetWrapper {
    public final static class Field extends FlagWrapper {
        private Field(final _BuildUpdate_Flag flag) {
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

    public static final Field NONE = new Field(_BuildUpdate_Flag.None);
    public static final Field BUILD_NUMBER = new Field(_BuildUpdate_Flag.BuildNumber);
    public static final Field DROP_LOCATION = new Field(_BuildUpdate_Flag.DropLocation);
    public static final Field LABEL_NAME = new Field(_BuildUpdate_Flag.LabelName);
    public static final Field LOG_LOCATION = new Field(_BuildUpdate_Flag.LogLocation);
    public static final Field STATUS = new Field(_BuildUpdate_Flag.Status);
    public static final Field QUALITY = new Field(_BuildUpdate_Flag.Quality);
    public static final Field COMPILATION_STATUS = new Field(_BuildUpdate_Flag.CompilationStatus);
    public static final Field TEST_STATUS = new Field(_BuildUpdate_Flag.TestStatus);
    public static final Field KEEP_FOREVER = new Field(_BuildUpdate_Flag.KeepForever);
    public static final Field SOURCE_GET_VERSION = new Field(_BuildUpdate_Flag.SourceGetVersion);

    public BuildUpdate() {
        super(new _BuildUpdate());
    }

    public BuildUpdate(final _BuildUpdate buildUpdate) {
        super(buildUpdate);
    }

    /**
     * Constructs a {@link BuildUpdate} with the given single field initially
     * set.
     */
    public BuildUpdate(final Field value) {
        this();
        add(value);
    }

    /**
     * Constructs a {@link BuildUpdate} with the given fields initially set.
     */
    public BuildUpdate(final Field[] values) {
        this();
        addAll(values);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _BuildUpdate getWebServiceObject() {
        return (_BuildUpdate) webServiceObject;
    }

    /*
     * Overrides.
     */

    public void add(final Field flagWrapper) {
        super.add(flagWrapper);
    }

    public void addAll(final BuildUpdate flagSetWrapper) {
        super.addAll(flagSetWrapper);
    }

    public void addAll(final Field[] flagWrappers) {
        super.addAll(flagWrappers);
    }

    public boolean contains(final Field flagWrapper) {
        return super.contains(flagWrapper);
    }

    public boolean containsAll(final Field[] flagWrappers) {
        return super.containsAll(flagWrappers);
    }

    public boolean containsAny(final Field[] flagWrappers) {
        return super.containsAny(flagWrappers);
    }

    public boolean containsAll(final BuildUpdate flagSetWrapper) {
        return super.containsAll(flagSetWrapper);
    }

    public boolean containsAny(final BuildUpdate flagSetWrapper) {
        return super.containsAny(flagSetWrapper);
    }

    public boolean containsOnly(final Field flagWrapper) {
        return super.containsOnly(flagWrapper);
    }

    public boolean remove(final Field flagWrapper) {
        return super.remove(flagWrapper);
    }

    public boolean removeAll(final Field[] flagWrappers) {
        return super.removeAll(flagWrappers);
    }

    public boolean removeAll(final BuildUpdate flagSetWrapper) {
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
