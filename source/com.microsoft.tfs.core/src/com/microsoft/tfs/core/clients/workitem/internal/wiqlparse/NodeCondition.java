// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NodeCondition extends NodeList {
    private Condition condition;
    private final Log log = LogFactory.getLog(NodeCondition.class);

    public NodeCondition() {
        super(NodeType.FIELD_CONDITION, 2);
    }

    public NodeCondition(final Condition condition, final NodeFieldName left, final Node right) {
        super(NodeType.FIELD_CONDITION, 2);
        this.condition = condition;
        setLeft(left);
        setRight(right);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        getLeft().bind(e, tableContext, fieldContext);
        getRight().bind(e, tableContext, fieldContext);
        Tools.ensureSyntax(
            getRight().isConst() || (getRight() instanceof NodeFieldName),
            SyntaxError.INVALID_RIGHT_EXPRESSION_IN_CONDITION,
            getRight());

        if (log.isDebugEnabled()) {
            log.debug(
                MessageFormat.format("right - {0} left - {1}", getRight().getDataType(), getLeft().getDataType())); //$NON-NLS-1$
        }

        if (e != null) {
            Tools.ensureSyntax(
                getRight().canCastTo(getLeft().getDataType(), e.getLocale()),
                SyntaxError.INCOMPATIBLE_CONDITION_PARTS_TYPE,
                this);
        }
        if (e != null && (getCondition() == Condition.CONTAINS || getCondition() == Condition.CONTAINS_WORDS)) {
            Tools.ensureSyntax(
                getLeft().getDataType() == DataType.STRING,
                SyntaxError.CONTAINS_WORKS_FOR_STRINGS_ONLY,
                this);
        }
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        setLeft((NodeFieldName) getLeft().optimize(e, tableContext, fieldContext));
        setRight(getRight().optimize(e, tableContext, getLeft()));
        return super.optimize(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        super.appendChildren(b, " " + ConditionalOperators.getString(condition) + " "); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(final Condition c) {
        condition = c;
    }

    @Override
    public DataType getDataType() {
        return DataType.BOOL;
    }

    public NodeFieldName getLeft() {
        return (NodeFieldName) getItem(0);
    }

    public void setLeft(final NodeFieldName left) {
        setItem(0, left);
    }

    @Override
    public Priority getPriority() {
        return Priority.CONDITIONAL_OPERATOR;
    }

    public Node getRight() {
        return getItem(1);
    }

    public void setRight(final Node right) {
        setItem(1, right);
    }
}
