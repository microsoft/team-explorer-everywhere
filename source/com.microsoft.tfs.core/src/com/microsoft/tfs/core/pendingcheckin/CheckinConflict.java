// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Represents a conflict discovered before checkin.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class CheckinConflict {
    private final String serverItem;
    private final String code;
    private final String message;
    private final boolean resolvable;

    /**
     * @param serverItem
     *        the server item that had a conflict (must not be <code>null</code>
     *        or empty).
     * @param code
     *        the error code from the server (may be null).
     * @param message
     *        the conflict message (may be null).
     * @param resolvable
     *        whether the conflict was resolvable.
     */
    public CheckinConflict(final String serverItem, final String code, final String message, final boolean resolvable) {
        Check.notNullOrEmpty(serverItem, "serverItem"); //$NON-NLS-1$
        Check.notNull(message, "message"); //$NON-NLS-1$

        this.serverItem = serverItem;
        this.code = code;
        this.message = (message != null) ? message : ""; //$NON-NLS-1$
        this.resolvable = resolvable;
    }

    public String getServerItem() {
        return serverItem;
    }

    /**
     * The error code from the server
     *
     * @return A string containing the error code from the server (may be
     *         <code>null</code>)
     */
    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isResolvable() {
        return resolvable;
    }
}
