// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.util.Transformer;

public class SelectionHelper {
    public static Collection fromSelection(final ISelection selection, final Transformer transformer) {
        final List items = new ArrayList();

        if (selection != null) {
            if (selection instanceof IStructuredSelection) {
                final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                for (final Iterator it = structuredSelection.iterator(); it.hasNext();) {
                    final Object input = it.next();
                    final Object transformed = transformer.transform(input);
                    items.add(transformed);
                }
            }
        }

        return items;
    }
}
