// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ActionsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsagesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyPropertiesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.RulesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoriesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMembersTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeUsagesTable;

public interface IMetadata {
    public ConstantsTable getConstantsTable();

    public ConstantHandler getConstantHandler();

    public HierarchyTable getHierarchyTable();

    public RulesTable getRulesTable();

    public ActionsTable getActionsTable();

    public HierarchyPropertiesTable getHierarchyPropertiesTable();

    public FieldsTable getFieldsTable();

    public WorkItemTypeUsagesTable getWorkItemTypeUsagesTable();

    public WorkItemTypeTable getWorkItemTypeTable();

    public FieldUsagesTable getFieldUsagesTable();

    public WorkItemLinkTypesTable getLinkTypesTable();

    public WorkItemTypeCategoriesTable getWorkItemTypeCategoriesTable();

    public WorkItemTypeCategoryMembersTable getWorkItemTypeCategoryMembersTable();

    public int getUserDisplayMode();
}
