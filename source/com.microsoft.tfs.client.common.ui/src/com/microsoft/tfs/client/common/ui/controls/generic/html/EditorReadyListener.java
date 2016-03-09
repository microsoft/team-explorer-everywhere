// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.html;

import java.util.EventListener;

public interface EditorReadyListener extends EventListener {
    /**
     * Called when the {@link HTMLEditor} has completed loading and is ready to
     * use (methods such as {@link HTMLEditor#setHTML(String)} may be called).
     */
    public void editorReady();
}
