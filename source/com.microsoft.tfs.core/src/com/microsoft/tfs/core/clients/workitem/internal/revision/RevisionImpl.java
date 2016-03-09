// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.revision;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionImpl;
import com.microsoft.tfs.core.clients.workitem.revision.Revision;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionField;

public class RevisionImpl implements Revision {
    private final List<RevisionFieldImpl> revisionFields = new ArrayList<RevisionFieldImpl>();
    private final Map<String, RevisionFieldImpl> fieldsByReferenceName = new HashMap<String, RevisionFieldImpl>();
    private final Map<Integer, RevisionFieldImpl> fieldsById = new HashMap<Integer, RevisionFieldImpl>();
    private int revisionNumber; // 0-based index of revision, where rev 0 is the
                                // first revision
    private final WITContext context;
    private RevisionCollectionImpl revisionCollection;

    public static RevisionImpl createFromFieldCollection(
        final FieldCollectionImpl fieldCollection,
        final WITContext context,
        final int revisionCount,
        final RevisionCollectionImpl revisionCollection) {
        final RevisionImpl revision = new RevisionImpl(context);
        revision.revisionNumber = revisionCount;
        revision.revisionCollection = revisionCollection;

        final FieldDefinitionImpl[] fieldDefinitions = context.getWorkItemFieldUsages().getFieldDefinitions();

        for (int i = 0; i < fieldDefinitions.length; i++) {
            final FieldDefinitionImpl fieldDefinition = fieldDefinitions[i];

            // It's possible for a new field to be present in the field
            // definitions array but not be present in the field collection
            // passed from the caller. A scenario where this occurs is when a
            // new field has been imported into the team project and has been
            // passed back to the client via a metadata update. Any open work
            // item will have a field collection that does not match the field
            // definitions contained by the metadata. New work items generate
            // their field collection based off the current field definitions
            // and thus have a corresponding fields for each definition.

            if (fieldDefinition.isUsedInPhysicalType() && fieldCollection.hasField(fieldDefinition.getID())) {
                final Field field = fieldCollection.getFieldInternal(fieldDefinition.getID());

                revision.addField(
                    new RevisionFieldImpl(field.getOriginalValue(), field.getValue(), fieldDefinition, revision));
            }
        }

        return revision;
    }

    private RevisionImpl(final WITContext context) {
        this.context = context;
    }

    /*
     * ************************************************************************
     * START of implementation of Revision interface
     * ***********************************************************************
     */

    @Override
    public RevisionField[] getFields() {
        return revisionFields.toArray(new RevisionField[] {});
    }

    @Override
    public RevisionField getField(final int id) {
        final Object key = new Integer(id);
        return fieldsById.get(key);
    }

    @Override
    public RevisionField getField(final String referenceName) {
        return fieldsByReferenceName.get(referenceName);
    }

    /**
     * Revision dates are tracked by AUTHORIZED_DATE in TFS Dev11 and above.
     * Prior to Dev11 the revision dates were tracked by CHANGED_DATE. A
     * revision should always contain one of these two fields.
     *
     *
     * @return The date the revision occurred.
     */
    @Override
    public Date getRevisionDate() {
        final RevisionField field = getField(WorkItemFieldIDs.AUTHORIZED_DATE);
        if (field != null) {
            return (Date) field.getValue();
        }

        return (Date) getField(WorkItemFieldIDs.CHANGED_DATE).getValue();
    }

    @Override
    public String getTagLine() {
        if (revisionNumber == 0) {
            return MessageFormat.format(
                Messages.getString("RevisionImpl.CreatedByPersonFormat"), //$NON-NLS-1$
                (String) getField(WorkItemFieldIDs.CHANGED_BY).getValue());
        }

        final String changedBy = (String) getField(WorkItemFieldIDs.CHANGED_BY).getValue();
        final String authorizedAs = (String) getField(WorkItemFieldIDs.AUTHORIZED_AS).getValue();

        boolean onBehalfOf = false;
        if (authorizedAs != null && !authorizedAs.equals(changedBy)) {
            onBehalfOf = true;
        }

        final String oldState = (String) getField(WorkItemFieldIDs.STATE).getOriginalValue();
        final String newState = (String) getField(WorkItemFieldIDs.STATE).getValue();

        if (!oldState.equals(newState)) {
            if (onBehalfOf) {
                return MessageFormat.format(
                    Messages.getString("RevisionImpl.EditedOldStateToNewStateByPersonOnBehalfOfPersonFormat"), //$NON-NLS-1$
                    oldState,
                    newState,
                    authorizedAs,
                    changedBy);
            } else {
                return MessageFormat.format(
                    Messages.getString("RevisionImpl.EditedOldStateToNewStateByPersonFormat"), //$NON-NLS-1$
                    oldState,
                    newState,
                    changedBy);

            }
        } else {
            if (onBehalfOf) {
                return MessageFormat.format(
                    Messages.getString("RevisionImpl.EditedByPersonOnBehalfOfPersonFormat"), //$NON-NLS-1$
                    authorizedAs,
                    changedBy);
            } else {
                return MessageFormat.format(Messages.getString("RevisionImpl.EditedBtPersonFormat"), changedBy); //$NON-NLS-1$
            }

        }
    }

    /*
     * ************************************************************************
     * END of implementation of Revision interface
     * ***********************************************************************
     */

    public void addField(final RevisionFieldImpl field) {
        revisionFields.add(field);
        fieldsByReferenceName.put(field.getFieldDefinition().getReferenceName(), field);
        fieldsById.put(new Integer(field.getFieldDefinition().getID()), field);
    }

    public RevisionImpl createCopyForPreviousRevision() {
        final RevisionImpl copy = new RevisionImpl(context);
        copy.revisionNumber = revisionNumber - 1;
        copy.revisionCollection = revisionCollection;
        for (final Iterator<RevisionFieldImpl> it = revisionFields.iterator(); it.hasNext();) {
            final RevisionFieldImpl field = it.next();
            copy.addField(RevisionFieldImpl.createCopy(field, copy));
        }
        return copy;
    }

    public RevisionFieldImpl getFieldInternal(final String fieldReferenceName) {
        return fieldsByReferenceName.get(fieldReferenceName);
    }

    public RevisionFieldImpl getFieldInternal(final int id) {
        return fieldsById.get(new Integer(id));
    }

    public void convertToInitialRevision() {
        for (final Iterator<RevisionFieldImpl> it = revisionFields.iterator(); it.hasNext();) {
            final RevisionFieldImpl field = it.next();
            field.setOriginalValue(null);
        }
    }

    public WITContext getContext() {
        return context;
    }

    public RevisionImpl getNextRevision() {
        final int nextIx = revisionNumber + 1;
        if (nextIx < revisionCollection.size()) {
            return revisionCollection.getRevisionInternal(nextIx);
        }
        return null;
    }
}
