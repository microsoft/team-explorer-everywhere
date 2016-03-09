// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

import com.microsoft.tfs.util.BitField;

public class EnumParentsOptions extends BitField {
    private static final long serialVersionUID = -1539243279354928382L;

    // Default behavior.
    public static final EnumParentsOptions NONE = new EnumParentsOptions(0);

    // Enumerate sparse nodes in the tree that do not have associated objects.
    // The referenced object will be enumerated as default(T) in this case.
    public static final EnumParentsOptions ENUMERATE_SPARSE_NODES = new EnumParentsOptions(1);

    // Include additional data bits on the EnumeratedSparseTreeNode instances
    // returned, such as HasChildren and NoChildrenBelow. If this flag is not
    // specified, then the value of these fields is undefined.
    //
    // This flag is not respected by the legacy EnumParents methods which take
    // a callback delegate as a parameter. For that family of methods, the
    // return of additional data is controlled by the value of the
    // additionalData parameter.
    public static final EnumParentsOptions INCLUDE_ADDITIONAL_DATA = new EnumParentsOptions(2);

    private EnumParentsOptions(final int value) {
        super(value);
    }

    public EnumParentsOptions remove(final EnumParentsOptions other) {
        return new EnumParentsOptions(removeInternal(other));
    }

    public EnumParentsOptions combine(final EnumParentsOptions other) {
        return new EnumParentsOptions(combineInternal(other));
    }

    public boolean contains(final EnumParentsOptions other) {
        return containsInternal(other);
    }
}
