// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class NodeSelect extends NodeList {
    public NodeSelect() {
        /*
         * a select node is a node list with (up to) 6 child nodes 0: the select
         * field list (NodeFieldList) 1: the "from" table name (NodeTableName)
         * 2: the where clause (Node) 3: group by (NodeFieldList) 4: the order
         * by clause (NodeFieldList) 5: the as of date (Node)
         */
        super(NodeType.SELECT, 7);
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        Tools.ensureSyntax(getFrom() != null, SyntaxError.FROM_IS_NOT_SPECIFIED, null);
        getFrom().bind(e, null, null);
        Tools.ensureSyntax(getFrom().getNodeType() == NodeType.TABLE_NAME, SyntaxError.EXPECTING_TABLE_NAME, getFrom());
        if (getMode() != null) {
            getMode().bind(e, getFrom(), null);
        }
        if (getFields() != null) {
            getFields().bind(e, getFrom(), null);
            Tools.ensureSyntax(
                getFields().getNodeType() == NodeType.FIELD_LIST,
                SyntaxError.EXPECTING_FIELD_LIST,
                getFields());
        }
        if (getGroupBy() != null) {
            getGroupBy().bind(e, getFrom(), null);
            Tools.ensureSyntax(
                getGroupBy().getNodeType() == NodeType.GROUP_FIELD_LIST,
                SyntaxError.EXPECTING_FIELD_LIST,
                getGroupBy());
        }
        if (getOrderBy() != null) {
            getOrderBy().bind(e, getFrom(), null);
            Tools.ensureSyntax(
                getOrderBy().getNodeType() == NodeType.ORDER_FIELD_LIST,
                SyntaxError.EXPECTING_FIELD_LIST,
                getOrderBy());
        }
        if (getWhere() != null) {
            getWhere().bind(e, getFrom(), null);
            Tools.ensureSyntax(getWhere().getDataType() == DataType.BOOL, SyntaxError.EXPECTING_CONDITION, getWhere());
        }
        if (getAsOf() != null) {
            getAsOf().bind(e, getFrom(), null);
            if (e != null) {
                Tools.ensureSyntax(
                    getAsOf().isScalar() && getAsOf().canCastTo(DataType.DATE, e.getLocale()),
                    SyntaxError.EXPECTING_DATE,
                    getAsOf());
            }
        }
        super.bind(e, tableContext, fieldContext);
    }

    /**
     * Get independent groups of 'where' clause. Each of them has a different
     * prefix inside.
     */
    public Map<String, NodeAndOperator> getWhereGroups() {
        final Map<String, NodeAndOperator> whereGroups = new HashMap<String, NodeAndOperator>();

        Node node = getWhere();

        if (node instanceof NodeAndOperator) {
            final NodeAndOperator and = (NodeAndOperator) node;

            for (int i = 0; i < and.getCount(); i++) {
                node = and.getItem(i);

                // get it's prefix
                final String prefix = node.checkPrefix(null);

                if (prefix != null) {
                    // Do we have this group already?
                    NodeAndOperator list = whereGroups.get(prefix);
                    if (list != null) {
                        list.add(node);
                    } else {
                        list = new NodeAndOperator();
                        list.add(node);
                        whereGroups.put(prefix, list);
                    }
                }
            }
        } else if (node != null) {
            // Just one element
            final String prefix = node.checkPrefix(null);

            if (prefix != null) {
                final NodeAndOperator list = new NodeAndOperator();
                list.add(node);
                whereGroups.put(prefix, list);
            }
        }

        // remove AND operator with single AND operator inside
        final String[] keys = whereGroups.keySet().toArray(new String[whereGroups.keySet().size()]);
        for (int i = 0; i < keys.length; i++) {
            final NodeAndOperator a = whereGroups.get(keys[i]);
            if ((a.getCount() == 1) && (a.getItem(0) instanceof NodeAndOperator)) {
                whereGroups.put(keys[i], (NodeAndOperator) a.getItem(0));
            }
        }

        return whereGroups;
    }

    @Override
    public Node optimize(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (getWhere() != null) {
            setWhere(getWhere().optimize(e, getFrom(), null));
        }
        if (getAsOf() != null) {
            setAsOf(getAsOf().optimize(e, null, null));
        }
        return super.optimize(e, tableContext, fieldContext);
    }

    @Override
    public void appendTo(final StringBuffer b) {
        final String[] strArray = new String[] {
            "{0}", //$NON-NLS-1$
            " from {0}", //$NON-NLS-1$
            " where {0}", //$NON-NLS-1$
            "group by {0}", //$NON-NLS-1$
            " order by {0}", //$NON-NLS-1$
            " asof {0}", //$NON-NLS-1$
            " mode ({0})" //$NON-NLS-1$
        };
        b.append("select "); //$NON-NLS-1$
        for (int i = 0; i < strArray.length; i++) {
            Node objA = getItem(i);
            if ((objA == getWhere()) && (objA instanceof NodeBoolConst) && ((NodeBoolConst) objA).getValue()) {
                objA = null;
            }
            if (objA != null) {
                final String str = objA.toString();
                if (str != null && !str.equals("")) //$NON-NLS-1$
                {
                    b.append(new MessageFormat(strArray[i]).format(new Object[] {
                        str
                    }));
                }
            }
        }
    }

    public Node getAsOf() {
        return getItem(5);
    }

    public void setAsOf(final Node node) {
        setItem(5, node);
    }

    @Override
    public DataType getDataType() {
        return DataType.VOID;
    }

    public NodeFieldList getFields() {
        return (NodeFieldList) getItem(0);
    }

    public void setFields(final NodeFieldList fields) {
        setItem(0, fields);
    }

    public NodeTableName getFrom() {
        return (NodeTableName) getItem(1);
    }

    public void setFrom(final NodeTableName from) {
        setItem(1, from);
    }

    public NodeFieldList getGroupBy() {
        return (NodeFieldList) getItem(3);
    }

    public void setGroupBy(final NodeFieldList groupBy) {
        setItem(3, groupBy);
    }

    public NodeFieldList getOrderBy() {
        return (NodeFieldList) getItem(4);
    }

    public void setOrderBy(final NodeFieldList orderBy) {
        setItem(4, orderBy);
    }

    @Override
    public Priority getPriority() {
        return Priority.SELECT_OPERATOR;
    }

    public Node getWhere() {
        return getItem(2);
    }

    public void setWhere(final Node where) {
        setItem(2, where);
    }

    public NodeMode getMode() {
        return (NodeMode) getItem(6);
    }

    public void setMode(final NodeMode mode) {
        setItem(6, mode);
    }

}
