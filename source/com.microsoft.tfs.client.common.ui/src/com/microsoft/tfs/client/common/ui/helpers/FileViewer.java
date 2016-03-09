// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.ui.IWorkbenchPage;

/**
 * Views a file or folder (in the workbench or with an external tool).
 *
 * @threadsafety thread-compatible
 */
public interface FileViewer {
    /**
     * Views the given file or folder path.
     *
     * @param path
     *        the local path of the file to view (may be <code>null</code> or
     *        empty)
     * @param page
     *        the active workbench page (must not be <code>null</code>)
     * @param inModalContext
     *        true if the application is in a modal context and a viewer that
     *        works inside a modal context must be used (for example, the viewer
     *        is internal and opens a new top-level window, or the viewer is
     *        external and opens its own application), false if the context is
     *        non-modal and the viewer may open an editor in the workbench
     * @return true if the file was viewed, false if it was not viewed
     */
    public boolean viewFile(String path, IWorkbenchPage page, boolean inModalContext);
}