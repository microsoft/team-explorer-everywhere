// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

/**
 * <p>
 * {@link TEClientException} is the base class for exceptions thrown by TEE
 * client classes. Clients communicate with Team Foundation Server web services,
 * perform high-level tasks that use these services, and manage the in-memory or
 * on-disk state that accompanies these actions. Clients exists for version
 * control, work item tracking, build services, etc.
 * </p>
 * <p>
 * Many exceptions thrown by client classes include error information from the
 * Team Foundation Server. However, TFS encodes error information differently
 * for different web services. A common base class for exceptions related to
 * clients provides a convenient place to implement shared error
 * decoding/formatting logic so these errors can be rethrown consistently. See
 * derived classes for specialized decoding logic.
 * </p>
 * <p>
 * This class is abstract because the concept of a generic client exception is
 * not very useful when each client performs almost all of its own work.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public abstract class TEClientException extends TECoreException {
    public TEClientException() {
        super();
    }

    public TEClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TEClientException(final String message) {
        super(message);
    }

    public TEClientException(final Throwable cause) {
        super(cause);
    }
}
