// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.tree;

import org.eclipse.jface.viewers.ITreeContentProvider;

import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;

/**
 * A base class for implementing the ITreeContentProvider interface. Provides
 * do-nothing implementations of some of the less commonly used methods
 * (getParent, dispose, inputChanged) on the interface.
 */
public abstract class TreeContentProvider extends ContentProviderAdapter implements ITreeContentProvider {
    @Override
    public Object getParent(final Object element) {
        return null;
    }
}
