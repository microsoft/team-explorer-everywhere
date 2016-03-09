// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

import com.microsoft.tfs.util.BitField;

public class EnumSubTreeOptions extends BitField {
    private static final long serialVersionUID = -8612266194119834867L;

    // Default behavior.
    public static final EnumSubTreeOptions NONE = new EnumSubTreeOptions(0);

    // Enumerate sparse nodes in the tree that do not have associated objects.
    // The callback will be called with default(T) for the object.
    public static final EnumSubTreeOptions ENUMERATE_SPARSE_NODES = new EnumSubTreeOptions(1);

    // Enumerate the root of the subtree. Normally only the contents are
    // enumerated.
    public static final EnumSubTreeOptions ENUMERATE_SUB_TREE_ROOT = new EnumSubTreeOptions(2);

    // Include additional data bits on the EnumeratedSparseTreeNode instances
    // returned, such as HasChildren and NoChildrenBelow. If this flag is not
    // specified, then the value of these fields is undefined.
    //
    // This flag is not respected by the legacy EnumSubTree methods which take
    // a callback delegate as a parameter. For that family of methods, the
    // return of additional data is controlled by the value of the
    // additionalData
    // parameter.
    public static final EnumSubTreeOptions INCLUDE_ADDITIONAL_DATA = new EnumSubTreeOptions(4);

    private EnumSubTreeOptions(final int value) {
        super(value);
    }

    public EnumSubTreeOptions remove(final EnumSubTreeOptions other) {
        return new EnumSubTreeOptions(removeInternal(other));
    }

    public EnumSubTreeOptions combine(final EnumSubTreeOptions other) {
        return new EnumSubTreeOptions(combineInternal(other));
    }

    public boolean contains(final EnumSubTreeOptions other) {
        return containsInternal(other);
    }
}
