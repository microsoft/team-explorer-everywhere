// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import java.util.EventListener;

public interface ElementListener extends EventListener {
    public void elementsChanged(ElementEvent event);
}
