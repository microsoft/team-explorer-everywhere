// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.IWITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.ConstantHandler;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IConstantSet;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ActionsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsagesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyPropertiesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.RulesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoriesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeUsagesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.WorkItemTypeCategoryMembersTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.IRuleCache;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.RuleCache;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.RuleCache.RuleCacheResults;

public class WITContextStub implements IWITContext {
    public static final String CURRENT_USER_DISPLAY_NAME = "current-user-display-name"; //$NON-NLS-1$

    private final RuleCacheStub ruleCache = new RuleCacheStub();
    private final MetadataStub metadata = new MetadataStub();

    public void addRule(final Rule rule) {
        ruleCache.addRule(rule);
    }

    public void addConstant(final int id, final String constant) {
        metadata.addConstant(id, constant);
    }

    public void addConstantSet(
        final int rootConstantID,
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior,
        final IConstantSet constantSet) {
        metadata.addConstantSet(rootConstantID, oneLevel, twoPlusLevels, leaf, interior, constantSet);
    }

    @Override
    public String getCurrentUserDisplayName() {
        return CURRENT_USER_DISPLAY_NAME;
    }

    @Override
    public IMetadata getMetadata() {
        return metadata;
    }

    @Override
    public IRuleCache getRuleCache() {
        return ruleCache;
    }

    private static final class RuleCacheStub implements IRuleCache {
        private final Map<Integer, List<Rule>> areaIdToRules = new HashMap<Integer, List<Rule>>();

        public void addRule(final Rule rule) {
            final Integer key = new Integer(rule.getAreaID());
            List<Rule> rules = areaIdToRules.get(key);
            if (rules == null) {
                rules = new ArrayList<Rule>();
                areaIdToRules.put(key, rules);
            }
            rules.add(rule);
        }

        @Override
        public RuleCacheResults getRules(final int areaId) {
            final RuleCacheResults results = new RuleCache.RuleCacheResults();

            results.defaultRules = new ArrayList<Rule>();
            results.nonDefaultRules = new ArrayList<Rule>();
            results.affectedFieldIds = new HashSet<Integer>();

            final List<Rule> rules = areaIdToRules.get(new Integer(areaId));
            if (rules != null) {
                for (final Rule rule : rules) {
                    if (rule.isFlagDefault()) {
                        results.defaultRules.add(rule);
                    } else {
                        results.nonDefaultRules.add(rule);
                    }
                    results.affectedFieldIds.add(new Integer(rule.getThenFldID()));
                }
            }

            return results;
        }

        @Override
        public RuleCacheResults getRules(final int areaId, final int changedFieldId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class MetadataStub implements IMetadata {
        private final ConstantsTableStub constantsTable = new ConstantsTableStub();
        private final ConstantHandlerStub constantHandler = new ConstantHandlerStub();

        public void addConstantSet(
            final int rootConstantID,
            final boolean oneLevel,
            final boolean twoPlusLevels,
            final boolean leaf,
            final boolean interior,
            final IConstantSet constantSet) {
            constantHandler.addConstantSet(rootConstantID, oneLevel, twoPlusLevels, leaf, interior, constantSet);
        }

        public void addConstant(final int id, final String constant) {
            constantsTable.addConstant(id, constant);
        }

        @Override
        public ActionsTable getActionsTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConstantHandler getConstantHandler() {
            return constantHandler;
        }

        @Override
        public ConstantsTable getConstantsTable() {
            return constantsTable;
        }

        @Override
        public FieldUsagesTable getFieldUsagesTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FieldsTable getFieldsTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HierarchyPropertiesTable getHierarchyPropertiesTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HierarchyTable getHierarchyTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RulesTable getRulesTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkItemTypeTable getWorkItemTypeTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkItemTypeUsagesTable getWorkItemTypeUsagesTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkItemLinkTypesTable getLinkTypesTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkItemTypeCategoriesTable getWorkItemTypeCategoriesTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkItemTypeCategoryMembersTableImpl getWorkItemTypeCategoryMembersTable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getUserDisplayMode() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ConstantsTableStub implements ConstantsTable {
        private final Map<Integer, String> constants = new HashMap<Integer, String>();

        public void addConstant(final int id, final String constant) {
            constants.put(new Integer(id), constant);
        }

        @Override
        public String getConstantByID(final int id) {
            final Integer key = new Integer(id);

            if (!constants.containsKey(key)) {
                throw new IllegalArgumentException("no constant exists with id [" + id + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            return constants.get(key);
        }

        @Override
        public ConstantMetadata getConstantByString(final String string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer getIDByConstant(final String constant) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getUserGroupDisplayNames(final String serverGuid, final String projectGuid) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ConstantHandlerStub implements ConstantHandler {
        private final Map<ConstantSetKey, IConstantSet> constantSets = new HashMap<ConstantSetKey, IConstantSet>();

        public void addConstantSet(
            final int rootConstantID,
            final boolean oneLevel,
            final boolean twoPlusLevels,
            final boolean leaf,
            final boolean interior,
            final IConstantSet constantSet) {
            final ConstantSetKey key = new ConstantSetKey(rootConstantID, oneLevel, twoPlusLevels, leaf, interior);
            constantSets.put(key, constantSet);
        }

        @Override
        public IConstantSet getConstantSet(
            final int[] rootConstantIDs,
            final boolean oneLevel,
            final boolean twoPlusLevels,
            final boolean leaf,
            final boolean interior,
            final boolean useCache) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IConstantSet getConstantSet(
            final int rootConstantID,
            final boolean oneLevel,
            final boolean twoPlusLevels,
            final boolean leaf,
            final boolean interior,
            final boolean useCache) {
            final ConstantSetKey key = new ConstantSetKey(rootConstantID, oneLevel, twoPlusLevels, leaf, interior);

            if (!constantSets.containsKey(key)) {
                throw new IllegalArgumentException("no constant set exists for: " + key); //$NON-NLS-1$
            }

            return constantSets.get(key);
        }

        private static class ConstantSetKey {
            private final int id;
            private final boolean oneLevel;
            private final boolean twoPlusLevels;
            private final boolean leaf;
            private final boolean interior;

            public ConstantSetKey(
                final int id,
                final boolean oneLevel,
                final boolean twoPlusLevels,
                final boolean leaf,
                final boolean interior) {
                this.id = id;
                this.oneLevel = oneLevel;
                this.twoPlusLevels = twoPlusLevels;
                this.leaf = leaf;
                this.interior = interior;
            }

            @Override
            public String toString() {
                return "id=" //$NON-NLS-1$
                    + id
                    + " oneLevel=" //$NON-NLS-1$
                    + oneLevel
                    + " twoPlusLevels=" //$NON-NLS-1$
                    + twoPlusLevels
                    + " leaf=" //$NON-NLS-1$
                    + leaf
                    + " interior=" //$NON-NLS-1$
                    + interior;
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == this) {
                    return true;
                }

                if (!(obj instanceof ConstantSetKey)) {
                    return false;
                }

                final ConstantSetKey other = (ConstantSetKey) obj;
                return this.id == other.id
                    && this.oneLevel == other.oneLevel
                    && this.twoPlusLevels == other.twoPlusLevels
                    && this.leaf == other.leaf
                    && this.interior == other.interior;
            }

            @Override
            public int hashCode() {
                return id + (oneLevel ? 2 : 1) + (twoPlusLevels ? 2 : 1) + (leaf ? 2 : 1) + (interior ? 2 : 1);
            }
        }
    }
}
