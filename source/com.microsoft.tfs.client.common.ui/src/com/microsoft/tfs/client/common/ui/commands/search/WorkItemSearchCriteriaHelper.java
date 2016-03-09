// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.search;

import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;

/**
 * A class containing a mapping from invariant search operator syntax to natural
 * language operators, as well as some helper functions to evaluate macros
 * before displaying them within the informational text. This class is
 * structured in the same fashion as the WiqlOperators class.
 *
 *
 * @threadsafety unknown
 */
public class WorkItemSearchCriteriaHelper {
    public static final String INVARIANT_KEYWORD_CONTAINS = "KeywordContains"; //$NON-NLS-1$
    public static final String INVARIANT_KEYWORD_CONTAINS_WORDS = "KeywordContainsWords"; //$NON-NLS-1$
    public static final String INVARIANT_KEYWORD_NOT_CONTAINS = "KeywordNotContains"; //$NON-NLS-1$
    public static final String INVARIANT_KEYWORD_NOT_CONTAINS_WORDS = "KeywordNotContainsWords"; //$NON-NLS-1$

    private static final String SearchPageHelpTextContainsOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextContainsOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextContainsWordsOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextContainsWordsOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextEqualsOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextEqualsOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextNotContainsOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextNotContainsOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextNotContainsWordsOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextNotContainsWordsOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextNotEqualsOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextNotEqualsOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextNotUnderOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextNotUnderOperator"); //$NON-NLS-1$
    private static final String SearchPageHelpTextUnderOperator =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageHelpTextUnderOperator"); //$NON-NLS-1$

    private static final String SearchPageKeywordHelpTextOperatorContains =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageKeywordHelpTextOperatorContains"); //$NON-NLS-1$
    private static final String SearchPageKeywordHelpTextOperatorContainsWords =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageKeywordHelpTextOperatorContainsWords"); //$NON-NLS-1$
    private static final String SearchPageKeywordHelpTextOperatorNotContains =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageKeywordHelpTextOperatorNotContains"); //$NON-NLS-1$
    private static final String SearchPageKeywordHelpTextOperatorNotContainsWords =
        Messages.getString("WorkItemSearchCriteriaHelper.SearchPageKeywordHelpTextOperatorNotContainsWords"); //$NON-NLS-1$

    private static final String SearchPageMacroToday =
        Messages.getString("WorkItemSearchCriteriaHelper.MacroTodayWithoutInitialAtSign"); //$NON-NLS-1$
    private static final String SearchPageMacroMe =
        Messages.getString("WorkItemSearchCriteriaHelper.MacroMeWithoutInitialAtSign"); //$NON-NLS-1$
    private static final String SearchPageMacroProject =
        Messages.getString("WorkItemSearchCriteriaHelper.MacroProjectWithoutInitialAtSign"); //$NON-NLS-1$
    private static final String SearchPageMacroCurrentIteration =
        Messages.getString("WorkItemSearchCriteriaHelper.MacroCurrentIterationWithoutInitialAtSign"); //$NON-NLS-1$

    private static final Map<String, String> invariantToLocalizedMap =
        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, String> localizedToInvariantMap =
        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * The default constructor, which simply initializes the mappings from
     * invariant operators to their natural language counterparts so that they
     * may be trivially accessed later.
     */
    static {
        // this is supposed to be a singleton class, so just initialize the
        // static dictionary once
        if (invariantToLocalizedMap.keySet().size() == 0) {
            mapOperatorPair(WIQLOperators.CONTAINS, SearchPageHelpTextContainsOperator);
            mapOperatorPair(WIQLOperators.CONTAINS_WORDS, SearchPageHelpTextContainsWordsOperator);
            mapOperatorPair(WIQLOperators.NOT_CONTAINS, SearchPageHelpTextNotContainsOperator);
            mapOperatorPair(WIQLOperators.NOT_CONTAINS_WORDS, SearchPageHelpTextNotContainsWordsOperator);
            mapOperatorPair(WIQLOperators.UNDER, SearchPageHelpTextUnderOperator);
            mapOperatorPair(WIQLOperators.NOT_UNDER, SearchPageHelpTextNotUnderOperator);
            mapOperatorPair(WIQLOperators.EQUAL_TO, SearchPageHelpTextEqualsOperator);
            mapOperatorPair(WIQLOperators.NOT_EQUAL_TO, SearchPageHelpTextNotEqualsOperator);
            mapOperatorPair(WIQLOperators.MACRO_TODAY, SearchPageMacroToday);
            mapOperatorPair(WIQLOperators.MACRO_ME, SearchPageMacroMe);
            mapOperatorPair(WIQLOperators.MACRO_PROJECT, SearchPageMacroProject);
            mapOperatorPair(WIQLOperators.MACRO_CURRENT_ITERATION, SearchPageMacroCurrentIteration);
            mapOperatorPair(INVARIANT_KEYWORD_CONTAINS, SearchPageKeywordHelpTextOperatorContains);
            mapOperatorPair(INVARIANT_KEYWORD_CONTAINS_WORDS, SearchPageKeywordHelpTextOperatorContainsWords);
            mapOperatorPair(INVARIANT_KEYWORD_NOT_CONTAINS, SearchPageKeywordHelpTextOperatorNotContains);
            mapOperatorPair(INVARIANT_KEYWORD_NOT_CONTAINS_WORDS, SearchPageKeywordHelpTextOperatorNotContainsWords);
        }
    }

    /**
     * A function to establish a two-way mapping between an invariant operator
     * and its corresponding localized (natural language) operator.
     *
     *
     * @param invariantOperator
     *        The mapping's invariant operator.
     * @param localizedOperator
     *        The mapping's localized natural language operator.
     */
    private static void mapOperatorPair(final String invariantOperator, final String localizedOperator) {
        invariantToLocalizedMap.put(invariantOperator, localizedOperator);
        localizedToInvariantMap.put(localizedOperator, invariantOperator);
    }

    public static String getLocalizedOperator(final String invariantOperator) {
        if ((invariantOperator != null) && invariantToLocalizedMap.containsKey(invariantOperator)) {
            return invariantToLocalizedMap.get(invariantOperator);
        }

        return invariantOperator;
    }
}
