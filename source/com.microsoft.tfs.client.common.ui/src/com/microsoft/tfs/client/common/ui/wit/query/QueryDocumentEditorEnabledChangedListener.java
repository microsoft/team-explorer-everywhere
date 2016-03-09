// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.query;

import java.util.EventListener;

public interface QueryDocumentEditorEnabledChangedListener extends EventListener {
    public void onEnabledChanged(final BaseQueryDocumentEditor editor, final boolean enabled);
}
