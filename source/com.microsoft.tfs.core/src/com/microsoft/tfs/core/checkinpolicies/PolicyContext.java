// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link PolicyContext} contains optional values that are passed down to a
 * check-in policy by the execution environment that hosts the policy. A context
 * is available during several operations on {@link PolicyInstance}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class PolicyContext {
    private final Map properties = new HashMap();

    /**
     * Creates a {@link PolicyContext} with no keys initially set.
     */
    public PolicyContext() {
    }

    /**
     * Adds a property to this {@link PolicyContext}. If the property is already
     * defined, the old value will be overwritten.
     *
     * @param key
     *        the property key (must not be <code>null</code>)
     * @param value
     *        the property value (can be <code>null</code>)
     */
    public void addProperty(final String key, final Object value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$

        synchronized (properties) {
            properties.put(key, value);
        }
    }

    /**
     * Gets a property from this {@link PolicyContext}.
     *
     * @param key
     *        the property key (must not be <code>null</code>)
     * @return the value for the key (possibly <code>null</code> if not set or
     *         previously set to null)
     */
    public Object getProperty(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$

        synchronized (properties) {
            return properties.get(key);
        }
    }
}
