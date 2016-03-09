// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class AttachedPropertyDictionary extends TreeMap<String, Object> {
    private final Map<String, Object> delta;

    public AttachedPropertyDictionary() {
        delta = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
    }

    public AttachedPropertyDictionary(final PropertyValue[] properties) {
        for (final PropertyValue value : properties) {
            this.put(value.getPropertyName(), value.getInternalValue());
        }

        delta = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
    }

    public void clearChangedProperties() {
        delta.clear();
    }

    public void copyFrom(final Map<String, Object> source) {
        // Remove properties that no longer exist
        final String[] keys = keySet().toArray(new String[size()]);
        for (final String key : keys) {
            if (!source.containsKey(key)) {
                remove(key);
            }
        }

        // Update properties
        for (final java.util.Map.Entry<String, Object> pair : entrySet()) {
            put(pair.getKey(), pair.getValue());
        }
    }

    public PropertyValue[] getChangedProperties() {
        final PropertyValue[] properties = new PropertyValue[delta.size()];
        int index = 0;

        for (final java.util.Map.Entry<String, Object> pair : delta.entrySet()) {
            properties[index++] = new PropertyValue(pair.getKey(), pair.getValue());
        }

        return properties;
    }
}
