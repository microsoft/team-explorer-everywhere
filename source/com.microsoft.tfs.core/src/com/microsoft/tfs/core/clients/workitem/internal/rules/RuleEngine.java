// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.WorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.internal.IWITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.ServerComputedFieldType;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IConstantSet;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.SpecialConstantIDs;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.RuleCache.RuleCacheResults;
import com.microsoft.tfs.util.GUID;

/**
 * <p>
 * The work item rule engine implementation.
 * </p>
 * <p>
 * The rule engine is a major abstraction in the WIT system. Much of the
 * client-side WIT functionality is provided by declarative rules delivered
 * through the metadata. This class is responsible for the bulk of the work of
 * interpreting those rules.
 * </p>
 * <p>
 * In Product Studio / Microsoft terms, their rule engine implementation is
 * called BRIE. BRIE is a complex, legacy unmanaged codebase.
 * </p>
 * <p>
 * This class provides a subset of the BRIE functionality, as it only has to be
 * compatible with the rules that are possible using TFS WIT v1. Much care has
 * been taken to reverse engineer and examine all of the possible rule sources
 * (in-the-box rules and rules created through provisioning aka witimport). By
 * doing this we can be reasonably certain that <code>RuleEngine</code>
 * implements the right subset of BRIE functionality and handles all edge cases,
 * while not implementing code for rules that would never exist in TFS.
 * </p>
 * <p>
 * This <code>RuleEngine</code> implementation is designed to fail-fast. In the
 * event that a rule is encountered that has an unexpected configuration, the
 * RuleEngine will throw one of the following unchecked exceptions:
 * <ul>
 * <li><code>UnhandledRuleStateException</code></li>
 * <li><code>UnhandledSpecialConstantIdException</code></li>
 * </ul>
 * </p>
 * <p>
 * This class is NOT thread-safe by design. Clients create an instance of
 * <code>RuleEngine</code> and then use that instance from a single thread.
 * <code>RuleEngine</code> performs no internal caching, and instances are
 * intended to be short-lived. If RuleEngine is ever needed to be made thread
 * safe, modifying the class to make it thread safe wouldn't be much work, as it
 * is mostly stateless.
 * </p>
 */
public class RuleEngine {
    /*
     * The Log instance for the RuleEngine class
     */
    private static final Log log = LogFactory.getLog(RuleEngine.class);

    /*
     * These three objects are used when evaluating value-providing rules to
     * keep track of "special" value states that represent values computed by
     * the server.
     */
    private static final Object SERVERCOMPUTED_CURRENT_USER_SET_VALUE_OPERATION = new Object();
    private static final Object SERVERCOMPUTED_DATETIME_SET_VALUE_OPERATION = new Object();
    private static final Object SERVERCOMPUTED_RANDOM_GUID_SET_VALUE_OPERATION = new Object();

    /*
     * The target of this RuleEngine instance
     */
    private final IRuleTarget target;

    /*
     * The IWITContext used by this RuleEngine instance
     */
    private final IWITContext witContext;

    /*
     * When evaluating value-providing rules, the effects of such rules are
     * temporarily kept in this Map until all value-providing rules in scope
     * have been evaluated. This is done to make the RuleEngine more
     * deterministic, so that the effects of one value-providing rule in a batch
     * don't affect the validity of a subsequent value-providing rule in the
     * batch.
     */
    private final Map<Integer, Object> setValueOperations = new HashMap<Integer, Object>();

    public RuleEngine(final IRuleTarget target, final IWITContext witContext) {
        this.target = target;
        this.witContext = witContext;
    }

    public void open() {
        final int id = target.getID();
        final boolean isNew = id == 0;

        log.trace(MessageFormat.format("opening target: {0}", Integer.toString(id))); //$NON-NLS-1$

        // Run all global rules first. Global rules are identified by having
        // areaId=0 in TFS.
        runRulesOnOpen(isNew, 0);

        // Run all rules for the target areaId.
        runRulesOnOpen(isNew, target.getAreaID());
    }

    public void runRulesOnOpen(final boolean isNew, final int areaId) {
        log.trace("Run rules for area " + areaId + " on OPEN ***************"); //$NON-NLS-1$ //$NON-NLS-2$
        final RuleCacheResults results = witContext.getRuleCache().getRules(areaId);

        preProcessFields(results.affectedFieldIds);
        runDefaultRules(results.defaultRules);

        if (isNew) {
            /*
             * If the target is new, then we re-run the default rules. This
             * seems to be the way Visual Studio's client does it.
             */
            log.trace("re-running default rules for new target"); //$NON-NLS-1$
            runDefaultRules(results.defaultRules);
        }

        runNonDefaultRules(results.nonDefaultRules);
        postProcessFields(results.affectedFieldIds);

        log.trace("Done running rules for area " + areaId + " on OPEN ***************"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean fieldChanged(final int changedFieldId) {
        // Gather all the field IDS that are affected by the rules that are
        // about to be run. This set is passed to the recursive fieldChanged
        // method and will accumulate the union of the affected IDs through all
        // levels of recursion.
        final Set<Integer> affectedFieldIDs = new HashSet<Integer>();

        // Run the rules affected by this field change. The integer argument is
        // for the number of levels of recursion that should occur. For example,
        // field A has changed and rules cause a change to fields B and C. We'll
        // run rules for B and C. Then run the rules for the fields affected by
        // those rules. There is the possibility of an infinite cycle, so the
        // number of times we'll recurse is limited.
        fieldChanged(changedFieldId, 2, affectedFieldIDs);

        // Run post field process on all the affected fields which will cause
        // the UI to update properly.
        postProcessFields(affectedFieldIDs);

        // Return true if the list of affected fields IDS included the original
        // changed field.
        return affectedFieldIDs.contains(new Integer(changedFieldId));
    }

    private void fieldChanged(final int changedFieldId, final int recursive, final Set<Integer> allAffectedFieldIDs) {
        log.trace(MessageFormat.format(
            "field [{0}] changed for work item: {1}", //$NON-NLS-1$
            Integer.toString(changedFieldId),
            Integer.toString(target.getID())));

        // Get the global and project level rules for this field.
        final RuleCacheResults resultsGlobal = witContext.getRuleCache().getRules(0, changedFieldId);
        final RuleCacheResults resultsArea = witContext.getRuleCache().getRules(target.getAreaID(), changedFieldId);

        // Get the union of affected field IDs.
        final Set<Integer> affectedFieldIDs = new HashSet<Integer>();
        affectedFieldIDs.addAll(resultsGlobal.affectedFieldIds);
        affectedFieldIDs.addAll(resultsArea.affectedFieldIds);

        // Union this set of affected field IDs with the overall set.
        allAffectedFieldIDs.addAll(affectedFieldIDs);

        // Pre process all affected Field IDs.
        preProcessFields(affectedFieldIDs);

        // Run the global rules.
        runDefaultRules(resultsGlobal.defaultRules);
        runNonDefaultRules(resultsGlobal.nonDefaultRules);

        // Run the project level rules.
        runDefaultRules(resultsArea.defaultRules);
        runNonDefaultRules(resultsArea.nonDefaultRules);

        // New in Dev10 SP1. The VS WIT OM now recurses to trigger rules on
        // fields that changed during this update. The recursion depth is
        // limited by the integer argument passed to this method. This algorithm
        // mirrors that found in om\core\WorkItem.cs ApplyFieldChange method in
        // the Dev10 SP1 branch.
        if (recursive > 0) {
            for (final Integer affectedFieldId : affectedFieldIDs) {
                fieldChanged(affectedFieldId.intValue(), recursive - 1, allAffectedFieldIDs);
            }
        }
    }

    private void preProcessFields(final Set<Integer> fieldIds) {
        for (final Integer fieldId : fieldIds) {
            final IRuleTargetField field = target.getRuleTargetField(fieldId.intValue());

            /*
             * Here we "reset" the state of the field that can be changed by
             * rules. We're about to run all the rules currently applicable for
             * the field, so the state will be rebuilt from scratch.
             */
            field.getPickListSupport().reset();
            field.setStatus(FieldStatus.VALID);
            field.setReadOnly(false);
        }
    }

    private void postProcessFields(final Set<Integer> fieldIds) {
        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format("post processing: {0}", fieldIds)); //$NON-NLS-1$
        }

        for (final Integer fieldId : fieldIds) {
            final IRuleTargetField field = target.getRuleTargetField(fieldId.intValue());
            field.postProcessAfterRuleRun();
        }
    }

    private void runNonDefaultRules(final List<Rule> nonDefaultRules) {
        for (final Rule rule : nonDefaultRules) {
            if (isRuleInScope(rule)) {
                runNonDefaultRule(rule);
            }
        }
    }

    private void runDefaultRules(final List<Rule> defaultRules) {
        for (final Rule rule : defaultRules) {
            if (isRuleInScope(rule)) {
                runDefaultRule(rule);
            }
        }

        for (final Integer fieldId : setValueOperations.keySet()) {
            final Object valueToSet = setValueOperations.get(fieldId);
            final IRuleTargetField field = target.getRuleTargetField(fieldId.intValue());

            if (valueToSet == SERVERCOMPUTED_CURRENT_USER_SET_VALUE_OPERATION) {
                field.setServerComputed(ServerComputedFieldType.CURRENT_USER);
            } else if (valueToSet == SERVERCOMPUTED_DATETIME_SET_VALUE_OPERATION) {
                field.setServerComputed(ServerComputedFieldType.DATE_TIME);
            } else if (valueToSet == SERVERCOMPUTED_RANDOM_GUID_SET_VALUE_OPERATION) {
                field.setServerComputed(ServerComputedFieldType.RANDOM_GUID);
            } else {
                field.setValueFromRule(valueToSet);
            }
        }

        setValueOperations.clear();
    }

    private void runDefaultRule(final Rule rule) {
        final int thenConst = rule.getThenConstID();
        final int thenFldID = rule.getThenFldID();

        if (WorkItemFieldIDs.WORK_ITEM_FORM_ID == thenFldID) {
            /*
             * skip value providing rules that set the WorkItemFormId field -
             * the Java OM currently gets the form ID a different way (see
             * WorkItemTypeImpl.calculateForm())
             *
             * triggered by: provisioning-generated rules
             * (UpdatePackage.InsertForm)
             */
            return;
        }

        final Integer setValueOperationkey = new Integer(thenFldID);
        final String traceOutput = MessageFormat.format(
            "applying VP rule [{0}] in area [{1}] to field [{2}]: ", //$NON-NLS-1$
            Integer.toString(rule.getRuleID()),
            Integer.toString(rule.getAreaID()),
            getFieldNameForTrace(thenFldID));

        if (SpecialConstantIDs.isSpecialConstantID(thenConst)) {
            switch (thenConst) {
                case SpecialConstantIDs.CONST_CURRENT_USER:
                    /*
                     * triggered by: <COPY-DEFAULT from="currentuser" />
                     *
                     * during import,
                     * Microsoft.TeamFoundation.WorkItemTracking.Client
                     * .Provision.ImporterField.SetCopyDefaultRule ensures that
                     * this type of rule will only apply to fields of type
                     * String
                     */
                    final String currentUserDisplayName = witContext.getCurrentUserDisplayName();
                    log.trace(MessageFormat.format("{0}ConstCurrentUser ({1})", traceOutput, currentUserDisplayName)); //$NON-NLS-1$
                    setValueOperations.put(setValueOperationkey, currentUserDisplayName);
                    break;

                case SpecialConstantIDs.CONST_OLD_VALUE_IN_OTHER_FIELD:
                    /*
                     * triggered by: <COPY-DEFAULT from="field" />
                     *
                     * during import,
                     * Microsoft.TeamFoundation.WorkItemTracking.Client
                     * .Provision.ImporterField.SetCopyDefaultRule ensures that
                     * the types of the source and target fields are the same
                     */
                    if (rule.getIf2ConstID() != SpecialConstantIDs.CONST_OLD_VALUE_IN_OTHER_FIELD) {
                        throw new UnhandledRuleStateException(
                            rule,
                            MessageFormat.format(
                                "default ConstOldValueInOtherField rule with If2ConstID={0}", //$NON-NLS-1$
                                Integer.toString(rule.getIf2ConstID())));
                    }
                    final int otherFieldID = rule.getIf2FldID();
                    if (otherFieldID == 0) {
                        throw new UnhandledRuleStateException(
                            rule,
                            MessageFormat.format(
                                "default ConstOldValueInOtherField with If2FldID={0}", //$NON-NLS-1$
                                Integer.toString(rule.getIf2FldID())));
                    }
                    final Object oldValue = target.getRuleTargetField(otherFieldID).getOriginalValue();
                    log.trace(MessageFormat.format("{0}ConstOldValueInOtherField ({1})", traceOutput, oldValue)); //$NON-NLS-1$
                    setValueOperations.put(setValueOperationkey, oldValue);
                    break;

                case SpecialConstantIDs.CONST_EMPTY_VALUE:
                    /*
                     * triggered by: <COPY-DEFAULT from="value" value="" />
                     */
                    setValueOperations.put(setValueOperationkey, null);
                    log.trace(MessageFormat.format("{0}ConstEmptyValue", traceOutput)); //$NON-NLS-1$
                    break;

                case SpecialConstantIDs.CONST_SERVER_CURRENT_USER:
                    /*
                     * triggered by: <SERVERDEFAULT from="currentuser" />
                     *
                     * during import,
                     * Microsoft.TeamFoundation.WorkItemTracking.Client
                     * .Provision.ImporterField.AddServerDefaultRule ensures
                     * that this type of rule will only apply to fields of type
                     * String
                     */
                    log.trace(MessageFormat.format("{0}ConstServerCurrentUser", traceOutput)); //$NON-NLS-1$
                    setValueOperations.put(setValueOperationkey, SERVERCOMPUTED_CURRENT_USER_SET_VALUE_OPERATION);
                    break;

                case SpecialConstantIDs.CONST_SERVER_DATE_TIME:
                    /*
                     * triggered by: <SERVERDEFAULT from="clock" />
                     *
                     * during import,
                     * Microsoft.TeamFoundation.WorkItemTracking.Client
                     * .Provision.ImporterField.AddServerDefaultRule ensures
                     * that this type of rule will only apply to fields of type
                     * DateTime
                     */
                    log.trace(MessageFormat.format("{0}ConstServerDateTime", traceOutput)); //$NON-NLS-1$
                    setValueOperations.put(setValueOperationkey, SERVERCOMPUTED_DATETIME_SET_VALUE_OPERATION);
                    break;

                case SpecialConstantIDs.CONST_SERVER_RANDOM_GUID:
                    log.trace(MessageFormat.format("{0}ConstServerRandomGuid", traceOutput)); //$NON-NLS-1$
                    setValueOperations.put(setValueOperationkey, SERVERCOMPUTED_RANDOM_GUID_SET_VALUE_OPERATION);
                    break;

                case SpecialConstantIDs.CONST_UTC_DATE_TIME:
                    /*
                     * triggered by: <COPY-DEFAULT from="clock" />
                     *
                     * during import,
                     * Microsoft.TeamFoundation.WorkItemTracking.Client
                     * .Provision.ImporterField.SetCopyDefaultRule ensures that
                     * this type of rule will only apply to fields of type
                     * DateTime
                     */
                    log.trace(MessageFormat.format("{0}ConstUtcDateTime", traceOutput)); //$NON-NLS-1$
                    setValueOperations.put(setValueOperationkey, new Date());
                    break;

                default:
                    throw new UnhandledSpecialConstantIDException(thenConst, rule, "value providing rule ThenConstID"); //$NON-NLS-1$
            }
        } else {
            /*
             * non-special constant
             *
             * triggered by: <COPY-DEFAULT from="value" />
             * provisioning-generated rules: WITImporter.ProcessDefaultState
             * WITImporter.ProcessReasons
             *
             * during import,
             * Microsoft.TeamFoundation.WorkItemTracking.Client.Provision
             * .ImporterField.CheckValidValue ensures that the String constant
             * referenced by this rule can be converted into the proper data
             * type for the field referenced by this rule
             */

            final String value = witContext.getMetadata().getConstantsTable().getConstantByID(thenConst);
            log.trace(MessageFormat.format("{0}constant {1} ({2})", traceOutput, thenConst, value)); //$NON-NLS-1$
            setValueOperations.put(setValueOperationkey, value);
        }
    }

    private void runNonDefaultRule(final Rule rule) {
        if (rule.getThenFldID() == 0) {
            return;
        } else if (rule.isFlagDenyWrite()) {
            runDenyWriteRule(rule);
        } else if (rule.isFlagSuggestion()) {
            runSuggestionRule(rule);
        } else if (rule.isFlagThenHelptext()) {
            runHelpTextRule(rule);
        }
    }

    private void runDenyWriteRule(final Rule rule) {
        final IRuleTargetField field = target.getRuleTargetField(rule.getThenFldID());

        /*
         * The unless and thennot flags determine semantics for the rule.
         * Ignoring the reverse flag (which is not used in TFS v1), a denywrite
         * rule can be interpreted as: deny write (unless ? UNLESS : WHEN) the
         * condition is (thennot ? FALSE : TRUE).
         *
         * Note that (unless==true && thennot==true) is the same condition as
         * (unless==false && thennot==false). Likewise, (unless==true &&
         * thennot==false) is the same condition as (unless==false &&
         * thennot==true).
         *
         * Therefore, there are only two states. If (unless == thennot), deny
         * write if the condition is true. If (unless != thennot), deny write if
         * the condition is false.
         */
        final boolean not = rule.isFlagThenNot();
        final boolean unless = rule.isFlagUnless();

        String logRuleEffect = "no effect"; //$NON-NLS-1$

        if (SpecialConstantIDs.isSpecialConstantID(rule.getThenConstID())) {
            switch (rule.getThenConstID()) {
                case SpecialConstantIDs.CONST_EMPTY_VALUE:

                    // triggered by:
                    // * <REQUIRED /> (unless==true, thennot==true)
                    // * <CANNOTLOSEVALUE /> (unless==true, thennot==true)
                    // * <EMPTY /> (unless==true, thennot==false)
                    // * lots of in-the-box rules (unless==false,
                    // thennot==false)

                    /*
                     * ThenImplicitUnchanged will be true if there was an
                     * <ALLOWEXISTINGVALUE /> rule at in scope as of a <REQUIRED
                     * />, <CANNOTLOSEVALUE />, or <EMPTY /> rule.
                     * ThenImplicitUnchanged is ignored by this type of rule in
                     * Visual Studio's implementation.
                     */

                    /*
                     * empty is true if the field has no value and the field is
                     * not set to be server computed
                     */
                    final boolean empty = (field.getValue() == null) && (field.getServerComputedType() == null);

                    if ((not != unless)) {
                        /*
                         * This is a rare case where a data validation rule can
                         * end up behaving like a value providing rule. In this
                         * case, the field holds a value but a data validation
                         * rule says that it must be empty.
                         *
                         * We could simply set the field invalid:
                         * field.setStatus(FieldStatus.INVALID_NOT_EMTPY);
                         * logRuleEffect = "INVALID_NOT_EMTPY";
                         *
                         * However, that's not very user friendly (it's also not
                         * what the Microsoft implementation does). A more
                         * user-friendly approach will be to set the field's
                         * value to empty (if it is not already empty), and then
                         * set the field to read-only so the user can't set a
                         * value which would then be invalid. For most data
                         * validation rules we can't do this, since we only know
                         * "what's wrong", and not "how to fix it". This is a
                         * rare case where we do know "how to fix it".
                         */
                        if (!empty) {
                            field.setValueFromRule(null);
                            field.setReadOnly(true);
                            logRuleEffect = "cleared value and set readonly true"; //$NON-NLS-1$
                        } else {
                            field.setReadOnly(true);
                            logRuleEffect = "set readonly true"; //$NON-NLS-1$
                        }
                    } else if ((not == unless) && empty) {
                        setInvalidStatus(field, FieldStatus.INVALID_EMPTY);
                        logRuleEffect = "INVALID_EMTPY"; //$NON-NLS-1$
                    }
                    break;

                case SpecialConstantIDs.CONST_SAME_AS_OLD_VALUE:

                    // triggered by:
                    // * <READONLY /> (unless==true, thennot==false)
                    // * multiple in-the-box rules (unless==true,
                    // thennot==false)

                    /*
                     * ThenImplicitUnchanged will be true if there was an
                     * <ALLOWEXISTINGVALUE /> rule at in scope as of a <READONLY
                     * /> rule. ThenImplicitUnchanged is ignored by this type of
                     * rule in Visual Studio's implementation.
                     */

                    if (not != unless) {
                        field.setReadOnly(true);
                        field.unsetNewValue();
                        logRuleEffect = "unset new value and set readonly true"; //$NON-NLS-1$
                    } else {
                        /*
                         * should never happen in TFS WIT v1
                         */
                        throw new UnhandledRuleStateException(
                            rule,
                            "unless==thennot for denywrite ConstSameAsOldValue"); //$NON-NLS-1$
                    }
                    break;

                case SpecialConstantIDs.CONST_WAS_EMPTY_OR_SAME_AS_OLD_VALUE:

                    // triggered by:
                    // <FROZEN /> (unless==true, thennot==false)

                    /*
                     * ThenImplicitUnchanged will be true if there was an
                     * <ALLOWEXISTINGVALUE /> rule at in scope as of a <FROZEN
                     * /> rule. ThenImplicitUnchanged is ignored by this type of
                     * rule in Visual Studio's implementation.
                     */

                    final boolean wasEmpty = (field.getOriginalValue() == null);
                    final boolean sameAsOldValue = isSameAsOldValue(field);
                    final boolean wasEmptyOrSameAsOldValue = wasEmpty | sameAsOldValue;

                    if (not != unless) {
                        if (!wasEmptyOrSameAsOldValue) {
                            setInvalidStatus(field, FieldStatus.INVALID_NOT_EMPTY_OR_OLD_VALUE);
                            logRuleEffect = "INVALID_NOT_EMPTY_OR_OLD_VALUE"; //$NON-NLS-1$
                        }
                    } else {
                        /*
                         * Should never happen in TFS WIT v1 Presumably this
                         * case is what FieldStatus.INVALID_EMPTY_OR_OLD_VALUE
                         * is for
                         */
                        throw new UnhandledRuleStateException(
                            rule,
                            "unless==thennot for denywrite ConstWasEmptyOrSameAsOldValue"); //$NON-NLS-1$
                    }
                    break;

                case SpecialConstantIDs.CONST_CURRENT_USER:

                    // only triggered by in-the-box rules:
                    // * [Created By] should default to the person doing the
                    // action (Will cause OM to set it to the right value)
                    // (unless==false, thennot==false)
                    // * If [Created By] became non-empty (-10014) then it
                    // should be equal to Person doing action (unless==true,
                    // thennot==false)

                    /*
                     * seems that we can ignore this as things are working fine
                     * without it
                     */
                    break;

                case SpecialConstantIDs.CONST_OLD_VALUE_PLUS_ONE:

                    // only triggered by an in-the-box rule:
                    // * The Rev of a new item must be increased by one from
                    // the old version (unless==true, thennot==false).

                    /*
                     * So it will only ever show up for ThenFld == System.Rev.
                     * We can safely ignore as this is really more of a
                     * server-side concern than a client-side concern - the rev
                     * field is read-only as far as the client is concerned.
                     */
                    break;

                case SpecialConstantIDs.CONST_SERVER_DATE_TIME:

                    // triggered by:
                    // * <SERVERDEFAULT from="clock" /> (unless==true,
                    // thennot==false)
                    // * multiple in-the-box-rules (unless==true,
                    // thennot==false)

                    /*
                     * ThenImplicitUnchanged will be true if there was an
                     * <ALLOWEXISTINGVALUE /> rule at in scope as of a
                     * <SERVERDEFAULT /> rule. Unknown what Visual Studio's
                     * implementation does if this flag is set - it likely
                     * ignores it.
                     */

                    /*
                     * As far as the WITD triggering, we can ignore as this is
                     * handled automatically by our OM without the need for a
                     * denywrite rule. Not sure if we need to care about the
                     * in-the-box rules - seems unlikely but probably need to
                     * review them to be sure.
                     */

                    /*
                     * If the field is empty, apply "make true" logic since this
                     * field can only have one value.
                     *
                     * TODO: this is a temporary or at least partial fix for the
                     * Dev11 BUILD release. We have a user story to investigate
                     * the .NET rule engine's "make true" logic.
                     */
                    if (field.getValue() == null && field.getServerComputedType() == null) {
                        field.setServerComputed(ServerComputedFieldType.DATE_TIME);
                    }
                    break;

                case SpecialConstantIDs.CONST_VALUE_IN_OTHER_FIELD:

                    // triggered by:
                    // * <NOTSAMEAS field="x" /> (unless==true, thennot==true)

                    /*
                     * ThenImplicitUnchanged will be true if there was an
                     * <ALLOWEXISTINGVALUE /> rule at in scope as of a
                     * <NOTSAMEAS /> rule. ThenImplicitUnchanged is ignored by
                     * this type of rule in Visual Studio's implementation.
                     */

                    if (rule.getIf2ConstID() != SpecialConstantIDs.CONST_VALUE_IN_OTHER_FIELD) {
                        throw new UnhandledRuleStateException(
                            rule,
                            MessageFormat.format(
                                "denywrite ConstValueInOtherField rule with If2ConstID={0}", //$NON-NLS-1$
                                Integer.toString(rule.getIf2ConstID())));
                    }

                    final int otherFieldID = rule.getIf2FldID();
                    if (otherFieldID == 0) {
                        throw new UnhandledRuleStateException(
                            rule,
                            MessageFormat.format(
                                "denywrite ConstValueInOtherField with If2FldID={0}", //$NON-NLS-1$
                                Integer.toString(rule.getIf2FldID())));
                    }

                    final Object otherFieldValue = target.getRuleTargetField(otherFieldID).getValue();
                    final Object targetFieldValue = field.getValue();
                    final boolean fieldValuesEqual = fieldValuesEqual(targetFieldValue, otherFieldValue);

                    if (unless == not) {
                        if (fieldValuesEqual) {
                            setInvalidStatus(field, FieldStatus.INVALID_VALUE_IN_OTHER_FIELD);
                            logRuleEffect = "INVALID_VALUE_IN_OTHER_FIELD (" + otherFieldID + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    } else {
                        /*
                         * Should never happen in TFS WIT v1. Presumably this
                         * case is what
                         * FieldStatus.INVALID_VALUE_NOT_IN_OTHER_FIELD is for.
                         */
                        throw new UnhandledRuleStateException(
                            rule,
                            "unless!=thennot for denywrite ConstValueInOtherField"); //$NON-NLS-1$
                    }
                    break;

                case SpecialConstantIDs.CONST_SERVER_CURRENT_USER:

                    // triggered by:
                    // * <SERVERDEFAULT from="currentuser" /> (unless==true,
                    // thennot==false)

                    /*
                     * ThenImplicitUnchanged will be true if there was an
                     * <ALLOWEXISTINGVALUE /> rule at in scope as of a
                     * <SERVERDEFAULT /> rule. Unknown what Visual Studio's
                     * implementation does if this flag is set - it likely
                     * ignores it.
                     */

                    /*
                     * currently do-nothing as this is handled automatically
                     * without the need for a denywrite rule
                     */
                    break;

                case SpecialConstantIDs.CONST_SERVER_RANDOM_GUID:
                    break;

                case SpecialConstantIDs.CONST_GREATER_THAN_OLD_VALUE:

                    // only triggered by an in-the-box rule:
                    // * [Changed Date] is greater than [Changed Date] of the
                    // previous revision (unless==true, thennot==false)

                    /*
                     * can safely ignore this case, as this is a server-side
                     * concern. the client side doesn't modify the changed date
                     */
                    break;

                case SpecialConstantIDs.CONST_DELETED_TREE_LOCATION:

                    // only triggered by in-the-box rules:
                    // * WorkItem cannot be in an invalid location
                    // (unless==false, thennot==false), ThenFld == System.AreaId
                    // * WorkItem cannot have an invalid IterationID
                    // (unless==false, thennot==false), ThenFld =
                    // System.IterationId

                    /*
                     * This rule will only be applied to one of the tree id
                     * fields (System.AreaId or System.IterationId). The rule
                     * must check if the field value is the id of a deleted
                     * node.
                     *
                     * Not sure if this rule will ever get triggered - is it
                     * possible to set area id / iteration id to a deleted node
                     * using TEE?
                     */
                    break;

                case SpecialConstantIDs.CONST_ADMIN_ONLY_TREE_LOCATION:

                    // only triggered by an in-the-box rule:
                    // * WorkItem cannot be written when in an admin only tree
                    // location (unless==false, thennot==false), ThenFld ==
                    // System.AreaId

                    /*
                     * This rule will only be applied to one of the tree id
                     * fields (System.AreaId or System.IterationId). The rule
                     * must check if the field value is the id of a admin node.
                     *
                     * Need to check on the semantics of that and what is
                     * considered an admin node. In fact, I think there is a
                     * good chance that TFS doesn't even support the concept of
                     * "admin-only" tree nodes, and the in-the-box rule that
                     * triggers this case is simply left over from product
                     * studio.
                     */
                    break;

                case SpecialConstantIDs.CONST_NOT_GREATER_THAN_SERVER_TIME:
                    // New rule in Dev11. "Server time" is terminology used to
                    // refer to "Authorized Date". This rule is specifically
                    // created to ensure that "Changed date" is not set to an
                    // invalid value. The VS WIT OM does nothing for this rule,
                    // it is enforced on the server.
                    break;

                default:
                    /*
                     * All of the special constants that are possible in TFS WIT
                     * should be handled by the above cases.
                     */
                    throw new UnhandledSpecialConstantIDException(
                        rule.getThenConstID(),
                        rule,
                        "deny write rule ThenConstID"); //$NON-NLS-1$
            }
        } else {
            // triggered by:
            // * <ALLOWEDVALUES /> (unless==true, thennot==false,
            // ThenImplicitEmpty==true)
            // * <PROHIBITEDVALUES /> (unless==true, thennot==true)
            // * <VALIDUSER /> (unless==true, thennot==false,
            // ThenImplicitEmpty==true)
            // * <MATCH /> (unless==true, thennot==false,
            // ThenImplicitEmpty==true, ThenLike==true)
            // provisioning-generated rules:
            // * WITImporter.ProcessDefaultState (unless==true,
            // thennot==false)
            // * WITImporter.ProcessReasons (unless==true, thennot==false)
            // * WITImporter.ProcessStates (unless==true, thennot==false)
            // * WITImporter.ProcessWorkItemType (unless==true,
            // thennot==false)
            // * WITImporter.RestrictTransition (unless==false,
            // thennot==false)

            /*
             * ThenImplicitUnchanged will be true if there was an
             * <ALLOWEXISTINGVALUE /> rule at in scope as of the <ALLOWEDVALUES
             * />, <VALIDUSER />, or <MATCH /> rule (ThenImplicitUnchanged is
             * disabled by the importer for <PROHIBITEDVALUES /> rules). If this
             * flag is set, existing values should be allowed, even if they
             * would not otherwise pass this rule.
             */

            final IConstantSet constantSet = getConstantSetFromThenFields(rule);
            final boolean allowedValues = (unless != not);

            /*
             * update the picklist if the rule is not a pattern match rule
             */
            if (!rule.isFlagThenLike()) {
                final Set<String> listValues = constantSet.getValues();
                final IFieldPickListSupport pickList = field.getPickListSupport();
                if (allowedValues) {
                    pickList.addAllowedValues(listValues);
                    logRuleEffect = "allowed values (" + listValues.size() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    field.getPickListSupport().addProhibitedValues(listValues);
                    logRuleEffect = "prohibited values (" + listValues.size() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            if (rule.isFlagThenImplicitEmpty() && field.getValue() == null) {
                /*
                 * This rule has no effect, since ThenImplicitEmpty is set and
                 * the field is empty.
                 */
                logRuleEffect += " (no denywrite - ThenImplicitEmpty)"; //$NON-NLS-1$
            } else if (rule.isFlagThenImplicitUnchanged() && isSameAsOldValue(field)) {
                /*
                 * This rule has no effect, since ThenImplicitUnchanged is set
                 * and the field has not changed.
                 */
                logRuleEffect += " (no denywrite - ThenImplicitUnchanged)"; //$NON-NLS-1$
            } else {
                if (rule.isFlagThenLike()) {
                    if (!allowedValues) {
                        /*
                         * The unless == thennot state should never occur for
                         * pattern match rules in TFS WIT v1.
                         */
                        throw new UnhandledRuleStateException(
                            rule,
                            "pattern match unless=" //$NON-NLS-1$
                                + unless
                                + " thennot=" //$NON-NLS-1$
                                + not);
                    }

                    final boolean matches = constantSet.patternMatch(
                        field.getValue(),
                        "ruleid:" //$NON-NLS-1$
                            + rule.getRuleID()
                            + ",fldid:" //$NON-NLS-1$
                            + rule.getThenFldID());

                    if (!matches) {
                        setInvalidStatus(field, FieldStatus.INVALID_FORMAT);
                        logRuleEffect = "INVALID_FORMAT"; //$NON-NLS-1$
                    } else {
                        /*
                         * This rule has no effect, since the current value in
                         * the field matches the pattern.
                         */
                        logRuleEffect += " (pattern matches)"; //$NON-NLS-1$
                    }
                } else {
                    final Object fieldValue = field.getValue();
                    final String fieldValueAsString = translateFieldValueIntoString(field.getValue());

                    /*
                     * The value is considered in the list if non-null (constant
                     * sets never contain empty values), the conversion to a
                     * string succeeded, and the constant set contains the
                     * value.
                     */
                    final boolean valueInList =
                        fieldValue != null && fieldValueAsString != null && constantSet.contains(fieldValueAsString);

                    if ((allowedValues && !valueInList) || (!allowedValues && valueInList)) {
                        setInvalidStatus(field, FieldStatus.INVALID_LIST_VALUE);
                        logRuleEffect += ", INVALID_LIST_VALUE"; //$NON-NLS-1$
                    } else {
                        logRuleEffect += " (no denywrite effect - value " //$NON-NLS-1$
                            + (valueInList ? "is" : "is not") //$NON-NLS-1$ //$NON-NLS-2$
                            + " in list)"; //$NON-NLS-1$
                    }
                }
            }
        }

        log.trace(MessageFormat.format(
            "applied DW rule [{0}] in area [{1}] to field [{2}]: {3}", //$NON-NLS-1$
            Integer.toString(rule.getRuleID()),
            Integer.toString(rule.getAreaID()),
            getFieldNameForTrace(rule.getThenFldID()),
            logRuleEffect));
    }

    private void runSuggestionRule(final Rule rule) {
        final IRuleTargetField field = target.getRuleTargetField(rule.getThenFldID());

        final IConstantSet constantSet = getConstantSetFromThenFields(rule);

        field.getPickListSupport().addSuggestedValues(constantSet.getValues());

        log.trace(MessageFormat.format(
            "applied SG rule [{0}] to field [{1}]", //$NON-NLS-1$
            Integer.toString(rule.getRuleID()),
            Integer.toString(rule.getThenFldID())));
    }

    private void runHelpTextRule(final Rule rule) {
        final int fieldId = rule.getThenFldID();
        final int constId = rule.getThenConstID();
        final String helpText = witContext.getMetadata().getConstantsTable().getConstantByID(constId);

        log.trace(MessageFormat.format(
            "applying HelpText rule [{0}] to field [{1}]", //$NON-NLS-1$
            Integer.toString(rule.getRuleID()),
            getFieldNameForTrace(fieldId)));

        final IRuleTargetField field = target.getRuleTargetField(fieldId);
        field.setHelpText(helpText);
    }

    private boolean isRuleInScope(final Rule rule) {
        return passesConditionTest(rule.getFld1ID(), rule.getFld1IsConstID(), rule.getFld1WasConstID(), rule, "Fld1") //$NON-NLS-1$
            && passesConditionTest(rule.getFld2ID(), rule.getFld2IsConstID(), rule.getFld2WasConstID(), rule, "Fld2") //$NON-NLS-1$
            && passesConditionTest(rule.getFld3ID(), rule.getFld3IsConstID(), rule.getFld3WasConstID(), rule, "Fld3") //$NON-NLS-1$
            && passesConditionTest(rule.getFld4ID(), rule.getFld4IsConstID(), rule.getFld4WasConstID(), rule, "Fld4") //$NON-NLS-1$
            && passesIf1Test(rule.getIfFldID(), rule.getIfConstID(), rule.isFlagIfNot(), rule)
            && passesIf2Test(rule.getIf2FldID(), rule.getIf2ConstID(), rule.isFlagIf2Not(), rule);
    }

    private boolean passesConditionTest(
        final int fieldId,
        final int isConstid,
        final int wasConstId,
        final Rule rule,
        final String type) {
        if (fieldId == 0) {
            /*
             * If fieldId is unspecified (0), then this test passes
             */
            return true;
        }

        /*
         * Get the field under test
         */
        final IRuleTargetField field = target.getRuleTargetField(fieldId);

        /*
         * Do the IS test, if specified
         */
        if (isConstid != 0) {
            //
            // The current value may already have been changed by a
            // previously-run (but not-yet-applied) rule.
            // So check the list of pending field-set actions to see if the
            // target field is being changed.
            // If so, use the new field value
            //
            Object fieldValue = field.getValue();
            final Integer setValueOperationsId = new Integer(fieldId);
            if (setValueOperations.containsKey(setValueOperationsId)) {
                fieldValue = setValueOperations.get(setValueOperationsId);
            }

            if (!passesIsOrWasConditionTest(fieldValue, isConstid, rule, type + "IsConstID")) //$NON-NLS-1$
            {
                return false;
            }
        }

        /*
         * Do the WAS test, if specified
         */
        if (wasConstId != 0) {
            if (!passesIsOrWasConditionTest(field.getOriginalValue(), wasConstId, rule, type + "WasConstID")) //$NON-NLS-1$
            {
                return false;
            }
        }

        return true;
    }

    private boolean passesIsOrWasConditionTest(
        final Object fieldValue,
        final int constId,
        final Rule rule,
        final String type) {
        if (SpecialConstantIDs.isSpecialConstantID(constId)) {
            switch (constId) {
                case SpecialConstantIDs.CONST_EMPTY_VALUE:
                    if (fieldValue != null) {
                        return false;
                    }
                    break;

                default:
                    /*
                     * Special constants other than ConstEmptyValue should never
                     * occur as the ConstID of is/was conditions in TFS WIT v1.
                     */
                    throw new UnhandledSpecialConstantIDException(constId, rule, type);
            }
        } else {
            if (!fieldValueEqualToConstant(fieldValue, constId)) {
                return false;
            }
        }

        return true;
    }

    private boolean passesIf2Test(final int fieldId, final int constId, final boolean not, final Rule rule) {
        if (fieldId == 0) {
            /*
             * If fieldId is unspecified (0), then this If2 test passes
             */
            return true;
        }

        /*
         * Get the field under test
         */
        final IRuleTargetField field = target.getRuleTargetField(fieldId);

        if (SpecialConstantIDs.isSpecialConstantID(constId)) {
            switch (constId) {
                case SpecialConstantIDs.CONST_EMPTY_VALUE:
                    /*
                     * triggered by: <FROZEN /> (not == true)
                     */
                    final boolean empty = (field.getValue() == null);
                    if (not == empty) {
                        return false;
                    }
                    break;

                case SpecialConstantIDs.CONST_WAS_EMPTY_VALUE:
                    /*
                     * triggered by: <CANNOTLOSEVALUE /> (not == true)
                     */
                    final boolean empty2 = (field.getOriginalValue() == null);
                    if (not == empty2) {
                        return false;
                    }
                    break;

                case SpecialConstantIDs.CONST_VALUE_IN_OTHER_FIELD:
                    /*
                     * triggered by: <NOTSAMEAS /> (not == false)
                     */
                case SpecialConstantIDs.CONST_OLD_VALUE_IN_OTHER_FIELD:
                    /*
                     * triggered by: <DEFAULT-COPY source="field" /> (not ==
                     * false)
                     */

                    /*
                     * ConstValueInOtherField and ConstOldValueInOtherField are
                     * special cases. They indicate values used to locate a
                     * field for other rule engine functionality. They do not
                     * indicate a test to be performed, so from the perspective
                     * of this method they are ignored.
                     */
                    break;

                default:
                    /*
                     * All other constant IDs should never happen for If2ConstId
                     * in TFS WIT v1.
                     */
                    throw new UnhandledSpecialConstantIDException(constId, rule, "If2ConstId"); //$NON-NLS-1$
            }
        } else {
            /*
             * Not a special constant ID. This should never happen in TFS WIT
             * v1, since this method is only called for the If2 case.
             */
            throw new UnhandledRuleStateException(rule, "If2 with a non special constant id (" + constId + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return true;
    }

    private boolean passesIf1Test(final int fieldId, final int constId, final boolean not, final Rule rule) {
        if (fieldId == 0) {
            /*
             * If fieldId is unspecified (0), then this If1 test passes
             */
            return true;
        }

        /*
         * Get the field under test
         */
        final IRuleTargetField field = target.getRuleTargetField(fieldId);

        if (SpecialConstantIDs.isSpecialConstantID(constId)) {
            switch (constId) {
                case SpecialConstantIDs.CONST_SAME_AS_OLD_VALUE:

                    // triggered by:
                    // * <WHENCHANGED /> (not == true)
                    // * <WHENNOTCHANGED /> (not == false)
                    // * in-the-box rules (not == false)

                    final boolean sameAsOldValue = isSameAsOldValue(field);
                    if (not == sameAsOldValue) {
                        return false;
                    }
                    break;

                case SpecialConstantIDs.CONST_EMPTY_VALUE:

                    // triggered by:
                    // * <WHEN value="" /> (not == false)
                    // * <WHENNOT value="" /> (not == true)
                    // * in-the-box rules (not == false)
                    // * provisioning-generated rules
                    // (WITImporter.ProcessDefaultState) (not == false)

                    final boolean empty = (field.getValue() == null);
                    if (not == empty) {
                        return false;
                    }
                    break;

                case SpecialConstantIDs.CONST_BECAME_NON_EMPTY_VALUE:

                    /*
                     * GUESS - was empty and became non empty
                     */

                    // only triggered by in-the-box rules:
                    // * If [Created Date] became non-empty, then it should be
                    // equal to serverdatetime (not == false)
                    // * If [Created By] became non-empty (-10014) then it
                    // should be equal to Person doing action (not == false)
                    /*
                     * we could probably safely ignore rules with this condition
                     */

                    if (field.getOriginalValue() == null) {
                        final boolean becameNonEmpty = (field.getValue() != null);
                        if (not == becameNonEmpty) {
                            return false;
                        }
                    }
                    break;

                case SpecialConstantIDs.CONST_REMAINED_NON_EMPTY_VALUE:
                    /*
                     * GUESS - was non empty and remained non empty
                     */

                    // only triggered by in-the-box rules:
                    // If [Created Date] remained non-empty (-10015) then it
                    // should be equal to old value. (not == false)
                    // If [Created By] remained non-empty (-10015) then it
                    // should be equal to old value (not == false)
                    /*
                     * we could probably safely ignore rules with this condition
                     */
                    if (field.getOriginalValue() != null) {
                        final boolean remainedNonEmpty = (field.getValue() != null);
                        if (not == remainedNonEmpty) {
                            return false;
                        }
                        break;
                    } else {
                        /*
                         * the original value is empty, so the "was non empty
                         * and remained non empty" condition is not satisfied
                         */
                        return false;
                    }

                case SpecialConstantIDs.CONST_WAS_EMPTY_VALUE:
                    /*
                     * triggered by: provisioning-generated rule
                     * (WITImporter.ProcessDefaultState) (not == false)
                     */
                    final boolean empty2 = (field.getOriginalValue() == null);
                    if (not == empty2) {
                        return false;
                    }
                    break;

                default:
                    /*
                     * All other constant IDs should never happen for IfConstId
                     * in TFS WIT v1.
                     */
                    throw new UnhandledSpecialConstantIDException(constId, rule, "IfConstId"); //$NON-NLS-1$
            }
        } else {
            // Not a special constant ID.
            // Triggered by:
            // * <WHEN value="bar" /> (not == false)
            // * <WHENNOT value="bar" /> (not == true)
            // * provisioning generated rule (UpdatePackage.InsertForm) (not ==
            // false)

            final boolean equal = fieldValueEqualToConstant(field.getValue(), constId);

            if (not == equal) {
                return false;
            }
        }

        return true;
    }

    private boolean fieldValueEqualToConstant(final Object fieldValue, final int constantId) {
        if (fieldValue == null) {
            /*
             * A null field value will never match a constant - all constants
             * are non-null (except of course for the special constant
             * ConstEmptyValue which is not handled by this method)
             */
            return false;
        }

        final String fieldValueAsString = translateFieldValueIntoString(fieldValue);

        if (fieldValueAsString == null) {
            /*
             * Couldn't convert the field value into a String. Such a value will
             * never match a constant, since all constants are strings.
             */
            return false;
        }

        final String constantValue = witContext.getMetadata().getConstantsTable().getConstantByID(constantId);

        /*
         * I18N: need to use a java.text.Collator with a specified Locale
         */
        return constantValue.equalsIgnoreCase(fieldValueAsString);
    }

    private boolean fieldValuesEqual(final Object fieldValue1, final Object fieldValue2) {
        if (fieldValue1 == null) {
            return (fieldValue2 == null);
        }
        if (fieldValue1 instanceof String && fieldValue2 instanceof String) {
            final String s1 = (String) fieldValue1;
            final String s2 = (String) fieldValue2;

            /*
             * I18N: need to use a java.text.Collator with a specified Locale
             */
            return s1.equalsIgnoreCase(s2);
        }
        return fieldValue1.equals(fieldValue2);
    }

    /**
     * Method called to translate some strongly-typed Field value into a value
     * that can be compared to a String constant or a ConstantSet (which
     * contains Strings). If this method returns null, that indicates that the
     * input value could not be converted to a String. The translation is done
     * by looking at the runtime type of the field value and attempting a string
     * conversion for supported types.
     *
     * @param input
     *        value to convert
     * @return String value as described above, or null if a conversion wasn't
     *         possible
     */
    private String translateFieldValueIntoString(final Object input) {
        /*
         * See: Microsoft.TeamFoundation.WorkItemTracking.Client.Provision.
         * ImporterField .CheckValidValue for some of the reasoning behind these
         * conversions.
         */

        if (input == null) {
            return null;
        }

        if (input instanceof String) {
            return (String) input;
        }

        if (input instanceof Integer) {
            return ((Integer) input).toString();
        }

        if (input instanceof Double) {
            return WorkItemUtils.doubleToString((Double) input);
        }

        if (input instanceof GUID) {
            return ((GUID) input).getGUIDString();
        }

        /*
         * TODO: Dates. Not sure what to do about them. Need to check Visual
         * Studio's implementation and see what they do. It's possible that the
         * importer won't let you use a date-valued field anywhere that a
         * constant comparison can be made, so we may not have to worry about
         * this case. Need to check on it.
         */

        return null;
    }

    private boolean isSameAsOldValue(final IRuleTargetField field) {
        /*
         * Note that for a new work item, a field is considered "same as old
         * value" until a value has been set on it. This seems to be consistent
         * with Visual Studio's rule engine.
         */

        boolean sameAsOldValue = !field.isNewValueSet();

        /*
         * isNewValueSet will be true for server computed values while both the
         * original value and new value are equal to null. Skip the following
         * test if this is a server computed value.
         */

        if (!sameAsOldValue && field.getServerComputedType() == null) {
            /*
             * We need to check an edge case here. field.isNewValueSet() will
             * return true (and FieldImpl.isDirty() will be true) when the new
             * value and the old value differ only by case (eg "test" vs
             * "TEST"). This is desired behavior, as it must be possible to
             * update a work item changing only the case of a field value.
             * However, from the point of view of the rule engine, the case of
             * values is ignored. So in that example from the point of view of
             * the rule engine, "sameAsOldValue" equals true, even though from
             * the point of view of the Field object, "sameAsOldValue" equals
             * false.
             *
             * This definition of "changed" (that ignores case) is used by the
             * If1 test (<WHENCHANGED /> and <WHENNOTCHANGED /> in WITD), the
             * ConstWasEmptyOrSameAsOldValue denywrite rule (<FROZEN /> in
             * WITD), and any WITD rule that was created while a
             * <ALLOWEXISTINGVALUE /> element was in scope
             * (ThenImplicitUnchanged flag is set). In all of these cases, case
             * is ignored by the rule engine when making the determination of
             * whether or not a field has changed.
             *
             * We call the rule engine fieldValuesEqual() method, which ignores
             * case when comparing String values.
             */
            sameAsOldValue = fieldValuesEqual(field.getOriginalValue(), field.getValue());
        }

        return sameAsOldValue;
    }

    private IConstantSet getConstantSetFromThenFields(final Rule rule) {
        return witContext.getMetadata().getConstantHandler().getConstantSet(
            rule.getThenConstID(),
            rule.isFlagThenOneLevel(),
            rule.isFlagThenTwoPlusLevels(),
            rule.isFlagThenLeaf(),
            rule.isFlagThenInterior(),
            true);
    }

    private void setInvalidStatus(final IRuleTargetField field, final FieldStatus status) {
        if (field.isEditable()) {
            field.setStatus(status);
        }
    }

    /**
     * Get a value to display in a log trace for the specified field ID. Get the
     * field reference name if possible, otherwise just return the ID as a
     * string.
     *
     * @param fieldId
     *        The field Id.
     * @return A string to display within a log.trace message.
     */
    private String getFieldNameForTrace(final Integer fieldId) {
        try {
            return ((FieldImpl) target.getRuleTargetField(fieldId)).getReferenceName();
        } catch (final Exception e) {
            return fieldId.toString();
        }
    }
}
