// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.status;

import org.eclipse.core.runtime.Status;

/**
 * An extension of {@link Status} that exists so that clients know that the
 * command threw an exception that was not handled by an exception handler.
 *
 * @threadsafety unknown
 */
public class UncaughtCommandExceptionStatus extends TeamExplorerStatus {
    public UncaughtCommandExceptionStatus(
        final int severity,
        final String pluginId,
        final int code,
        final String message,
        final Throwable exception) {
        super(severity, pluginId, code, message, exception);
    }
}
