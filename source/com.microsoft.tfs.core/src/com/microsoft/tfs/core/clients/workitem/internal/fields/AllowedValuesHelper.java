// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IConstantSet;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.SpecialConstantIDs;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.NodeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.rules.Rule;
import com.microsoft.tfs.util.PrimitiveArrayHelpers;

public class AllowedValuesHelper {
    private static final Log log = LogFactory.getLog(AllowedValuesHelper.class);

    private final int fieldDefinitionId;
    private final int internalFieldType;
    private final WITContext witContext;

    public AllowedValuesHelper(final int fieldDefinitionId, final int internalFieldType, final WITContext witContext) {
        this.fieldDefinitionId = fieldDefinitionId;
        this.internalFieldType = internalFieldType;
        this.witContext = witContext;
    }

    public String[] compute() {
        if (log.isDebugEnabled()) {
            log.debug(
                MessageFormat.format("computing allowed values for field: {0}", Integer.toString(fieldDefinitionId))); //$NON-NLS-1$
        }

        if ((internalFieldType & FieldTypeConstants.MASK_FIELD_TYPE_ONLY) == FieldTypeConstants.TYPE_TREENODE) {
            return computeValuesForTreeNodesWithType(fieldDefinitionId);
        } else if (fieldDefinitionId == WorkItemFieldIDs.NODE_NAME) {
            return computeAllNodeNameValues();
        } else if (fieldDefinitionId == WorkItemFieldIDs.NODE_TYPE) {
            return computeAllNodeTypeValues();
        } else if (fieldDefinitionId == WorkItemFieldIDs.AUTHORIZED_AS) {
            return computeAllPersonNameValues();
        } else {
            return computeFromRules(fieldDefinitionId);
        }
    }

    private String[] computeValuesForTreeNodesWithType(final int typeId) {
        final NodeMetadata[] nodes = witContext.getMetadata().getHierarchyTable().getNodesWithTypeID(typeId);
        return computeNodeNames(nodes);
    }

    private String[] computeAllNodeNameValues() {
        final NodeMetadata[] allNodes = witContext.getMetadata().getHierarchyTable().getAllNodes();
        return computeNodeNames(allNodes);
    }

    private String[] computeAllNodeTypeValues() {
        /*
         * It's not clear from reversing the MS implementation exactly how this
         * value should be computed. Since the corresponding field definition
         * (System.NodeType) is internal and we don't currently use the allowed
         * values for this field definition internally, figuring it out isn't
         * important right now.
         */
        return null;
    }

    private String[] computeAllPersonNameValues() {
        final IConstantSet constantSet =
            witContext.getMetadata().getConstantHandler().getConstantSet(-2, true, true, true, true, false);
        return constantSet.toArray();
    }

    private String[] computeNodeNames(final NodeMetadata[] nodeMetadataArray) {
        final Set<String> names = new HashSet<String>();

        for (int i = 0; i < nodeMetadataArray.length; i++) {
            names.add(nodeMetadataArray[i].getName());
        }

        return names.toArray(new String[names.size()]);
    }

    private String[] computeFromRules(final int fieldIdForRules) {
        /*
         * This array implements a fast mapping between a set of boolean
         * parameters and a List. Specifically, it allows us to map the set of 4
         * booleans: (onelevel, twolevelsplus, leaf, interior) to a List of root
         * constant ids that can be passed into a ConstantSet constructor with
         * the corresponding boolean parameters.
         *
         * Given a set of 4 booleans, we can calculate a proper index into this
         * array. Inversely, given an index, we can work backwards to the set of
         * 4 booleans. There are methods below to implement these calculations.
         */
        final List[] ruleSlots = new List[16];

        /*
         * This will hold the actual values as we build them up.
         */
        final Set<String> allowedValueSet = new HashSet<String>();

        /*
         * Get every rule that has a ThenFldId of our field definition's ID.
         */
        final Rule[] rules = witContext.getMetadata().getRulesTable().getRulesForThenFieldID(fieldIdForRules);

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format(
                "allowed values({0}): found {1} candidate rule(s)", //$NON-NLS-1$
                Integer.toString(fieldDefinitionId),
                rules.length));
        }

        for (int i = 0; i < rules.length; i++) {
            /*
             * if the rule is applicable for allowed value computation
             */
            if (shouldUseRule(rules[i])) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        MessageFormat.format(
                            "allowed values for {0} using rule {1}: thenConst={2} oneLevel={3} twoPlus={4} leaf={5} interior={6}", //$NON-NLS-1$
                            Integer.toString(fieldDefinitionId),
                            Integer.toString(rules[i].getRuleID()),
                            Integer.toString(rules[i].getThenConstID()),
                            rules[i].isFlagThenOneLevel(),
                            rules[i].isFlagThenTwoPlusLevels(),
                            rules[i].isFlagThenLeaf(),
                            rules[i].isFlagThenInterior()));
                }

                /*
                 * compute the proper slot into the array
                 */
                final int slotIndex = calculateIndex(rules[i]);

                /*
                 * ensure the slot contains a List
                 */
                if (ruleSlots[slotIndex] == null) {
                    ruleSlots[slotIndex] = new ArrayList();
                }

                /*
                 * add the current rule's thenconstid to this slot's list
                 */
                ruleSlots[slotIndex].add(new Integer(rules[i].getThenConstID()));
            }
        }

        for (int i = 0; i < ruleSlots.length; i++) {
            final List rootIdList = ruleSlots[i];

            /*
             * if this slot contains a list
             */
            if (rootIdList != null) {
                /*
                 * make a corresponding constant set
                 */
                final IConstantSet constantSet = createConstantSetFromSlot(rootIdList, i);

                /*
                 * and add all of the constant set's values to our allowed value
                 * set
                 */
                allowedValueSet.addAll(constantSet.getValues());
            }
        }

        /*
         * finally, convert the allowed values set to an array and sort it
         */
        return allowedValueSet.toArray(new String[] {});
    }

    private boolean shouldUseRule(final Rule rule) {
        /*
         * We're answering the question: Does the rule define either an
         * "allowed values" or a "suggested values" list association for the
         * field?
         */

        boolean shouldUseRule = false;

        if (rule.isFlagSuggestion()) {
            shouldUseRule = true;
        } else if (rule.isFlagDenyWrite()) {
            shouldUseRule =

                /*
                 * rule must not be a pattern match rule
                 */
                !rule.isFlagThenLike() &&

            /*
             * then const id must be a regular constant id
             */
                    !SpecialConstantIDs.isSpecialConstantID(rule.getThenConstID())
                    &&

            /*
             * this condition indicates an allowed values rule
             */
                    rule.isFlagUnless() != rule.isFlagThenNot();
        }

        if (!shouldUseRule && log.isTraceEnabled()) {
            if (!rule.isFlagDenyWrite() && !rule.isFlagSuggestion()) {
                log.trace(
                    MessageFormat.format(
                        "allowedvalues({0}): rejecting rule {1} - not a denywrite or a suggestion rule", //$NON-NLS-1$
                        Integer.toString(fieldDefinitionId),
                        Integer.toString(rule.getRuleID())));
            }
            if (rule.isFlagDenyWrite()) {
                if (rule.isFlagThenLike()) {
                    log.trace(
                        MessageFormat.format(
                            "allowedvalues({0}): rejecting denywrite rule {1} - a pattern match rule", //$NON-NLS-1$
                            Integer.toString(fieldDefinitionId),
                            Integer.toString(rule.getRuleID())));
                } else if (SpecialConstantIDs.isSpecialConstantID(rule.getThenConstID())) {
                    log.trace(
                        MessageFormat.format(
                            "allowedvalues({0}): rejecting denywrite rule {1} - thenconstid is a special constant", //$NON-NLS-1$
                            Integer.toString(fieldDefinitionId),
                            Integer.toString(rule.getRuleID())));
                } else if (rule.isFlagUnless() == rule.isFlagThenNot()) {
                    log.trace(
                        MessageFormat.format(
                            "allowedvalues({0}): rejecting denywrite rule {1} - unless == thennot", //$NON-NLS-1$
                            Integer.toString(fieldDefinitionId),
                            Integer.toString(rule.getRuleID())));
                }
            }
        }

        return shouldUseRule;
    }

    private int calculateIndex(final Rule rule) {
        return ((rule.isFlagThenOneLevel() ? 1 : 0) * 8)
            + ((rule.isFlagThenTwoPlusLevels() ? 1 : 0) * 4)
            + ((rule.isFlagThenLeaf() ? 1 : 0) * 2)
            + ((rule.isFlagThenInterior() ? 1 : 0) * 1);
    }

    private IConstantSet createConstantSetFromSlot(final List rootIdList, final int slotIx) {
        final int[] rootIds = new int[rootIdList.size()];
        for (int loopIx = 0; loopIx < rootIds.length; loopIx++) {
            rootIds[loopIx] = ((Integer) rootIdList.get(loopIx)).intValue();
        }

        /*
         * to compute the 4 boolean parameters, we basically do the inverse of
         * the calculateIndex method above
         */

        int x = slotIx;

        final boolean oneLevel = ((x / 8) == 1);
        x = x % 8;

        final boolean twoPlusLevels = ((x / 4) == 1);
        x = x % 4;

        final boolean leaf = ((x / 2) == 1);
        x = x % 2;

        final boolean interior = ((x / 1) == 1);

        final IConstantSet constantSet = witContext.getMetadata().getConstantHandler().getConstantSet(
            rootIds,
            oneLevel,
            twoPlusLevels,
            leaf,
            interior,
            false);

        if (log.isTraceEnabled()) {
            log.trace(
                MessageFormat.format(
                    "constant set from root ids {0} (oneLevel={1} twoPlus={2} leaf={3} interior={4}): {5}", //$NON-NLS-1$
                    PrimitiveArrayHelpers.asList(rootIds).toString(),
                    oneLevel,
                    twoPlusLevels,
                    leaf,
                    interior,
                    constantSet.getSize()));
        }

        return constantSet;
    }
}
