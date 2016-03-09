// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import org.eclipse.jface.viewers.IElementComparer;

import com.microsoft.tfs.client.common.item.ServerItemPath;

/**
 * A comparer for use with ItemPaths and PathElements. This comparer allows
 * PathElements, such as TFSItems, and ItemPaths to be treated similarly, so a
 * control using this comparer can work with either type of object.
 */
public class ItemPathComparer implements IElementComparer {
    @Override
    public boolean equals(final Object a, final Object b) {
        // first, attempt to coerce both inputs to ItemPaths
        final ServerItemPath pathA = coerceToItemPath(a);
        final ServerItemPath pathB = coerceToItemPath(b);

        // if both inputs were converted to ItemPaths
        if (pathA != null && pathB != null) {
            return pathA.equals(pathB);
        } else {
            // either pathA or pathB were null, so one or both inputs were not
            // convertable to ItemPaths. fall back to comparing the original
            // inputs
            return (a == null ? b == null : a.equals(b));
        }
    }

    private ServerItemPath coerceToItemPath(final Object x) {
        if (x instanceof ServerItemPath) {
            return (ServerItemPath) x;
        } else if (x instanceof PathElement) {
            return ((PathElement) x).getItemPath();
        } else {
            return null;
        }
    }

    @Override
    public int hashCode(final Object element) {
        if (element instanceof PathElement) {
            return ((PathElement) element).getItemPath().hashCode();
        }
        return (element == null ? 0 : element.hashCode());
    }
}
