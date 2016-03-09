// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * General exception from windows registry native API.
 *
 *
 * @threadsafety unknown
 */
public class RegistryException extends RuntimeException {
    private static final long serialVersionUID = 8916128239110226164L;

    public RegistryException(final String message) {
        super(message);
    }
}
