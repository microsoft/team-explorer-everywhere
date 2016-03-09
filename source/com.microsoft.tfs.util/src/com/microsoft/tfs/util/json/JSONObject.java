// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.microsoft.tfs.util.Check;

/**
 * A JSON object that (currently) only supports string values.
 * <p>
 * Currently only String values are supported.
 *
 * @see http://json.org/
 * @see http://www.ietf.org/rfc/rfc4627.txt
 * @threadsafety thread-safe
 */
public class JSONObject {
    /**
     * Member names are case-sensitive as per the spec.
     */
    private final Map<String, String> members = new HashMap<String, String>();

    public JSONObject() {
    }

    // Delegates to Map methods

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public boolean containsKey(final String key) {
        return members.containsKey(key);
    }

    public boolean containsValue(final String value) {
        return members.containsValue(value);
    }

    public String get(final String key) {
        return members.get(key);
    }

    public String put(final String key, final String value) {
        Check.notNull(key, "key"); //$NON-NLS-1$
        return members.put(key, value);
    }

    public String remove(final String key) {
        return members.remove(key);
    }

    public void putAll(final Map<? extends String, ? extends String> t) {
        for (final Entry<? extends String, ? extends String> entry : t.entrySet()) {
            Check.notNull(entry.getValue(), "entry.getValue()"); //$NON-NLS-1$
        }

        members.putAll(t);
    }

    public void clear() {
        members.clear();
    }

    public Set<String> keySet() {
        return members.keySet();
    }

    public Collection<String> values() {
        return members.values();
    }

    public Set<Entry<String, String>> entrySet() {
        return members.entrySet();
    }

    // Overrides

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JSONObject == false) {
            return false;
        }

        return this.members.equals(((JSONObject) obj).members);
    }

    @Override
    public int hashCode() {
        return members.hashCode();
    }
}
