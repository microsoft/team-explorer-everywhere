// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 * Can be closed to free up all resources. Substitute for Java 5's version.
 */
public interface Closable {
    /**
     * Frees up all resources associated with the object. This method may be
     * invoked multiple times on an object, and the object should handle this
     * case appropriately (most likely ignoring the subsequent calls).
     */
    public void close();
}
