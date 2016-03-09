// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

public class EnumeratedSparseTreeNode<X> {
    public EnumeratedSparseTreeNode(
        final String token,
        final X referencedObject,
        final boolean hasChildren,
        final String noChildrenBelow) {
        this.token = token;
        this.referencedObject = referencedObject;
        this.hasChildren = hasChildren;
        this.noChildrenBelow = noChildrenBelow;
    }

    /**
     * The token of this SparseTree entry.
     */
    public String token;

    /**
     * The object referenced by this SparseTree entry. The value may be null if
     * EnumerateSparseNodes was specified as an option.
     */
    public X referencedObject;

    /**
     * True if the enumerated token has no children below it in the SparseTree.
     * This field is only valid if includeAdditionalData was set to true.
     */
    public boolean hasChildren;

    /**
     * If HasChildren is false, then this field contains the same String
     * instance as the Token field. If HasChildren is true, then this field may
     * be non-null if this is the first token enumerated along an EnumParents
     * traversal where HasChildren is true. If it is non-null, then while there
     * are children in the tree under the Token field, there are no children
     * under the value of NoChildrenBelow.
     *
     * This field is only valid if includeAdditionalData was set to true. This
     * field is also probably only useful to you when you are using EnumParents.
     *
     * Example: When calling EnumParents on $/A/B/C/D in a tree which contains
     * nodes $/A and $/A/Z, the first node to be enumerated will be $/A. In this
     * case HasChildren will be true because $/A has a child $/A/Z. But this
     * child node is not on the path to $/A/B/C/D. In this case, the value for
     * NoChildrenBelow for the enumeration of node $/A will be $/A/B, indicating
     * that $/A is the first node on the path for items under $/A/B to the root.
     */
    public String noChildrenBelow;
}
