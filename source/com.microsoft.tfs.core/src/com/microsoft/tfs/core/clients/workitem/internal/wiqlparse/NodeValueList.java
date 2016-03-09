// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Locale;

public class NodeValueList extends NodeVariableList {
    public NodeValueList() {
        super(NodeType.VALUE_LIST);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        bindChildren(e, tableContext, fieldContext);
        if (e != null) {
            Tools.ensureSyntax(
                getChildrenDataType() != DataType.UNKNOWN,
                SyntaxError.UNKNOWN_OR_INCOMPATIBLE_TYPES_IN_THE_LIST,
                this);
        }
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        optimizeChildren(e, tableContext, fieldContext);
        return super.optimize(e, tableContext, fieldContext);
    }

    @Override
    public boolean canCastTo(final DataType dataType, final Locale locale) {
        return isChildrenCanCastTo(dataType, locale);
    }

    @Override
    public Priority getPriority() {
        return Priority.COMMA_OPERATOR;
    }
}
