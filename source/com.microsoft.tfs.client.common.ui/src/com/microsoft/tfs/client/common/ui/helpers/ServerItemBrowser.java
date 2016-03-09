// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

/**
 * An interface for objects which can browse to (display to the user) a version
 * control server path.
 */
public interface ServerItemBrowser {
    /**
     * Browses to the given server path, showing it to the user. This must be
     * run on the UI thread.
     *
     * @param serverPath
     *        The server item to browse to (not <code>null</code>)
     * @return true if the path could be shown to the user, false if it could
     *         not
     */
    public boolean browse(String serverPath);
}
