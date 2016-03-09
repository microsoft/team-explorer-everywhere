// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.RulesTable;
import com.microsoft.tfs.core.clients.workitem.internal.rules.Rule;
import com.microsoft.tfs.core.clients.workitem.internal.rules.RuleResultHandler;

public class RulesTableImpl extends BaseMetadataDAO implements RulesTable {
    @Override
    public int getWorkItemFormID(final int teamProjectId, final String workItemTypeName) {
        final int workItemTypeNameConstID =
            getMetadata().getConstantsTable().getIDByConstant(workItemTypeName).intValue();

        final String sql = "select ThenConstID from Rules where " //$NON-NLS-1$
            + "RULES.fDeleted = 0 AND " //$NON-NLS-1$
            + "AreaID = ? AND " //$NON-NLS-1$
            + "ThenFldID = ? AND " //$NON-NLS-1$
            + "IfConstID = ?"; //$NON-NLS-1$

        final Integer result = getConnection().createStatement(sql).executeIntQuery(new Object[] {
            new Integer(teamProjectId),
            new Integer(WorkItemFieldIDs.WORK_ITEM_FORM_ID),
            new Integer(workItemTypeNameConstID)
        });

        if (result == null) {
            throw new RuntimeException(
                MessageFormat.format(
                    "unable to find a form for work item type [{0}] project [{1}]", //$NON-NLS-1$
                    workItemTypeName,
                    Integer.toString(teamProjectId)));
        }

        return result.intValue();
    }

    @Override
    public Rule[] getRulesForAreaNode(final int areaId) {
        return runRuleQuery("where AreaID = ? and fDeleted = 0", new Object[] //$NON-NLS-1$
        {
            new Integer(areaId)
        });
    }

    @Override
    public Rule[] getRulesForThenFieldID(final int fieldId) {
        return runRuleQuery("where ThenFldID = ?", new Object[] //$NON-NLS-1$
        {
            new Integer(fieldId)
        });
    }

    private Rule[] runRuleQuery(final String queryWhereFragment, final Object[] parameters) {
        final String sql = Rule.SELECT_STRING + " " + queryWhereFragment; //$NON-NLS-1$
        final List rules = new ArrayList();
        final RuleResultHandler handler = new RuleResultHandler(rules);
        getConnection().createStatement(sql).executeQuery(parameters, handler);
        return (Rule[]) rules.toArray(new Rule[] {});
    }
}
