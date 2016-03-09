// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.wrappers;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.ws.runtime.types.Enumeration;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Base class for wrapping {@link Enumeration} web service objects. Provides
 * storage of the web service object (by extending
 * {@link WebServiceObjectWrapper}) as well as some methods to map web service
 * types to known singleton instances of derived classes (for deserialization
 * from web service data).
 * </p>
 * <p>
 * All {@link EnumerationWrapper} derived classes should implement a
 * "getWebServiceObject" method with a return type specific to their wrapped
 * type. This can't be enforced via abstract method requirement (because
 * different return types are different methods).
 * </p>
 * <p>
 * To prevent Eclipse compile warnings in projects that depend on this class via
 * the plug-in classloader, derived classes must override
 * {@link #equals(Object)} and {@link #hashCode()}. The implementations should
 * simply chain to the this class's implementations, which chain up to
 * {@link Enumeration}'s implementations (object equality, object address
 * hashcode). This is good behavior for singleton enumeration instances.
 * </p>
 *
 * @threadsafety thread-compatible
 */
public abstract class EnumerationWrapper extends WebServiceObjectWrapper {
    /**
     * Maps web service {@link Enumeration} instances (which are generated as
     * singletons so the key space is limited in this map) to instances of
     * classes that extend {@link EnumerationWrapper}.
     */
    private final static Map WEB_SERVICE_TO_WRAPPER_INSTANCES_MAP = new HashMap();

    /**
     * Creates a {@link EnumerationWrapper} that wraps the given
     * {@link Enumeration} object.
     *
     * @param webServiceObject
     *        the {@link Enumeration} object to wrap (must not be
     *        <code>null</code>)
     */
    protected EnumerationWrapper(final Enumeration webServiceObject) {
        super(webServiceObject);

        WEB_SERVICE_TO_WRAPPER_INSTANCES_MAP.put(webServiceObject, this);
    }

    /**
     * Gets the correct wrapper enumeration singleton instance for the given web
     * service object.
     *
     * @param webServiceObject
     *        the web service type to get the correct wrapper enumeration
     *        instance for (must not be <code>null</code>)
     * @return the correct wrapper enumeration instance
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service type
     */
    protected static EnumerationWrapper fromWebServiceObject(final Enumeration webServiceObject) {
        Check.notNull(webServiceObject, "webServiceObject"); //$NON-NLS-1$

        final EnumerationWrapper wrapperObject =
            (EnumerationWrapper) WEB_SERVICE_TO_WRAPPER_INSTANCES_MAP.get(webServiceObject);

        if (wrapperObject == null) {
            throw new RuntimeException(
                MessageFormat.format(
                    "No wrapper enumeration value is known for web service type {0}", //$NON-NLS-1$
                    webServiceObject));
        }

        return wrapperObject;
    }
}
