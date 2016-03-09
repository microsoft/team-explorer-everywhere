// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLHelpers;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;

/**
 * A class through which the natural language informational text for a
 * particular search query may be generated. The text is generated as a list of
 * strings containing the query's criteria.
 */
public class WorkItemSearchCriteria {
    /*
     * Class ported from VS
     */

    private final Map<String, Map<String, List<String>>> criteria =
        new TreeMap<String, Map<String, List<String>>>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Raw criteria information available for testing purposes. For a proper
     * bullet list of search criteria, please use the "LocalizedCriteriaBullets"
     * property instead.
     * <p>
     *
     * This represents the field, operator and value data through a compacted
     * structure of nested dictionaries/lists:
     * <p>
     *
     * <pre>
     *    FIELD NAME -->        (OPERATOR --> VALUE LIST)
     *  ___________________________________________________________________________
     * |             |    _____________________________________________________    |
     * |             |   |                |                                    |   |
     * |             |   |   OPERATOR 1   |   [ VALUE 1 | VALUE 2 | VALUE 3 ]  |   |
     * |   FIELD 1   |   |________________|____________________________________|   |
     * |             |   |      ...       |                ...                 |   |
     * |             |   |________________|____________________________________|   |
     * |_____________|_____________________________________________________________|
     * |     ...     |                             ...                             |
     * |_____________|_____________________________________________________________|
     * </pre>
     */
    public Map<String, Map<String, List<String>>> getRawCriteriaInformation() {
        return criteria;
    }

    public List<String> getLocalizedCriteriaBullets() {
        final List<String> localizedCriteriaBullets = new ArrayList<String>();
        // for each field name, for each operation, generate string containing
        // all conditions in list
        for (final String fieldName : criteria.keySet()) {
            for (final String operation : criteria.get(fieldName).keySet()) {
                String formattedValues = null;
                final List<String> valuesList = criteria.get(fieldName).get(operation);
                for (int i = 0; i < valuesList.size(); i++) {
                    // first, enclose the value in single quotes
                    final String enclosedValue = WIQLHelpers.getDoubleQuotedValue(valuesList.get(i));

                    // if we're dealing with the first value, just start the
                    // list of values
                    if (i == 0) {
                        formattedValues = enclosedValue;
                        continue;
                    }

                    // if we're dealing with the last value, need to separate it
                    // differently i.e. with 'and' (if operator is positive)
                    // or 'or' (if operator is negative)
                    else if (i == (valuesList.size() - 1)) {
                        String listEndSeparator = null;
                        if (operation == WIQLOperators.NOT_CONTAINS
                            || operation == WIQLOperators.NOT_EQUAL_TO
                            || operation == WIQLOperators.NOT_UNDER
                            || operation == WorkItemSearchCriteriaHelper.INVARIANT_KEYWORD_NOT_CONTAINS) {
                            listEndSeparator =
                                Messages.getString("WorkItemSearchCriteria.CriterionListEndSeparatorOrFormat"); //$NON-NLS-1$
                        } else {
                            listEndSeparator =
                                Messages.getString("WorkItemSearchCriteria.CriterionListEndSeparatorAndFormat"); //$NON-NLS-1$
                        }

                        formattedValues = MessageFormat.format(listEndSeparator, formattedValues, enclosedValue);
                        continue;
                    }

                    // otherwise, just use something like a comma between the
                    // values
                    formattedValues =
                        MessageFormat.format(
                            Messages.getString("WorkItemSearchCriteria.CriterionListSeparatorFormat"), //$NON-NLS-1$
                            formattedValues,
                            enclosedValue);
                }

                // so now we have a list of values, a fieldname, and an
                // operation -- we can build the entire bullet point.
                final String criterion =
                    MessageFormat.format(
                        Messages.getString("WorkItemSearchCriteria.CriterionFormat"), //$NON-NLS-1$
                        fieldName,
                        WorkItemSearchCriteriaHelper.getLocalizedOperator(operation),
                        formattedValues);

                // add the criterion to the list of criteria.
                localizedCriteriaBullets.add(criterion);
            }
        }
        return localizedCriteriaBullets;
    }

    /**
     * A static function to add a criterion to the list of criteria stored
     * within this class.
     *
     * @param fieldName
     *        The fieldname associated with this criterion
     * @param operation
     *        The operation associated with this criterion, in the form of a
     *        WiqlOperators constant
     * @param value
     *        The value for this criterion
     */
    public void addCriterion(final String fieldName, final String operation, final String value) {
        if (!criteria.containsKey(fieldName)) {
            criteria.put(fieldName, new HashMap<String, List<String>>());
        }

        if (!criteria.get(fieldName).containsKey(operation)) {
            criteria.get(fieldName).put(operation, new ArrayList<String>());
        }

        if (!criteria.get(fieldName).get(operation).contains(value)) {
            criteria.get(fieldName).get(operation).add(value);
        }
    }
}