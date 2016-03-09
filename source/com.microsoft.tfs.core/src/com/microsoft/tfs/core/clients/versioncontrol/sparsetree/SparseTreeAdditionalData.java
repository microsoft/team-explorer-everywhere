// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

/**
 * The SparseTree EnumParents and EnumSubTree methods can return additional data
 * back to the caller, if a SparseTreeAdditionalData object is provided to the
 * enumeration function.
 *
 * @threadsafety unknown
 */
public class SparseTreeAdditionalData {
    public void Reset() {
        hasChildren = false;
        noChildrenBelow = null;
    }

    /*
     * True if the enumerated token has no children below it in the SparseTree.
     */
    public boolean hasChildren;

    /*
     * Example: When calling EnumParents on $/A/B/C/D in a tree which contains
     * nodes $/A and $/A/Z, the first node to be enumerated will be $/A. In this
     * case HasChildren will be true because $/A has a child $/A/Z. But this
     * child node is not on the path to $/A/B/C/D. In this case, the value for
     * NoChildrenBelow for the enumeration of node $/A will be $/A/B, child node
     * is not on the path to $/A/B/C/D. In this case, the value for
     * NoChildrenBelow for the enumeration of node $/A will be $/A/B, indicating
     * that $/A is the first node on the path for items under $/A/B to the root.
     */
    public String noChildrenBelow;
}
