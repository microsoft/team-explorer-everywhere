// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.editors;

/**
 * Listener used to listen to source control change from tfvc to Git
 */
public interface SourceControlListener {
    public void onSourceControlChanged(final boolean tfvc);
}
