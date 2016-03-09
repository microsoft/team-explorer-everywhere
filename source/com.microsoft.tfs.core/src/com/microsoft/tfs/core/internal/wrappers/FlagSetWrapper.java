// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.wrappers;

import java.util.Set;

import com.microsoft.tfs.core.ws.runtime.types.Flag;
import com.microsoft.tfs.core.ws.runtime.types.FlagSet;

/**
 * <p>
 * Base class for wrapping {@link FlagSet} web service objects. Provides storage
 * of the web service object (by extending {@link WebServiceObjectWrapper}) as
 * well as some {@link Set}-oriented methods to keep duplicated code to a
 * minimum.
 * </p>
 * <p>
 * All {@link FlagSetWrapper} derived classes should implement a
 * "getWebServiceObject" method with a return type specific to their wrapped
 * type. This can't be enforced via abstract method requirement (because
 * different return types are different methods).
 * </p>
 * <p>
 * To prevent Eclipse compile warnings in projects that depend on this class via
 * the plug-in classloader, derived classes must override
 * {@link #equals(Object)} and {@link #hashCode()}. The implementations can
 * simply chain to the super implementation, which compares the flag set
 * contents as {@link Set}s.
 * </p>
 *
 * @threadsafety thread-compatible
 */
public abstract class FlagSetWrapper extends WebServiceObjectWrapper {
    /**
     * <p>
     * Lightweight class for wrapping {@link Flag} objects with inner types in
     * {@link FlagSetWrapper}s. The class exists primarily to establish a type
     * which the {@link Set}-oriented methods in {@link FlagSetWrapper} can use
     * so it can accept/compare {@link Flag} objects.
     * </p>
     * <p>
     * To prevent Eclipse compile warnings in projects that depend on this class
     * via the plug-in classloader, derived classes must override
     * {@link #equals(Object)} and {@link #hashCode()}. The implementations can
     * simply chain to the super implementation, which compares the inner
     * {@link Flag} objects (which compare SOAP identity strings).
     * </p>
     */
    protected static abstract class FlagWrapper {
        protected final Flag flag;

        public FlagWrapper(final Flag flag) {
            this.flag = flag;
        }

        @Override
        public String toString() {
            return flag.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            /*
             * Since FlagWrapper and FlagSetWrapper singletons are exposed next
             * to each other (as statics in FlagSetWrapper-derived classes),
             * check for the easy-to-make mistake of comparing a set with a
             * flag.
             */
            if (obj instanceof FlagSetWrapper) {
                // Throwing for programming safety (see bug 543)
                throw new IllegalArgumentException(
                    "Error, cannot compare a FlagSetWrapper to a FlagWrapper; can only compare sets to sets and flags to flags"); //$NON-NLS-1$
            }
            if (obj instanceof FlagWrapper == false) {
                return false;
            }

            return flag.equals(obj);
        }

        @Override
        public int hashCode() {
            return flag.hashCode();
        }
    }

    /**
     * Creates a {@link FlagSetWrapper} that wraps the given {@link FlagSet}
     * object.
     *
     * @param webServiceObject
     *        the {@link FlagSet} object to wrap (must not be <code>null</code>)
     */
    protected FlagSetWrapper(final FlagSet webServiceObject) {
        super(webServiceObject);
    }

    /*
     * These methods accept/return FlagWrapper objects but read/store values
     * from the inner web service object's raw flag type. Extending classes
     * should provide public versions of this methods that use their own
     * FlagWrapper-derived type.
     */

    protected void add(final FlagWrapper flagWrapper) {
        ((FlagSet) webServiceObject).add(flagWrapper.flag);
    }

    protected void addAll(final FlagWrapper[] flagWrappers) {
        /*
         * Do our own iteration because we need the flag inside the wrapper.
         */
        for (int i = 0; i < flagWrappers.length; i++) {
            ((FlagSet) webServiceObject).add(flagWrappers[i].flag);
        }
    }

    protected void addAll(final FlagSetWrapper flagSetWrapper) {
        /*
         * Pass one collection (Set) directly to the other.
         */
        ((FlagSet) webServiceObject).addAll(((FlagSet) flagSetWrapper.webServiceObject));
    }

    protected boolean contains(final FlagWrapper flagWrapper) {
        return ((FlagSet) webServiceObject).contains(flagWrapper.flag);
    }

    protected boolean containsOnly(final FlagWrapper flagWrapper) {
        return ((FlagSet) webServiceObject).containsOnly(flagWrapper.flag);
    }

    protected boolean containsAll(final FlagWrapper[] flagWrappers) {
        /*
         * Do our own iteration because we need the flag inside the wrapper.
         */
        for (int i = 0; i < flagWrappers.length; i++) {
            if (((FlagSet) webServiceObject).contains(flagWrappers[i].flag) == false) {
                return false;
            }
        }

        return true;
    }

    protected boolean containsAny(final FlagWrapper[] flagWrappers) {
        /*
         * Do our own iteration because we need the flag inside the wrapper.
         */
        for (int i = 0; i < flagWrappers.length; i++) {
            if (((FlagSet) webServiceObject).contains(flagWrappers[i].flag)) {
                return true;
            }
        }

        return false;
    }

    protected boolean containsAll(final FlagSetWrapper flagSetWrapper) {
        return ((FlagSet) webServiceObject).containsAll((FlagSet) flagSetWrapper.webServiceObject);
    }

    protected boolean containsAny(final FlagSetWrapper flagSetWrapper) {
        return ((FlagSet) webServiceObject).containsAny((FlagSet) flagSetWrapper.webServiceObject);
    }

    protected boolean remove(final FlagWrapper flagWrapper) {
        return ((FlagSet) webServiceObject).remove(flagWrapper.flag);
    }

    protected boolean removeAll(final FlagWrapper[] flagWrappers) {
        /*
         * Do our own iteration because we need the flag inside the wrapper.
         */
        boolean changed = false;
        for (int i = 0; i < flagWrappers.length; i++) {
            if (((FlagSet) webServiceObject).remove(flagWrappers[i].flag) == true) {
                changed = true;
            }
        }

        return changed;
    }

    protected boolean removeAll(final FlagSetWrapper flagSetWrapper) {
        return ((FlagSet) webServiceObject).removeAll((FlagSet) flagSetWrapper.webServiceObject);
    }

    /**
     * @return the count of flags contained in this set.
     */
    public int size() {
        return ((FlagSet) webServiceObject).size();
    }

    /**
     * @return true if this set contains no flags, false if it contains at least
     *         one flag
     */
    public boolean isEmpty() {
        return ((FlagSet) webServiceObject).isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof FlagSetWrapper == false) {
            return false;
        }

        return ((FlagSet) webServiceObject).equals(o);
    }

    @Override
    public int hashCode() {
        return ((FlagSet) webServiceObject).hashCode();
    }
}
