// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Rule {
    public static final String SELECT_STRING = "select " //$NON-NLS-1$
        + "AreaID," //$NON-NLS-1$
        + "Cachestamp," //$NON-NLS-1$
        + "fDeleted," //$NON-NLS-1$
        + "Fld1ID," //$NON-NLS-1$
        + "Fld1IsConstID," //$NON-NLS-1$
        + "Fld1WasConstID," //$NON-NLS-1$
        + "Fld2ID," //$NON-NLS-1$
        + "Fld2IsConstID," //$NON-NLS-1$
        + "Fld2WasConstID," //$NON-NLS-1$
        + "Fld3ID," //$NON-NLS-1$
        + "Fld3IsConstID," //$NON-NLS-1$
        + "Fld3WasConstID," //$NON-NLS-1$
        + "Fld4ID," //$NON-NLS-1$
        + "Fld4IsConstID," //$NON-NLS-1$
        + "Fld4WasConstID," //$NON-NLS-1$
        + "If2ConstID," //$NON-NLS-1$
        + "If2FldID," //$NON-NLS-1$
        + "IfConstID," //$NON-NLS-1$
        + "IfFldID," //$NON-NLS-1$
        + "ObjectTypeScopeID," //$NON-NLS-1$
        + "PersonID," //$NON-NLS-1$
        + "RootTreeID," //$NON-NLS-1$
        + "RuleFlags1," //$NON-NLS-1$
        + "RuleFlags2," //$NON-NLS-1$
        + "RuleID," //$NON-NLS-1$
        + "ThenConstID," //$NON-NLS-1$
        + "ThenFldID" //$NON-NLS-1$
        + " from Rules"; //$NON-NLS-1$

    public static Rule fromRow(final ResultSet resultSet) throws SQLException {
        final Rule rule = new Rule();

        rule.setAreaID(resultSet.getInt(1));
        rule.setCachestamp(resultSet.getLong(2));
        rule.setDeleted(resultSet.getBoolean(3));
        rule.setFld1ID(resultSet.getInt(4));
        rule.setFld1IsConstID(resultSet.getInt(5));
        rule.setFld1WasConstID(resultSet.getInt(6));
        rule.setFld2ID(resultSet.getInt(7));
        rule.setFld2IsConstID(resultSet.getInt(8));
        rule.setFld2WasConstID(resultSet.getInt(9));
        rule.setFld3ID(resultSet.getInt(10));
        rule.setFld3IsConstID(resultSet.getInt(11));
        rule.setFld3WasConstID(resultSet.getInt(12));
        rule.setFld4ID(resultSet.getInt(13));
        rule.setFld4IsConstID(resultSet.getInt(14));
        rule.setFld4WasConstID(resultSet.getInt(15));
        rule.setIf2ConstID(resultSet.getInt(16));
        rule.setIf2FldID(resultSet.getInt(17));
        rule.setIfConstID(resultSet.getInt(18));
        rule.setIfFldID(resultSet.getInt(19));
        rule.setObjectTypeScopeID(resultSet.getInt(20));
        rule.setPersonID(resultSet.getInt(21));
        rule.setRootTreeID(resultSet.getInt(22));
        rule.setRuleFlags1(resultSet.getInt(23));
        rule.setRuleFlags2(resultSet.getInt(24));
        rule.setRuleID(resultSet.getInt(25));
        rule.setThenConstID(resultSet.getInt(26));
        rule.setThenFldID(resultSet.getInt(27));

        return rule;
    }

    public static Rule makeCopy(final Rule inputRule) {
        final Rule rule = new Rule();
        rule.setAreaID(inputRule.getAreaID());
        rule.setCachestamp(inputRule.getCachestamp());
        rule.setDeleted(inputRule.isDeleted());
        rule.setFld1ID(inputRule.getFld1ID());
        rule.setFld1IsConstID(inputRule.getFld1IsConstID());
        rule.setFld1WasConstID(inputRule.getFld1WasConstID());
        rule.setFld2ID(inputRule.getFld2ID());
        rule.setFld2IsConstID(inputRule.getFld2IsConstID());
        rule.setFld2WasConstID(inputRule.getFld2WasConstID());
        rule.setFld3ID(inputRule.getFld3ID());
        rule.setFld3IsConstID(inputRule.getFld3IsConstID());
        rule.setFld3WasConstID(inputRule.getFld3WasConstID());
        rule.setFld4ID(inputRule.getFld4ID());
        rule.setFld4IsConstID(inputRule.getFld4IsConstID());
        rule.setFld4WasConstID(inputRule.getFld4WasConstID());
        rule.setIf2ConstID(inputRule.getIf2ConstID());
        rule.setIf2FldID(inputRule.getIf2FldID());
        rule.setIfConstID(inputRule.getIfConstID());
        rule.setIfFldID(inputRule.getIfFldID());
        rule.setObjectTypeScopeID(inputRule.getObjectTypeScopeID());
        rule.setPersonID(inputRule.getPersonID());
        rule.setRootTreeID(inputRule.getRootTreeID());
        rule.setRuleFlags1(inputRule.getRuleFlags1());
        rule.setRuleFlags2(inputRule.getRuleFlags2());
        rule.setRuleID(inputRule.getRuleID());
        rule.setThenConstID(inputRule.getThenConstID());
        rule.setThenFldID(inputRule.getThenFldID());
        return rule;
    }

    public String extendedToString() {
        final String newline = System.getProperty("line.separator"); //$NON-NLS-1$
        return "Rule: [" //$NON-NLS-1$
            + newline
            + "areaID = [" //$NON-NLS-1$
            + areaID
            + "]" //$NON-NLS-1$
            + newline
            + "cachestamp = [" //$NON-NLS-1$
            + cachestamp
            + "]" //$NON-NLS-1$
            + newline
            + "deleted = [" //$NON-NLS-1$
            + deleted
            + "]" //$NON-NLS-1$
            + newline
            + "fld1ID = [" //$NON-NLS-1$
            + fld1ID
            + "]" //$NON-NLS-1$
            + newline
            + "fld1IsConstID = [" //$NON-NLS-1$
            + fld1IsConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld1WasConstID = [" //$NON-NLS-1$
            + fld1WasConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld2ID = [" //$NON-NLS-1$
            + fld2ID
            + "]" //$NON-NLS-1$
            + newline
            + "fld2IsConstID = [" //$NON-NLS-1$
            + fld2IsConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld2WasConstID = [" //$NON-NLS-1$
            + fld2WasConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld3ID = [" //$NON-NLS-1$
            + fld3ID
            + "]" //$NON-NLS-1$
            + newline
            + "fld3IsConstID = [" //$NON-NLS-1$
            + fld3IsConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld3WasConstID = [" //$NON-NLS-1$
            + fld3WasConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld4ID = [" //$NON-NLS-1$
            + fld4ID
            + "]" //$NON-NLS-1$
            + newline
            + "fld4IsConstID = [" //$NON-NLS-1$
            + fld4IsConstID
            + "]" //$NON-NLS-1$
            + newline
            + "fld4WasConstID = [" //$NON-NLS-1$
            + fld4WasConstID
            + "]" //$NON-NLS-1$
            + newline
            + "if2ConstID = [" //$NON-NLS-1$
            + if2ConstID
            + "]" //$NON-NLS-1$
            + newline
            + "if2FldID = [" //$NON-NLS-1$
            + if2FldID
            + "]" //$NON-NLS-1$
            + newline
            + "ifConstID = [" //$NON-NLS-1$
            + ifConstID
            + "]" //$NON-NLS-1$
            + newline
            + "ifFldID = [" //$NON-NLS-1$
            + ifFldID
            + "]" //$NON-NLS-1$
            + newline
            + "objectTypeScopeID = [" //$NON-NLS-1$
            + objectTypeScopeID
            + "]" //$NON-NLS-1$
            + newline
            + "personID = [" //$NON-NLS-1$
            + personID
            + "]" //$NON-NLS-1$
            + newline
            + "rootTreeID = [" //$NON-NLS-1$
            + rootTreeID
            + "]" //$NON-NLS-1$
            + newline
            + "ruleFlags1 = [" //$NON-NLS-1$
            + ruleFlags1
            + "]" //$NON-NLS-1$
            + newline
            + "ruleFlags2 = [" //$NON-NLS-1$
            + ruleFlags2
            + "]" //$NON-NLS-1$
            + newline
            + "ruleID = [" //$NON-NLS-1$
            + ruleID
            + "]" //$NON-NLS-1$
            + newline
            + "thenConstID = [" //$NON-NLS-1$
            + thenConstID
            + "]" //$NON-NLS-1$
            + newline
            + "thenFldID = [" //$NON-NLS-1$
            + thenFldID
            + "]" //$NON-NLS-1$
            + newline
            + "]" //$NON-NLS-1$
            + newline;
    }

    private int areaID;
    private long cachestamp;
    private boolean deleted;
    private int fld1ID;
    private int fld1IsConstID;
    private int fld1WasConstID;
    private int fld2ID;
    private int fld2IsConstID;
    private int fld2WasConstID;
    private int fld3ID;
    private int fld3IsConstID;
    private int fld3WasConstID;
    private int fld4ID;
    private int fld4IsConstID;
    private int fld4WasConstID;
    private int if2ConstID;
    private int if2FldID;
    private int ifConstID;
    private int ifFldID;
    private int objectTypeScopeID;
    private int personID;
    private int rootTreeID;
    private int ruleFlags1;
    private int ruleFlags2;
    private int ruleID;
    private int thenConstID;
    private int thenFldID;

    @Override
    public String toString() {
        return String.valueOf(ruleID);
    }

    public int getAreaID() {
        return areaID;
    }

    public Rule setAreaID(final int areaID) {
        this.areaID = areaID;
        return this;
    }

    public long getCachestamp() {
        return cachestamp;
    }

    public Rule setCachestamp(final long cachestamp) {
        this.cachestamp = cachestamp;
        return this;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Rule setDeleted(final boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    public int getFld1ID() {
        return fld1ID;
    }

    public Rule setFld1ID(final int fld1ID) {
        this.fld1ID = fld1ID;
        return this;
    }

    public int getFld1IsConstID() {
        return fld1IsConstID;
    }

    public Rule setFld1IsConstID(final int fld1IsConstID) {
        this.fld1IsConstID = fld1IsConstID;
        return this;
    }

    public int getFld1WasConstID() {
        return fld1WasConstID;
    }

    public Rule setFld1WasConstID(final int fld1WasConstID) {
        this.fld1WasConstID = fld1WasConstID;
        return this;
    }

    public int getFld2ID() {
        return fld2ID;
    }

    public Rule setFld2ID(final int fld2ID) {
        this.fld2ID = fld2ID;
        return this;
    }

    public int getFld2IsConstID() {
        return fld2IsConstID;
    }

    public Rule setFld2IsConstID(final int fld2IsConstID) {
        this.fld2IsConstID = fld2IsConstID;
        return this;
    }

    public int getFld2WasConstID() {
        return fld2WasConstID;
    }

    public Rule setFld2WasConstID(final int fld2WasConstID) {
        this.fld2WasConstID = fld2WasConstID;
        return this;
    }

    public int getFld3ID() {
        return fld3ID;
    }

    public Rule setFld3ID(final int fld3ID) {
        this.fld3ID = fld3ID;
        return this;
    }

    public int getFld3IsConstID() {
        return fld3IsConstID;
    }

    public Rule setFld3IsConstID(final int fld3IsConstID) {
        this.fld3IsConstID = fld3IsConstID;
        return this;
    }

    public int getFld3WasConstID() {
        return fld3WasConstID;
    }

    public Rule setFld3WasConstID(final int fld3WasConstID) {
        this.fld3WasConstID = fld3WasConstID;
        return this;
    }

    public int getFld4ID() {
        return fld4ID;
    }

    public Rule setFld4ID(final int fld4ID) {
        this.fld4ID = fld4ID;
        return this;
    }

    public int getFld4IsConstID() {
        return fld4IsConstID;
    }

    public Rule setFld4IsConstID(final int fld4IsConstID) {
        this.fld4IsConstID = fld4IsConstID;
        return this;
    }

    public int getFld4WasConstID() {
        return fld4WasConstID;
    }

    public Rule setFld4WasConstID(final int fld4WasConstID) {
        this.fld4WasConstID = fld4WasConstID;
        return this;
    }

    public int getIf2ConstID() {
        return if2ConstID;
    }

    public Rule setIf2ConstID(final int if2ConstID) {
        this.if2ConstID = if2ConstID;
        return this;
    }

    public int getIf2FldID() {
        return if2FldID;
    }

    public Rule setIf2FldID(final int if2FldID) {
        this.if2FldID = if2FldID;
        return this;
    }

    public int getIfConstID() {
        return ifConstID;
    }

    public Rule setIfConstID(final int ifConstID) {
        this.ifConstID = ifConstID;
        return this;
    }

    public int getIfFldID() {
        return ifFldID;
    }

    public Rule setIfFldID(final int ifFldID) {
        this.ifFldID = ifFldID;
        return this;
    }

    public int getObjectTypeScopeID() {
        return objectTypeScopeID;
    }

    public Rule setObjectTypeScopeID(final int objectTypeScopeID) {
        this.objectTypeScopeID = objectTypeScopeID;
        return this;
    }

    public int getPersonID() {
        return personID;
    }

    public Rule setPersonID(final int personID) {
        this.personID = personID;
        return this;
    }

    public int getRootTreeID() {
        return rootTreeID;
    }

    public Rule setRootTreeID(final int rootTreeID) {
        this.rootTreeID = rootTreeID;
        return this;
    }

    public int getRuleFlags1() {
        return ruleFlags1;
    }

    public Rule setRuleFlags1(final int ruleFlags1) {
        this.ruleFlags1 = ruleFlags1;
        return this;
    }

    public int getRuleFlags2() {
        return ruleFlags2;
    }

    public Rule setRuleFlags2(final int ruleFlags2) {
        this.ruleFlags2 = ruleFlags2;
        return this;
    }

    public int getRuleID() {
        return ruleID;
    }

    public Rule setRuleID(final int ruleID) {
        this.ruleID = ruleID;
        return this;
    }

    public int getThenConstID() {
        return thenConstID;
    }

    public Rule setThenConstID(final int thenConstID) {
        this.thenConstID = thenConstID;
        return this;
    }

    public int getThenFldID() {
        return thenFldID;
    }

    public Rule setThenFldID(final int thenFldID) {
        this.thenFldID = thenFldID;
        return this;
    }

    public boolean isFlagEditable() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_EDITABLE) > 0;
    }

    public boolean isFlagGrantRead() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_GRANT_READ) > 0;
    }

    public boolean isFlagDenyRead() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_DENY_READ) > 0;
    }

    public boolean isFlagGrantWrite() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_GRANT_WRITE) > 0;
    }

    public boolean isFlagDenyWrite() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_DENY_WRITE) > 0;
    }

    public boolean isFlagGrantAdmin() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_GRANT_ADMIN) > 0;
    }

    public boolean isFlagDenyAdmin() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_DENY_ADMIN) > 0;
    }

    public boolean isFlagUnless() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_UNLESS) > 0;
    }

    public boolean isFlagFlowdownTree() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_FLOWDOWN_TREE) > 0;
    }

    public boolean isFlagDefault() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_DEFAULT) > 0;
    }

    public boolean isFlagSuggestion() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_SUGGESTION) > 0;
    }

    public boolean isFlagReverse() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_REVERSE) > 0;
    }

    public boolean isFlagIfNot() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_NOT) > 0;
    }

    public boolean isFlagIfLike() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_LIKE) > 0;
    }

    public boolean isFlagIfLeaf() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_LEAF) > 0;
    }

    public boolean isFlagIfInterior() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_INTERIOR) > 0;
    }

    public boolean isFlagIfOneLevel() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_ONE_LEVEL) > 0;
    }

    public boolean isFlagIfTwoPlusLevels() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_TWO_PLUS_LEVELS) > 0;
    }

    public boolean isFlagIfImplicitAlso() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF_IMPLICIT_ALSO) > 0;
    }

    public boolean isFlagIf2Not() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_IF2NOT) > 0;
    }

    public boolean isFlagSemiEditable() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_SEMI_EDITABLE) > 0;
    }

    public boolean isFlagInversePerson() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_INVERSE_PERSON) > 0;
    }

    public boolean isFlagThenNot() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_NOT) > 0;
    }

    public boolean isFlagThenLike() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_LIKE) > 0;
    }

    public boolean isFlagThenLeaf() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_LEAF) > 0;
    }

    public boolean isFlagThenInterior() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_INTERIOR) > 0;
    }

    public boolean isFlagThenOneLevel() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_ONE_LEVEL) > 0;
    }

    public boolean isFlagThenTwoPlusLevels() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_TWO_PLUS_LEVELS) > 0;
    }

    public boolean isFlagThenImplicitAlso() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_IMPLICIT_ALSO) > 0;
    }

    public boolean isFlagThenHelptext() {
        return (ruleFlags1 & RuleFlagValues.RULE_FLAG_1_THEN_HELPTEXT) > 0;
    }

    public boolean isFlagIfConstLargeText() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_IF_CONST_LARGE_TEXT) > 0;
    }

    public boolean isFlagIfImplicitEmpty() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_IF_IMPLICIT_EMPTY) > 0;
    }

    public boolean isFlagIfImplicitUnchanged() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_IF_IMPLICIT_UNCHANGED) > 0;
    }

    public boolean isFlagThenConstLargetext() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_THEN_CONST_LARGETEXT) > 0;
    }

    public boolean isFlagThenImplicitEmpty() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_THEN_IMPLICIT_EMPTY) > 0;
    }

    public boolean isFlagThenImplicitUnchanged() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_THEN_IMPLICIT_UNCHANGED) > 0;
    }

    public boolean isFlagFlowaroundTree() {
        return (ruleFlags2 & RuleFlagValues.RULE_FLAG_2_FLOWAROUND_TREE) > 0;
    }
}
