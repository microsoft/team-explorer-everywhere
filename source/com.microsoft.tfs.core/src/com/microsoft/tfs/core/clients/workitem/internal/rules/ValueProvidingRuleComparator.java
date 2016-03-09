// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.Comparator;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.SpecialConstantIDs;

public class ValueProvidingRuleComparator implements Comparator<Rule> {
    private static final Integer R1_GREATER = new Integer(1);
    private static final Integer R2_GREATER = new Integer(-1);

    @Override
    public int compare(final Rule r1, final Rule r2) {
        Integer compareValue = compareByScopingRule(
            r1.getFld4ID(),
            r1.getFld4IsConstID(),
            r1.getFld4WasConstID(),
            r2.getFld4ID(),
            r2.getFld4IsConstID(),
            r2.getFld4WasConstID());
        if (compareValue == null) {
            compareValue = compareByScopingRule(
                r1.getFld3ID(),
                r1.getFld3IsConstID(),
                r1.getFld3WasConstID(),
                r2.getFld3ID(),
                r2.getFld3IsConstID(),
                r2.getFld3WasConstID());
        }
        if (compareValue == null) {
            compareValue = compareByScopingRule(
                r1.getFld2ID(),
                r1.getFld2IsConstID(),
                r1.getFld2WasConstID(),
                r2.getFld2ID(),
                r2.getFld2IsConstID(),
                r2.getFld2WasConstID());
        }
        if (compareValue == null) {
            compareValue = compareByScopingRule(
                r1.getFld1ID(),
                r1.getFld1IsConstID(),
                r1.getFld1WasConstID(),
                r2.getFld1ID(),
                r2.getFld1IsConstID(),
                r2.getFld1WasConstID());
        }

        if (compareValue != null) {
            return compareValue.intValue();
        }

        /*
         * If r1 is a conditional rule and r2 isn't, r1 > r2
         */
        if (r1.getIfFldID() != 0 && r2.getIfFldID() == 0) {
            return 1;
        }

        /*
         * If r2 is a conditional rule and r1 isn't, r1 < r2
         */
        if (r2.getIfFldID() != 0 && r1.getIfFldID() == 0) {
            return -1;
        }

        /*
         * If both are conditional rules...
         */
        if (r1.getIfFldID() != 0 && r2.getIfFldID() != 0) {
            final int diff = getConditionTypeRanking(r1) - getConditionTypeRanking(r2);
            if (diff != 0) {
                return diff;
            }
        }

        /*
         * At this point, either both are conditional rules with the same
         * condition type, or neither are conditional rules.
         */

        final boolean r1Default = hasDefaultEnforcingCondition(r1);
        final boolean r2Default = hasDefaultEnforcingCondition(r2);

        /*
         * If r1 is a default rule and r2 isn't, r1 < r2
         */
        if (r1Default && !r2Default) {
            return -1;
        }
        /*
         * If r2 is a default rule and r1 isn't, r1 > r2
         */
        if (!r1Default && r2Default) {
            return 1;
        }

        /*
         * The rules are equivalent from the point of view of this comparator
         */
        return 0;
    }

    private Integer compareByScopingRule(
        final int r1FldId,
        final int r1IsConstId,
        final int r1WasConstId,
        final int r2Fldid,
        final int r2IsConstId,
        final int r2WasConstId) {
        if (r1FldId != 0 && r2Fldid == 0) {
            return R1_GREATER;
        }

        if (r1FldId == 0 && r2Fldid != 0) {
            return R2_GREATER;
        }

        if (r1FldId != 0 && r2Fldid != 0) {
            final int r1Count = (r1IsConstId != 0 ? 1 : 0) + (r1WasConstId != 0 ? 1 : 0);
            final int r2Count = (r2IsConstId != 0 ? 1 : 0) + (r2WasConstId != 0 ? 1 : 0);
            if (r1Count != r2Count) {
                return new Integer(r1Count - r2Count);
            }
        }

        return null;
    }

    private int getConditionTypeRanking(final Rule rule) {
        if (hasWhenChangedCondition(rule)) {
            return 3;
        }
        if (rule.isFlagIfNot()) {
            return 2;
        }
        return 1;
    }

    private boolean hasWhenChangedCondition(final Rule rule) {
        return (rule.getIfConstID() == SpecialConstantIDs.CONST_SAME_AS_OLD_VALUE);
    }

    private boolean hasDefaultEnforcingCondition(final Rule rule) {
        /*
         * A default rule is differentiated from a copy rule by having a
         * condition that specifies that the field the rule applies to
         * (ThenFldID) must by empty.
         */
        final int thenFldId = rule.getThenFldID();
        return (rule.getFld1ID() == thenFldId && rule.getFld1IsConstID() == SpecialConstantIDs.CONST_EMPTY_VALUE)
            || (rule.getFld2ID() == thenFldId && rule.getFld2IsConstID() == SpecialConstantIDs.CONST_EMPTY_VALUE)
            || (rule.getFld3ID() == thenFldId && rule.getFld3IsConstID() == SpecialConstantIDs.CONST_EMPTY_VALUE)
            || (rule.getFld4ID() == thenFldId && rule.getFld4IsConstID() == SpecialConstantIDs.CONST_EMPTY_VALUE);
    }
}
