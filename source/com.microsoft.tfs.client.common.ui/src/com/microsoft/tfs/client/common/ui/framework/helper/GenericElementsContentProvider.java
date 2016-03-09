// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * An IStructuredContentProvider that can handle a number of collection-oriented
 * input objects, such as Collections, Maps, arrays, and Iterators.
 */
public class GenericElementsContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement == null) {
            return new Object[] {};
        } else if (inputElement instanceof Collection) {
            return ((Collection) inputElement).toArray();
        } else if (inputElement instanceof Map) {
            return ((Map) inputElement).values().toArray();
        } else if (inputElement instanceof Object[]) {
            return (Object[]) inputElement;
        } else if (inputElement instanceof Iterator) {
            final List l = new ArrayList();
            for (final Iterator it = (Iterator) inputElement; it.hasNext();) {
                l.add(it.next());
            }
            return l.toArray();
        } else {
            return new Object[] {};
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
    }
}
