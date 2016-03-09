// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

public class KeyValuePair<KeyT, ValueT> {
    private final KeyT key;
    private final ValueT value;

    public KeyValuePair(final KeyT key, final ValueT value) {
        this.key = key;
        this.value = value;
    }

    public KeyT getKey() {
        return key;
    }

    public ValueT getValue() {
        return value;
    }
}
