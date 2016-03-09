// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.artifact;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown when a URI string is invalid.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class MalformedURIException extends TECoreException {
    public MalformedURIException(final String uri) {
        super(uri);
    }
}
