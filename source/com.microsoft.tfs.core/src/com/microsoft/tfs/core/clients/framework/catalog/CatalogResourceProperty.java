// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import ms.ws._KeyValueOfStringString;

/**
 * A simple class which holds a {@link CatalogNode} propery name/value pair.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogResourceProperty {
    private final String propertyName;
    private final String propertyValue;

    /**
     * Constructor
     */
    public CatalogResourceProperty(final String name, final String value) {
        propertyName = name;
        propertyValue = value;
    }

    /**
     * Returns the property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the property value.
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * Convenience method to convert a an array of CatalogResourceProperty
     * values to an equivalent _KeyValueOfStringString array which is required
     * by the catalog web service proxy.
     *
     *
     * @param values
     *        An array containing the values to convert.
     *
     * @return An array containing the converted values.
     */
    public static _KeyValueOfStringString[] toKeyValueOfStringStringArray(final CatalogResourceProperty[] values) {
        if (values == null) {
            return null;
        }

        final _KeyValueOfStringString[] pairs = new _KeyValueOfStringString[values.length];
        for (int i = 0; i < values.length; i++) {
            pairs[i] = new _KeyValueOfStringString(values[i].getPropertyName(), values[i].getPropertyValue());
        }
        return pairs;
    }
}
