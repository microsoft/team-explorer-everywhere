// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

/**
 * Thrown when a download via download proxy fails, but does not leave core.
 * Core methods will catch this and retry the download directly against the TFS
 * automatically.
 *
 * @since TEE-SDK-10.1
 */
public final class DownloadProxyException extends VersionControlException {
    public DownloadProxyException() {
    }

    public DownloadProxyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DownloadProxyException(final String message) {
        super(message);
    }

    public DownloadProxyException(final Throwable cause) {
        super(cause);
    }
}
