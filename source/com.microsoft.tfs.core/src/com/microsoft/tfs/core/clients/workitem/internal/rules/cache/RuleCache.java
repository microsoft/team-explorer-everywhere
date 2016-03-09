// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.rules.Rule;
import com.microsoft.tfs.core.clients.workitem.internal.rules.ValueProvidingRuleComparator;

/**
 * The idea here is to build an acceptably performing cache that can answer
 * these requests: 1) given an area node, return: -- the (sorted) list of
 * default rules that are scoped to that node -- the (unsorted) list of
 * non-default rules that are scoped to that node 2) given an area node and a
 * changed field ID, return the same two categories of results, and the results
 * should be the rules we need to run in response to a field change
 *
 * The following properties *can be* exploited for performance:
 *
 * a) The rule tree is top-heavy - most rules are concentrated in the top 2
 * levels. This means that a common case is that the complete answer to the
 * above requests for a given node can be had be consulting its parent node. b)
 * Changed fields follow something like an 80/20 rule - 20% of the fields get
 * changed 80% of the time. c) Fields that have been changed recently are more
 * likely to be changed next than fields that have been changed less recently.
 * Especially since each keystroke in a field on the work item form results in a
 * field change and rule execution. d) Since we're on the client, we don't need
 * to evaluate the rules in the context of multiple users. This means we can
 * cache rules that apply to the current user only and completely ignore the
 * rest. e) "Default" (value providing) rules must be sorted before execution.
 * We can do this sort once (for perf reasons) and keep the sorted results
 * around. This only works if we include *all* of the current user's default
 * rules in the results. This means that it also makes sense to separate default
 * rules and all other rule types in the cache results.
 */
public class RuleCache implements IRuleCache {
    private final IMetadata metadata;
    private final Map<Integer, RuleCacheNode> idToCacheNode = new HashMap<Integer, RuleCacheNode>();
    private final RulePersonScopeCache rulePersonScopeCache;

    public RuleCache(final WITContext witContext) {
        metadata = witContext.getMetadata();
        rulePersonScopeCache = new RulePersonScopeCache(witContext);
    }

    @Override
    public RuleCacheResults getRules(final int areaId) {
        return getCacheNode(areaId).getRules();
    }

    @Override
    public RuleCacheResults getRules(final int areaId, final int changedFieldId) {
        return getCacheNode(areaId).getRulesForChangedFieldID(changedFieldId);
    }

    public synchronized void clearCache() {
        idToCacheNode.clear();
        rulePersonScopeCache.clear();
    }

    private synchronized RuleCacheNode getCacheNode(final int areaId) {
        final Integer key = new Integer(areaId);

        RuleCacheNode node = idToCacheNode.get(key);
        if (node == null) {
            final int parentId = metadata.getHierarchyTable().getParentID(areaId);

            if (parentId == areaId) {
                /*
                 * when parentId and areaId are the same, we've reached the root
                 * node
                 */
                node = new RuleCacheNode(areaId, null, metadata, rulePersonScopeCache);
            } else {
                /*
                 * otherwise, make a recursive call to compute the new node's
                 * parent node
                 */
                node = new RuleCacheNode(areaId, getCacheNode(parentId), metadata, rulePersonScopeCache);
            }

            idToCacheNode.put(key, node);
        }

        return node;
    }

    public static class RuleCacheResults {
        /**
         * Default (value-providing) rules. Guaranteed sorted in the correct
         * order to apply.
         */
        public List<Rule> defaultRules;

        /**
         * All rules but default (value-providing) rules. Not sorted in any
         * meaningful way - only default rules have a meaningful ordering.
         */
        public List<Rule> nonDefaultRules;

        /**
         * The set of all field IDs that reference fields that are affected by
         * rules in this result set.
         */
        public Set<Integer> affectedFieldIds;
    }

    private static class RuleCacheNode {
        private final RuleCacheNode parent;
        private final int areaId;
        private final IMetadata metadata;
        private final RulePersonScopeCache rulePersonScopeCache;

        private RuleCacheResults allNodeRules;
        private Map<Integer, RuleCacheResults> changedFieldIdToRuleCacheResults;
        private boolean calculatedRules = false;
        private boolean delegateToParent = false;

        public RuleCacheNode(
            final int areaId,
            final RuleCacheNode parent,
            final IMetadata metadata,
            final RulePersonScopeCache rulePersonScopeCache) {
            this.areaId = areaId;
            this.parent = parent;
            this.metadata = metadata;
            this.rulePersonScopeCache = rulePersonScopeCache;
        }

        public synchronized RuleCacheResults getRules() {
            if (!calculatedRules) {
                calculateRules();
            }
            if (delegateToParent) {
                return parent.getRules();
            } else {
                return allNodeRules;
            }
        }

        public synchronized RuleCacheResults getRulesForChangedFieldID(final int changedFieldId) {
            if (!calculatedRules) {
                calculateRules();
            }
            if (delegateToParent) {
                return parent.getRulesForChangedFieldID(changedFieldId);
            } else {
                final Integer key = new Integer(changedFieldId);
                if (!changedFieldIdToRuleCacheResults.containsKey(key)) {
                    final List<Rule> affectedRules = getAffectedRules(changedFieldId);
                    final Set<Integer> affectedFields = getAffectedFields(affectedRules);

                    final RuleCacheResults results = new RuleCacheResults();
                    results.affectedFieldIds = affectedFields;
                    results.defaultRules = new ArrayList<Rule>();
                    results.nonDefaultRules = new ArrayList<Rule>();

                    calculateRulesForChangedFieldID(
                        allNodeRules.defaultRules,
                        results.defaultRules,
                        affectedFields,
                        changedFieldId);
                    calculateRulesForChangedFieldID(
                        allNodeRules.nonDefaultRules,
                        results.nonDefaultRules,
                        affectedFields,
                        changedFieldId);

                    /*
                     * the default rules in results are already in the correct
                     * order, since allnodeRules.defaultRules is in sorted order
                     * when calculateRulesForChangedFieldId iterates over them
                     */

                    changedFieldIdToRuleCacheResults.put(key, results);
                }
                return changedFieldIdToRuleCacheResults.get(key);
            }
        }

        private void calculateRulesForChangedFieldID(
            final List<Rule> sourceRules,
            final List<Rule> targetRules,
            final Set<Integer> affectedFields,
            final int changedFieldId) {
            for (final Rule rule : sourceRules) {
                final Integer thenFldId = new Integer(rule.getThenFldID());
                if (affectedFields.contains(thenFldId)) {
                    if (!(rule.getThenFldID() == changedFieldId && rule.isFlagDefault())) {
                        /*
                         * if the rule is a default (value-providing) rule that
                         * provides a value for the field that changed, we do
                         * not include it
                         */
                        targetRules.add(rule);
                    }
                }
            }
        }

        private Set<Integer> getAffectedFields(final List<Rule> affectedRules) {
            final Set<Integer> affectedFields = new HashSet<Integer>();

            for (final Rule rule : affectedRules) {
                affectedFields.add(new Integer(rule.getThenFldID()));
            }

            return affectedFields;
        }

        private List<Rule> getAffectedRules(final int changedFieldId) {
            final List<Rule> affectedRules = new ArrayList<Rule>();

            for (final Rule rule : allNodeRules.defaultRules) {
                if (isAffectedByFieldChange(rule, changedFieldId)) {
                    affectedRules.add(rule);
                }
            }

            for (final Rule rule : allNodeRules.nonDefaultRules) {
                if (isAffectedByFieldChange(rule, changedFieldId)) {
                    affectedRules.add(rule);
                }
            }

            return affectedRules;
        }

        private boolean isAffectedByFieldChange(final Rule rule, final int changedFieldId) {
            return

            /*
             * If any of FldXID or IfXFldID contain the changed field id, then
             * by definition these rules may be affected by a change to
             * changedFieldId.
             */
            rule.getFld1ID() == changedFieldId
                || rule.getFld2ID() == changedFieldId
                || rule.getFld3ID() == changedFieldId
                || rule.getFld4ID() == changedFieldId
                || rule.getIfFldID() == changedFieldId
                || rule.getIf2FldID() == changedFieldId
                ||

            /*
             * If the rule is a denywrite rule, and the thenfldid is the
             * changedFieldId, then by definition the rule is affected by a
             * changed to changedFieldId.
             */
                (rule.isFlagDenyWrite() && rule.getThenFldID() == changedFieldId);
        }

        private void calculateRules() {
            calculatedRules = true;

            /*
             * get all of the rules scoped directly to this area node
             */
            final Rule[] areaNodeRules = getRulesForAreaNode();

            if (areaNodeRules.length == 0 && parent != null) {
                /*
                 * simple/common case - this node didn't have any rules of its
                 * own - we simply use the parent node's results
                 */
                delegateToParent = true;
            } else {
                changedFieldIdToRuleCacheResults = new HashMap<Integer, RuleCacheResults>();
                allNodeRules = new RuleCacheResults();
                allNodeRules.defaultRules = new ArrayList<Rule>();
                allNodeRules.nonDefaultRules = new ArrayList<Rule>();
                allNodeRules.affectedFieldIds = new HashSet<Integer>();

                for (int i = 0; i < areaNodeRules.length; i++) {
                    if (areaNodeRules[i].isFlagDefault()) {
                        allNodeRules.defaultRules.add(areaNodeRules[i]);
                        allNodeRules.affectedFieldIds.add(new Integer(areaNodeRules[i].getThenFldID()));
                    } else {
                        allNodeRules.nonDefaultRules.add(areaNodeRules[i]);
                        allNodeRules.affectedFieldIds.add(new Integer(areaNodeRules[i].getThenFldID()));
                    }
                }

                if (parent != null) {
                    final RuleCacheResults parentRules = parent.getRules();

                    for (final Rule rule : parentRules.defaultRules) {
                        if (rule.isFlagFlowdownTree()) {
                            allNodeRules.defaultRules.add(rule);
                            allNodeRules.affectedFieldIds.add(new Integer(rule.getThenFldID()));
                        }
                    }

                    for (final Rule rule : parentRules.nonDefaultRules) {
                        if (rule.isFlagFlowdownTree()) {
                            allNodeRules.nonDefaultRules.add(rule);
                            allNodeRules.affectedFieldIds.add(new Integer(rule.getThenFldID()));
                        }
                    }
                }

                /*
                 * sort the default rules into the proper order for firing them
                 */
                Collections.sort(allNodeRules.defaultRules, new ValueProvidingRuleComparator());

                /*
                 * ACL rules do not reference a thenfldid (thenfldid == 0), and
                 * we don't want the id 0 to appear in the affected field id set
                 */
                allNodeRules.affectedFieldIds.remove(new Integer(0));
            }
        }

        private Rule[] getRulesForAreaNode() {
            final List<Rule> rules =
                new ArrayList<Rule>(Arrays.asList(metadata.getRulesTable().getRulesForAreaNode(areaId)));

            for (final Iterator<Rule> it = rules.iterator(); it.hasNext();) {
                final Rule rule = it.next();
                if (!rulePersonScopeCache.isRuleInScope(rule.getPersonID(), rule.isFlagInversePerson())) {
                    it.remove();
                }
            }

            return rules.toArray(new Rule[rules.size()]);
        }
    }

}
