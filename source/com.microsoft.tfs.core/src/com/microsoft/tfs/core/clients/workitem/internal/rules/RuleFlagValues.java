// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.ArrayList;
import java.util.List;

public class RuleFlagValues {
    public static final int RULE_FLAG_1_EDITABLE = 1 << 0;
    public static final int RULE_FLAG_1_GRANT_READ = 1 << 1;
    public static final int RULE_FLAG_1_DENY_READ = 1 << 2;
    public static final int RULE_FLAG_1_GRANT_WRITE = 1 << 3;
    public static final int RULE_FLAG_1_DENY_WRITE = 1 << 4;
    public static final int RULE_FLAG_1_GRANT_ADMIN = 1 << 5;
    public static final int RULE_FLAG_1_DENY_ADMIN = 1 << 6;
    public static final int RULE_FLAG_1_UNLESS = 1 << 7;
    public static final int RULE_FLAG_1_FLOWDOWN_TREE = 1 << 8;
    public static final int RULE_FLAG_1_DEFAULT = 1 << 9;
    public static final int RULE_FLAG_1_SUGGESTION = 1 << 10;
    public static final int RULE_FLAG_1_REVERSE = 1 << 11;
    public static final int RULE_FLAG_1_IF_NOT = 1 << 12;
    public static final int RULE_FLAG_1_IF_LIKE = 1 << 13;
    public static final int RULE_FLAG_1_IF_LEAF = 1 << 14;
    public static final int RULE_FLAG_1_IF_INTERIOR = 1 << 15;
    public static final int RULE_FLAG_1_IF_ONE_LEVEL = 1 << 16;
    public static final int RULE_FLAG_1_IF_TWO_PLUS_LEVELS = 1 << 17;
    public static final int RULE_FLAG_1_IF_IMPLICIT_ALSO = 1 << 18;
    public static final int RULE_FLAG_1_IF2NOT = 1 << 19;
    public static final int RULE_FLAG_1_SEMI_EDITABLE = 1 << 20;
    public static final int RULE_FLAG_1_INVERSE_PERSON = 1 << 21;
    public static final int RULE_FLAG_1_THEN_NOT = 1 << 22;
    public static final int RULE_FLAG_1_THEN_LIKE = 1 << 23;
    public static final int RULE_FLAG_1_THEN_LEAF = 1 << 24;
    public static final int RULE_FLAG_1_THEN_INTERIOR = 1 << 25;
    public static final int RULE_FLAG_1_THEN_ONE_LEVEL = 1 << 26;
    public static final int RULE_FLAG_1_THEN_TWO_PLUS_LEVELS = 1 << 27;
    public static final int RULE_FLAG_1_THEN_IMPLICIT_ALSO = 1 << 28;
    public static final int RULE_FLAG_1_THEN_HELPTEXT = 1 << 29;

    public static final int RULE_FLAG_2_IF_CONST_LARGE_TEXT = 1 << 0;
    public static final int RULE_FLAG_2_IF_IMPLICIT_EMPTY = 1 << 1;
    public static final int RULE_FLAG_2_IF_IMPLICIT_UNCHANGED = 1 << 2;
    public static final int RULE_FLAG_2_THEN_CONST_LARGETEXT = 1 << 4;
    public static final int RULE_FLAG_2_THEN_IMPLICIT_EMPTY = 1 << 5;
    public static final int RULE_FLAG_2_THEN_IMPLICIT_UNCHANGED = 1 << 6;
    public static final int RULE_FLAG_2_FLOWAROUND_TREE = 1 << 8;

    public static final String RULE_FLAG_1_EDITABLE_NAME = "Editable"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_GRANT_READ_NAME = "GrantRead"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_DENY_READ_NAME = "DenyRead"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_GRANT_WRITE_NAME = "GrantWrite"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_DENY_WRITE_NAME = "DenyWrite"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_GRANT_ADMIN_NAME = "GrantAdmin"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_DENY_ADMIN_NAME = "DenyAdmin"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_UNLESS_NAME = "Unless"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_FLOWDOWN_TREE_NAME = "FlowdownTree"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_DEFAULT_NAME = "Default"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_SUGGESTION_NAME = "Suggestion"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_REVERSE_NAME = "Reverse"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_NOT_NAME = "IfNot"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_LIKE_NAME = "IfLike"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_LEAF_NAME = "IfLeaf"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_INTERIOR_NAME = "IfInterior"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_ONE_LEVEL_NAME = "IfOneLevel"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_TWO_PLUS_LEVELS_NAME = "IfTwoPlusLevels"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF_IMPLICIT_ALSO_NAME = "IfImplicitAlso"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_IF2NOT_NAME = "If2Not"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_SEMI_EDITABLE_NAME = "SemiEditable"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_INVERSE_PERSON_NAME = "InversePerson"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_NOT_NAME = "ThenNot"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_LIKE_NAME = "ThenLike"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_LEAF_NAME = "ThenLeaf"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_INTERIOR_NAME = "ThenInterior"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_ONE_LEVEL_NAME = "ThenOneLevel"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_TWO_PLUS_LEVELS_NAME = "ThenTwoPlusLevels"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_IMPLICIT_ALSO_NAME = "ThenImplicitAlso"; //$NON-NLS-1$
    public static final String RULE_FLAG_1_THEN_HELPTEXT_NAME = "ThenHelptext"; //$NON-NLS-1$

    public static final String RULE_FLAG_2_IF_CONST_LARGE_TEXT_NAME = "IfConstLargeText"; //$NON-NLS-1$
    public static final String RULE_FLAG_2_IF_IMPLICIT_EMPTY_NAME = "IfImplicitEmpty"; //$NON-NLS-1$
    public static final String RULE_FLAG_2_IF_IMPLICIT_UNCHANGED_NAME = "IfImplicitUnchanged"; //$NON-NLS-1$
    public static final String RULE_FLAG_2_THEN_CONST_LARGETEXT_NAME = "ThenConstLargetext"; //$NON-NLS-1$
    public static final String RULE_FLAG_2_THEN_IMPLICIT_EMPTY_NAME = "ThenImplicitEmpty"; //$NON-NLS-1$
    public static final String RULE_FLAG_2_THEN_IMPLICIT_UNCHANGED_NAME = "ThenImplicitUnchanged"; //$NON-NLS-1$
    public static final String RULE_FLAG_2_FLOWAROUND_TREE_NAME = "FlowaroundTree"; //$NON-NLS-1$

    public static List<String> getRuleFlag1Names(final int ruleFlag1) {
        final List<String> names = new ArrayList<String>();

        if ((ruleFlag1 & RULE_FLAG_1_EDITABLE) > 0) {
            names.add(RULE_FLAG_1_EDITABLE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_GRANT_READ) > 0) {
            names.add(RULE_FLAG_1_GRANT_READ_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_DENY_READ) > 0) {
            names.add(RULE_FLAG_1_DENY_READ_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_GRANT_WRITE) > 0) {
            names.add(RULE_FLAG_1_GRANT_WRITE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_DENY_WRITE) > 0) {
            names.add(RULE_FLAG_1_DENY_WRITE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_GRANT_ADMIN) > 0) {
            names.add(RULE_FLAG_1_GRANT_ADMIN_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_DENY_ADMIN) > 0) {
            names.add(RULE_FLAG_1_DENY_ADMIN_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_UNLESS) > 0) {
            names.add(RULE_FLAG_1_UNLESS_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_FLOWDOWN_TREE) > 0) {
            names.add(RULE_FLAG_1_FLOWDOWN_TREE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_DEFAULT) > 0) {
            names.add(RULE_FLAG_1_DEFAULT_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_SUGGESTION) > 0) {
            names.add(RULE_FLAG_1_SUGGESTION_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_REVERSE) > 0) {
            names.add(RULE_FLAG_1_REVERSE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_NOT) > 0) {
            names.add(RULE_FLAG_1_IF_NOT_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_LIKE) > 0) {
            names.add(RULE_FLAG_1_IF_LIKE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_LEAF) > 0) {
            names.add(RULE_FLAG_1_IF_LEAF_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_INTERIOR) > 0) {
            names.add(RULE_FLAG_1_IF_INTERIOR_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_ONE_LEVEL) > 0) {
            names.add(RULE_FLAG_1_IF_ONE_LEVEL_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_TWO_PLUS_LEVELS) > 0) {
            names.add(RULE_FLAG_1_IF_TWO_PLUS_LEVELS_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF_IMPLICIT_ALSO) > 0) {
            names.add(RULE_FLAG_1_IF_IMPLICIT_ALSO_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_IF2NOT) > 0) {
            names.add(RULE_FLAG_1_IF2NOT_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_SEMI_EDITABLE) > 0) {
            names.add(RULE_FLAG_1_SEMI_EDITABLE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_INVERSE_PERSON) > 0) {
            names.add(RULE_FLAG_1_INVERSE_PERSON_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_NOT) > 0) {
            names.add(RULE_FLAG_1_THEN_NOT_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_LIKE) > 0) {
            names.add(RULE_FLAG_1_THEN_LIKE_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_LEAF) > 0) {
            names.add(RULE_FLAG_1_THEN_LEAF_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_INTERIOR) > 0) {
            names.add(RULE_FLAG_1_THEN_INTERIOR_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_ONE_LEVEL) > 0) {
            names.add(RULE_FLAG_1_THEN_ONE_LEVEL_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_TWO_PLUS_LEVELS) > 0) {
            names.add(RULE_FLAG_1_THEN_TWO_PLUS_LEVELS_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_IMPLICIT_ALSO) > 0) {
            names.add(RULE_FLAG_1_THEN_IMPLICIT_ALSO_NAME);
        }

        if ((ruleFlag1 & RULE_FLAG_1_THEN_HELPTEXT) > 0) {
            names.add(RULE_FLAG_1_THEN_HELPTEXT_NAME);
        }

        return names;
    }

    public static List<String> getRuleFlag2Names(final int ruleFlag2) {
        final List<String> names = new ArrayList<String>();

        if ((ruleFlag2 & RULE_FLAG_2_IF_CONST_LARGE_TEXT) > 0) {
            names.add(RULE_FLAG_2_IF_CONST_LARGE_TEXT_NAME);
        }

        if ((ruleFlag2 & RULE_FLAG_2_IF_IMPLICIT_EMPTY) > 0) {
            names.add(RULE_FLAG_2_IF_IMPLICIT_EMPTY_NAME);
        }

        if ((ruleFlag2 & RULE_FLAG_2_IF_IMPLICIT_UNCHANGED) > 0) {
            names.add(RULE_FLAG_2_IF_IMPLICIT_UNCHANGED_NAME);
        }

        if ((ruleFlag2 & RULE_FLAG_2_THEN_CONST_LARGETEXT) > 0) {
            names.add(RULE_FLAG_2_THEN_CONST_LARGETEXT_NAME);
        }

        if ((ruleFlag2 & RULE_FLAG_2_THEN_IMPLICIT_EMPTY) > 0) {
            names.add(RULE_FLAG_2_THEN_IMPLICIT_EMPTY_NAME);
        }

        if ((ruleFlag2 & RULE_FLAG_2_THEN_IMPLICIT_UNCHANGED) > 0) {
            names.add(RULE_FLAG_2_THEN_IMPLICIT_UNCHANGED_NAME);
        }

        if ((ruleFlag2 & RULE_FLAG_2_FLOWAROUND_TREE) > 0) {
            names.add(RULE_FLAG_2_FLOWAROUND_TREE_NAME);
        }

        return names;
    }
}
