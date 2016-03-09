// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeFieldList extends NodeVariableList {
    public NodeFieldList(final NodeType nodeType) {
        super(nodeType);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        bindChildren(e, null, null);
        for (int childIx = 0; childIx < getCount(); childIx++) {
            Tools.ensureSyntax(
                getItem(childIx).getNodeType() == NodeType.FIELD_NAME,
                SyntaxError.EXPECTING_FIELD_NAME,
                getItem(childIx));
        }
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        optimizeChildren(e, tableContext, fieldContext);
        return super.optimize(e, tableContext, fieldContext);
    }

    // this method makes use of covariant return types in MS code
    public NodeFieldName getNodeFieldNameItem(final int ix) {
        return (NodeFieldName) getItem(ix);
    }

    // this method makes use of covariant return types in MS code
    public void setNodeFieldNameItem(final int ix, final NodeFieldName node) {
        setItem(ix, node);
    }

    @Override
    public Priority getPriority() {
        return Priority.COMMA_OPERATOR;
    }
}
