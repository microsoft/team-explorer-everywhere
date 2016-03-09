// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class NodeOrOperator extends NodeVariableList {
    public NodeOrOperator() {
        super(NodeType.OR);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        for (int childIx = 0; childIx < getCount(); childIx++) {
            Tools.ensureSyntax(
                getItem(childIx).getDataType() == DataType.BOOL,
                SyntaxError.EXPECTING_BOOLEAN,
                getItem(childIx));
        }
        bindChildren(e, tableContext, fieldContext);
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        optimizeChildren(e, tableContext, fieldContext);
        int childIx = 0;
        while (childIx < getCount()) {
            if (getItem(childIx).getNodeType() == NodeType.BOOL_CONST) {
                if (!((NodeBoolConst) getItem(childIx)).getValue()) {
                    removeAt(childIx);
                    continue;
                }
                return new NodeBoolConst(true);
            }
            ++childIx;
        }
        if (getCount() == 0) {
            return new NodeBoolConst(false);
        }
        if (getCount() == 1) {
            return getItem(0);
        }
        return super.optimize(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        super.appendChildren(b, " or "); //$NON-NLS-1$
    }

    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }

    @Override
    public Priority getPriority() {
        return Priority.OR_OPERATOR;
    }
}
