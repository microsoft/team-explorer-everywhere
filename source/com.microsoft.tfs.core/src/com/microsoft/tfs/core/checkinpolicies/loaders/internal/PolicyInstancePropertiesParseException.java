// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.loaders.internal;

import com.microsoft.tfs.core.checkinpolicies.loaders.ClasspathPolicyLoader;
import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown when the {@link ClasspathPolicyLoader} can't parse the properties file
 * input stream.
 *
 * @threadsafety thread-safe
 */
public class PolicyInstancePropertiesParseException extends TECoreException {
    public PolicyInstancePropertiesParseException() {
        super();
    }

    public PolicyInstancePropertiesParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PolicyInstancePropertiesParseException(final String message) {
        super(message);
    }

    public PolicyInstancePropertiesParseException(final Throwable cause) {
        super(cause);
    }
}
