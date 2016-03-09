// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

/**
 * <p>
 * Any object which can obtain a label for itself could be tagged as
 * {@link Labelable}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface Labelable {
    public String getLabel();
}
