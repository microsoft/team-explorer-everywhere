// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

/**
 * <p>
 * {@link TECoreException} is the <b>unchecked</b> base exception for all other
 * exception types that are:
 * </p>
 * <p>
 * <ul>
 * <li>Defined in com.microsoft.tfs.core<br>
 * <br>
 * <b>and</b><br>
 * <br>
 * <li>Thrown by methods in com.microsoft.tfs.core
 * </ul>
 * </p>
 * <p>
 * Unchecked exceptions are preferred by core classes.
 * </p>
 * <p>
 * Classes in core try to interpret all exceptions thrown by methods in "lower"
 * packages (e.g. com.microsoft.tfs.core.ws) by wrapping them in other types and
 * adding information. This is done to minimize leakage from these lower layers
 * and add value where core knowledge is available. Only choice exceptions are
 * given this treatment; exceptions like {@link NullPointerException}, errors
 * like {@link OutOfMemoryError}, and some network connectivity exceptions are
 * thrown unaltered if they do not have a higher meaning in the context of the
 * core client that encountered them.
 * </p>
 * <p>
 * This class is concrete so general core exceptions can be constructed
 * directly, though generally an available subclass should be thrown instead.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class TECoreException extends RuntimeException {
    public TECoreException() {
        super();
    }

    public TECoreException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TECoreException(final String message) {
        super(message);
    }

    public TECoreException(final Throwable cause) {
        super(cause);
    }
}
