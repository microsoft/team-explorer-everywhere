// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.revision;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.LookupFailedException;
import com.microsoft.tfs.core.clients.workitem.internal.type.WITypeConverterException;
import com.microsoft.tfs.core.clients.workitem.internal.type.WIValueSource;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionField;

public class RevisionFieldImpl implements RevisionField {
    public static RevisionFieldImpl createCopy(final RevisionFieldImpl source, final RevisionImpl revision) {
        final RevisionFieldImpl newField =
            new RevisionFieldImpl(source.originalValue, source.originalValue, source.fieldDefinition, revision);
        return newField;
    }

    private Object originalValue;
    private Object newValue;
    private final FieldDefinitionImpl fieldDefinition;
    private final RevisionImpl revision;

    public RevisionFieldImpl(
        final Object originalValue,
        final Object newValue,
        final FieldDefinitionImpl fieldDefinition,
        final RevisionImpl revision) {
        this.originalValue = originalValue;
        this.newValue = newValue;
        this.fieldDefinition = fieldDefinition;
        this.revision = revision;
    }

    /*
     * ************************************************************************
     * START of implementation of RevisionField interface
     * ***********************************************************************
     */

    @Override
    public Object getOriginalValue() {
        return originalValue;
    }

    @Override
    public Object getValue() {
        return newValue;
    }

    @Override
    public String getName() {
        return fieldDefinition.getName();
    }

    @Override
    public String getReferenceName() {
        return fieldDefinition.getReferenceName();
    }

    @Override
    public int getID() {
        return fieldDefinition.getID();
    }

    @Override
    public boolean shouldIgnoreForDeltaTable() {
        final int id = fieldDefinition.getID();

        if ((id == WorkItemFieldIDs.REVISED_DATE)
            || (id == WorkItemFieldIDs.CHANGED_DATE)
            || (id == WorkItemFieldIDs.AUTHORIZED_AS)
            || (id == WorkItemFieldIDs.CHANGED_BY)
            || (id == WorkItemFieldIDs.HISTORY)
            || (id == WorkItemFieldIDs.PERSON_ID)
            || (id == WorkItemFieldIDs.AUTHORIZED_DATE)
            || (id == WorkItemFieldIDs.REVISION)
            || (id == WorkItemFieldIDs.WATERMARK)) {
            return true;
        }

        /*
         * Visual Studio's object model shows link / attachment count fields for
         * the initial revision of a work item as having an old value of null
         * however these fields don't show up in the delta table for the first
         * revision if the new values are 0
         */
        if (id == WorkItemFieldIDs.HYPERLINK_COUNT
            || id == WorkItemFieldIDs.EXTERNAL_LINK_COUNT
            || id == WorkItemFieldIDs.RELATED_LINK_COUNT
            || id == WorkItemFieldIDs.ATTACHED_FILE_COUNT) {
            if (originalValue == null && newValue != null && ((Integer) newValue).intValue() == 0) {
                return true;
            }
        }

        return false;
    }

    /*
     * ************************************************************************
     * END of implementation of RevisionField interface
     * ***********************************************************************
     */

    /**
     * Called from the RevisionsRowSetHandler to handle revision data from the
     * server.
     */
    public void setOldValueFromString(final String input) {
        final String oldValueAsString = fieldDefinition.getTypeConverter().toString(originalValue);
        if (oldValueAsString != null && oldValueAsString.equals(input)) {
            if (fieldDefinition.getID() != WorkItemFieldIDs.ID) {
                /*
                 * this indicates that the old value should be null
                 */
                originalValue = null;
            }
        } else {
            try {
                originalValue = fieldDefinition.getTypeConverter().translate(input, WIValueSource.SERVER);
            } catch (final WITypeConverterException e) {
                /*
                 * An exception here would indicate either totally unexpected
                 * data from the server or a bug in one of the type converters.
                 */
                throw new RuntimeException(e);
            }
        }

        final int id = fieldDefinition.getID();

        if (id == WorkItemFieldIDs.PERSON_ID) {
            final RevisionFieldImpl authorizedAsField = revision.getFieldInternal(WorkItemFieldIDs.AUTHORIZED_AS);
            if (originalValue == null) {
                authorizedAsField.originalValue = null;
            } else {
                try {
                    authorizedAsField.originalValue =
                        revision.getContext().getMetadata().getConstantsTable().getConstantByID(
                            ((Integer) originalValue).intValue());
                } catch (final LookupFailedException ex) {
                    /*
                     * This happened to a customer (on 8-15-2006 running
                     * Teamprise 2.0 preview 1). Apparently the value of
                     * System.PersonId does not always reference a valid
                     * constant.
                     *
                     * Until more information is known, ignore.
                     */
                    authorizedAsField.originalValue = null;
                }
            }
        } else if (id == WorkItemFieldIDs.ITERATION_ID) {
            final Node iterationNode =
                revision.getContext().getRootNode().findNodeDownwards(((Integer) originalValue).intValue());

            /*
             * iterationNode will be null if the iteration id value didn't
             * correspond to any existing node in the metadata. This is the case
             * when a node is deleted.
             */

            final RevisionFieldImpl iterationPathField = revision.getFieldInternal(WorkItemFieldIDs.ITERATION_PATH);
            iterationPathField.originalValue = (iterationNode != null ? iterationNode.getPath() : null);
        } else if (id == WorkItemFieldIDs.AREA_ID) {
            final Node areaNode =
                revision.getContext().getRootNode().findNodeDownwards(((Integer) originalValue).intValue());

            /*
             * areaNode will be null if the area id value didn't correspond to
             * any existing node in the metadata. This is the case when a node
             * is deleted.
             */

            final RevisionFieldImpl areaPathField = revision.getFieldInternal(WorkItemFieldIDs.AREA_PATH);
            areaPathField.originalValue = (areaNode != null ? areaNode.getPath() : null);

            final RevisionFieldImpl nodeNameField = revision.getFieldInternal(WorkItemFieldIDs.NODE_NAME);
            nodeNameField.originalValue = (areaNode != null ? areaNode.getName() : null);
        }
    }

    public void setOriginalValue(final Object value) {
        originalValue = value;
    }

    public void setNewValue(final Object value) {
        newValue = value;
    }

    public FieldDefinitionImpl getFieldDefinition() {
        return fieldDefinition;
    }
}
