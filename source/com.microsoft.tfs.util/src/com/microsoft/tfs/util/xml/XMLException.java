// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

/**
 * {@link XMLException} is the unchecked exception thrown by classes in the
 * <code>com.microsoft.tfs.util.xml</code> package in response to an XML
 * exception.
 */
public class XMLException extends RuntimeException {
    public XMLException(final Throwable cause) {
        super(cause);
    }

    public XMLException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
