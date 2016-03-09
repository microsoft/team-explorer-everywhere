// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.CoreFields;
import com.microsoft.tfs.core.clients.workitem.SupportedFeatures;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.internal.QueryPackageNames;
import com.microsoft.tfs.core.clients.workitem.internal.UpdatePackageNames;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeImpl;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeStructureType;
import com.microsoft.tfs.core.clients.workitem.internal.provision.ProvisionValues;
import com.microsoft.tfs.core.clients.workitem.internal.query.DisplayFieldListImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.SortFieldListImpl;
import com.microsoft.tfs.core.clients.workitem.link.Topology;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;
import com.microsoft.tfs.core.clients.workitem.query.SortFieldList;
import com.microsoft.tfs.core.clients.workitem.query.SortType;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.xml.DOMCreateUtils;

/**
 * Binds the WIQL parser with Product Studio. Formerly called
 * ProductStudioExternal, renamed to WiqlAdapter in Rosario
 */
public class WIQLAdapter implements IExternal {
    private static final String[] operators;
    private static final SortType[] sortTypes;

    static {
        /*
         * The operators array is divided into sections of 4 related elements.
         * Each section starts at an index that is a multiple of 4 (ie 4, 8,
         * 12...).
         *
         * Each section defines an operators "class". The first element in the
         * section is the standard operator for that class. The second element
         * is the "not" version of the operator. The third element is the "ever"
         * version of the operator. The fourth element is the "never" version of
         * the operator. Not all operator classes contain all versions of an
         * operator.
         *
         * The private method ProductStudioExternal#getConditionalOperator
         * computes the correct index into the operators array given some
         * parameters.
         *
         * The 8 operator classes are:
         *
         * index 4: "equals" class index 8: "notEquals" class index 12: "less"
         * class index 16: "greater" class index 20: "equalsLess" class index
         * 24: "equalsGreater" class index 28: "under" class index 36:
         * "contains" class
         */

        operators = new String[48];
        operators[4] = "equals"; //$NON-NLS-1$
        operators[5] = "notEquals"; //$NON-NLS-1$
        operators[6] = "ever"; //$NON-NLS-1$
        operators[8] = "notEquals"; //$NON-NLS-1$
        operators[9] = "equals"; //$NON-NLS-1$
        operators[11] = "ever"; //$NON-NLS-1$
        operators[12] = "less"; //$NON-NLS-1$
        operators[13] = "equalsGreater"; //$NON-NLS-1$
        operators[16] = "greater"; //$NON-NLS-1$
        operators[17] = "equalsLess"; //$NON-NLS-1$
        operators[20] = "equalsLess"; //$NON-NLS-1$
        operators[21] = "greater"; //$NON-NLS-1$
        operators[24] = "equalsGreater"; //$NON-NLS-1$
        operators[25] = "less"; //$NON-NLS-1$
        operators[28] = "under"; //$NON-NLS-1$
        operators[29] = "notUnder"; //$NON-NLS-1$
        operators[36] = "contains"; //$NON-NLS-1$
        operators[37] = "notContains"; //$NON-NLS-1$
        operators[38] = "everContains"; //$NON-NLS-1$
        operators[39] = "neverContains"; //$NON-NLS-1$
        operators[40] = "containsWords"; //$NON-NLS-1$
        operators[41] = "notContainsWords"; //$NON-NLS-1$
        operators[42] = "everContainsWords"; //$NON-NLS-1$
        operators[43] = "neverContainsWords"; //$NON-NLS-1$
        operators[44] = "equals"; //$NON-NLS-1$
        operators[45] = "notequals"; //$NON-NLS-1$
    }

    static {
        sortTypes = new SortType[3];

        sortTypes[Direction.UNKNOWN.getValue()] = SortType.ASCENDING;
        sortTypes[Direction.ASCENDING.getValue()] = SortType.ASCENDING;
        sortTypes[Direction.DESCENDING.getValue()] = SortType.DESCENDING;
    }

    private static final String ME = "me"; //$NON-NLS-1$
    private static final String TODAY = "today"; //$NON-NLS-1$

    private Map<String, Object> context;

    private final WITContext witContext;

    private boolean dayPrecision;

    public WIQLAdapter(final WITContext witContext) {
        this.witContext = witContext;
    }

    @Override
    public Object findField(final String name, final String prefix, final Object tableTag) {
        if (witContext.getClient().getFieldDefinitions().contains(name) || tableTag == null) {
            return witContext.getClient().getFieldDefinitions().get(name);
        }

        // TODO Port ignoring prefix
        // TODO Omitting tableTag != null

        throw new RuntimeException(
            MessageFormat.format(
                Messages.getString("WiqlAdapter.QueryReferencesNonExistingFieldFormat"), //$NON-NLS-1$
                name,
                prefix));

        // return null;
    }

    @Override
    public Object findTable(final String name) {
        final LinkQueryMode queryMode = getQueryMode(new NodeTableName(name));
        if (LinkQueryMode.UNKNOWN.equals(queryMode)) {
            return null;
        }

        if ((!LinkQueryMode.WORK_ITEMS.equals(queryMode))
            && !witContext.getServerInfo().isSupported(SupportedFeatures.WORK_ITEM_LINKS)) {
            throw new NotSupportedException(
                Messages.getString("WiqlAdapter.CannotCreateCustomTypesOfLinksOnThisServer")); //$NON-NLS-1$
        }

        return queryMode;
    }

    @Override
    public Object findVariable(final String name) {
        if (context.containsKey(name)) {
            return name;
        }

        if (ME.equalsIgnoreCase(name)) {
            return ME;
        }

        if (TODAY.equalsIgnoreCase(name)) {
            return TODAY;
        }

        // Evaluate variables/custom macros
        // Allow validation of macros to pass on clients on >=Dev14, evaluation
        // happens on the server
        if (witContext.getServerInfo().isSupported(SupportedFeatures.WIQL_EVALUATION_ON_SERVER)) {
            context.put(name, name);
            return name;
        }

        return null;
    }

    public Date getAsOfUTC(final NodeSelect nodeSelect) {
        if (nodeSelect.getAsOf() == null) {
            return null;
        }
        final Date time1 = DateTime.parse(((NodeItem) nodeSelect.getAsOf()).getValue(), getLocale(), getTimeZone());
        return time1;
    }

    private String getConditionalOperator(final int c, final boolean not, final boolean ever, final Node errorNode) {
        final String operator = operators[(c * 4) + (ever ? 2 : 0) + (not ? 1 : 0)];
        Tools.ensureSyntax(operator != null, SyntaxError.INVALID_CONDITIONAL_OPERATOR, errorNode);
        return operator;
    }

    @Override
    public DataType getFieldDataType(final Object fieldTag) {
        final FieldDefinition definition = (FieldDefinition) fieldTag;

        if (definition.getID() == CoreFields.LINK_TYPE) {
            // Link Types pretend to be strings
            return DataType.STRING;
        }

        final FieldType fieldType = definition.getFieldType();

        if (FieldType.STRING == fieldType
            || FieldType.PLAINTEXT == fieldType
            || FieldType.HTML == fieldType
            || FieldType.TREEPATH == fieldType
            || FieldType.HISTORY == fieldType) {
            return DataType.STRING;
        }

        if (FieldType.INTEGER == fieldType || FieldType.DOUBLE == fieldType) {
            return DataType.NUMERIC;
        }

        if (FieldType.DATETIME == fieldType) {
            return DataType.DATE;
        }
        if (FieldType.GUID == fieldType) {
            return DataType.GUID;
        }
        if (FieldType.BOOLEAN == fieldType) {
            return DataType.BOOL;
        }

        return DataType.UNKNOWN;
    }

    public static LinkQueryMode getQueryMode(final NodeSelect nodeSelect) {
        return getQueryMode(nodeSelect.getMode(), nodeSelect.getFrom());
    }

    private static LinkQueryMode getQueryMode(final NodeMode nodeMode, final NodeTableName tableContext) {
        final LinkQueryMode queryMode = getQueryMode(tableContext);
        if (nodeMode == null) {
            return queryMode;
        }

        Tools.ensureSyntax(
            queryMode.getValue() > LinkQueryMode.WORK_ITEMS.getValue(),
            SyntaxError.MODE_ON_WORK_ITEMS,
            nodeMode);

        int mask = 0;
        for (final Iterator<Node> it = nodeMode.iterator(); it.hasNext();) {
            final String keyword = ((NodeName) it.next()).getValue();

            if (WIQLConstants.MUST_CONTAIN.equalsIgnoreCase(keyword)) {
                mask = mask | 1;
            } else if (WIQLConstants.MAY_CONTAIN.equalsIgnoreCase(keyword)) {
                mask = mask | 2;
            } else if (WIQLConstants.DOES_NOT_CONTAIN.equalsIgnoreCase(keyword)) {
                mask = mask | 4;
            } else if (WIQLConstants.RECURSIVE.equalsIgnoreCase(keyword)) {
                mask = mask | 8;
            } else {
                mask = -1; // incorrect
            }
        }

        // Check valid combinations
        switch (mask) {
            case 0:
            case 1:
                return LinkQueryMode.LINKS_MUST_CONTAIN;
            case 2:
                return LinkQueryMode.LINKS_MAY_CONTAIN;
            case 4:
                return LinkQueryMode.LINKS_DOES_NOT_CONTAIN;
            case 8:
            case 9:
                return LinkQueryMode.LINKS_RECURSIVE;
        }

        return LinkQueryMode.UNKNOWN;
    }

    private static LinkQueryMode getQueryMode(final NodeTableName tableContext) {
        final String from = tableContext.getValue();

        if (from != null) {
            if ("issue".equalsIgnoreCase(from) //$NON-NLS-1$
                || "issues".equalsIgnoreCase(from) //$NON-NLS-1$
                || "workitem".equalsIgnoreCase(from) //$NON-NLS-1$
                || "WorkItems".equalsIgnoreCase(from)) //$NON-NLS-1$
            {
                return LinkQueryMode.WORK_ITEMS;
            }

            if ("links".equalsIgnoreCase(from) || "WorkItemLinks".equalsIgnoreCase(from)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return LinkQueryMode.LINKS_MUST_CONTAIN;
            }
        }

        return LinkQueryMode.UNKNOWN;
    }

    public Element getQueryXML(final NodeSelect nodeSelect) {
        final LinkQueryMode mode = (LinkQueryMode) nodeSelect.getFrom().getTag();

        Tools.ensureSyntax(LinkQueryMode.WORK_ITEMS == mode, SyntaxError.INCORRECT_QUERY_METHOD, nodeSelect.getFrom());

        // Build Query XML
        final Document document = DOMCreateUtils.newDocument(WIQLConstants.QUERY);
        final Element queryXmlElement = document.getDocumentElement();

        // set product name
        queryXmlElement.setAttribute(WIQLConstants.PRODUCT, witContext.getProductValue());

        // Add query expressions
        Node whereNode = nodeSelect.getWhere();
        if (whereNode == null) {
            whereNode = new NodeBoolConst(true);
        }

        queryXmlElement.appendChild(queryXML(whereNode, queryXmlElement.getOwnerDocument(), false, false, false));

        // set asOf date
        final Date asOf = getAsOfUTC(nodeSelect);
        if (asOf != null) {
            final SimpleDateFormat externalDateFormat = InternalWorkItemUtils.newMetadataDateFormat();
            queryXmlElement.setAttribute(QueryPackageNames.QUERY_AS_OF, externalDateFormat.format(asOf));
        }
        return queryXmlElement;
    }

    public Element getQueryXML(
        final String wiql,
        final Map<String, Object> context,
        final boolean isLinkQuery,
        final boolean dayPrecision) {
        // build query XML
        final Document document =
            DOMCreateUtils.newDocument(isLinkQuery ? WIQLConstants.LINKS_QUERY : WIQLConstants.QUERY);
        final Element queryXml = document.getDocumentElement();

        // set product name
        queryXml.setAttribute(WIQLConstants.PRODUCT, StringUtil.EMPTY);

        // Add Wiql
        final Element wx = document.createElement(WIQLConstants.WIQL);
        wx.appendChild(document.createTextNode(wiql));
        queryXml.appendChild(wx);

        // Add DayPrecision
        final Element dx = document.createElement(WIQLConstants.DAY_PRECISION);
        dx.appendChild(document.createTextNode(dayPrecision ? "true" : "false")); //$NON-NLS-1$//$NON-NLS-2$
        queryXml.appendChild(dx);

        // Add Context
        if (context != null) {
            context.remove("currentIteration"); //$NON-NLS-1$
            context.put("currentIteration", "corso alm 2013\\Release 1\\Sprint 4"); //$NON-NLS-1$ //$NON-NLS-2$
            for (final Entry<String, Object> entry : context.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    final Element cx = document.createElement(WIQLConstants.CONTEXT);
                    cx.setAttribute(WIQLConstants.KEY, entry.getKey());
                    cx.setAttribute(WIQLConstants.VALUE, entry.getValue().toString());
                    cx.setAttribute(WIQLConstants.VALUE_TYPE, GetContextValueType(entry.getValue()));
                    queryXml.appendChild(cx);
                }
            }
        }
        return queryXml;
    }

    public LinkQueryXMLResult getLinkQueryXML(final NodeSelect nodeSelect) {
        final LinkQueryMode mode = (LinkQueryMode) nodeSelect.getFrom().getTag();

        // Get query type
        String type = null;
        if (LinkQueryMode.LINKS_MUST_CONTAIN.equals(mode)) {
            type = QueryPackageNames.QUERY_LINKS_MUST_CONTAIN_TYPE;
        } else if (LinkQueryMode.LINKS_RECURSIVE.equals(mode) || LinkQueryMode.LINKS_MAY_CONTAIN.equals(mode)) {
            type = QueryPackageNames.QUERY_LINKS_MAY_CONTAIN_TYPE;
        } else if (LinkQueryMode.LINKS_DOES_NOT_CONTAIN.equals(mode)) {
            type = QueryPackageNames.QUERY_LINKS_DOES_NOT_CONTAIN_TYPE;
        } else {
            Tools.ensureSyntax(false, SyntaxError.INCORRECT_QUERY_METHOD, nodeSelect.getFrom());
        }

        // Build Query XML
        final Document document = DOMCreateUtils.newDocument(QueryPackageNames.QUERY_LINKS_ELEMENT);
        final Element queryXmlElement = document.getDocumentElement();

        queryXmlElement.setAttribute(QueryPackageNames.QUERY_LINKS_TYPE_ATTRIBUTE, type);

        // take groups
        final Map<String, NodeAndOperator> groups = nodeSelect.getWhereGroups();

        final NodeAndOperator leftGroup = groups.get(WIQLConstants.SOURCE_PREFIX);
        if (leftGroup != null) {
            final Element x =
                queryXmlElement.getOwnerDocument().createElement(QueryPackageNames.QUERY_LINKS_LEFT_QUERY_ELEMENT);
            x.appendChild(queryXML(leftGroup, queryXmlElement.getOwnerDocument(), false, false, false));
            queryXmlElement.appendChild(x);
        }

        final NodeAndOperator linkGroup = groups.get(""); //$NON-NLS-1$

        if (mode == LinkQueryMode.LINKS_RECURSIVE) {
            Tools.ensureSyntax(linkGroup != null, SyntaxError.TREE_QUERY_NEEDS_ONE_LINK_TYPE, linkGroup);
            final Map<Integer, Boolean> types = computeLinkTypes(linkGroup);
            Tools.ensureSyntax(types.size() == 1, SyntaxError.TREE_QUERY_NEEDS_ONE_LINK_TYPE, linkGroup);

            // Should be only one of these now based on checks above
            queryXmlElement.setAttribute(
                QueryPackageNames.QUERY_LINKS_RECURSIVE_ATTRIBUTE,
                types.keySet().toArray()[0].toString());
        } else {
            if (linkGroup != null) {
                final Element x =
                    queryXmlElement.getOwnerDocument().createElement(QueryPackageNames.QUERY_LINKS_LINK_QUERY_ELEMENT);
                x.appendChild(queryXML(linkGroup, queryXmlElement.getOwnerDocument(), false, false, false));
                queryXmlElement.appendChild(x);
            }
        }

        final NodeAndOperator rightGroup = groups.get(WIQLConstants.TARGET_PREFIX);
        if (rightGroup != null) {
            final Element x =
                queryXmlElement.getOwnerDocument().createElement(QueryPackageNames.QUERY_LINKS_RIGHT_QUERY_ELEMENT);
            x.appendChild(queryXML(rightGroup, queryXmlElement.getOwnerDocument(), false, false, false));
            queryXmlElement.appendChild(x);
        }

        // Set AsOf date
        final Date asOf = getAsOfUTC(nodeSelect);
        if (asOf != null) {
            final SimpleDateFormat externalDateFormat = InternalWorkItemUtils.newMetadataDateFormat();
            queryXmlElement.setAttribute(QueryPackageNames.QUERY_AS_OF, externalDateFormat.format(asOf));
        }

        return new LinkQueryXMLResult(queryXmlElement, linkGroup);
    }

    public DisplayFieldList getDisplayFieldList(final WITContext witContext, final NodeSelect nodeSelect) {
        final DisplayFieldListImpl list = new DisplayFieldListImpl(witContext);

        final NodeFieldList fields = nodeSelect.getFields();
        if (fields != null) {
            // select field1, field2... etc
            for (final Iterator<Node> it = fields.iterator(); it.hasNext();) {
                final NodeFieldName field = (NodeFieldName) it.next();
                list.add(((FieldDefinitionImpl) field.getTag()).getReferenceName());
            }
        } else {
            // select *
            if (LinkQueryMode.WORK_ITEMS.equals(nodeSelect.getFrom().getTag())) {
                for (final FieldDefinition fd : witContext.getFieldDefinitions()) {
                    if (fd.getUsage() == FieldUsages.WORK_ITEM) {
                        list.add(fd);
                    }
                }
            } else {
                // Links
                witContext.getFieldDefinitions().getFieldDefinitionInternal(CoreFields.ID);
                witContext.getFieldDefinitions().getFieldDefinitionInternal(CoreFields.LINK_TYPE);
            }
        }

        return list;
    }

    public SortFieldList getSortFieldList(final WITContext witContext, final NodeSelect nodeSelect) {
        final SortFieldListImpl list = new SortFieldListImpl(witContext);

        final NodeFieldList fields = nodeSelect.getOrderBy();
        if (fields != null) {
            for (final Iterator<Node> iterator = fields.iterator(); iterator.hasNext();) {
                final NodeFieldName field = (NodeFieldName) iterator.next();
                final SortType sortType =
                    (field.getDirection() == Direction.DESCENDING ? SortType.DESCENDING : SortType.ASCENDING);
                list.add((FieldDefinition) field.getTag(), sortType);
            }
        }

        return list;
    }

    /**
     * Return map of Integer,Boolean pairs
     */
    public Map<Integer, Boolean> computeLinkTypes(final Node node) {
        if (node.getNodeType() == NodeType.FIELD_CONDITION) {
            final NodeCondition cond = (NodeCondition) node;

            final NodeFieldName field = cond.getLeft();
            final FieldDefinition fd = (FieldDefinition) field.getTag();
            if (fd.getID() == CoreFields.LINK_TYPE) {
                final WorkItemLinkTypeEnd linkTypeEnd = getLinkTypeByName(cond.getRight().getConstStringValue());

                Tools.ensureSyntax(linkTypeEnd != null, SyntaxError.INVALID_LINK_TYPE_NAME, cond.getRight());

                if (cond.getCondition() == Condition.EQUALS) {
                    final Map<Integer, Boolean> linkTypes = new HashMap<Integer, Boolean>();
                    linkTypes.put(new Integer(linkTypeEnd.getID()), Boolean.TRUE);
                    return linkTypes;
                } else if (cond.getCondition() == Condition.NOT_EQUALS) {
                    final Map<Integer, Boolean> linkTypes = new HashMap<Integer, Boolean>();

                    for (final Iterator<WorkItemLinkTypeEnd> it =
                        witContext.getClient().getLinkTypes().getLinkTypeEnds().iterator(); it.hasNext();) {
                        final WorkItemLinkTypeEnd t = it.next();

                        if (t.getID() != linkTypeEnd.getID()) {
                            linkTypes.put(new Integer(t.getID()), Boolean.TRUE);
                        }
                    }
                    return linkTypes;
                }

                // Others are not supported
                Tools.ensureSyntax(false, SyntaxError.INVALID_CONDITION_FOR_LINK_TYPE, cond);
            }

            // ignore other conditions
            return null;
        }

        if (node.getNodeType() == NodeType.AND) {
            final NodeAndOperator and = (NodeAndOperator) node;
            Map<Integer, Boolean> linkTypes = null;

            for (int i = 0; i < and.getCount(); i++) {
                final Map<Integer, Boolean> map = computeLinkTypes(and.getItem(i));
                if (linkTypes == null) {
                    linkTypes = map;
                } else if (map != null) {
                    final Map<Integer, Boolean> retMap = new HashMap<Integer, Boolean>();

                    // intersect linkTypes and map
                    for (final Iterator<Integer> it = map.keySet().iterator(); it.hasNext();) {
                        final Integer t = it.next();

                        if (linkTypes.containsKey(t)) {
                            retMap.put(t, Boolean.TRUE);
                        }
                    }

                    linkTypes = retMap;
                }
            }
            return linkTypes;
        }

        if (node.getNodeType() == NodeType.NOT) {
            final NodeNotOperator not = (NodeNotOperator) node;

            final Map<Integer, Boolean> linkTypes = computeLinkTypes(not.getValue());

            if (linkTypes != null) {
                final Map<Integer, Boolean> ret = new HashMap<Integer, Boolean>();

                // inversion condition
                for (final Iterator<WorkItemLinkTypeEnd> it =
                    witContext.getClient().getLinkTypes().getLinkTypeEnds().iterator(); it.hasNext();) {
                    final WorkItemLinkTypeEnd t = it.next();

                    /* TODO: this looks like a programming error */
                    if (!linkTypes.containsKey(new Integer(t.getID()))) {
                        ;
                    }
                    {
                        ret.put(new Integer(t.getID()), Boolean.TRUE);
                    }
                }

                return ret;
            }
            return null;
        }

        Tools.ensureSyntax(false, SyntaxError.INVALID_CONDITION_FOR_LINK_TYPE, node);
        return null;
    }

    private int getTreeID(final String path, final int nodeStructureType) {
        final NodeImpl targetNode = witContext.getRootNode().findNodeDownwards(path, false, nodeStructureType);
        if (targetNode == null) {
            return -1;
        }
        return targetNode.getID();
    }

    @Override
    public DataType getVariableDataType(final Object variableTag) {
        if (context != null) {
            final Object object = context.get(variableTag);
            if (object != null) {
                if (object instanceof Date) {
                    return DataType.DATE;
                }
                if (object instanceof Integer || object instanceof Double) {
                    return DataType.NUMERIC;
                }
                return DataType.STRING;
            }
        }

        if ("me".equals(variableTag)) //$NON-NLS-1$
        {
            return DataType.STRING;
        }

        if ("today".equals(variableTag)) //$NON-NLS-1$
        {
            return DataType.DATE;
        }

        if ("currentIteration".equals(variableTag)) //$NON-NLS-1$
        {
            return DataType.STRING;
        }

        return DataType.UNKNOWN;
    }

    private String GetContextValueType(final Object val) {
        // these are the only types supported by
        // WIQLAdapter.GetVariableDataType()
        if (val instanceof Date) {
            return "DateTime"; //$NON-NLS-1$
        } else if (val instanceof Integer) {
            return "Number"; //$NON-NLS-1$
        } else if (val instanceof Double) {
            return "Double"; //$NON-NLS-1$
        } else {
            return "String"; //$NON-NLS-1$
        }
    }

    @Override
    public Node optimizeNode(Node node, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (node.getNodeType() == NodeType.FIELD_CONDITION) {
            final NodeCondition nodeCondition = (NodeCondition) node;

            // convert (A in B) block to the multiple blocks
            if (nodeCondition.getCondition() == Condition.IN) {
                // create new Or node
                final NodeOrOperator nodeOr = new NodeOrOperator();
                final NodeValueList valueList = (NodeValueList) nodeCondition.getRight();

                // process all values inside value list
                for (final Iterator<Node> it = valueList.iterator(); it.hasNext();) {
                    final Node valueItem = it.next();
                    nodeOr.add(new NodeCondition(Condition.EQUALS, nodeCondition.getLeft(), valueItem));
                }

                // optimize new "Or" group
                return nodeOr.optimize(this, tableContext, fieldContext);
            } else if (nodeCondition.getCondition() == Condition.CONTAINS
                || nodeCondition.getCondition() == Condition.CONTAINS_WORDS) {
                // optimize contains
                final String s = nodeCondition.getRight().getConstStringValue();
                if (s != null && s.length() == 0) {
                    // empty pattern - always true
                    return new NodeBoolConst(true);
                }
            }

            // field usage is a tag of the left part
            final NodeFieldName field = nodeCondition.getLeft();
            final FieldDefinitionImpl fd = (FieldDefinitionImpl) field.getTag();

            // convert Project-field condition to Area Path conditions
            if (fd.getID() == CoreFields.TEAM_PROJECT) {
                final String pathValue = nodeCondition.getRight().getConstStringValue();
                if (pathValue != null) {
                    final FieldDefinitionImpl pathField =
                        witContext.getFieldDefinitions().getFieldDefinitionInternal(CoreFields.AREA_PATH);

                    // construct new condition
                    node = new NodeCondition(
                        Condition.UNDER,
                        new NodeFieldName(field.getPrefix(), pathField.getName()),
                        new NodeString(pathValue));

                    if (nodeCondition.getCondition() == Condition.NOT_EQUALS) {
                        node = new NodeNotOperator(node);
                    }

                    // Bind new operator
                    node.bind(this, tableContext, fieldContext);
                }
            }

            if (isNonNullableField(fd.getID())) {
                // optimize contains
                final String s = nodeCondition.getRight().getConstStringValue();
                if (s != null && s.length() == 0) {
                    if (nodeCondition.getCondition() == Condition.NOT_EQUALS) {
                        // it always not empty
                        return new NodeBoolConst(true);
                    }
                }
            }

            if (dayPrecision && (field.getDataType() == DataType.DATE)) {
                // check time part of a constant -- must be 0
                final String rightString = nodeCondition.getRight().getConstStringValue();
                if (rightString != null && rightString.length() != 0) {
                    Date d = DateTime.parse(rightString, getLocale(), getTimeZone());
                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTimeZone(getTimeZone());
                    calendar.setTime(d);

                    final boolean nonZeroTime = (calendar.get(Calendar.HOUR_OF_DAY) != 0)
                        || (calendar.get(Calendar.MINUTE) != 0)
                        || (calendar.get(Calendar.SECOND) != 0)
                        || (calendar.get(Calendar.MILLISECOND) != 0);
                    Tools.ensureSyntax(!nonZeroTime, SyntaxError.NON_ZERO_TIME, node);

                    // Create constants for start and end of period
                    final NodeString startDay = new NodeString(DateTime.formatRoundTripLocal(d, getTimeZone()));

                    // TODO: Handle d == 31/12/9999 case (VS Bug 613231)

                    // Add a day
                    final Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    c.setTimeZone(getTimeZone());
                    c.add(Calendar.DAY_OF_MONTH, 1);
                    d = c.getTime();

                    final NodeString nextDay = new NodeString(DateTime.formatRoundTripLocal(d, getTimeZone()));

                    if (nodeCondition.getCondition() == Condition.EQUALS) {
                        // Convert to a pair of conditions
                        final NodeAndOperator nodeAnd = new NodeAndOperator();
                        nodeAnd.add(new NodeCondition(Condition.GREATER_OR_EQUALS, nodeCondition.getLeft(), startDay));
                        nodeAnd.add(new NodeCondition(Condition.LESS, nodeCondition.getLeft(), nextDay));
                        node = nodeAnd;
                        node.bind(this, tableContext, fieldContext);
                    } else if (nodeCondition.getCondition() == Condition.NOT_EQUALS) {
                        final NodeOrOperator nodeOr = new NodeOrOperator();
                        nodeOr.add(new NodeCondition(Condition.LESS, nodeCondition.getLeft(), startDay));
                        nodeOr.add(new NodeCondition(Condition.GREATER_OR_EQUALS, nodeCondition.getLeft(), nextDay));
                        node = nodeOr;
                        node.bind(this, tableContext, fieldContext);
                    } else if (nodeCondition.getCondition() == Condition.LESS
                        || nodeCondition.getCondition() == Condition.GREATER_OR_EQUALS) {
                        nodeCondition.setRight(startDay);
                    } else if (nodeCondition.getCondition() == Condition.GREATER) {
                        nodeCondition.setCondition(Condition.GREATER_OR_EQUALS);
                        nodeCondition.setRight(nextDay);
                    } else if (nodeCondition.getCondition() == Condition.LESS_OR_EQUALS) {
                        nodeCondition.setCondition(Condition.LESS);
                        nodeCondition.setRight(nextDay);
                    } else {
                        Tools.ensureSyntax(false, SyntaxError.INVALID_CONDITIONAL_OPERATOR, node);
                    }
                }
            }
        } else if (node.getNodeType() == NodeType.VARIABLE) {
            final NodeVariable nodeVariable = (NodeVariable) node;
            if (context != null) {
                final Object o = context.get(nodeVariable.getTag());
                if (o != null) {
                    if ((o instanceof Integer) || (o instanceof Double)) {
                        return new NodeNumber(o.toString());
                    }
                    return new NodeString(o.toString());
                }
            }
            if (ME.equals(nodeVariable.getTag())) {
                // replace "me" with user name
                return new NodeString(witContext.getCurrentUserDisplayName());
            }
            if (TODAY.equals(nodeVariable.getTag())) {
                final Date today = DateTime.today(getTimeZone());
                return new NodeString(DateTime.formatRoundTripLocal(today, getTimeZone()));
            }
        } else if (node.getNodeType() == NodeType.ARITHMETIC) {
            final NodeArithmetic a = (NodeArithmetic) node;
            final NodeNumber right = (NodeNumber) a.getRight();

            if (fieldContext == null || fieldContext.getDataType() == DataType.NUMERIC) {
                final NodeNumber left = (NodeNumber) a.getLeft();
                double d = Double.parseDouble(left.getValue());
                final double inc = Double.parseDouble(right.getValue());
                if (a.getArithmetic() == Arithmetic.ADD) {
                    d += inc;
                } else if (a.getArithmetic() == Arithmetic.SUBTRACT) {
                    d -= inc;
                } else {
                    throw new RuntimeException();
                }
                return new NodeNumber(String.valueOf(d));
            }

            if (fieldContext != null && fieldContext.getDataType() == DataType.DATE) {
                final NodeString left = (NodeString) a.getLeft();
                Date d = DateTime.parse(left.getValue(), getLocale(), getTimeZone());

                boolean isIntegerDays = false;
                int integerDays = -1;
                try {
                    integerDays = Integer.parseInt(right.getValue());
                    isIntegerDays = true;
                } catch (final NumberFormatException ex) {
                }

                if (isIntegerDays) {
                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTime(d);
                    calendar.setTimeZone(getTimeZone());

                    if (a.getArithmetic() == Arithmetic.ADD) {
                        calendar.add(Calendar.DAY_OF_MONTH, integerDays);
                    } else if (a.getArithmetic() == Arithmetic.SUBTRACT) {
                        calendar.add(Calendar.DAY_OF_MONTH, -1 * integerDays);
                    } else {
                        throw new UnsupportedOperationException();
                    }

                    d = calendar.getTime();
                } else {
                    final double doubleDays = Double.parseDouble(right.getValue());
                    final long millis = (long) ((doubleDays * (60 * 60 * 24 * 1000)) + 0.5);
                    if (a.getArithmetic() == Arithmetic.ADD) {
                        d = new Date(d.getTime() + millis);
                    } else if (a.getArithmetic() == Arithmetic.SUBTRACT) {
                        d = new Date(d.getTime() - millis);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }

                return new NodeString(DateTime.formatRoundTripLocal(d, getTimeZone()));
            }
        }

        return node;
    }

    private Element queryXML(final Node node, final Document doc, final boolean not, boolean ever, boolean num) {

        if (node.getNodeType() == NodeType.FIELD_NAME) {
            final FieldDefinition fd = (FieldDefinition) ((NodeFieldName) node).getTag();
            final Element element = doc.createElement(QueryPackageNames.VALUE_TYPE_COLUMN);
            element.appendChild(doc.createTextNode(fd.getReferenceName()));
            return element;
        }

        if (node.getNodeType() == NodeType.STRING) {
            final Element element = doc.createElement(QueryPackageNames.VALUE_TYPE_STRING);
            element.appendChild(doc.createTextNode(((NodeString) node).getValue()));
            return element;
        }

        if (node.getNodeType() == NodeType.NUMBER) {
            final Element element =
                doc.createElement((num ? QueryPackageNames.VALUE_TYPE_NUMBER : QueryPackageNames.VALUE_TYPE_STRING));
            element.appendChild(doc.createTextNode(((NodeNumber) node).getValue()));
            return element;
        }

        if (node.getNodeType() == NodeType.BOOL_VALUE) {
            final Element element = doc.createElement(QueryPackageNames.VALUE_TYPE_NUMBER);
            element.appendChild(doc.createTextNode(((NodeBoolValue) node).getBoolValue() ? "1" : "0")); //$NON-NLS-1$ //$NON-NLS-2$
            return element;
        }

        if (node.getNodeType() == NodeType.FIELD_CONDITION) {
            final NodeCondition cond = (NodeCondition) node;
            final NodeFieldName field = cond.getLeft();
            FieldDefinition fd = (FieldDefinition) field.getTag();

            // Store the original field type - this is what the .NET Client
            // does.
            final int fieldType = ((FieldDefinitionImpl) fd).getPSType();

            Node right = cond.getRight();

            if (fd.getID() == CoreFields.AREA_PATH) {
                // Replace tree path by tree ID
                final String path = right.getConstStringValue();
                fd = witContext.getClient().getFieldDefinitions().get(CoreFieldReferenceNames.AREA_ID);
                if (path.length() != 0) {
                    final int treeId = getTreeID(path, NodeStructureType.AREA);
                    Tools.ensureSyntax(treeId != -1, SyntaxError.TREE_PATH_IS_NOT_FOUND_IN_HIERARCHY, right);
                    right = new NodeNumber(String.valueOf(treeId));
                }
            } else if (fd.getID() == CoreFields.ITERATION_PATH) {
                // Replace iteration path by iterationId
                final String path = right.getConstStringValue();
                Tools.ensureSyntax(path != null, SyntaxError.EXPECTING_VALUE, right);
                fd = witContext.getClient().getFieldDefinitions().get(CoreFieldReferenceNames.ITERATION_ID);
                if (path.length() != 0) {
                    final int treeId = getTreeID(path, NodeStructureType.ITERATION);
                    Tools.ensureSyntax(treeId != -1, SyntaxError.TREE_PATH_IS_NOT_FOUND_IN_HIERARCHY, right);
                    right = new NodeNumber(String.valueOf(treeId));
                }
            } else if (fd.getID() == CoreFields.LINK_TYPE) {
                // Get the link end id
                final WorkItemLinkTypeEnd linkTypeEnd = getLinkTypeByName(right.getConstStringValue());
                Tools.ensureSyntax(linkTypeEnd != null, SyntaxError.INVALID_LINK_TYPE_NAME, right);
                right = new NodeNumber(String.valueOf(linkTypeEnd.getID()));
            }

            final Element x = doc.createElement(QueryPackageNames.EXPRESSION);
            x.setAttribute(UpdatePackageNames.COLUMN, fd.getReferenceName());
            x.setAttribute(QueryPackageNames.FIELD_TYPE, String.valueOf(fieldType));

            if (cond.getCondition() == Condition.CONTAINS || cond.getCondition() == Condition.CONTAINS_WORDS) {
                // Emulate using contains
                final int c = cond.getCondition().getValue();
                final String pattern = cond.getRight().getConstStringValue();

                // Always use ever for history
                if (fd.getID() == WorkItemFieldIDs.HISTORY) {
                    ever = true;
                }

                // set operator
                x.setAttribute(QueryPackageNames.OPERATOR, getConditionalOperator(c, not, ever, cond));

                // set children
                final Element xpat = doc.createElement(QueryPackageNames.VALUE_TYPE_STRING);
                xpat.appendChild(doc.createTextNode(pattern));
                x.appendChild(xpat);
            } else if (cond.getCondition() == Condition.GROUP) {
                // Set Operator
                x.setAttribute(
                    QueryPackageNames.OPERATOR,
                    getConditionalOperator(cond.getCondition().getValue(), not, ever, cond));
                x.setAttribute(QueryPackageNames.EXPAND_CONSTANT, "true"); //$NON-NLS-1$

                // Set Children
                final String groupName = cond.getRight().getConstStringValue();
                final Element xmlGroupElement = doc.createElement(QueryPackageNames.VALUE_TYPE_STRING);
                xmlGroupElement.appendChild(doc.createTextNode(groupName));
                x.appendChild(xmlGroupElement);
            } else {
                // Set operator
                x.setAttribute(
                    QueryPackageNames.OPERATOR,
                    getConditionalOperator(cond.getCondition().getValue(), not, ever, cond));

                if (right.getNodeType() == NodeType.FIELD_NAME) {
                    // Field condition
                    x.appendChild(queryXML(right, doc, not, ever, false));
                } else if (field.getDataType() == DataType.DATE) {
                    // convert dates to UTC
                    String s = right.getConstStringValue();
                    Tools.ensureSyntax(s != null, SyntaxError.EXPECTING_VALUE, right);

                    if (s.length() != 0) {
                        final Date dt = DateTime.parse(s, getLocale(), getTimeZone());
                        s = DateTime.formatRoundTripUniversal(dt);
                    }
                    final Element xdt = doc.createElement(QueryPackageNames.VALUE_TYPE_DATE_TIME);
                    xdt.appendChild(doc.createTextNode(s));
                    x.appendChild(xdt);
                } else if (field.getDataType() == DataType.GUID) {
                    final Element xdt = doc.createElement(QueryPackageNames.VALUE_TYPE_GUID);
                    xdt.appendChild(doc.createTextNode(right.getConstStringValue()));
                    x.appendChild(xdt);
                } else {
                    // we need to use <number> tag for integer fields only
                    num = (fd.getFieldType() == FieldType.INTEGER);
                    x.appendChild(queryXML(right, doc, not, ever, num));
                }
            }

            return x;

        }

        if (node.getNodeType() == NodeType.AND || node.getNodeType() == NodeType.OR) {
            final String op = ((node.getNodeType() == NodeType.OR) == not) ? QueryPackageNames.OPERATOR_AND
                : QueryPackageNames.OPERATOR_OR;
            final Element x = doc.createElement(QueryPackageNames.GROUP);
            x.setAttribute(QueryPackageNames.GROUP_OPERATOR, op);

            // Call recursively for all nodes
            final int c = node.getCount();
            for (int i = 0; i < c; i++) {
                x.appendChild(queryXML(node.getItem(i), doc, not, ever, num));
            }
            return x;
        }

        if (node.getNodeType() == NodeType.NOT) {
            return queryXML(((NodeNotOperator) node).getValue(), doc, !not, ever, num);
        }

        if (node.getNodeType() == NodeType.EVER) {
            return queryXML(((NodeEverOperator) node).getValue(), doc, not, true, num);
        }

        if (node.getNodeType() == NodeType.BOOL_CONST) {
            // emulate boolean constant by ID=0 or ID!=0
            final String op = (((NodeBoolConst) node).getValue() == not) ? QueryPackageNames.EXPRESSION_OPERATOR_EQUALS
                : QueryPackageNames.EXPRESSION_OPERATOR_NOT_EQUALS;

            final Element x = doc.createElement(QueryPackageNames.EXPRESSION);
            x.setAttribute(UpdatePackageNames.COLUMN, CoreFieldReferenceNames.ID);
            x.setAttribute(QueryPackageNames.OPERATOR, op);

            final Element x0 = doc.createElement(QueryPackageNames.VALUE_TYPE_NUMBER);
            x0.appendChild(doc.createTextNode("0")); //$NON-NLS-1$
            x.appendChild(x0);

            return x;
        }

        Tools.ensureSyntax(false, SyntaxError.INVALID_NODE_TYPE, node);
        return null;
    }

    private void verifyCondition(
        final NodeFieldName left,
        final Node right,
        final Condition op,
        final Node errorNode,
        final LinkQueryMode mode) {
        Tools.ensureSyntax(op != Condition.NONE, SyntaxError.INVALID_CONDITIONAL_OPERATOR, errorNode);

        final FieldDefinition fd = (FieldDefinition) left.getTag();

        if (NodeType.FIELD_NAME.equals(right.getNodeType())) {
            if (!witContext.getServerInfo().isSupported(SupportedFeatures.QUERY_FIELDS_COMPARISON)) {
                throw new NotSupportedException(
                    Messages.getString("WiqlAdapter.CannotRunQueryThatComparesFieldsOnThisServer")); //$NON-NLS-1$
            }

            if (LinkQueryMode.LINKS_MUST_CONTAIN.equals(mode)
                || LinkQueryMode.LINKS_MAY_CONTAIN.equals(mode)
                || LinkQueryMode.LINKS_DOES_NOT_CONTAIN.equals(mode)
                || LinkQueryMode.LINKS_RECURSIVE.equals(mode)) {
                Tools.ensureSyntax(
                    left.getPrefix().equalsIgnoreCase(((NodeFieldName) right).getPrefix()),
                    SyntaxError.FIELD_CONDITIONS_IN_LINK_QUERIES,
                    errorNode);
            }

            // Long text fields cannot be used in expressions

            Tools.ensureSyntax(
                (!FieldType.PLAINTEXT.equals(fd.getFieldType()))
                    && (!FieldType.HTML.equals(fd.getFieldType()))
                    && (!FieldType.HISTORY.equals(fd.getFieldType())),
                SyntaxError.INVALID_FIELD_TYPE_FOR_CONDITION,
                errorNode);

            final FieldDefinition rfd = (FieldDefinition) ((NodeFieldName) right).getTag();

            Tools.ensureSyntax(
                (!FieldType.PLAINTEXT.equals(rfd.getFieldType()))
                    && (!FieldType.HTML.equals(rfd.getFieldType()))
                    && (!FieldType.HISTORY.equals(rfd.getFieldType())),
                SyntaxError.INVALID_FIELD_TYPE_FOR_CONDITION,
                errorNode);

            return;
        }

        // Check possible operators and right part types
        final String rightString = right.getConstStringValue();

        if (op == Condition.IN) {
            final NodeValueList valueList = (NodeValueList) right;
            for (final Iterator<Node> it = valueList.iterator(); it.hasNext();) {
                // check all values inside list.
                verifyCondition(left, it.next(), Condition.EQUALS, errorNode, mode);
            }
            return;
        } else if (op == Condition.GROUP) {
            if (fd.getID() == CoreFields.WORK_ITEM_TYPE) {
                if (!witContext.getServerInfo().isSupported(SupportedFeatures.WORK_ITEM_TYPE_CATEGORIES)) {
                    throw new NotSupportedException(
                        Messages.getString("WiqlAdapter.CannotCreateCategoriesOfWorkItemsOnThisServer")); //$NON-NLS-1$
                }
            } else {
                if (!witContext.getServerInfo().isSupported(SupportedFeatures.QUERY_IN_GROUP_FILTER)) {
                    throw new NotSupportedException(
                        Messages.getString("WiqlAdapter.CannotUseInGroupOperatorToRunQueryOnThisServer")); //$NON-NLS-1$
                }
                // "in group" works for string fields only
                if (!FieldType.STRING.equals(fd.getFieldType())) {
                    throw new NotSupportedException(
                        MessageFormat.format(
                            Messages.getString("WiqlAdapter.CannotUseInGroupOperatorWithFieldOnThisServerFormat"), //$NON-NLS-1$
                            fd.getName(),
                            ProvisionValues.FIELD_TYPE_STRING));
                }
            }
        }

        // Comparing with empty string
        if (rightString != null && rightString.length() == 0) {
            Tools.ensureSyntax(
                op == Condition.EQUALS || op == Condition.NOT_EQUALS,
                SyntaxError.INVALID_CONDITION_FOR_EMPTY_STRING,
                errorNode);
        }

        // Check Tree path operators and constant
        if (FieldType.TREEPATH == fd.getFieldType()) {
            Tools.ensureSyntax(
                op == Condition.EQUALS || op == Condition.NOT_EQUALS || op == Condition.UNDER,
                SyntaxError.INVALID_CONDITION_FOR_TREE_FIELD,
                errorNode);

            Tools.ensureSyntax(
                right.getDataType() == DataType.STRING,
                SyntaxError.PATH_MUST_BE_A_STRING_NOT_STARTING_WITH_BACKSLASH,
                right);

            if (rightString != null && (rightString.length() != 0 || op == Condition.UNDER)) {
                Tools.ensureSyntax(
                    !rightString.startsWith("\\"), //$NON-NLS-1$
                    SyntaxError.PATH_MUST_BE_A_STRING_NOT_STARTING_WITH_BACKSLASH,
                    right);
                final int type =
                    fd.getID() == CoreFields.AREA_PATH ? NodeStructureType.AREA : NodeStructureType.ITERATION;

                Tools.ensureSyntax(
                    getTreeID(rightString, type) != -1,
                    SyntaxError.TREE_PATH_IS_NOT_FOUND_IN_HIERARCHY,
                    right);
            }
        } else if (FieldType.PLAINTEXT == fd.getFieldType()
            || FieldType.HISTORY == fd.getFieldType()
            || FieldType.HTML == fd.getFieldType()) {
            // Long text fields
            if (rightString != null) {
                Tools.ensureSyntax(
                    op == Condition.CONTAINS || op == Condition.CONTAINS_WORDS,
                    SyntaxError.INVALID_CONDITION_FOR_LONG_TEXT_FIELD,
                    errorNode);

                Tools.ensureSyntax(
                    rightString.trim().length() > 0,
                    SyntaxError.INVALID_LONG_TEXT_SEARCH_FOR_WHITESPACE,
                    errorNode);
            }
        } else if (FieldType.STRING == fd.getFieldType() && CoreFields.TEAM_PROJECT == fd.getID()) {
            // Check valid operators for tree fields
            Tools.ensureSyntax(
                op == Condition.EQUALS || op == Condition.NOT_EQUALS,
                SyntaxError.INVALID_CONDITION_FOR_NODE_FIELD,
                errorNode);

            if (rightString != null && (rightString.length() != 0 || op == Condition.UNDER)) {
                Tools.ensureSyntax(rightString.indexOf("\\") == -1, SyntaxError.INVALID_PROJECT_NAME, right); //$NON-NLS-1$

                final int treeId = getTreeID(rightString, NodeStructureType.AREA);
                Tools.ensureSyntax(treeId != -1, SyntaxError.PROJECT_NOT_FOUND, right);
            }
        } else if (FieldType.INTEGER == fd.getFieldType() && fd.getID() == CoreFields.LINK_TYPE) {
            // Check valid operators for the link field
            Tools.ensureSyntax(
                op == Condition.EQUALS || op == Condition.NOT_EQUALS,
                SyntaxError.INVALID_CONDITION_FOR_LINK_TYPE,
                errorNode);

            // Check string
            if (rightString != null && !(rightString.length() == 0 && op == Condition.NOT_EQUALS)) {
                final WorkItemLinkTypeEnd linkTypeEnd = getLinkTypeByName(rightString);
                Tools.ensureSyntax(linkTypeEnd != null, SyntaxError.INVALID_LINK_TYPE_NAME, right);

                if (LinkQueryMode.LINKS_RECURSIVE.equals(mode)) {
                    Tools.ensureSyntax(
                        Topology.TREE == linkTypeEnd.getLinkType().getLinkTopology() && linkTypeEnd.isForwardLink(),
                        SyntaxError.INVALID_LINK_TYPE_NAME_RECURSIVE,
                        right);
                }
            }
        }

        else if (FieldType.INTEGER == fd.getFieldType() && rightString != null && rightString.length() != 0) {
            if (rightString != null && rightString.length() > 0) {
                Tools.ensureSyntax(
                    Pattern.matches("[+-]?[0-9]+", rightString), //$NON-NLS-1$
                    SyntaxError.INCOMPATIBLE_RIGHT_CONST,
                    right);
            }
        }

        if (op == Condition.UNDER) {
            Tools.ensureSyntax(
                FieldType.TREEPATH == fd.getFieldType(),
                SyntaxError.UNDER_CAN_BE_USED_FOR_TREE_PATH_FIELD_ONLY,
                left);
        }
    }

    private WorkItemLinkTypeEnd getLinkTypeByName(final String linkTypeName) {
        return witContext.getClient().getLinkTypes().getLinkTypeEnds().get(linkTypeName);
    }

    @Override
    public void verifyNode(final Node node, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        // verify conditions
        if (node.getNodeType() == NodeType.FIELD_CONDITION) {
            final NodeCondition conditionNode = (NodeCondition) node;
            verifyCondition(
                conditionNode.getLeft(),
                conditionNode.getRight(),
                conditionNode.getCondition(),
                conditionNode,
                (LinkQueryMode) tableContext.getTag());
        } else if (node.getNodeType() == NodeType.EVER) {
            final NodeEverOperator nodeEver = (NodeEverOperator) node;

            // Check that is must be a simple condition inside ever
            Tools.ensureSyntax(
                nodeEver.getValue().getNodeType() == NodeType.FIELD_CONDITION,
                SyntaxError.TOO_COMPLEX_EVER_OPERATOR,
                nodeEver);

            // check that condition must be 'equals' or 'contains'
            final NodeCondition nodeCondition = (NodeCondition) nodeEver.getValue();
            Tools.ensureSyntax(
                nodeCondition.getCondition() == Condition.EQUALS
                    || nodeCondition.getCondition() == Condition.CONTAINS
                    || nodeCondition.getCondition() == Condition.CONTAINS_WORDS,
                SyntaxError.EVER_NOT_EQUAL_OPERATOR,
                nodeCondition);

            if (nodeCondition.getLeft().getDataType() == DataType.DATE && dayPrecision) {
                // can't support ever with date precision
                Tools.ensureSyntax(
                    nodeCondition.getRight().getConstStringValue().length() == 0,
                    SyntaxError.EVER_WITH_DATE_PRECISION,
                    nodeCondition);

                final FieldDefinition fd = (FieldDefinition) nodeCondition.getLeft().getTag();
                if (fd != null) {
                    // Ever is supported for work item fields only
                    Tools.ensureSyntax(
                        (fd.getUsage().getValue() & FieldUsages.WORK_ITEM.getValue()) != 0,
                        SyntaxError.EVER_WITH_LINK_QUERY,
                        node);
                }
            }
        } else if (node.getNodeType() == NodeType.FIELD_LIST) {
            for (final Iterator<Node> it = node.iterator(); it.hasNext();) {
                final NodeFieldName fieldNameNode = (NodeFieldName) it.next();
                final FieldDefinition tag = (FieldDefinition) fieldNameNode.getTag();

                // Check the field is queryable
                Tools.ensureSyntax(tag.isQueryable(), SyntaxError.NON_QUERYABLE_FIELD, fieldNameNode);
            }
        } else if (node.getNodeType() == NodeType.ORDER_FIELD_LIST) {
            for (final Iterator<Node> it = node.iterator(); it.hasNext();) {
                final NodeFieldName field = (NodeFieldName) it.next();
                final FieldDefinition fd = (FieldDefinition) field.getTag();

                if ((fd.getUsage().getValue() & FieldUsages.WORK_ITEM_LINK.getValue()) != 0) {
                    // Tools.ensureSyntax(
                    // (LinkQueryMode) tableContext.getTag() !=
                    // LinkQueryMode.WORK_ITEMS,
                    // SyntaxError.ORDER_BY_LINK_FIELD,
                    // field);
                }

                for (final Iterator<Node> it2 = node.iterator(); it2.hasNext();) {
                    final NodeFieldName field1 = (NodeFieldName) it2.next();
                    if (field1 == field) {
                        break;
                    }
                    Tools.ensureSyntax(
                        fd != (FieldDefinition) field1.getTag(),
                        SyntaxError.DUPLICATE_ORDER_BY_FIELD,
                        field);
                }

                Tools.ensureSyntax(fd.isSortable(), SyntaxError.NON_SORTABLE_FIELD, field);
            }
        } else if (node.getNodeType() == NodeType.GROUP_FIELD_LIST) {
            // nothing to verify
        } else if (node.getNodeType() == NodeType.ARITHMETIC) {
            final NodeArithmetic arithmeticNode = (NodeArithmetic) node;
            Tools.ensureSyntax(
                (arithmeticNode.getLeft().canCastTo(DataType.DATE, getLocale())
                    || arithmeticNode.getLeft().canCastTo(DataType.NUMERIC, getLocale())),
                SyntaxError.WRONG_TYPE_FOR_ARITHMETIC,
                node);
            Tools.ensureSyntax(
                arithmeticNode.getRight().getDataType() == DataType.NUMERIC,
                SyntaxError.WRONG_TYPE_FOR_ARITHMETIC_RIGHT_OPERAND,
                node);
        } else if (node.getNodeType() == NodeType.MODE) {
            // verify mode and store the query mode in the tableContext
            final NodeMode nodeMode = (NodeMode) node;
            final LinkQueryMode mode = getQueryMode(nodeMode, tableContext);
            Tools.ensureSyntax(mode != LinkQueryMode.UNKNOWN, SyntaxError.UNKNOWN_MODE, node);
            tableContext.setTag(mode);
        } else if (node.getNodeType() == NodeType.SELECT) {
            final NodeSelect nodeSelect = (NodeSelect) node;
            Tools.ensureSyntax(nodeSelect.getFrom().getTag() != null, SyntaxError.EXPECTING_TABLE_NAME, nodeSelect);
            Tools.ensureSyntax(
                nodeSelect.getGroupBy() == null,
                SyntaxError.GROUP_BY_IS_NOT_SUPPORTED,
                nodeSelect.getGroupBy());

            if ((LinkQueryMode) nodeSelect.getFrom().getTag() == LinkQueryMode.LINKS_RECURSIVE) {
                Tools.ensureSyntax(
                    nodeSelect.getAsOf() == null,
                    SyntaxError.NOT_SUPPORTED_TREE_QUERY,
                    nodeSelect.getAsOf());
            }
        }
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(final Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public Locale getLocale() {
        return witContext.getConnection().getLocale();
    }

    public boolean isDayPrecision() {
        return dayPrecision;
    }

    public void setDayPrecision(final boolean dayPrecision) {
        this.dayPrecision = dayPrecision;
    }

    @Override
    public TimeZone getTimeZone() {
        return witContext.getConnection().getTimeZone();
    }

    public static boolean fieldSupportsAnySyntax(final WorkItemClient client, final String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            return false;
        }

        final FieldDefinition fd = client.getFieldDefinitions().get(fieldName);
        return fd != null && fieldSupportsAnySyntax(fd.getID());
    }

    public static boolean fieldSupportsAnySyntax(final int fieldId) {
        return isNonNullableField(fieldId);
    }

    public static boolean isNonNullableField(final int fieldId) {
        return (fieldId == CoreFields.WORK_ITEM_TYPE || fieldId == CoreFields.STATE || fieldId == CoreFields.LINK_TYPE);
    }
}
