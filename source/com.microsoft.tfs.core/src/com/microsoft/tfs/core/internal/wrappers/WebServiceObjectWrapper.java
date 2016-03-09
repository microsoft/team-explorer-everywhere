// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.wrappers;

import com.microsoft.tfs.core.internal.wrappers.FlagSetWrapper.FlagWrapper;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Base class for wrapping web service objects. Provides storage of the object
 * and {@link #equals(Object)} and {@link #hashCode()} that delegates to the
 * wrapped object.
 * </p>
 * <p>
 * All {@link WebServiceObjectWrapper} derived classes should implement a
 * "getWebServiceObject" method with a return type specific to their wrapped
 * type. This can't be enforced via abstract method requirement (because
 * different return types are different methods).
 * </p>
 * <p>
 * To prevent Eclipse compile warnings in projects that depend on this class via
 * the plug-in classloader, and to provide correct behavior for complex wrapped
 * objects, derived classes must override {@link #equals(Object)} and
 * {@link #hashCode()}. Chaining to super implementations is not very useful
 * because this class's implementation simply chains to the wrapped object,
 * which doesn't offer useful implementations. However,
 * {@link EnumerationWrapper}, {@link FlagSetWrapper}, and {@link FlagWrapper}
 * provide useful implementations of these methods for derived classes to chain
 * to.
 * </p>
 *
 * @threadsafety thread-compatible
 */
public abstract class WebServiceObjectWrapper {
    protected volatile Object webServiceObject;

    /**
     * Creates a {@link WebServiceObjectWrapper} that wraps the given object.
     *
     * @param webServiceObject
     *        the object to wrap (must not be <code>null</code>)
     */
    protected WebServiceObjectWrapper(final Object webServiceObject) {
        Check.notNull(webServiceObject, "webServiceObject"); //$NON-NLS-1$
        this.webServiceObject = webServiceObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof WebServiceObjectWrapper == false) {
            return false;
        }

        return ((WebServiceObjectWrapper) o).webServiceObject.equals(webServiceObject);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return webServiceObject.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return webServiceObject.toString();
    }
}
