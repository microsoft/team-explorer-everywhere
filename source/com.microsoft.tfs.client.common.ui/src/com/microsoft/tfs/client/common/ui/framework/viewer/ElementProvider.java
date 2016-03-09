// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

public interface ElementProvider {
    public void addElementListener(ElementListener listener);

    public void removeElementListener(ElementListener listener);

    public Object[] getElements();
}
