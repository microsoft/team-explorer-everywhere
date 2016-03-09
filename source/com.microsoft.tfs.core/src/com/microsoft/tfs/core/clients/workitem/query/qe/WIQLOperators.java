// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;

/**
 * This class encapsulates mappings between invariant (non-Locale-sensitive)
 * representations of operators used in the GUI WIQL builder and localized
 * (Locale-sensitive) representations of those operators.
 *
 * @since TEE-SDK-10.1
 */
public final class WIQLOperators {
    /*
     * Similar to
     * Microsoft.TeamFoundation.WorkItemTracking.Controls.WiqlOperators.
     */

    /*
     * Locale invariant non-operators used to build operators. No localized
     * version is available.
     */
    public static final String PROJECT_CONTEXT_KEY = "project"; //$NON-NLS-1$
    public static final String TEAM_NAME_CONTEXT_KEY = "team"; //$NON-NLS-1$
    public static final String CURRENT_ITERATION_CONTEXT_KEY = "currentIteration"; //$NON-NLS-1$

    /*
     * Locale invariant operators. Localized versions can be queried with
     * getLocalizedOperator().
     */

    public static final String AND = "AND"; //$NON-NLS-1$
    public static final String CONTAINS = "CONTAINS"; //$NON-NLS-1$
    public static final String CONTAINS_WORDS = "CONTAINS WORDS"; //$NON-NLS-1$
    public static final String EQUAL_TO = "="; //$NON-NLS-1$
    public static final String EVER = "EVER"; //$NON-NLS-1$
    public static final String GREATER_THAN = ">"; //$NON-NLS-1$
    public static final String GREATER_THAN_OR_EQUAL_TO = ">="; //$NON-NLS-1$
    public static final String IN = "IN"; //$NON-NLS-1$
    public static final String LESS_THAN = "<"; //$NON-NLS-1$
    public static final String LESS_THAN_OR_EQUAL_TO = "<="; //$NON-NLS-1$
    public static final String MACRO_ME = "@me"; //$NON-NLS-1$
    public static final String MACRO_PROJECT = "@" + PROJECT_CONTEXT_KEY; //$NON-NLS-1$
    public static final String MACRO_CURRENT_ITERATION = "@" + CURRENT_ITERATION_CONTEXT_KEY; //$NON-NLS-1$
    public static final String MACRO_TODAY = "@today"; //$NON-NLS-1$
    public static final String SPECIAL_ANY = "[Any]"; //$NON-NLS-1$
    public static final String NOT_ = "NOT "; //$NON-NLS-1$
    public static final String NOT_CONTAINS = "NOT CONTAINS"; //$NON-NLS-1$
    public static final String NOT_CONTAINS_WORDS = "NOT CONTAINS WORDS"; //$NON-NLS-1$
    public static final String NOT_EQUAL_TO = "<>"; //$NON-NLS-1$
    public static final String NOT_EVER = "NOT EVER"; //$NON-NLS-1$
    public static final String NOT_UNDER = "NOT UNDER"; //$NON-NLS-1$
    public static final String OR = "OR"; //$NON-NLS-1$
    public static final String UNDER = "UNDER"; //$NON-NLS-1$
    public static final String IN_GROUP = "IN GROUP"; //$NON-NLS-1$
    public static final String NOT_IN_GROUP = "NOT IN GROUP"; //$NON-NLS-1$

    // These are the WIQL operators for comparison against WIT field values. In
    // WIQL these operators are the same whether you're comparing with a regular
    // value or the value of another field. To differentiate these field
    // operators from the other other comparison operators, we intentionally
    // place a space after operator which is a bit of a quick hack.
    public static final String EQUAL_TO_FIELD = "= "; //$NON-NLS-1$
    public static final String NOT_EQUAL_TO_FIELD = "<> "; //$NON-NLS-1$
    public static final String GREATER_THAN_FIELD = "> "; //$NON-NLS-1$
    public static final String LESS_THAN_FIELD = "< "; //$NON-NLS-1$
    public static final String GREATER_THAN_OR_EQUAL_TO_FIELD = ">= "; //$NON-NLS-1$
    public static final String LESS_THAN_OR_EQUAL_TO_FIELD = "<= "; //$NON-NLS-1$

    public static class LogicalOperator {
        public static final LogicalOperator AND = new LogicalOperator(0);
        public static final LogicalOperator NONE = new LogicalOperator(-1);
        public static final LogicalOperator OR = new LogicalOperator(1);

        private final int value;

        private LogicalOperator(final int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private static final Map<String, String> localizedOperatorLookup =
        new TreeMap<String, String>(CollatorFactory.getCaseInsensitiveCollator());
    private static final Map<String, String> invariantOperatorLookup =
        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    static {
        /*
         * Add all the locale-sensitive operators names.
         *
         * Note that the NOT constant is not added to the maps. This constant is
         * also suffixed by a "_" in the VS implementation, and is probably not
         * used to look up localized operators, but is instead referenced from
         * other places.
         */

        addOperatorPair(AND, Messages.getString("WiqlOperators.And")); //$NON-NLS-1$
        addOperatorPair(OR, Messages.getString("WiqlOperators.Or")); //$NON-NLS-1$
        addOperatorPair(CONTAINS, Messages.getString("WiqlOperators.Contains")); //$NON-NLS-1$
        addOperatorPair(CONTAINS_WORDS, Messages.getString("WIQLOperators.ContainsWords")); //$NON-NLS-1$
        addOperatorPair(NOT_CONTAINS, Messages.getString("WiqlOperators.NotContains")); //$NON-NLS-1$
        addOperatorPair(NOT_CONTAINS_WORDS, Messages.getString("WIQLOperators.NotContainsWords")); //$NON-NLS-1$
        addOperatorPair(IN, Messages.getString("WiqlOperators.In")); //$NON-NLS-1$
        addOperatorPair(IN_GROUP, Messages.getString("WiqlOperators.InGroup")); //$NON-NLS-1$
        addOperatorPair(NOT_IN_GROUP, Messages.getString("WiqlOperators.NotInGroup")); //$NON-NLS-1$
        addOperatorPair(EVER, Messages.getString("WiqlOperators.Ever")); //$NON-NLS-1$
        addOperatorPair(NOT_EVER, Messages.getString("WiqlOperators.NotEver")); //$NON-NLS-1$
        addOperatorPair(UNDER, Messages.getString("WiqlOperators.Under")); //$NON-NLS-1$
        addOperatorPair(NOT_UNDER, Messages.getString("WiqlOperators.NotUnder")); //$NON-NLS-1$
        addOperatorPair(EQUAL_TO, Messages.getString("WiqlOperators.EqualTo")); //$NON-NLS-1$
        addOperatorPair(NOT_EQUAL_TO, Messages.getString("WiqlOperators.NotEqualTo")); //$NON-NLS-1$
        addOperatorPair(GREATER_THAN, Messages.getString("WiqlOperators.GreaterThan")); //$NON-NLS-1$
        addOperatorPair(LESS_THAN, Messages.getString("WiqlOperators.LessThan")); //$NON-NLS-1$
        addOperatorPair(GREATER_THAN_OR_EQUAL_TO, Messages.getString("WiqlOperators.GreaterThanOrEqualTo")); //$NON-NLS-1$
        addOperatorPair(LESS_THAN_OR_EQUAL_TO, Messages.getString("WiqlOperators.LessThanOrEqualTo")); //$NON-NLS-1$
        addOperatorPair(EQUAL_TO_FIELD, Messages.getString("WiqlOperators.EqualToField")); //$NON-NLS-1$
        addOperatorPair(NOT_EQUAL_TO_FIELD, Messages.getString("WiqlOperators.NotEqualToField")); //$NON-NLS-1$
        addOperatorPair(GREATER_THAN_FIELD, Messages.getString("WiqlOperators.GreaterThanField")); //$NON-NLS-1$
        addOperatorPair(LESS_THAN_FIELD, Messages.getString("WiqlOperators.LessThanField")); //$NON-NLS-1$
        addOperatorPair(GREATER_THAN_OR_EQUAL_TO_FIELD, Messages.getString("WiqlOperators.GreaterThanOrEqualToField")); //$NON-NLS-1$
        addOperatorPair(LESS_THAN_OR_EQUAL_TO_FIELD, Messages.getString("WiqlOperators.LessThanOrEqualToField")); //$NON-NLS-1$
        addOperatorPair(SPECIAL_ANY, Messages.getString("WiqlOperators.SpecialAny")); //$NON-NLS-1$

        /*
         * To support PLOC tooling, the externalized message strings do not
         * start with a @ (this lets PLOC mangle them freely and they can still
         * be recognized as macros).
         */

        addOperatorPair(MACRO_TODAY, "@" + Messages.getString("WiqlOperators.MacroTodayWithoutInitialAtSign")); //$NON-NLS-1$ //$NON-NLS-2$
        addOperatorPair(MACRO_ME, "@" + Messages.getString("WiqlOperators.MacroMeWithoutInitialAtSign")); //$NON-NLS-1$ //$NON-NLS-2$
        addOperatorPair(MACRO_PROJECT, "@" + Messages.getString("WiqlOperators.MacroProjectWithoutInitialAtSign")); //$NON-NLS-1$ //$NON-NLS-2$
        addOperatorPair(
            MACRO_CURRENT_ITERATION,
            "@" + Messages.getString("WIQLOperators.MacroCurrentIterationWithoutInitialAtSign")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private WIQLOperators() {
    }

    private static void addOperatorPair(final String invariantOperator, final String localizedOperator) {
        localizedOperatorLookup.put(invariantOperator, localizedOperator);
        invariantOperatorLookup.put(localizedOperator, invariantOperator);
    }

    public static String getInvariantOperator(final String localizedOperator) {
        Check.notNull(localizedOperator, "localizedOperator"); //$NON-NLS-1$

        if (invariantOperatorLookup.containsKey(localizedOperator)) {
            return invariantOperatorLookup.get(localizedOperator);
        }
        return localizedOperator;
    }

    public static String getLocalizedOperator(final String invariantOperator) {
        Check.notNull(invariantOperator, "invariantOperator"); //$NON-NLS-1$

        if (localizedOperatorLookup.containsKey(invariantOperator)) {
            return localizedOperatorLookup.get(invariantOperator);
        }
        return invariantOperator;
    }

    public static String getLocalizedTodayMinusMacro(final int number) {
        return MessageFormat.format("{0} - {1}", getLocalizedOperator(MACRO_TODAY), Integer.toString(number)); //$NON-NLS-1$
    }

    public static String getLocalizedTodayMinusMacro(final String invariantValue) {
        // Preserve the value part after the invariant prefix
        return getLocalizedOperator(MACRO_TODAY) + invariantValue.substring(MACRO_TODAY.length());
    }

    public static String getInvariantTodayMinusMacro(final String localizedValue) {
        // Preserve the value part after the localized prefix
        return MACRO_TODAY + localizedValue.substring(getLocalizedOperator(MACRO_TODAY).length());
    }

    public static WIQLOperators.LogicalOperator getLogicalOperator(final String logicalOperatorText) {
        if (getInvariantOperator(logicalOperatorText).equals(AND)) {
            return WIQLOperators.LogicalOperator.AND;
        }
        if (getInvariantOperator(logicalOperatorText).equals(OR)) {
            return WIQLOperators.LogicalOperator.OR;
        }
        return WIQLOperators.LogicalOperator.NONE;
    }

    public static boolean isGroupOperator(final String op) {
        return op == WIQLOperators.IN_GROUP || op == WIQLOperators.NOT_IN_GROUP;
    }

    public static boolean isFieldNameOperator(final String op) {
        return op == WIQLOperators.EQUAL_TO_FIELD
            || op == WIQLOperators.NOT_EQUAL_TO_FIELD
            || op == WIQLOperators.LESS_THAN_FIELD
            || op == WIQLOperators.GREATER_THAN_FIELD
            || op == WIQLOperators.LESS_THAN_OR_EQUAL_TO_FIELD
            || op == WIQLOperators.GREATER_THAN_OR_EQUAL_TO_FIELD;
    }

    public static boolean isContainsOperator(final String op) {
        return op.equalsIgnoreCase(WIQLOperators.CONTAINS) || op.equalsIgnoreCase(WIQLOperators.NOT_CONTAINS);
    }

    public static boolean isContainsWordsOperator(final String op) {
        return op.equalsIgnoreCase(WIQLOperators.CONTAINS_WORDS)
            || op.equalsIgnoreCase(WIQLOperators.NOT_CONTAINS_WORDS);
    }

}
