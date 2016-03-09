// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;

/**
 * Holds the field value state for a WorkItem instance.
 *
 * Threadsafe: No MS-Equivalents:
 * Microsoft.TeamFoundation.WorkItemTracking.Client.FieldCollection
 */
public class FieldCollectionImpl implements FieldCollection {
    private static final Log log = LogFactory.getLog(FieldCollectionImpl.class);

    /*
     * The work item this field collection belongs to
     */
    private final WorkItemImpl workItem;

    /*
     * The WITContext to use
     */
    private final WITContext context;

    /*
     * The cache of Field instances
     */
    private final FieldReferenceBasedCache<Field> cache = new FieldReferenceBasedCache<Field>();

    public FieldCollectionImpl(final WorkItemImpl workItem, final WITContext context) {
        this.workItem = workItem;
        this.context = context;
    }

    /***************************************************************************
     * START of implementation of FieldCollection interface
     **************************************************************************/

    @Override
    public Field getField(final String name) {
        return getFieldInternal(name);
    }

    @Override
    public boolean contains(final String name) {
        return cache.get(name) != null;
    }

    @Override
    public Iterator<Field> iterator() {
        return cache.values().iterator();
    }

    @Override
    public int size() {
        return cache.values().size();
    }

    @Override
    public int getAreaID() {
        return ((Integer) getField(CoreFieldReferenceNames.AREA_ID).getValue()).intValue();
    }

    @Override
    public int getID() {
        return ((Integer) getField(CoreFieldReferenceNames.ID).getValue()).intValue();
    }

    @Override
    public int getRevision() {
        return ((Integer) getField(CoreFieldReferenceNames.REVISION).getValue()).intValue();
    }

    @Override
    public String getWorkItemType() {
        return (String) getField(CoreFieldReferenceNames.WORK_ITEM_TYPE).getValue();
    }

    /***************************************************************************
     * END of implementation of FieldCollection interface
     **************************************************************************/

    /***************************************************************************
     * START of internal-only FieldCollectionImpl methods
     **************************************************************************/

    private IllegalArgumentException newIllegalFieldNameException(final String illegalFieldName) {
        throw new IllegalArgumentException(
            MessageFormat.format(
                "field [{0}] does not exist in this collection (wi={1},size={2})", //$NON-NLS-1$
                illegalFieldName,
                Integer.toString(getID()),
                cache.size()));
    }

    private IllegalArgumentException newIllegalFieldIDException(final int illegalFieldId) {
        throw new IllegalArgumentException(
            MessageFormat.format(
                "field id [{0}] does not exist in this collection (wi={1},size={2})", //$NON-NLS-1$
                Integer.toString(illegalFieldId),
                Integer.toString(getID()),
                cache.size()));
    }

    public void resetAfterUpdate() {
        for (final Field field : cache.values()) {
            final FieldImpl fieldImpl = (FieldImpl) field;
            fieldImpl.resetAfterUpdate();
        }
    }

    /**
     * Resets this FieldCollection by calling reset() on every contained Field.
     */
    public void reset() {
        for (final Field field : cache.values()) {
            final FieldImpl fieldImpl = (FieldImpl) field;
            fieldImpl.reset();
        }
    }

    public void fireFieldChangeListeners() {
        for (final Field field : cache.values()) {
            final FieldImpl fieldImpl = (FieldImpl) field;
            fieldImpl.fireFieldChangeListeners();
        }
    }

    public void copy(final FieldCollectionImpl targetCollection) {
        final int targetProjectId = targetCollection.getWorkItemInternal().getType().getProject().getID();
        final int projectId = getWorkItemInternal().getType().getProject().getID();

        final boolean newProject = (projectId != targetProjectId);

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "copying fields from {0} (project {1}) to {2} (project {3})", //$NON-NLS-1$
                Integer.toString(workItem.getID()),
                Integer.toString(projectId),
                Integer.toString(targetCollection.getWorkItemInternal().getID()),
                Integer.toString(targetProjectId)));
        }

        for (final Field field : cache.values()) {
            final FieldImpl fieldImpl = (FieldImpl) field;
            if (fieldImpl.getFieldDefinitionInternal().canCopy()) {
                if ((field.getID() == WorkItemFieldIDs.AREA_ID || field.getID() == WorkItemFieldIDs.ITERATION_ID)
                    && newProject) {
                    if (log.isTraceEnabled()) {
                        log.trace(MessageFormat.format("skipping copy of field {0}", Integer.toString(field.getID()))); //$NON-NLS-1$
                    }
                    continue;
                }

                final Object value = field.getValue();
                targetCollection.getFieldInternal(field.getID()).setValue(value, FieldModificationType.NEW);

                if (log.isDebugEnabled()) {
                    log.debug(MessageFormat.format(
                        "copied field {0}, value = [{1}]", //$NON-NLS-1$
                        Integer.toString(field.getID()),
                        value));
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(MessageFormat.format("skipping copy of field {0}", Integer.toString(field.getID()))); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Ensures that all field definitions in the work item physical type have
     * corresponding Fields in this FieldCollection. If neccessary new Fields
     * are created and added to this FieldCollection, but any existing Fields
     * that are part of the work item physical type will not be affected.
     */
    public void ensureAllFieldsInWIPhysicalType() {
        final FieldDefinitionImpl[] fieldDefinitions = context.getWorkItemFieldUsages().getFieldDefinitions();

        final List<FieldDefinition> fieldDefsToEnsure = new ArrayList<FieldDefinition>();
        for (int i = 0; i < fieldDefinitions.length; i++) {
            if (fieldDefinitions[i].isUsedInPhysicalType()) {
                fieldDefsToEnsure.add(fieldDefinitions[i]);
            }
        }

        ensureFieldsExist(fieldDefsToEnsure.toArray(new FieldDefinitionImpl[fieldDefsToEnsure.size()]));
    }

    private FieldImpl addField(final FieldDefinitionImpl fieldDefinition) {
        /*
         * sanity check to catch programming bugs - once a Field has been added
         * to a FieldCollection, it should never be removed
         */
        if (cache.get(fieldDefinition.getID()) != null) {
            throw new IllegalStateException(MessageFormat.format(
                "attempting to add field [{0}] but it already exists", //$NON-NLS-1$
                Integer.toString(fieldDefinition.getID())));
        }

        final FieldImpl field = new FieldImpl(fieldDefinition, this, workItem, context);

        cache.put(field, fieldDefinition.getName(), fieldDefinition.getReferenceName(), fieldDefinition.getID());

        return field;
    }

    /**
     * Ensures that all of the given field definitions have corresponding Fields
     * in this FieldCollection. Fields are created and added to this
     * FieldCollection if neccessary to ensure this.
     *
     * @param fieldDefinitions
     *        FieldDefinitions to ensure
     */
    public void ensureFieldsExist(final FieldDefinitionImpl[] fieldDefinitions) {
        for (int i = 0; i < fieldDefinitions.length; i++) {
            if (cache.get(fieldDefinitions[i].getID()) == null) {
                addField(fieldDefinitions[i]);
            }
        }
    }

    /**
     * Used to set the original value of a Field in this FieldCollection,
     * possibly creating the Field if it does not already exist. This method
     * should only be called with character data obtained from the TFS server.
     *
     * @param name
     *        the field name
     * @param data
     *        the data from the Server
     * @param create
     *        true to create the Field if it doesn't exist in this
     *        FieldCollection
     * @throws IllegalArgumentException
     *         if the Field doesn't exist in this FieldCollection and create is
     *         false or the name is invalid
     */
    public void addOriginalFieldValueFromServer(final String name, final String data, final boolean create) {
        findFieldInternal(name, create).setOriginalValue(data);
    }

    /**
     * Used to set the original value of a Field in this FieldCollection,
     * possibly creating the Field if it does not already exist. This method
     * should only be called with character data obtained from the TFS server.
     *
     * @param fieldId
     *        id of the field
     * @param data
     *        the data from the Server
     * @param create
     *        true to create the Field if it doesn't exist in this
     *        FieldCollection
     * @throws IllegalArgumentException
     *         if the Field doesn't exist in this FieldCollection and create is
     *         false or the fieldId is invalid
     */
    public void addOriginalFieldValueFromServer(final int fieldId, final String data, final boolean create) {
        findFieldInternal(fieldId, create).setOriginalValue(data);
    }

    /**
     * Used to set the original value of a Field in this FieldCollection,
     * possibly creating the Field if it does not already exist. This method
     * should only be used with data generated locally (in internal WIT code).
     * If the field data comes from the server then addFieldValueFromServer()
     * should be used instead. The data must be of the correct type.
     *
     * @param fieldId
     *        id of the field
     * @param data
     *        the original value for the Field
     * @param create
     *        true to create the Field if it doesn't exist in this
     *        FieldCollection
     * @throws IllegalArgumentException
     *         if the Field doesn't exist in this FieldCollection and create is
     *         false or the fieldId is invalid
     */
    public void addOriginalFieldValueLocal(final int fieldId, final Object data, final boolean create) {
        findFieldInternal(fieldId, create).setOriginalValueLocal(data);
    }

    /**
     * Used to find a Field in this FieldCollection for the purposes of setting
     * an original value on that Field. Because of this, calculated fields that
     * are not already in this FieldCollection will not be implicitly added by
     * this method, since calculated Fields should never have original values
     * set on them.
     *
     * @param name
     *        the name of a Field to find
     * @param create
     *        true to create the Field if it does not exist in this
     *        FieldCollection
     * @return the specified Field
     */
    private FieldImpl findFieldInternal(final String name, final boolean create) {
        FieldImpl field = (FieldImpl) cache.get(name);

        if (field == null) {
            if (!create) {
                throw newIllegalFieldNameException(name);
            }

            final FieldDefinitionImpl fieldDefinition = context.getWorkItemFieldUsages().getFieldDefinitionByName(name);

            if (fieldDefinition == null || !fieldDefinition.isUsedInPhysicalType()) {
                throw newIllegalFieldNameException(name);
            }

            field = addField(fieldDefinition);
        }

        return field;
    }

    /**
     * Used to find a Field in this FieldCollection for the purposes of setting
     * an original value on that Field. Because of this, calculated fields that
     * are not already in this FieldCollection will not be implicitly added by
     * this method, since calculated Fields should never have original values
     * set on them.
     *
     * @param fieldId
     *        the ID of a Field to find
     * @param create
     *        true to create the Field if it does not exist in this
     *        FieldCollection
     * @return the specified Field
     */
    private FieldImpl findFieldInternal(final int fieldId, final boolean create) {
        FieldImpl field = (FieldImpl) cache.get(fieldId);

        if (field == null) {
            if (!create) {
                throw newIllegalFieldIDException(fieldId);
            }

            final FieldDefinitionImpl fieldDefinition =
                context.getWorkItemFieldUsages().getFieldDefinitionByID(fieldId);

            if (fieldDefinition == null || !fieldDefinition.isUsedInPhysicalType()) {
                throw newIllegalFieldIDException(fieldId);
            }

            field = addField(fieldDefinition);
        }

        return field;
    }

    public boolean isDirty() {
        /*
         * a field collection is dirty if it has at least one field modification
         * that has been made by the user
         */
        for (final Field field : cache.values()) {
            final FieldImpl fieldImpl = (FieldImpl) field;
            if (field.isDirty() && fieldImpl.getModificationType() == FieldModificationType.USER) {
                return true;
            }
        }
        return false;
    }

    public boolean isValid() {
        for (final Field field : cache.values()) {
            if (field.getStatus() != FieldStatus.VALID) {
                return false;
            }
        }
        return true;
    }

    public FieldImpl getFieldInternal(final String name) {
        FieldImpl field = (FieldImpl) cache.get(name);
        if (field == null) {
            field = addCalculatedFieldIfPossible(name);
            if (field == null) {
                throw newIllegalFieldNameException(name);
            }
        }
        return field;
    }

    public FieldImpl getFieldInternal(final int fieldId) {
        FieldImpl field = (FieldImpl) cache.get(fieldId);
        if (field == null) {
            field = addCalculatedFieldIfPossible(fieldId);
            if (field == null) {
                throw newIllegalFieldIDException(fieldId);
            }
        }
        return field;
    }

    public boolean hasField(final int fieldId) {
        return cache.get(fieldId) != null;
    }

    public WorkItemImpl getWorkItemInternal() {
        return workItem;
    }

    private FieldImpl addCalculatedFieldIfPossible(final int fieldId) {
        return addCalculatedFieldIfPossible(context.getWorkItemFieldUsages().getFieldDefinitionByID(fieldId));
    }

    private FieldImpl addCalculatedFieldIfPossible(final String fieldName) {
        return addCalculatedFieldIfPossible(context.getWorkItemFieldUsages().getFieldDefinitionByName(fieldName));
    }

    private FieldImpl addCalculatedFieldIfPossible(final FieldDefinitionImpl fieldDefinition) {
        /*
         * The rationale here is that when a client of this class asks for a
         * field that doesn't exist in the collection, we can occasionally do
         * better than just throwing an exception.
         *
         * If the requested field is known to be a calculated field AND this
         * collection already contains the corresponding non-calculated field,
         * this method will then add the requested field to this collection and
         * return it.
         *
         * This behavior is especially useful for query results. Queries that
         * are specified to select expensive calculated fields instead
         * transparently select their inexpensive non-calculated counterparts.
         * When the user of the OM then attempts to access the calcualted field
         * value from the query results, this code path is reached and we create
         * the calculated field on-the-fly.
         */

        if (fieldDefinition == null || !fieldDefinition.isUsedInPhysicalType()) {
            return null;
        }

        switch (fieldDefinition.getID()) {
            case WorkItemFieldIDs.AUTHORIZED_AS:
                if (hasField(WorkItemFieldIDs.PERSON_ID)) {
                    return addField(fieldDefinition);
                }
                break;

            case WorkItemFieldIDs.ITERATION_PATH:
                if (hasField(WorkItemFieldIDs.ITERATION_ID)) {
                    return addField(fieldDefinition);
                }
                break;

            case WorkItemFieldIDs.TEAM_PROJECT:
            case WorkItemFieldIDs.NODE_NAME:
            case WorkItemFieldIDs.AREA_PATH:
                if (hasField(WorkItemFieldIDs.AREA_ID)) {
                    return addField(fieldDefinition);
                }
                break;
        }

        return null;
    }
}
