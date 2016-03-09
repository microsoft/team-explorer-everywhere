// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import java.util.EventListener;

public interface ChangeItemProviderListener extends EventListener {
    public void onChangeItemsUpdated(ChangeItemProviderEvent event);
}
