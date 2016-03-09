// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.util.EventListener;

public interface CheckinSubControlTitleChangedListener extends EventListener {
    public void onTitleChanged(CheckinSubControlEvent event);
}
