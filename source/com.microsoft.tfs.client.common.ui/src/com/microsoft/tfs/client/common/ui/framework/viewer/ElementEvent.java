// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import java.util.EventObject;

import com.microsoft.tfs.util.Check;

public class ElementEvent extends EventObject {
    private final Object[] elements;

    public ElementEvent(final ElementProvider source, final Object[] elements) {
        super(source);

        Check.notNull(elements, "elements"); //$NON-NLS-1$

        this.elements = elements;
    }

    public ElementProvider getElementProvider() {
        return (ElementProvider) getSource();
    }

    public Object[] getElements() {
        return elements;
    }
}
