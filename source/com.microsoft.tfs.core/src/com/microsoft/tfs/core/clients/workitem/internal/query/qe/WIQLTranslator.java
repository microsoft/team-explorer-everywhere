// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.exceptions.FieldDefinitionNotExistException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.ArithmeticalOperators;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Condition;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.ConditionalOperators;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime.UncheckedParseException;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Node;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeArithmetic;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeCondition;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeEverOperator;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeFieldName;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeItem;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeNotOperator;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeSelect;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeString;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeType;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeValueList;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Parser;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnection;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryConnectionType;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRow;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRowCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * This class is extremely similar to:
 * Microsoft.TeamFoundation.WorkItemTracking.Controls.FilterGridConverter
 */
public class WIQLTranslator {
    private static final char CLOSE_PAREN = ')';
    private static final String CLOSE_PAREN_STRING = ")"; //$NON-NLS-1$
    private static final char OPEN_PAREN = '(';
    private static final String OPEN_PAREN_STRING = "("; //$NON-NLS-1$
    private static final String MACRO_START = "@"; //$NON-NLS-1$
    private static final String NEXT_ITEM = ", {0}"; //$NON-NLS-1$
    private static final String SELECT = "SELECT"; //$NON-NLS-1$
    private static final String SELECT_ID_FROM_WHERE = "SELECT ID FROM WorkItems WHERE "; //$NON-NLS-1$
    private static final String SINGLE_QUOTE = "'"; //$NON-NLS-1$
    private static final String TWO_SINGLE_QUOTES = "''"; //$NON-NLS-1$
    private static final String OPERATOR_IN_NEXT_ITEM = "{0} {1}"; //$NON-NLS-1$

    private final QEQuery query;
    private final WIQLTranslatorFieldService fieldService;

    private String parsedWiqlExpression;

    public WIQLTranslator(final QEQuery query, final WIQLTranslatorFieldService fieldService) {
        this.query = query;
        this.fieldService = fieldService;
    }

    public void fromWIQL(String wiqlString, final boolean localizeFieldNames) {
        if (wiqlString == null) {
            throw new IllegalArgumentException("wiqlString cannot be null"); //$NON-NLS-1$
        }

        if (!wiqlString.toUpperCase().startsWith(SELECT)) {
            if (wiqlString.trim().length() > 0) {
                wiqlString = SELECT_ID_FROM_WHERE + wiqlString;
            } else {
                return;
            }
        }

        final NodeSelect select1 = Parser.parseSyntax(wiqlString);
        parsedWiqlExpression = wiqlString;
        final Map<NodeCondition, Integer> hashtable1 = new HashMap<NodeCondition, Integer>();
        if (select1.getWhere() != null && select1.getWhere().getCount() > 0) {
            parseWIQLNode(select1.getWhere(), "", hashtable1, localizeFieldNames); //$NON-NLS-1$
            processChildGroups(select1.getWhere(), hashtable1);
        }

        if (query.getQueryType() == QueryType.LIST) {
            Check.isTrue(
                query.getTargetRowCollection().getRowCount() == 0,
                "query.getTargetRowCollection().getRowCount() == 0"); //$NON-NLS-1$

            // Set a default target node in the query which will only be used if
            // the user switches to a linked or tree query.
            final QEQueryRow newRow = query.getTargetRowCollection().addRow();
            newRow.setLogicalOperator(""); //$NON-NLS-1$
            newRow.setFieldName(getLocalizedFieldName(CoreFieldReferenceNames.WORK_ITEM_TYPE));
            newRow.setOperator(getLocalizedOperator(WIQLOperators.EQUAL_TO));
            newRow.setValue(getLocalizedOperator(WIQLOperators.SPECIAL_ANY));
        }
    }

    public String asWIQL(final boolean localizeFieldNames) {
        final StringBuffer sb = new StringBuffer();

        if (query.getQueryType() == QueryType.LIST) {
            sb.append(rowCollectionAsWIQLCondition(query.getSourceRowCollection(), null, localizeFieldNames));
        } else {
            sb.append(rowCollectionAsWIQLCondition(query.getSourceRowCollection(), "Source", localizeFieldNames)); //$NON-NLS-1$
            sb.append(" "); //$NON-NLS-1$
            sb.append(WIQLOperators.getInvariantOperator("AND")); //$NON-NLS-1$
            sb.append(" "); //$NON-NLS-1$

            if (query.getQueryType() == QueryType.ONE_HOP) {
                sb.append(linkQueryTypeAsWIQLCondition());
            } else {
                sb.append(treeQueryTypeAsWIQLCondition());
            }

            final String targetClause = rowCollectionAsWIQLCondition(
                query.getTargetRowCollection(),
                "Target", //$NON-NLS-1$
                localizeFieldNames);

            if (targetClause.length() > 0) {
                sb.append(" AND "); //$NON-NLS-1$
                sb.append(targetClause);
            }
        }

        return sb.toString().trim();
    }

    private String treeQueryTypeAsWIQLCondition() {
        final StringBuffer sb = new StringBuffer();

        sb.append("([System.Links.LinkType] = '"); //$NON-NLS-1$
        sb.append(query.getTreeQueryLinkType());
        sb.append("')"); //$NON-NLS-1$

        return sb.toString();
    }

    private String linkQueryTypeAsWIQLCondition() {
        final StringBuffer sb = new StringBuffer();
        sb.append("([System.Links.LinkType] "); //$NON-NLS-1$

        final String[] referenceNames = query.getLinkQueryLinkTypes();
        if (!query.getUseSelectedLinkTypes() || referenceNames.length == 0) {
            sb.append("<> \'\'"); //$NON-NLS-1$
        } else if (referenceNames.length == 1) {
            sb.append("= '"); //$NON-NLS-1$
            sb.append(referenceNames[0]);
            sb.append("'"); //$NON-NLS-1$
        } else {
            sb.append("IN ("); //$NON-NLS-1$
            for (int i = 0; i < referenceNames.length; i++) {
                if (i > 0) {
                    sb.append(","); //$NON-NLS-1$
                }
                sb.append("\'"); //$NON-NLS-1$
                sb.append(referenceNames[i]);
                sb.append("\'"); //$NON-NLS-1$
            }
            sb.append(")"); //$NON-NLS-1$
        }

        sb.append(")"); //$NON-NLS-1$
        return sb.toString();
    }

    private String rowCollectionAsWIQLCondition(
        final QEQueryRowCollection rowCollection,
        final String fieldNamePrefix,
        final boolean localizeFieldNames) {
        final StringBuffer sb = new StringBuffer();

        for (int num1 = 0; num1 < rowCollection.getRowCount(); num1++) {
            if (!skipRow(num1, rowCollection)) {
                int num2 = 0;
                int num3 = 0;
                final int num4 = rowCollection.getGrouping().getMaxDepth();
                for (int num5 = 1; num5 <= num4; num5++) {
                    final QEQueryConnection connection = rowCollection.getGrouping().getConnection(num5, num1);
                    if (connection.getType() == QEQueryConnectionType.UP) {
                        num3++;
                    } else if (connection.getType() == QEQueryConnectionType.DOWN) {
                        num2++;
                    }
                }

                // Retrieve the clause components from the row.
                final QEQueryRow row = rowCollection.getRow(num1);
                String fieldName = row.getFieldName();
                String invariantOperator = getInvariantOperator(row.getOperator());
                final String invariantLogicalOperator = getInvariantOperator(row.getLogicalOperator());
                String fieldValue = row.getValue();

                // Handle the special case of "field equals [any]".
                if (WIQLAdapter.fieldSupportsAnySyntax(query.getWorkItemClient(), fieldName)
                    && isAnySyntax(invariantOperator, fieldValue)) {
                    invariantOperator = WIQLOperators.NOT_EQUAL_TO;
                    fieldValue = ""; //$NON-NLS-1$
                }

                // Store "Contains Words" as "Contains" for Text fields for
                // back-compat with TFS 2010 and earlier. For Text fields,
                // "Contains Words" and "Contains" are equivalent on the server.
                if (WIQLOperators.isContainsWordsOperator(invariantOperator)) {
                    final FieldDefinition fd = tryGetFieldDefinition(fieldName);
                    if (fd != null && fd.isLongText()) {
                        invariantOperator = convertToContains(invariantOperator);
                    }
                }

                // Decorate the field value for WIQL.
                if (WIQLOperators.isFieldNameOperator(invariantOperator) && fieldValue != null) {
                    String referenceName;
                    try {
                        referenceName = getInvariantFieldName(fieldValue);
                    } catch (final FieldDefinitionNotExistException e) {
                        referenceName = fieldValue;
                    }

                    final StringBuffer sbField = new StringBuffer();
                    sbField.append("["); //$NON-NLS-1$
                    sbField.append(referenceName);
                    sbField.append("]"); //$NON-NLS-1$
                    fieldValue = sbField.toString();
                } else {
                    fieldValue = getInvariantFieldValue(fieldValue, row);
                }

                if (localizeFieldNames) {
                    fieldName = getLocalizedFieldName(fieldName);
                } else {
                    fieldName = getInvariantFieldName(fieldName);
                }

                if (invariantLogicalOperator.length() > 0) {
                    sb.append(" "); //$NON-NLS-1$
                    sb.append(invariantLogicalOperator);
                    sb.append(" "); //$NON-NLS-1$
                }

                for (int i = 0; i < num2; i++) {
                    sb.append(OPEN_PAREN);
                }

                if (fieldNamePrefix != null) {
                    sb.append("["); //$NON-NLS-1$
                    sb.append(fieldNamePrefix);
                    sb.append("]."); //$NON-NLS-1$
                }

                sb.append("["); //$NON-NLS-1$
                sb.append(fieldName);
                sb.append("] "); //$NON-NLS-1$
                sb.append(invariantOperator);
                sb.append(" "); //$NON-NLS-1$
                sb.append(fieldValue);

                for (int i = 0; i < num3; i++) {
                    sb.append(CLOSE_PAREN);
                }
            }
        }

        return sb.toString();
    }

    private String getInvariantFieldValue(final Object valueObject, final QEQueryRow row) {
        /*
         * In MS code: string text1 = string.Format("{0}", valueObject);
         */
        final String text1 = (valueObject == null ? "" : valueObject.toString()); //$NON-NLS-1$

        final String text2 = row.getOperator();
        if (!getInvariantOperator(text2).equals(WIQLOperators.IN)) {
            return getInvariantFieldValueFromString(text1, row);
        }

        /*
         * In MS code:
         *
         * string[] textArray1 = new string[] {
         * CultureInfo.CurrentCulture.TextInfo.ListSeparator }; string[]
         * textArray2 = text1.Split(textArray1,
         * StringSplitOptions.RemoveEmptyEntries);
         */
        /*
         * I18N: need to use a specified Locale's list separator
         */
        final String[] textArray2 = splitRemovingEmptyEntries(text1, ","); //$NON-NLS-1$
        final StringBuffer builder1 = new StringBuffer();
        builder1.append(OPEN_PAREN);
        for (int i = 0; i < textArray2.length; i++) {
            if (i == 0) {
                builder1.append(getInvariantFieldValueFromString(textArray2[i].trim(), row));
            } else {
                builder1.append(MessageFormat.format(NEXT_ITEM, new Object[] {
                    getInvariantFieldValueFromString(textArray2[i].trim(), row)
                }));
            }
        }
        builder1.append(CLOSE_PAREN);
        return builder1.toString();
    }

    private String getInvariantFieldValueFromString(String value, final QEQueryRow row) {
        final boolean flag1 = value.startsWith(MACRO_START);
        final boolean flag2 = !flag1 && isDateTimeField(row);
        final boolean flag3 = !flag1 && isDecimalField(row);
        final boolean flag4 = !flag1 && isStringField(row);
        final boolean flag5 = (value.length() == 0);
        if (flag1) {
            // The @today macro can have additional text such as "@today - 1".
            final String localized = WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_TODAY);
            if (value.startsWith(localized)) {
                value = WIQLOperators.MACRO_TODAY + value.substring(localized.length());
            } else {
                value = getInvariantOperator(value);
            }
        } else if (flag2 && !flag5) {
            /*
             * In MS code: value = DateTime.Parse(value,
             * CultureInfo.CurrentCulture).ToString("o",
             * CultureInfo.InvariantCulture);
             */
            /*
             * I18N: need to use a specified Locale
             */
            final Date date = DateTime.parse(value, Locale.getDefault(), TimeZone.getDefault());
            value = DateTime.formatRoundTripUnspecified(date, TimeZone.getDefault());
        } else if (flag3 && !flag5) {
            /*
             * In MS code: value = double.Parse(value,
             * CultureInfo.CurrentCulture
             * ).ToString(CultureInfo.InvariantCulture);
             */
            /*
             * I18N: need to use a specified Locale
             */
            Number number;
            try {
                number = NumberFormat.getInstance().parse(value);
                value = number.toString();
            } catch (final ParseException e) {
            }
        }
        if (flag4 || flag2 || flag5) {
            value = SINGLE_QUOTE + value.replaceAll(SINGLE_QUOTE, TWO_SINGLE_QUOTES) + SINGLE_QUOTE;
        }
        return value;
    }

    private String[] splitRemovingEmptyEntries(final String input, final String delimiters) {
        final String[] array = StringUtil.split(delimiters, input);
        final List<String> sections = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (array[i].trim().length() > 0) {
                sections.add(array[i]);
            }
        }
        return sections.toArray(new String[sections.size()]);
    }

    private void processChildGroups(final Node node, final Map<NodeCondition, Integer> hash) {
        for (int i = 0; i < node.getCount(); i++) {
            final Node node1 = node.getItem(i);
            if (node1.getNodeType() == NodeType.AND || node1.getNodeType() == NodeType.OR) {
                processGroupNode(node, node1, hash);
            }
            if (node1.getCount() > 1) {
                processChildGroups(node1, hash);
            }
        }
    }

    private void processGroupNode(final Node parentNode, final Node node, final Map<NodeCondition, Integer> hash) {
        /*
         * If node is an "AND" node or an "OR" node, and the node's priority is
         * greater than the parentNode's priority or the node has explicit
         * grouping...
         */
        if (((node.getNodeType() == NodeType.AND) || (node.getNodeType() == NodeType.OR))
            && ((node.getPriority().isGreaterThan(parentNode.getPriority())) || hasExplicitGrouping(node))) {
            final NodeCondition node1 = getFirstCondition(node);
            if (node1 == null) {
                throw new RuntimeException("grouping missing first row"); //$NON-NLS-1$
            }
            final NodeCondition node2 = getLastCondition(node);
            if (node2 == null) {
                throw new RuntimeException("grouping missing last row"); //$NON-NLS-1$
            }
            if (!hash.containsKey(node1) || !hash.containsKey(node2)) {
                throw new RuntimeException("grouping: missing indices"); //$NON-NLS-1$
            }
            final int num1 = hash.get(node1).intValue();
            final int num2 = hash.get(node2).intValue();

            QEQueryRowCollection rowCollection;
            final String fieldPrefix = node1.getLeft().getPrefix();
            if (fieldPrefix == null || fieldPrefix.equalsIgnoreCase("Source")) //$NON-NLS-1$
            {
                rowCollection = query.getSourceRowCollection();
            } else {
                rowCollection = query.getTargetRowCollection();
            }

            rowCollection.getGrouping().addGrouping(num1, num2);
        }
    }

    private boolean hasExplicitGrouping(final Node node) {
        final String text1 = wiqlNodeAsString(node, parsedWiqlExpression);
        if (text1.startsWith(OPEN_PAREN_STRING)) {
            return text1.endsWith(CLOSE_PAREN_STRING);
        }
        return false;
    }

    public static String wiqlNodeAsString(final Node node, final String wiqlStatement) {
        String text1;
        if ((wiqlStatement == null)
            || (node.getStartOffset() < 0)
            || (node.getStartOffset() >= wiqlStatement.length())
            || (node.getEndOffset() < 0)
            || (node.getEndOffset() > wiqlStatement.length())) {
            return ""; //$NON-NLS-1$
        }
        if (node.getEndOffset() == wiqlStatement.length()) {
            text1 = wiqlStatement.substring(node.getStartOffset());
        } else {
            text1 = wiqlStatement.substring(node.getStartOffset(), node.getEndOffset() + 1);
        }
        return text1.trim();
    }

    private NodeCondition getFirstCondition(final Node node) {
        if (node != null) {
            if (node.getNodeType() == NodeType.FIELD_CONDITION) {
                return (NodeCondition) node;
            }
            if (node.getCount() > 0) {
                return getFirstCondition(node.getItem(0));
            }
        }
        return null;
    }

    private NodeCondition getLastCondition(final Node node) {
        if (node != null) {
            if (node.getNodeType() == NodeType.FIELD_CONDITION) {
                return (NodeCondition) node;
            }
            if (node.getCount() > 0) {
                return getLastCondition(node.getItem(node.getCount() - 1));
            }
        }
        return null;
    }

    private void parseWIQLNode(
        final Node node,
        final String parentOp,
        final Map<NodeCondition, Integer> hash,
        final boolean localizeFieldNames) {
        if (node != null) {
            if (node.getNodeType() == NodeType.AND || node.getNodeType() == NodeType.OR) {
                for (int i = 0; i < node.getCount(); i++) {
                    if (i == 0) {
                        parseWIQLNode(node.getItem(i), parentOp, hash, localizeFieldNames);
                    } else {
                        String text1;
                        if (node.getNodeType() == NodeType.AND) {
                            text1 = WIQLOperators.AND;
                        } else {
                            text1 = WIQLOperators.OR;
                        }
                        parseWIQLNode(node.getItem(i), text1, hash, localizeFieldNames);
                    }
                }
            } else {
                addNodeAsRow(node, localizeFieldNames, parentOp, hash);
            }
        }
    }

    private void addNodeAsRow(
        final Node node,
        final boolean localizeFieldNames,
        final String parentOp,
        final Map<NodeCondition, Integer> hash) {
        final NodeConditionTypeHolder type1 = new NodeConditionTypeHolder();
        final NodeCondition nodeCondition = getNodeCondition(node, type1);
        if (nodeCondition == null || nodeCondition.getCount() != 2) {
            throw new RuntimeException("unexpected node: " + node.toString()); //$NON-NLS-1$
        }

        final Condition condition = nodeCondition.getCondition();
        final NodeFieldName nodeFieldName = nodeCondition.getLeft();
        final String namePrefix = nodeFieldName.getPrefix();

        // Don't include the portion of the WHERE clause that filters the link
        // types of link or tree queries. This condition is not shown in neither
        // the source nor target tables.
        if (query.getQueryType() != QueryType.LIST && namePrefix == null) {
            parseLinkTypesCondition(nodeCondition);
            return;
        }

        // Choose the proper row collection based on the field name prefix. A
        // list query will have a null or empty field name prefix. A link or
        // tree query will have a prefix of "Source" or "Target" for conditions
        // which should appear in a row collection. The source and target row
        // collections are displayed in UI as separate tables.
        QEQueryRowCollection rowCollection;
        if (query.getQueryType() == QueryType.LIST || namePrefix.equalsIgnoreCase("Source")) //$NON-NLS-1$
        {
            rowCollection = query.getSourceRowCollection();
        } else {
            rowCollection = query.getTargetRowCollection();
        }

        // Get the field name from the left node.
        final String fieldName = getFieldName(nodeFieldName, localizeFieldNames);

        // Get the conditional operator.
        String invariantOperator = ConditionalOperators.getString(condition);
        String localizedOperator = invariantOperator;

        // Test for the special case of field operators.
        String fieldValue;
        final Node nodeRight = nodeCondition.getRight();
        if (nodeRight.getNodeType() == NodeType.FIELD_NAME) {
            localizedOperator = getLocalizedOperator(localizedOperator + " "); //$NON-NLS-1$
            fieldValue = getFieldName((NodeFieldName) nodeRight, localizeFieldNames);
        } else {
            if (type1.nodeConditionType == NodeConditionType.NOT) {
                invariantOperator = WIQLOperators.NOT_ + localizedOperator;
                localizedOperator = getLocalizedOperator(WIQLOperators.NOT_ + localizedOperator);
            } else if (type1.nodeConditionType == NodeConditionType.NOT_EVER) {
                localizedOperator = getLocalizedOperator(WIQLOperators.NOT_EVER);
            } else if (type1.nodeConditionType == NodeConditionType.EVER) {
                localizedOperator = getLocalizedOperator(WIQLOperators.EVER);
            } else {
                localizedOperator = getLocalizedOperator(localizedOperator);
            }

            fieldValue = getLocalizedFieldValue(nodeRight);
        }

        // Get the logical operator. The first clause should never have a
        // logical operator.
        final String localizedLogicalOperator = getLocalizedOperator(rowCollection.getRowCount() > 0 ? parentOp : ""); //$NON-NLS-1$

        // Handle the "special any" syntax.
        if (WIQLAdapter.fieldSupportsAnySyntax(query.getWorkItemClient(), fieldName)
            && isAnySyntaxCondition(invariantOperator, fieldValue)) {
            localizedOperator = getLocalizedOperator(WIQLOperators.EQUAL_TO);
            fieldValue = getLocalizedOperator(WIQLOperators.SPECIAL_ANY);
        }

        // Show "Contains" as "Contains Words" for Text fields that
        // support text search, since that's what the server will use.
        // We convert back to "Contains" for back-compat on save.
        if (WIQLOperators.isContainsOperator(invariantOperator)) {
            final FieldDefinition fd = tryGetFieldDefinition(fieldName);
            if (fd != null && fd.isLongText() && fd.supportsTextQuery()) {
                final String replacement = convertToContainsWord(invariantOperator);
                localizedOperator = getLocalizedOperator(replacement);

            }
        } else if (WIQLOperators.isContainsWordsOperator(invariantOperator)) {
            final FieldDefinition fd = tryGetFieldDefinition(fieldName);
            if (fd != null && !fd.supportsTextQuery()) {
                final String replacement = convertToContains(invariantOperator);
                localizedOperator = getLocalizedOperator(replacement);
            }
        }

        final QEQueryRow newRow = rowCollection.addRow();
        newRow.setLogicalOperator(localizedLogicalOperator);
        newRow.setFieldName(fieldName);
        newRow.setOperator(localizedOperator);
        newRow.setValue(fieldValue);

        hash.put(nodeCondition, new Integer(rowCollection.indexOf(newRow)));
    }

    private void parseLinkTypesCondition(final NodeCondition nodeCondition) {
        final Condition condition = nodeCondition.getCondition();
        final Node nodeRight = nodeCondition.getRight();
        final ArrayList<String> listLinkReferenceNames = new ArrayList<String>();

        if (Condition.EQUALS.equals(condition)) {
            final NodeString nodeString = (NodeString) nodeRight;
            listLinkReferenceNames.add(nodeString.getValue());
        } else if (Condition.IN.equals(condition)) {
            final NodeValueList nodeValueList = (NodeValueList) nodeRight;
            for (int i = 0; i < nodeValueList.getCount(); i++) {
                final NodeString nodeString = (NodeString) nodeValueList.getItem(i);
                listLinkReferenceNames.add(nodeString.getValue());
            }
        }

        if (query.getQueryType() == QueryType.ONE_HOP) {
            query.setUseSelectedLinkTypes(listLinkReferenceNames.size() > 0);
            for (int i = 0; i < listLinkReferenceNames.size(); i++) {
                query.addLinkQueryLinkType(listLinkReferenceNames.get(i));
            }
        } else {
            if (listLinkReferenceNames.size() > 0) {
                query.setTreeQueryLinkType(listLinkReferenceNames.get(0));
            }
        }
    }

    private String getLocalizedFieldValue(final Node valueNode) {
        if (valueNode.getNodeType() == NodeType.VALUE_LIST) {
            final NodeValueList list1 = (NodeValueList) valueNode;
            final StringBuffer builder1 = new StringBuffer();
            for (int num1 = 0; num1 < list1.getCount(); num1++) {
                if (num1 == 0) {
                    builder1.append(getLocalizedFieldValueFromValueNode((NodeItem) list1.getItem(num1)));
                } else {
                    builder1.append(MessageFormat.format(OPERATOR_IN_NEXT_ITEM, new Object[] {
                        /* I18N: need a Locale-sensitive list separator */",", //$NON-NLS-1$
                        getLocalizedFieldValueFromValueNode((NodeItem) list1.getItem(num1))
                    }));
                }
            }
            return builder1.toString();
        }
        if (valueNode.getNodeType() == NodeType.ARITHMETIC) {
            final NodeArithmetic arithmetic1 = (NodeArithmetic) valueNode;
            return MessageFormat.format(
                "{0} {1} {2}", //$NON-NLS-1$
                new Object[] {
                    getLocalizedFieldValueFromValueNode((NodeItem) arithmetic1.getLeft()),
                    ArithmeticalOperators.getString(arithmetic1.getArithmetic()),
                    getLocalizedFieldValueFromValueNode((NodeItem) arithmetic1.getRight())
            });
        }
        return getLocalizedFieldValueFromValueNode((NodeItem) valueNode);
    }

    private String getLocalizedFieldValueFromValueNode(final NodeItem valueNode) {
        String text1 = valueNode.getValue();
        if (valueNode.getNodeType() == NodeType.VARIABLE) {
            return getLocalizedOperator(valueNode.toString());
        }

        /*
         * In MS code:
         */
        /*
         * if ((valueNode.NodeType == NodeType.String) &&
         * DateTime.TryParseExact(valueNode.Value, "o",
         * CultureInfo.InvariantCulture, DateTimeStyles.None, out time1)) {
         * return DateTime.ParseExact(text1, "o",
         * CultureInfo.InvariantCulture).ToString("d",
         * CultureInfo.CurrentCulture); } if ((valueNode.NodeType ==
         * NodeType.Number) &&
         * text1.Contains(CultureInfo.InvariantCulture.NumberFormat
         * .NumberDecimalSeparator)) { text1 = double.Parse(text1,
         * CultureInfo.InvariantCulture).ToString(CultureInfo.CurrentCulture); }
         */
        if (valueNode.getNodeType() == NodeType.STRING) {
            /*
             * I18N: need to use a specified Locale (and TimeZone?)
             */
            try {
                final Date date = DateTime.parseRoundtripFormat(text1, TimeZone.getDefault());
                text1 = DateFormat.getDateInstance(DateFormat.SHORT).format(date);
            } catch (final UncheckedParseException ex) {
                // ignore
            }
        }
        if (valueNode.getNodeType() == NodeType.NUMBER) {
            /*
             * I18N: need to use a specified Locale
             */
            final char decimalSeparator = new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator();
            if (text1 != null && text1.indexOf(decimalSeparator) != -1) {
                try {
                    final Double d = Double.valueOf(text1);
                    text1 = NumberFormat.getInstance().format(d.doubleValue());
                } catch (final NumberFormatException ex) {
                    // ignore
                }
            }
        }
        return text1;
    }

    private boolean skipRow(final int row, final QEQueryRowCollection rowCollection) {
        final QEQueryRow qeQueryRow = rowCollection.getRow(row);

        if ((qeQueryRow.getLogicalOperator() != null && qeQueryRow.getLogicalOperator().length() != 0) || row == 0) {
            if (qeQueryRow.getFieldName() != null && qeQueryRow.getFieldName().length() != 0) {
                return false;
            }
        }

        if (rowCollection.getGrouping().rowInGroup(row)) {
            return false;
        }

        return true;
    }

    private NodeCondition getNodeCondition(final Node node, final NodeConditionTypeHolder conditionTypeHolder) {
        NodeCondition condition1 = null;

        if (node.getNodeType() == NodeType.FIELD_CONDITION) {
            conditionTypeHolder.nodeConditionType = NodeConditionType.NORMAL;
            return (NodeCondition) node;
        }

        if (node.getNodeType() == NodeType.NOT) {
            conditionTypeHolder.nodeConditionType = NodeConditionType.NOT;
            final NodeNotOperator operator1 = (NodeNotOperator) node;
            if (operator1.getCount() == 1) {
                final NodeConditionTypeHolder type1 = new NodeConditionTypeHolder();
                condition1 = getNodeCondition(operator1.getItem(0), type1);
                if (type1.nodeConditionType == NodeConditionType.EVER) {
                    conditionTypeHolder.nodeConditionType = NodeConditionType.NOT_EVER;
                }
                return condition1;
            }
            throw new RuntimeException(MessageFormat.format("unexpected node: {0}", node.toString())); //$NON-NLS-1$
        }

        if (node.getNodeType() != NodeType.EVER) {
            throw new RuntimeException(MessageFormat.format("unexpected node: {0}", node.toString())); //$NON-NLS-1$
        }

        conditionTypeHolder.nodeConditionType = NodeConditionType.EVER;
        final NodeEverOperator operator2 = (NodeEverOperator) node;
        if (operator2.getCount() == 1) {
            final NodeConditionTypeHolder type2 = new NodeConditionTypeHolder();
            condition1 = getNodeCondition(operator2.getItem(0), type2);
            if (type2.nodeConditionType != NodeConditionType.NORMAL) {
                throw new RuntimeException(MessageFormat.format("unexpected node: {0}", node.toString())); //$NON-NLS-1$
            }
            return condition1;
        }

        throw new RuntimeException(MessageFormat.format("unexpected node: {0}", node.toString())); //$NON-NLS-1$
    }

    private boolean isAnySyntax(final String invariantOperator, final String fieldValue) {
        if (invariantOperator == null || fieldValue == null) {
            return false;
        }

        return invariantOperator.equals(WIQLOperators.EQUAL_TO)
            && fieldValue.equalsIgnoreCase(WIQLOperators.getLocalizedOperator(WIQLOperators.SPECIAL_ANY));
    }

    private boolean isAnySyntaxCondition(final String invariantOperator, final String fieldValue) {
        if (invariantOperator == null || fieldValue == null) {
            return false;
        }

        return invariantOperator.equals(WIQLOperators.NOT_EQUAL_TO) && fieldValue.length() == 0;
    }

    private static class NodeConditionTypeHolder {
        public NodeConditionType nodeConditionType;
    }

    private static class NodeConditionType {
        public static final NodeConditionType NORMAL = new NodeConditionType(0);
        public static final NodeConditionType EVER = new NodeConditionType(1);
        public static final NodeConditionType NOT = new NodeConditionType(2);
        public static final NodeConditionType NOT_EVER = new NodeConditionType(3);

        private final int type;

        private NodeConditionType(final int type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return String.valueOf(type);
        }
    }

    /*
     * below are methods equivalent to FilterGridAdapter / QueryAdapter methods
     * in MS code
     */

    private String getFieldName(final NodeFieldName node, final boolean localize) {
        if (localize) {
            return getLocalizedFieldName(node.getValue());
        } else {
            return getInvariantFieldName(node.getValue());
        }
    }

    private String getLocalizedFieldName(final String fieldName) {
        return fieldService.getLocalizedFieldName(fieldName);
    }

    private String getInvariantFieldName(final String fieldName) {
        return fieldService.getInvariantFieldName(fieldName);
    }

    private String getLocalizedOperator(final String invariantOperator) {
        return WIQLOperators.getLocalizedOperator(invariantOperator);
    }

    private String getInvariantOperator(final String localizedOperator) {
        return WIQLOperators.getInvariantOperator(localizedOperator);
    }

    private boolean isDateTimeField(final QEQueryRow row) {
        return fieldService.isDateTimeField(row.getFieldName());
    }

    private boolean isDecimalField(final QEQueryRow row) {
        return fieldService.isDecimalField(row.getFieldName());
    }

    private boolean isStringField(final QEQueryRow row) {
        return fieldService.isStringField(row.getFieldName());
    }

    private FieldDefinition tryGetFieldDefinition(final String fieldName) {
        try {
            return query.getWorkItemClient().getFieldDefinitions().get(fieldName);
        } catch (final FieldDefinitionNotExistException e) {
            return null;
        }
    }

    /**
     * Convert the given "[Not] Contains" operator to the equivalent
     * "[Not] Contains Words" operator.
     *
     *
     * @param operatorName
     * @return
     */
    private String convertToContainsWord(final String containsOperator) {
        if (containsOperator.equalsIgnoreCase(WIQLOperators.CONTAINS)) {
            return WIQLOperators.CONTAINS_WORDS;
        } else if (containsOperator.equalsIgnoreCase(WIQLOperators.NOT_CONTAINS)) {
            return WIQLOperators.NOT_CONTAINS_WORDS;
        } else {
            throw new IllegalArgumentException("Expected CONTAINS operator"); //$NON-NLS-1$
        }
    }

    private String convertToContains(final String containsWordsOperator) {
        if (containsWordsOperator.equalsIgnoreCase(WIQLOperators.CONTAINS_WORDS)) {
            return WIQLOperators.CONTAINS;
        } else if (containsWordsOperator.equalsIgnoreCase(WIQLOperators.NOT_CONTAINS_WORDS)) {
            return WIQLOperators.NOT_CONTAINS;
        } else {
            throw new IllegalArgumentException("Expected CONTAINS WORDS operator"); //$NON-NLS-1$
        }
    }
}
