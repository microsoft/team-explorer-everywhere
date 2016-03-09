// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ActionsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantSetsTable;
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
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.ActionsTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.BaseMetadataDAO;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.ConstantSetsTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.ConstantsTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.FieldUsagesTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.FieldsTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.HierarchyPropertiesTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.HierarchyTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.RulesTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.WorkItemLinkTypesTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.WorkItemTypeCategoriesTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.WorkItemTypeCategoryMembersTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.WorkItemTypeTableImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.WorkItemTypeUsagesTableImpl;
import com.microsoft.tfs.core.internal.db.ConnectionPool;
import com.microsoft.tfs.core.internal.db.dao.BaseDAOFactory;

public class MetadataDAOFactory extends BaseDAOFactory {
    private final Metadata metadata;

    public MetadataDAOFactory(final Metadata metadata, final ConnectionPool connectionPool) {
        super(connectionPool);
        this.metadata = metadata;
    }

    @Override
    protected void doAddImplementationMappings() {
        addImplementation(ActionsTable.class, ActionsTableImpl.class);
        addImplementation(ConstantsTable.class, ConstantsTableImpl.class);
        addImplementation(FieldsTable.class, FieldsTableImpl.class);
        addImplementation(HierarchyTable.class, HierarchyTableImpl.class);
        addImplementation(RulesTable.class, RulesTableImpl.class);
        addImplementation(WorkItemTypeTable.class, WorkItemTypeTableImpl.class);
        addImplementation(HierarchyPropertiesTable.class, HierarchyPropertiesTableImpl.class);
        addImplementation(ConstantSetsTable.class, ConstantSetsTableImpl.class);
        addImplementation(WorkItemTypeUsagesTable.class, WorkItemTypeUsagesTableImpl.class);
        addImplementation(FieldUsagesTable.class, FieldUsagesTableImpl.class);
        addImplementation(WorkItemLinkTypesTable.class, WorkItemLinkTypesTableImpl.class);
        addImplementation(WorkItemTypeCategoriesTable.class, WorkItemTypeCategoriesTableImpl.class);
        addImplementation(WorkItemTypeCategoryMembersTable.class, WorkItemTypeCategoryMembersTableImpl.class);
    }

    @Override
    protected void doInitializeDAOImplementation(final Object implementaion) {
        if (implementaion instanceof BaseMetadataDAO) {
            ((BaseMetadataDAO) implementaion).setMetadata(metadata);
        }
    }
}
