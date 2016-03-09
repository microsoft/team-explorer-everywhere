// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy.QueryDefinitionUtil;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime.UncheckedParseException;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryGrouping;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryModifiedListener;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRow;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRowCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.StandardListenerList;

public class QEQueryImpl implements QEQuery {
    private final WorkItemClient workItemClient;
    private QueryType queryType;
    private LinkQueryMode linkQueryMode;
    private final QEQueryRowCollectionImpl sourceRows;
    private final QEQueryRowCollectionImpl targetRows;
    private boolean useSelectedLinkTypes;
    private final HashSet linkQueryLinkTypes;
    private String treeQueryLinkType;
    private final ListenerList modifiedListeners = new StandardListenerList();

    public QEQueryImpl(final WorkItemClient workItemClient, final String wiql, final LinkQueryMode linkQueryMode) {
        this.workItemClient = workItemClient;
        this.linkQueryMode = linkQueryMode;
        queryType = QueryDefinitionUtil.getQueryType(linkQueryMode);

        sourceRows = new QEQueryRowCollectionImpl(this);
        targetRows = new QEQueryRowCollectionImpl(this);

        linkQueryLinkTypes = new HashSet();
        treeQueryLinkType = getDefaultTreeQueryLinkName();

        final WIQLTranslator wiqlTranslator = new WIQLTranslator(
            this,
            new WIQLTranslatorFieldDefinitionCollectionAdatper(workItemClient.getFieldDefinitions()));

        wiqlTranslator.fromWIQL(wiql, true);
    }

    /*
     * ************************************************************************
     * START of implementation of QEQuery interface
     * ***********************************************************************
     */

    @Override
    public String getFilterExpression() {
        final WIQLTranslator wiqlTranslator = new WIQLTranslator(
            this,
            new WIQLTranslatorFieldDefinitionCollectionAdatper(workItemClient.getFieldDefinitions()));

        return wiqlTranslator.asWIQL(false);
    }

    @Override
    public WorkItemClient getWorkItemClient() {
        return workItemClient;
    }

    @Override
    public QEQueryRowCollection getSourceRowCollection() {
        return sourceRows;
    }

    @Override
    public QEQueryRowCollection getTargetRowCollection() {
        return targetRows;
    }

    @Override
    public boolean getUseSelectedLinkTypes() {
        return useSelectedLinkTypes;
    }

    @Override
    public String[] getLinkQueryLinkTypes() {
        return (String[]) linkQueryLinkTypes.toArray(new String[linkQueryLinkTypes.size()]);
    }

    @Override
    public String getTreeQueryLinkType() {
        return treeQueryLinkType;
    }

    @Override
    public void setUseSelectedLinkTypes(final boolean useSelected) {
        if (useSelectedLinkTypes != useSelected) {
            useSelectedLinkTypes = useSelected;
            notifyModifiedListeners();
        }
    }

    @Override
    public void setTreeQueryLinkType(final String referenceName) {
        if (treeQueryLinkType == null || !treeQueryLinkType.equals(referenceName)) {
            treeQueryLinkType = referenceName;
            notifyModifiedListeners();
        }
    }

    @Override
    public void addLinkQueryLinkType(final String referenceName) {
        if (!linkQueryLinkTypes.contains(referenceName)) {
            linkQueryLinkTypes.add(referenceName);
            notifyModifiedListeners();
        }
    }

    @Override
    public void removeLinkQueryLinkType(final String referenceName) {
        if (linkQueryLinkTypes.contains(referenceName)) {
            linkQueryLinkTypes.remove(referenceName);
            notifyModifiedListeners();
        }
    }

    @Override
    public QueryType getQueryType() {
        return queryType;
    }

    @Override
    public void setQueryType(final QueryType value) {
        if (queryType != value) {
            queryType = value;
            setLinkQueryMode(QueryDefinitionUtil.getDefaultLinkQueryMode(value));
            notifyModifiedListeners();
        }
    }

    @Override
    public LinkQueryMode getLinkQueryMode() {
        return linkQueryMode;
    }

    @Override
    public void setLinkQueryMode(final LinkQueryMode mode) {
        if (mode != linkQueryMode) {
            linkQueryMode = mode;
            notifyModifiedListeners();
        }
    }

    @Override
    public boolean isValid() {
        return getInvalidMessage() == null;
    }

    @Override
    public String getInvalidMessage() {
        String message = null;
        final QEQueryRow[] sourceRows = getSourceRowCollection().getRows();
        final QEQueryRow[] targetRows = getTargetRowCollection().getRows();

        final QEQueryGrouping sourceGrouping = getSourceRowCollection().getGrouping();
        final QEQueryGrouping targetGrouping = getTargetRowCollection().getGrouping();

        for (int i = 0; i < sourceRows.length; i++) {
            message = getInvalidMessage(sourceGrouping, sourceRows[i], i, true);
            if (message != null) {
                return message;
            }
        }

        for (int i = 0; i < targetRows.length; i++) {
            message = getInvalidMessage(targetGrouping, targetRows[i], i, false);
            if (message != null) {
                return message;
            }
        }

        // Test to see if the user has chosen "Return selected link types" with
        // 0 selected link types.
        if (queryType == QueryType.ONE_HOP && linkQueryLinkTypes.size() == 0 && getUseSelectedLinkTypes()) {
            return Messages.getString("QEQueryImp.MustChooseAtLeastOneTypeOfLinkOrAny"); //$NON-NLS-1$
        }

        return null;
    }

    private String getInvalidMessage(
        final QEQueryGrouping grouping,
        final QEQueryRow row,
        final int rowIx,
        final boolean isSource) {
        String message = null;

        if (isNullOrEmpty(row.getLogicalOperator())) {
            if (rowIx > 0) {
                if (grouping.rowInGroup(rowIx) || !isNullOrEmpty(row.getFieldName())) {
                    message = Messages.getString("QEQueryImp.LogicalOperatorForRowNotFilledInCorrectlyFormat"); //$NON-NLS-1$
                }
            }
        }

        if (isNullOrEmpty(row.getFieldName())) {
            if ((rowIx == 0 && isSource) || grouping.rowInGroup(rowIx)) {
                message = Messages.getString("QEQueryImp.FieldForRowNotFilledInCorrectlyFormat"); //$NON-NLS-1$
            }
        }

        if (isNullOrEmpty(row.getOperator())) {
            message = Messages.getString("QEQueryImp.OperatorForRowNotRecognizedOrMissingFormat"); //$NON-NLS-1$
        } else {
            final String operator = WIQLOperators.getInvariantOperator(row.getOperator());
            if (WIQLOperators.isFieldNameOperator(operator)) {
                final String fieldName = row.getValue();
                final FieldDefinitionCollection fieldDefinitions = workItemClient.getFieldDefinitions();

                if (fieldName == null) {
                    message = Messages.getString("QEQueryImp.ValueForRowNotFilledInCorrectlyFormat"); //$NON-NLS-1$
                } else if (!fieldDefinitions.contains(fieldName)) {
                    message = Messages.getString("QEQueryImp.QueryReferencesFieldThatDoesNotExistInRowFormat"); //$NON-NLS-1$
                }
            } else if (!isNullOrEmpty(row.getFieldName())) {
                final String value = row.getValue();
                if (!isNullOrEmpty(value) && !value.startsWith("@")) //$NON-NLS-1$
                {
                    final FieldDefinitionCollection fieldDefinitions = workItemClient.getFieldDefinitions();
                    final FieldDefinition fd = fieldDefinitions.get(row.getFieldName());

                    if (fd.getFieldType() == FieldType.DATETIME) {
                        try {
                            DateTime.parse(value, Locale.getDefault(), TimeZone.getDefault());
                        } catch (final UncheckedParseException e) {
                            message = MessageFormat.format(
                                Messages.getString("QEQueryImp.ValueInRowNotRecognizedAsDateTimeFormat"), //$NON-NLS-1$
                                value);
                        }
                    } else if (fd.getFieldType() == FieldType.DOUBLE) {
                        try {
                            NumberFormat.getInstance().parse(value);
                        } catch (final ParseException e) {
                            message = MessageFormat.format(
                                Messages.getString("QEQueryImp.ValueInRowNotRecognizedAsNumberFormat"), //$NON-NLS-1$
                                value);
                        }
                    } else if (fd.getFieldType() == FieldType.GUID) {
                        try {
                            new GUID(value);
                        } catch (final IllegalArgumentException e) {
                            message = MessageFormat.format(
                                Messages.getString("QEQueryImp.ValueInRowNotGuidFormat"), //$NON-NLS-1$
                                value);
                        }
                    }
                }
            }
        }

        if (WIQLAdapter.fieldSupportsAnySyntax(workItemClient, row.getFieldName())) {
            final String value = row.getValue();
            if (value != null && value.equalsIgnoreCase(WIQLOperators.SPECIAL_ANY)) {
                final String operator = WIQLOperators.getInvariantOperator(row.getOperator());
                if (!operator.equals(WIQLOperators.EQUAL_TO)) {
                    message = MessageFormat.format(
                        Messages.getString("QEQueryImp.SpecialValueInRowCanOnlyBeUsedWithEqualsOperatorFormat"), //$NON-NLS-1$
                        WIQLOperators.getLocalizedOperator(WIQLOperators.SPECIAL_ANY),
                        WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
                }
            }
        }

        if (message != null) {
            return MessageFormat.format(message, new Object[] {
                new Integer(rowIx + 1).toString()
            });
        }

        return null;
    }

    /*
     * ************************************************************************
     * END of implementation of QEQuery interface
     * ***********************************************************************
     */

    private boolean isNullOrEmpty(final String s) {
        return s == null || s.trim().length() == 0;
    }

    private String getDefaultTreeQueryLinkName() {
        return "System.LinkTypes.Hierarchy-Forward"; //$NON-NLS-1$
    }

    @Override
    public void addModifiedListener(final QEQueryModifiedListener listener) {
        modifiedListeners.addListener(listener);
    }

    @Override
    public void removeModifiedListener(final QEQueryModifiedListener listener) {
        modifiedListeners.removeListener(listener);
    }

    public synchronized void notifyModifiedListeners() {
        final QEQueryModifiedListener[] listeners =
            (QEQueryModifiedListener[]) modifiedListeners.getListeners(new QEQueryModifiedListener[] {});
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onQueryModified(this);
        }
    }
}