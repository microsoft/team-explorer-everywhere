// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;

/**
 * A helper class for working with ICheckable elements, such as
 * CheckboxTableViewer. This class can keep track of which elements are
 * currently checked in the ICheckable. It can be subclassed to perform specific
 * actions when an element is checked or unchecked.
 */
public class CheckedElements {
    private final List elements = new ArrayList();
    private final ICheckable checkable;

    public CheckedElements(final ICheckable checkable) {
        this.checkable = checkable;
        checkable.addCheckStateListener(new CheckStateListener());
    }

    public void setCheckState(final List inputElements, final boolean state) {
        for (final Iterator it = inputElements.iterator(); it.hasNext();) {
            final Object element = it.next();
            checkable.setChecked(element, state);
            if (state) {
                elements.add(element);
                onCheck(element);
            } else {
                elements.remove(element);
                onUncheck(element);
            }
        }
    }

    public List getElements() {
        return elements;
    }

    public boolean contains(final Object element) {
        return elements.contains(element);
    }

    public void clearElements() {
        elements.clear();
    }

    protected void onCheck(final Object element) {

    }

    protected void onUncheck(final Object element) {

    }

    private class CheckStateListener implements ICheckStateListener {
        @Override
        public void checkStateChanged(final CheckStateChangedEvent event) {
            final boolean checked = event.getChecked();
            final Object element = event.getElement();
            if (checked) {
                elements.add(element);
                onCheck(element);
            } else {
                elements.remove(element);
                onUncheck(element);
            }
        }
    }
}
