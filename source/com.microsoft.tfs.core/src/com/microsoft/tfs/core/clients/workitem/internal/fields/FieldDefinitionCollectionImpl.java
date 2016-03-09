// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.exceptions.FieldDefinitionNotExistException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldDefinitionMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.wittype.WorkItemTypeImpl;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

/**
 * Implements the FieldDefinitionsCollection interface.
 *
 * This implementation can operate in one of three scoping modes:
 *
 * 1) All field definitions (including internal field definitions and including
 * field definitions not in the work item physical type) are in the collection.
 * This is equivalent to the "FieldDefinitionsClass" class in MS code. For
 * internal use only and not to be exposed through the public OM.
 *
 * 2) All non-internal field definitions that are in the work item physical type
 * are in the collection (regardless of work item type scoping). This is
 * equivalent to the "FieldDefinitionCollection" returned from
 * "WorkItemStore.FieldDefinitions" in MS code. This is for providing a "global"
 * list of non-internal field definitions.
 *
 * 3) All non-internal field definitions that are scoped to a work item type
 * (including non-internal core field definitions) are in the collection. This
 * is equivalent to the "FieldDefinitionCollection" returned from
 * "WorkItemType.FieldDefinitions" in MS code.
 */
public class FieldDefinitionCollectionImpl implements FieldDefinitionCollection {
    private final FieldReferenceBasedCache<FieldDefinition> cache = new FieldReferenceBasedCache<FieldDefinition>();
    private List<FieldDefinition> allFieldDefinitionsSorted;
    private WorkItemType wit = null;

    public FieldDefinitionCollectionImpl(
        final boolean allFieldDefinitionsMode,
        final WITContext context,
        final WorkItemTypeImpl scopeType) {
        if (allFieldDefinitionsMode) {
            initializeFromAllFieldDefinitions(context);
        } else {
            initializeFromWorkItemPhysicalType(context, scopeType);
        }
    }

    /***************************************************************************
     * START of implementation of FieldDefinitionCollection interface
     **************************************************************************/

    @Override
    public Iterator<FieldDefinition> iterator() {
        return allFieldDefinitionsSorted.iterator();
    }

    @Override
    public int size() {
        return allFieldDefinitionsSorted.size();
    }

    @Override
    public FieldDefinition[] getFieldDefinitions() {
        return allFieldDefinitionsSorted.toArray(new FieldDefinition[allFieldDefinitionsSorted.size()]);
    }

    @Override
    public FieldDefinition get(final String fieldName) {
        final FieldDefinition fieldDefinition = cache.get(fieldName);
        if (fieldDefinition == null) {
            throw new FieldDefinitionNotExistException(
                MessageFormat.format(
                    "the field definition with name [{0}] does not exist", //$NON-NLS-1$
                    fieldName));
        }
        return fieldDefinition;
    }

    @Override
    public boolean contains(final String fieldName) {
        return cache.get(fieldName) != null;
    }

    public int getSize() {
        return cache.size();
    }

    /***************************************************************************
     * END of implementation of FieldDefinitionCollection interface
     **************************************************************************/

    /***************************************************************************
     * START of implementation of internal (FieldDefinitionCollectionImpl)
     * methods
     **************************************************************************/

    public FieldDefinitionImpl getFieldDefinitionInternal(final String fieldName) {
        return (FieldDefinitionImpl) get(fieldName);
    }

    public FieldDefinitionImpl getFieldDefinitionInternal(final int id) {
        final FieldDefinitionImpl fieldDefinition = (FieldDefinitionImpl) cache.get(id);
        if (fieldDefinition == null) {
            throw new FieldDefinitionNotExistException(
                MessageFormat.format(
                    "the field definition with id [{0}] does not exist", //$NON-NLS-1$
                    Integer.toString(id)));
        }
        return fieldDefinition;
    }

    private void initializeFromAllFieldDefinitions(final WITContext context) {
        final FieldDefinitionMetadata[] allFieldDefinitionsMetadata =
            context.getMetadata().getFieldsTable().getAllFieldDefinitions();

        final List<FieldDefinition> fieldDefinitions = new ArrayList<FieldDefinition>();

        for (int i = 0; i < allFieldDefinitionsMetadata.length; i++) {
            fieldDefinitions.add(new FieldDefinitionImpl(context, allFieldDefinitionsMetadata[i], wit, null));
        }

        finishInitialization(fieldDefinitions);
    }

    private void initializeFromWorkItemPhysicalType(final WITContext context, final WorkItemTypeImpl scopeType) {
        wit = scopeType;
        final List<FieldDefinition> fieldDefinitions = new ArrayList<FieldDefinition>();

        final Set<Integer> scopeTypeFieldIds = new HashSet<Integer>();
        if (scopeType != null) {
            final int[] workItemFieldIds =
                context.getMetadata().getWorkItemTypeUsagesTable().getFieldIDsForWorkItemType(scopeType.getID());
            for (int i = 0; i < workItemFieldIds.length; i++) {
                scopeTypeFieldIds.add(new Integer(workItemFieldIds[i]));
            }
        }

        addAllFieldDefinition(
            context.getWorkItemFieldUsages().getFieldUsages(),
            scopeType,
            scopeTypeFieldIds,
            fieldDefinitions,
            FieldUsages.WORK_ITEM);

        if (context.isVersion3OrHigher()) {
            addAllFieldDefinition(
                context.getWorkItemLinkFieldUsages().getFieldUsages(),
                scopeType,
                scopeTypeFieldIds,
                fieldDefinitions,
                FieldUsages.WORK_ITEM_LINK);
        }

        finishInitialization(fieldDefinitions);
    }

    private void addAllFieldDefinition(
        final DatastoreItemFieldUsage[] fieldUsages,
        final WorkItemTypeImpl scopeType,
        final Set<Integer> scopeTypeFieldIds,
        final List<FieldDefinition> fieldDefinitions,
        final FieldUsages usage) {
        for (int i = 0; i < fieldUsages.length; i++) {
            final DatastoreItemFieldUsage fieldUsage = fieldUsages[i];

            if (scopeType != null) {
                if (!fieldUsage.isCore()) {
                    if (!scopeTypeFieldIds.contains(new Integer(fieldUsage.getFieldID()))) {
                        continue;
                    }
                }
            }

            final FieldDefinitionImpl fieldDefinition = fieldUsage.getFieldDefinition();
            fieldDefinition.setUsage(usage);

            if (scopeType != null && isInternalFieldDefinition(fieldDefinition)) {
                continue;
            }

            fieldDefinitions.add(fieldDefinition);
        }

    }

    private void finishInitialization(final List<FieldDefinition> fieldDefinitions) {
        Collections.sort(fieldDefinitions);
        allFieldDefinitionsSorted = fieldDefinitions;

        for (final FieldDefinition fieldDefinition : fieldDefinitions) {
            cache.put(
                fieldDefinition,
                fieldDefinition.getName(),
                fieldDefinition.getReferenceName(),
                fieldDefinition.getID());
        }
    }

    private boolean isInternalFieldDefinition(final FieldDefinitionImpl field) {
        if (!isInternalFieldType(field.getPSType())) {
            return field.isIgnored();
        }
        return true;
    }

    private boolean isInternalFieldType(final int psType) {
        switch (psType) {
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREEPATH:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_NAME:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_TYPE:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_PERSON:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER_TREEID:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_DOUBLE:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_DATE_TIME:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_PLAINTEXT:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HISTORY:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HTML:
            case PSFieldDefinitionTypeEnum.TREE_NODE:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_GUID:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_BOOLEAN:
                return false;
        }

        return true;
    }

    /***************************************************************************
     * END of implementation of internal (FieldDefinitionCollectionImpl) methods
     **************************************************************************/
}
