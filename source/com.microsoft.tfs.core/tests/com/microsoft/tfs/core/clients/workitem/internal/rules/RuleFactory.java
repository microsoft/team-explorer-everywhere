// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;

/**
 * A factory that can create rule instances for use in unit tests.
 */
public class RuleFactory {
    /**
     * Creates a new deny-write (validation) rule as a convenience method for
     * unit tests.
     *
     * @param thenFldID
     *        the id of the field affected by the rule
     * @param thenConstID
     *        the id of the constant value being provided by the rule (help
     *        text)
     * @param ruleID
     *        the id to give the new rule
     * @param areaID
     *        the area id to give the new rule
     * @param ruleFlags1
     *        any additional ruleflags1 - the help text flag is implicitly
     *        included
     * @param ruleFlags2
     *        any ruleflags2 - none are implicitly included
     * @return a new rule
     */
    public static Rule newHelpTextRule(
        final int thenFldID,
        final int thenConstID,
        final int ruleID,
        final int areaID,
        final int ruleFlags1,
        final int ruleFlags2) {
        final Rule rule = new Rule();

        rule.setThenFldID(thenFldID);
        rule.setThenConstID(thenConstID);

        addCommonProperties(rule, ruleID, areaID, ruleFlags1 | RuleFlagValues.RULE_FLAG_1_THEN_HELPTEXT, ruleFlags2);

        return rule;
    }

    /**
     * Creates a new deny-write (validation) rule as a convenience method for
     * unit tests.
     *
     * @param thenFldID
     *        the id of the field affected by the rule
     * @param thenConstID
     *        the id of the constant value being provided by the rule
     * @param ruleID
     *        the id to give the new rule
     * @param areaID
     *        the area id to give the new rule
     * @param ruleFlags1
     *        any additional ruleflags1 - the deny write flag is implicitly
     *        included
     * @param ruleFlags2
     *        any ruleflags2 - none are implicitly included
     * @return a new rule
     */
    public static Rule newDenyWriteRule(
        final int thenFldID,
        final int thenConstID,
        final int ruleID,
        final int areaID,
        final int ruleFlags1,
        final int ruleFlags2) {
        final Rule rule = new Rule();

        rule.setThenFldID(thenFldID);
        rule.setThenConstID(thenConstID);

        addCommonProperties(rule, ruleID, areaID, ruleFlags1 | RuleFlagValues.RULE_FLAG_1_DENY_WRITE, ruleFlags2);

        return rule;
    }

    /**
     * Creates a new suggestion rule as a convenience method for unit tests.
     *
     * @param thenFldID
     *        the id of the field affected by the rule
     * @param thenConstID
     *        the id of the constant value being provided by the rule
     * @param ruleID
     *        the id to give the new rule
     * @param areaID
     *        the area id to give the new rule
     * @param ruleFlags1
     *        any additional ruleflags1 - the suggestion flag is implicitly
     *        included
     * @param ruleFlags2
     *        any ruleflags2 - none are implicitly included
     * @return a new rule
     */
    public static Rule newSuggestionRule(
        final int thenFldID,
        final int thenConstID,
        final int ruleID,
        final int areaID,
        final int ruleFlags1,
        final int ruleFlags2) {
        final Rule rule = new Rule();

        rule.setThenFldID(thenFldID);
        rule.setThenConstID(thenConstID);

        addCommonProperties(rule, ruleID, areaID, ruleFlags1 | RuleFlagValues.RULE_FLAG_1_SUGGESTION, ruleFlags2);

        return rule;
    }

    /**
     * Creates a new default (value-providing) rule as a convenience method for
     * unit tests.
     *
     * @param thenFldID
     *        the id of the field affected by the rule
     * @param thenConstID
     *        the id of the constant value being provided by the rule
     * @param ruleID
     *        the id to give the new rule
     * @param areaID
     *        the area id to give the new rule
     * @param ruleFlags1
     *        any additional ruleflags1 - the default flag is implicitly
     *        included
     * @param ruleFlags2
     *        any ruleflags2 - none are implicitly included
     * @return a new rule
     */
    public static Rule newDefaultRule(
        final int thenFldID,
        final int thenConstID,
        final int ruleID,
        final int areaID,
        final int ruleFlags1,
        final int ruleFlags2) {
        final Rule rule = new Rule();

        rule.setThenFldID(thenFldID);
        rule.setThenConstID(thenConstID);

        addCommonProperties(rule, ruleID, areaID, ruleFlags1 | RuleFlagValues.RULE_FLAG_1_DEFAULT, ruleFlags2);

        return rule;
    }

    /**
     * Creates a new do-nothing rule as a convenience method for unit tests.
     *
     * @param ruleID
     *        the id to give the new rule
     * @param areaID
     *        the area id to give the new rule
     * @return a new rule
     */
    public static Rule newNullRule(final int ruleID, final int areaID) {
        final Rule rule = new Rule();

        addCommonProperties(rule, ruleID, areaID, 0, 0);

        return rule;
    }

    private static void addCommonProperties(
        final Rule rule,
        final int ruleID,
        final int areaID,
        final int ruleFlags1,
        final int ruleFlags2) {
        rule.setRuleID(ruleID);
        rule.setAreaID(areaID);
        rule.setRootTreeID(areaID);
        rule.setRuleFlags1(ruleFlags1);
        rule.setRuleFlags2(ruleFlags2);
        rule.setPersonID(InternalWorkItemConstants.TFS_EVERYONE_CONSTANT_SET_ID);
        rule.setObjectTypeScopeID(WorkItemFieldIDs.WORK_ITEM);
    }
}
