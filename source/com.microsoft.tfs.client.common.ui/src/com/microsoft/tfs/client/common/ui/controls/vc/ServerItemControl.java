// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;

public interface ServerItemControl {
    public void setSelectedItem(TypedServerItem item);

    public TypedServerItem getSelectedItem();
}
