// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * <p>
 * An abstract base class for writing <code>IStructuredContentProviders</code>.
 * Provides do-nothing implementations of some of the less commonly used methods
 * of the <code>IStructuredContentProvider</code> interface.
 * </p>
 * <p>
 * Typical use will look like:
 *
 * <pre>
 *  viewer.setContentProvider(new ContentProviderAdapter() {
 *      public Object[] getElements(Object inputElement) {
 *          ...
 *      }
 *  });
 * </pre>
 *
 * </p>
 */
public abstract class ContentProviderAdapter implements IStructuredContentProvider {
    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
    }
}
