// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

/**
 * Listener used to trigger TE page navigation when new undocked views show
 */
public interface NewViewShowsListener {
    public void onNewViewShows(final String pageId);
}
