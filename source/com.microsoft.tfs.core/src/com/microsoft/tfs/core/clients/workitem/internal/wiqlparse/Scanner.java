// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.List;
import java.util.Stack;

public class Scanner {
    private final List<Node> tokens;

    private int currentPosition = 0;

    public Scanner(final List<Node> tokens) {
        this.tokens = tokens;
    }

    public NodeSelect scan() {
        return scanSelect();
    }

    public void checkTail() {
        final int num1 = currentPosition;
        final NodeItem item1 = nextToken();
        if ((item1 != null) && (item1.getNodeType() != NodeType.OPERATION || item1.getValue().length() != 0)) {
            currentPosition = num1;
            throwSyntaxError(SyntaxError.EXPECTING_END_OF_STRING);
        }
    }

    private NodeSelect scanSelect() {
        final Node node1 = ensureToken(NodeType.NAME, "select", SyntaxError.EXPECTING_SELECT); //$NON-NLS-1$
        final NodeSelect select1 = new NodeSelect();
        if (scanToken(NodeType.OPERATION, "*") != null) //$NON-NLS-1$
        {
            select1.setFields(null);
        } else {
            select1.setFields(scanFieldNameList(false, NodeType.FIELD_LIST));
        }

        while (true) {
            if (scanToken(NodeType.NAME, "from") != null) //$NON-NLS-1$
            {
                if (select1.getFrom() != null) {
                    --currentPosition;
                    throwSyntaxError(SyntaxError.DUPLICATE_FROM);
                }
                select1.setFrom(scanTableName());
                continue;
            }

            if (scanToken(NodeType.NAME, "where") != null) //$NON-NLS-1$
            {
                if (select1.getWhere() != null) {
                    --currentPosition;
                    throwSyntaxError(SyntaxError.DUPLICATE_WHERE);
                }
                select1.setWhere(scanWhere(select1.getFrom()));
                continue;
            }

            if (scanToken(NodeType.NAME, "group") != null) //$NON-NLS-1$
            {
                ensureToken(NodeType.NAME, "by", SyntaxError.EXPECTING_BY); //$NON-NLS-1$
                if (select1.getGroupBy() != null) {
                    --currentPosition;
                    throwSyntaxError(SyntaxError.DUPLICATE_GROUP_BY);
                }
                select1.setGroupBy(scanFieldNameList(false, NodeType.GROUP_FIELD_LIST));
                continue;
            }

            if (scanToken(NodeType.NAME, "order") != null) //$NON-NLS-1$
            {
                ensureToken(NodeType.NAME, "by", SyntaxError.EXPECTING_BY); //$NON-NLS-1$
                if (select1.getOrderBy() != null) {
                    --currentPosition;
                    throwSyntaxError(SyntaxError.DUPLICATE_ORDER_BY);
                }
                select1.setOrderBy(scanFieldNameList(true, NodeType.ORDER_FIELD_LIST));
                continue;
            }

            if (scanToken(NodeType.NAME, "asof") != null) //$NON-NLS-1$
            {
                if (select1.getAsOf() != null) {
                    --currentPosition;
                    throwSyntaxError(SyntaxError.DUPLICATE_AS_OF);
                }
                select1.setAsOf(scanValue(select1.getFrom()));
                continue;
            }

            if (scanToken(NodeType.NAME, "mode") != null) //$NON-NLS-1$
            {
                if (select1.getMode() != null) {
                    currentPosition--;
                    throwSyntaxError(SyntaxError.DUPLICATE_MODE);
                }
                select1.setMode(ScanMode());
                continue;
            }

            select1.setStartOffset(node1.getStartOffset());
            select1.setEndOffset(((NodeItem) tokens.get(tokens.size() - 1)).getEndOffset());
            return select1;
        }
    }

    private Node scanWhere(final NodeTableName table) {
        return scanCondition4(table);
    }

    private Node scanCondition4(final NodeTableName table) {
        Node node1 = scanCondition3(table);
        NodeOrOperator operator1 = null;
        while (true) {
            Node node2 = scanToken(NodeType.NAME, "or"); //$NON-NLS-1$
            if (node2 == null) {
                node2 = scanToken(NodeType.OPERATION, "||"); //$NON-NLS-1$
            }
            if (node2 == null) {
                break;
            }
            if (operator1 == null) {
                operator1 = new NodeOrOperator();
                operator1.add(node1);
            }
            operator1.add(scanCondition3(table));
        }
        if (operator1 != null) {
            operator1.setStartOffset(operator1.getItem(0).getStartOffset());
            operator1.setEndOffset(operator1.getItem(operator1.getCount() - 1).getEndOffset());
            node1 = operator1;
        }
        return node1;
    }

    private Node scanCondition3(final NodeTableName table) {
        Node node1 = scanCondition2(table);
        NodeAndOperator operator1 = null;
        while (true) {
            Node node2 = scanToken(NodeType.NAME, "and"); //$NON-NLS-1$
            if (node2 == null) {
                node2 = scanToken(NodeType.OPERATION, "&&"); //$NON-NLS-1$
            }
            if (node2 == null) {
                break;
            }
            if (operator1 == null) {
                operator1 = new NodeAndOperator();
                operator1.add(node1);
            }
            operator1.add(scanCondition2(table));
        }
        if (operator1 != null) {
            operator1.setStartOffset(operator1.getItem(0).getStartOffset());
            operator1.setEndOffset(operator1.getItem(operator1.getCount() - 1).getEndOffset());
            node1 = operator1;
        }
        return node1;
    }

    private Node scanCondition2(final NodeTableName table) {
        Node node1 = scanToken(NodeType.NAME, "not"); //$NON-NLS-1$
        if (node1 == null) {
            node1 = scanToken(NodeType.OPERATION, "!"); //$NON-NLS-1$
        }
        if (node1 != null) {
            final Node node2 = scanCondition2(table);
            final Node node3 = new NodeNotOperator(node2);
            node3.setStartOffset(node1.getStartOffset());
            node3.setEndOffset(node2.getEndOffset());
            return node3;
        }
        node1 = scanToken(NodeType.NAME, "ever"); //$NON-NLS-1$
        if (node1 != null) {
            final Node node4 = scanCondition2(table);
            final Node node5 = new NodeEverOperator(node4);
            node5.setStartOffset(node1.getStartOffset());
            node5.setEndOffset(node4.getEndOffset());
            return node5;
        }
        node1 = scanToken(NodeType.NAME, "never"); //$NON-NLS-1$
        if (node1 != null) {
            final Node node6 = scanCondition2(table);
            final Node node7 = new NodeNotOperator(new NodeEverOperator(node6));
            node7.setStartOffset(node1.getStartOffset());
            node7.setEndOffset(node6.getEndOffset());
            return node7;
        }
        return scanCondition1(table);
    }

    private Node scanCondition1(final NodeTableName table) {
        final Node node1 = scanToken(NodeType.OPERATION, "("); //$NON-NLS-1$
        if (node1 != null) {
            final Node node2 = scanWhere(table);
            final Node node3 = ensureToken(NodeType.OPERATION, ")", SyntaxError.EXPECTING_LEFT_BRACKET); //$NON-NLS-1$
            node2.setStartOffset(node1.getStartOffset());
            node2.setEndOffset(node3.getEndOffset());
            node2.setHasParantheses(true);
            return node2;
        }
        final NodeCondition condition1 = new NodeCondition();
        Node node4 = condition1;
        condition1.setLeft(scanFieldName(table));
        final Stack<NodeType> stack1 = new Stack<NodeType>();

        while (true) {
            if (scanToken(NodeType.NAME, "not") != null) //$NON-NLS-1$
            {
                stack1.push(NodeType.NOT);
                continue;
            }

            if (scanToken(NodeType.NAME, "ever") != null) //$NON-NLS-1$
            {
                stack1.push(NodeType.EVER);
                continue;
            }

            if (scanToken(NodeType.NAME, "never") != null) //$NON-NLS-1$
            {
                stack1.push(NodeType.NOT);
                stack1.push(NodeType.EVER);
                continue;
            }

            break;
        }

        while (true) {
            if (stack1.size() <= 0) {
                condition1.setCondition(scanConditionOperator(node4 != condition1));
                if (condition1.getCondition() == Condition.NONE) {
                    throwSyntaxError(SyntaxError.EXPECTING_COMPARISON_OPERATOR);
                }
                if (condition1.getCondition() == Condition.GROUP) {
                    condition1.setRight(scanValue(table));
                } else if (condition1.getCondition() == Condition.IN) {
                    final Node node5 = ensureToken(NodeType.OPERATION, "(", SyntaxError.EXPECTING_LEFT_BRACKET); //$NON-NLS-1$
                    final Node node6 = scanValueList(table);
                    final Node node7 = ensureToken(NodeType.OPERATION, ")", SyntaxError.EXPECTING_RIGHT_BRACKET); //$NON-NLS-1$
                    node6.setStartOffset(node5.getStartOffset());
                    node6.setEndOffset(node7.getEndOffset());
                    condition1.setRight(node6);
                } else if (condition1.getCondition() == Condition.UNDER) {
                    condition1.setRight(scanValue(table));
                } else {
                    Node node8 = TryScanFieldName();
                    if (node8 == null) {
                        node8 = TryScanExpression();
                    }
                    if (node8 != null) {
                        condition1.setRight(node8);
                    } else {
                        throwSyntaxError(SyntaxError.EXPECTING_FIELD_OR_EXPRESSION);
                    }
                }
                node4.setStartOffset(condition1.getLeft().getStartOffset());
                node4.setEndOffset(condition1.getRight().getEndOffset());
                return node4;
            }

            final NodeType nodeType = stack1.pop();
            if (nodeType == NodeType.NOT) {
                node4 = new NodeNotOperator(node4);
            } else if (nodeType == NodeType.EVER) {
                node4 = new NodeEverOperator(node4);
            }
        }
    }

    private Node scanValueList(final NodeTableName table) {
        final NodeValueList list1 = new NodeValueList();
        do {
            list1.add(scanExpression(table));
        } while (scanToken(NodeType.OPERATION, ",") != null); //$NON-NLS-1$
        list1.setStartOffset(list1.getItem(0).getStartOffset());
        list1.setEndOffset(list1.getItem(list1.getCount() - 1).getEndOffset());
        return list1;
    }

    private Node scanValue(final NodeTableName table) {
        final Node node = TryScanSingleValue();
        if (node == null) {
            throwSyntaxError(SyntaxError.EXPECTING_VALUE);
        }
        return node;
    }

    private Node scanExpression(final NodeTableName nodeTable) {
        final Node node = TryScanExpression();
        if (node == null) {
            throwSyntaxError(SyntaxError.EXPECTING_VALUE);
            return null;
        }
        return node;
    }

    private Condition scanConditionOperator(final boolean afterNotOrEver) {
        final int num1 = currentPosition;
        final NodeItem item1 = nextToken();
        if ((item1 != null)
            && ((item1.getNodeType() == NodeType.NAME)
                || ((item1.getNodeType() == NodeType.OPERATION) && !afterNotOrEver))) {
            Condition condition1 = ConditionalOperators.find(item1.getValue());

            if (condition1 == Condition.IN && scanToken(NodeType.NAME, "group") != null) //$NON-NLS-1$
            {
                condition1 = Condition.GROUP;
            } else if (condition1 == Condition.CONTAINS && scanToken(NodeType.NAME, "words") != null) //$NON-NLS-1$
            {
                condition1 = Condition.CONTAINS_WORDS;
            }

            if (condition1 != Condition.NONE) {
                return condition1;
            }
        }
        currentPosition = num1;
        if (!afterNotOrEver) {
            return Condition.NONE;
        }
        return Condition.EQUALS;
    }

    private NodeTableName scanTableName() {
        final int savedCurrentPosition = currentPosition;
        final NodeItem tokenNode = nextToken();
        if (tokenNode != null && tokenNode.getNodeType() == NodeType.NAME) {
            return new NodeTableName((NodeName) tokenNode);
        }
        currentPosition = savedCurrentPosition;
        throwSyntaxError(SyntaxError.EXPECTING_TABLE_NAME);
        return null;
    }

    // direction is true if each field name in the field name list can be
    // suffixed by an optional
    // direction indicator - asc or desc
    private NodeFieldList scanFieldNameList(final boolean direction, final NodeType nodeType) {
        final NodeFieldList fieldListNode = new NodeFieldList(nodeType);
        while (true) {
            final NodeFieldName fieldNameNode = scanFieldName(null);
            if (direction) {
                if (scanToken(NodeType.NAME, "asc") != null) //$NON-NLS-1$
                {
                    fieldNameNode.setDirection(Direction.ASCENDING);
                } else if (scanToken(NodeType.NAME, "desc") != null) //$NON-NLS-1$
                {
                    fieldNameNode.setDirection(Direction.DESCENDING);
                }
            }
            fieldListNode.add(fieldNameNode);

            /*
             * if the next token is not a comma operator, then the field list is
             * finished
             */
            if (scanToken(NodeType.OPERATION, ",") == null) //$NON-NLS-1$
            {
                if (fieldListNode.getCount() != 0) {
                    /*
                     * if the field list node is not empty, set it's start and
                     * end offset by using the starting and ending node in the
                     * list
                     */
                    fieldListNode.setStartOffset(fieldListNode.getItem(0).getStartOffset());
                    fieldListNode.setEndOffset(fieldListNode.getItem(fieldListNode.getCount() - 1).getEndOffset());
                }
                return fieldListNode;
            }
        }
    }

    private NodeFieldName scanFieldName(final NodeTableName table) {
        final Node node = TryScanFieldName();
        if (node != null) {
            return (NodeFieldName) node;
        }
        throwSyntaxError(SyntaxError.EXPECTING_FIELD_NAME);
        return null;
    }

    private NodeMode ScanMode() {
        ensureToken(NodeType.OPERATION, "(", SyntaxError.EXPECTING_LEFT_BRACKET); //$NON-NLS-1$
        final NodeMode mode = new NodeMode();
        do {
            final int pos = currentPosition;
            final NodeItem item = nextToken();
            if ((item == null) || !(item.getNodeType().equals(NodeType.NAME))) {
                currentPosition = pos;
                throwSyntaxError(SyntaxError.EXPECTING_MODE);
                return null;
            }
            mode.add(item);
        } while (scanToken(NodeType.OPERATION, ",") != null); //$NON-NLS-1$
        ensureToken(NodeType.OPERATION, ")", SyntaxError.EXPECTING_RIGHT_BRACKET); //$NON-NLS-1$
        mode.setStartOffset(mode.getItem(0).getStartOffset());
        mode.setEndOffset(mode.getItem(mode.getCount() - 1).getEndOffset());
        return mode;
    }

    private NodeItem ensureToken(final NodeType type, final String str, final SyntaxError syntaxError) {
        final NodeItem tokenNode = scanToken(type, str);
        if (tokenNode == null) {
            throwSyntaxError(syntaxError);
        }
        return tokenNode;
    }

    private void throwSyntaxError(final SyntaxError error) {
        NodeItem causeNode = null;
        if (currentPosition < tokens.size()) {
            causeNode = (NodeItem) tokens.get(currentPosition);
        }
        throw new SyntaxException(causeNode, error);
    }

    private NodeItem scanToken(final NodeType type, final String str) {
        final int savedCurrentPosition = currentPosition;
        final NodeItem tokenNode = nextToken();
        if (tokenNode != null && tokenNode.getNodeType() == type && str.equalsIgnoreCase(tokenNode.getValue())) {
            return tokenNode;
        }
        currentPosition = savedCurrentPosition;
        return null;
    }

    private NodeItem nextToken() {
        if (currentPosition < tokens.size()) {
            return (NodeItem) tokens.get(currentPosition++);
        }
        return null;
    }

    private Node TryScanExpression() {
        int num;
        Node node = TryScanSingleValue();
        if (node == null) {
            return null;
        }
        while (true) {
            num = currentPosition;
            final NodeItem item = nextToken();
            if (item != null) {
                Arithmetic none = Arithmetic.NONE;
                Node node2 = null;
                if (item.getNodeType() == NodeType.NUMBER) {
                    none = ArithmeticalOperators.find(item.getValue().substring(0, 1));
                    if (none != Arithmetic.NONE) {
                        item.setValue(item.getValue().substring(1));
                        node2 = item;
                    }
                } else if (item.getNodeType() == NodeType.OPERATION) {
                    none = ArithmeticalOperators.find(item.getValue());
                    if (none != Arithmetic.NONE) {
                        node2 = TryScanSingleValue();
                    }
                }
                if ((none != Arithmetic.NONE) && (node2 != null)) {
                    final NodeArithmetic arithmetic2 = new NodeArithmetic();
                    arithmetic2.setArithmetic(none);
                    arithmetic2.setLeft(node);
                    arithmetic2.setRight(node2);
                    arithmetic2.setStartOffset(arithmetic2.getLeft().getStartOffset());
                    arithmetic2.setEndOffset(arithmetic2.getRight().getEndOffset());
                    node = arithmetic2;
                    continue;
                }
            }
            currentPosition = num;
            return node;
        }
    }

    private Node TryScanFieldName() {
        Node node = null;
        final int pos = currentPosition;
        final Node node2 = nextToken();
        if ((node2 != null) && (node2.getNodeType() == NodeType.NAME)) {
            final NodeItem item = scanToken(NodeType.OPERATION, "."); //$NON-NLS-1$
            Node node3 = null;
            if (item != null) {
                node3 = nextToken();
                if ((node3 != null) && (node3.getNodeType() == NodeType.NAME)) {
                    node = new NodeFieldName((NodeName) node2, (NodeName) node3);
                }
            } else {
                node = new NodeFieldName((NodeName) node2);
            }
        }
        if (node == null) {
            currentPosition = pos;
            return null;
        }
        return node;
    }

    private Node TryScanSingleValue() {
        final int pos = currentPosition;
        final Node node2 = nextToken();
        if ((node2 != null)
            && (((node2.getNodeType() == NodeType.NUMBER) || (node2.getNodeType() == NodeType.STRING))
                || ((node2.getNodeType() == NodeType.VARIABLE) || (node2.getNodeType() == NodeType.BOOL_VALUE)))) {
            return node2;
        }
        currentPosition = pos;
        return null;
    }

}
