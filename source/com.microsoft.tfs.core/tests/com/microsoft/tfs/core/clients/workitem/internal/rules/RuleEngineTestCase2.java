// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.Date;

import com.microsoft.tfs.core.clients.workitem.internal.fields.ServerComputedFieldType;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.SpecialConstantIDs;

import junit.framework.TestCase;

public class RuleEngineTestCase2 extends TestCase {
    private static final int RULE_TARGET_ID = 25;
    private static final int RULE_TARGET_AREA_ID = 50;

    private RuleTargetStub ruleTarget;
    private WITContextStub witContext;
    private RuleEngine ruleEngine;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ruleTarget = new RuleTargetStub(RULE_TARGET_ID, RULE_TARGET_AREA_ID);
        witContext = new WITContextStub();
        ruleEngine = new RuleEngine(ruleTarget, witContext);
    }

    public void testValueProvidingRule() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        final int constId = 500;
        final String constant = "a constant value"; //$NON-NLS-1$
        witContext.addRule(RuleFactory.newDefaultRule(field.getID(), constId, 1, RULE_TARGET_AREA_ID, 0, 0));
        ruleTarget.addField(field);
        witContext.addConstant(constId, constant);

        ruleEngine.open();

        assertEquals(constant, field.getValue());
    }

    public void testValueProvidingRuleConstCurrentUser() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        witContext.addRule(
            RuleFactory.newDefaultRule(
                field.getID(),
                SpecialConstantIDs.CONST_CURRENT_USER,
                1,
                RULE_TARGET_AREA_ID,
                0,
                0));
        ruleTarget.addField(field);

        ruleEngine.open();

        assertEquals(WITContextStub.CURRENT_USER_DISPLAY_NAME, field.getValue());
    }

    public void testValueProvidingRuleConstServerCurrentUser() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        witContext.addRule(
            RuleFactory.newDefaultRule(
                field.getID(),
                SpecialConstantIDs.CONST_SERVER_CURRENT_USER,
                1,
                RULE_TARGET_AREA_ID,
                0,
                0));
        ruleTarget.addField(field);

        ruleEngine.open();

        assertTrue(field.isNewValueSet());
        assertEquals(ServerComputedFieldType.CURRENT_USER, field.getServerComputedType());
    }

    public void testValueProvidingRuleConstServerDateTime() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        witContext.addRule(
            RuleFactory.newDefaultRule(
                field.getID(),
                SpecialConstantIDs.CONST_SERVER_DATE_TIME,
                1,
                RULE_TARGET_AREA_ID,
                0,
                0));
        ruleTarget.addField(field);

        ruleEngine.open();

        assertTrue(field.isNewValueSet());
        assertEquals(ServerComputedFieldType.DATE_TIME, field.getServerComputedType());
    }

    public void testValueProvidingRuleConstUTCDateTime() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        witContext.addRule(
            RuleFactory.newDefaultRule(
                field.getID(),
                SpecialConstantIDs.CONST_UTC_DATE_TIME,
                1,
                RULE_TARGET_AREA_ID,
                0,
                0));
        ruleTarget.addField(field);

        ruleEngine.open();

        assertTrue(field.getValue() instanceof Date);
    }

    public void testValueProvidingRuleBadSpecialConstant() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        witContext.addRule(
            RuleFactory.newDefaultRule(
                field.getID(),
                SpecialConstantIDs.SPECIAL_CONSTANT_IDS_START_RANGE + 1,
                1,
                RULE_TARGET_AREA_ID,
                0,
                0));
        ruleTarget.addField(field);

        try {
            ruleEngine.open();
            fail();
        } catch (final UnhandledSpecialConstantIDException ex) {
            assertEquals(SpecialConstantIDs.SPECIAL_CONSTANT_IDS_START_RANGE + 1, ex.getConstantID());
        }
    }

    public void testValueProvidingRuleConstOldValueInOtherField() {
        final RuleTargetFieldStub field1 = new RuleTargetFieldStub(100);
        final RuleTargetFieldStub field2 = new RuleTargetFieldStub(200);
        final String valueToCopy = "value to copy"; //$NON-NLS-1$

        field1.setOriginalValue(valueToCopy);

        final Rule rule = RuleFactory.newDefaultRule(
            field2.getID(),
            SpecialConstantIDs.CONST_OLD_VALUE_IN_OTHER_FIELD,
            1,
            RULE_TARGET_AREA_ID,
            0,
            0);
        rule.setIf2FldID(field1.getID());
        rule.setIf2ConstID(SpecialConstantIDs.CONST_OLD_VALUE_IN_OTHER_FIELD);
        witContext.addRule(rule);
        ruleTarget.addField(field1);
        ruleTarget.addField(field2);

        ruleEngine.open();

        assertEquals(valueToCopy, field2.getValue());
    }

    public void testValueProvidingRuleConstEmptyValue() {
        final RuleTargetFieldStub field = new RuleTargetFieldStub(100);
        field.setValue(new Object());

        witContext.addRule(
            RuleFactory.newDefaultRule(
                field.getID(),
                SpecialConstantIDs.CONST_EMPTY_VALUE,
                1,
                RULE_TARGET_AREA_ID,
                0,
                0));
        ruleTarget.addField(field);

        ruleEngine.open();

        assertNull(field.getValue());
    }
}
