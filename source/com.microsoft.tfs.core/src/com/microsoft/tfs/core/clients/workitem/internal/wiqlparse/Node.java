// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Iterator;
import java.util.Locale;

/**
 * Base class representing a single node of the syntax tree.
 */
public abstract class Node {
    // inclusive
    private int startOffset;

    // exclusive
    private int endOffset;

    private final NodeType nodeType;

    private boolean hasParentheses;

    protected Node(final NodeType nodeType) {
        startOffset = -1;
        endOffset = -1;
        this.nodeType = nodeType;
    }

    public boolean isHasParantheses() {
        return hasParentheses;
    }

    public void setHasParantheses(final boolean hasParantheses) {
        hasParentheses = hasParantheses;
    }

    protected void appendChildren(final StringBuffer b, final String sep) {
        final int length = b.length();
        final int count = getCount();
        for (int i = 0; i < count; i++) {
            final Node node = getItem(i);
            if (node != null) {
                if (b.length() != length) {
                    b.append(sep);
                }
                if ((node.getPriority().isGreaterThanOrEqualTo(getPriority())) || node.hasParentheses) {
                    b.append("("); //$NON-NLS-1$
                    node.appendTo(b);
                    b.append(")"); //$NON-NLS-1$
                } else {
                    node.appendTo(b);
                }
            }
        }
    }

    public abstract void appendTo(StringBuffer b);

    // override in some subclasses
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (e != null) {
            e.verifyNode(this, tableContext, fieldContext);
        }
    }

    protected void bindChildren(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        final int childCount = getCount();
        for (int childIx = 0; childIx < childCount; childIx++) {
            getItem(childIx).bind(e, tableContext, fieldContext);
        }
    }

    // override in some subclasses
    public boolean canCastTo(final DataType dataType, final Locale locale) {
        return getDataType() == dataType;
    }

    public String checkPrefix(String prefix) {
        final int count = getCount();
        for (int i = 0; i < count; i++) {
            prefix = getItem(i).checkPrefix(prefix);
        }

        return prefix;
    }

    protected boolean isChildrenCanCastTo(final DataType dataType, final Locale locale) {
        final int childCount = getCount();
        for (int childIx = 0; childIx < childCount; childIx++) {
            if (!getItem(childIx).canCastTo(dataType, locale)) {
                return false;
            }
        }
        return true;
    }

    protected DataType getChildrenDataType() {
        final int childCount = getCount();
        if (childCount == 0) {
            return DataType.VOID;
        }
        final DataType childrenDataType = getItem(0).getDataType();
        for (int childIx = 1; childIx < childCount; childIx++) {
            if (getItem(childIx).getDataType() != childrenDataType) {
                return DataType.UNKNOWN;
            }
        }
        return childrenDataType;
    }

    protected boolean isChildrenConst() {
        final int childCount = getCount();
        for (int childIx = 0; childIx < childCount; childIx++) {
            if (!getItem(childIx).isConst()) {
                return false;
            }
        }
        return true;
    }

    public Iterator<Node> iterator() {
        return new NodeIterator(this);
    }

    // override in some subclasses
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (e == null) {
            return this;
        }
        return e.optimizeNode(this, tableContext, fieldContext);
    }

    protected void optimizeChildren(
        final IExternal e,
        final NodeTableName tableContext,
        final NodeFieldName fieldContext) {
        final int childCount = getCount();
        for (int childIx = 0; childIx < childCount; childIx++) {
            setItem(childIx, getItem(childIx).optimize(e, tableContext, fieldContext));
        }
    }

    public void setPrefix(final String prefix) {
        final int count = getCount();
        for (int i = 0; i < count; i++) {
            getItem(i).setPrefix(prefix);
        }
    }

    @Override
    public String toString() {
        final StringBuffer b = new StringBuffer();
        appendTo(b);
        return b.toString();
    }

    // override in some subclasses
    public String getConstStringValue() {
        return null;
    }

    // get the count of child nodes
    public abstract int getCount();

    public abstract DataType getDataType();

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(final int endOffset) {
        this.endOffset = endOffset;
    }

    public abstract boolean isConst();

    public abstract boolean isScalar();

    // get a child node at index i
    public abstract Node getItem(int i);

    // set a child node at index i
    public abstract void setItem(int i, Node value);

    public NodeType getNodeType() {
        return nodeType;
    }

    public abstract Priority getPriority();

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(final int startOffset) {
        this.startOffset = startOffset;
    }
}
