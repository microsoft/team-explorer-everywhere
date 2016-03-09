// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import java.util.EventObject;

public class ChangeItemProviderEvent extends EventObject {
    public ChangeItemProviderEvent(final ChangeItemProvider source) {
        super(source);
    }

    public ChangeItemProvider getProvider() {
        return (ChangeItemProvider) getSource();
    }
}
