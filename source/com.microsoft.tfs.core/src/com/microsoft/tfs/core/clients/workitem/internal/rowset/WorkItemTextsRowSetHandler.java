// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.internal.GetWorkItemFieldNames;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionFieldImpl;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionImpl;

/**
 * Handles the Texts table that comes back from the GetWorkItem call.
 */
public class WorkItemTextsRowSetHandler extends BaseGetWorkItemRowSetHandler {
    private final Map<Integer, Map<Date, String>> data = new HashMap<Integer, Map<Date, String>>();

    public WorkItemTextsRowSetHandler(final WorkItemImpl workItem, final IMetadata metadataManager) {
        super(workItem, metadataManager);
    }

    @Override
    protected void doHandleRow() {
        final Integer fieldId = new Integer(getIntValue(GetWorkItemFieldNames.FIELD_ID));
        final Date dateAdded = getDateValue(GetWorkItemFieldNames.ADDED_DATE);
        String value = getStringValue(GetWorkItemFieldNames.WORDS);

        /*
         * normalize the value to null if it's an empty string or whitespace
         */
        if (value != null && value.trim().length() == 0) {
            value = null;
        }

        /*
         * the data map contains a sub-map of dates to values for each field id
         */
        Map<Date, String> valuesForField = data.get(fieldId);
        if (valuesForField == null) {
            valuesForField = new HashMap<Date, String>();
            data.put(fieldId, valuesForField);
        }

        /*
         * add the current date and value to the sub-map
         */
        valuesForField.put(dateAdded, value);
    }

    @Override
    public void handleEndParsing() {
        /*
         * keeps track of the current values of the long text fields as we
         * iterate over the revisions
         */
        final Map<Integer, Object> currentValues = new HashMap<Integer, Object>();

        /*
         * initially populate the current values map with null for each long
         * text field
         */
        for (final Iterator<Integer> fieldIdIt = data.keySet().iterator(); fieldIdIt.hasNext();) {
            currentValues.put(fieldIdIt.next(), null);
        }

        /*
         * keep a list of the long text field ids this allows us to modify the
         * current values map while we're iterating over the field ids
         */
        final List<Integer> fieldIds = new ArrayList<Integer>(currentValues.keySet());

        /*
         * iterate over the revisions from earliest to latest
         */
        for (int i = 0; i < getWorkItem().getRevisionsInternal().size(); i++) {
            final RevisionImpl revision = getWorkItem().getRevisionsInternal().getRevisionInternal(i);
            final Date revisionDate = revision.getRevisionDate();

            /*
             * process each long text field for this revision
             */
            for (final Iterator<Integer> it = fieldIds.iterator(); it.hasNext();) {
                final Integer fieldId = it.next();
                final Object value = currentValues.get(fieldId);
                final RevisionFieldImpl revisionField = revision.getFieldInternal(fieldId.intValue());

                /*
                 * the current value becomes the old value for this revision
                 */
                revisionField.setOriginalValue(value);

                final Map<Date, String> fieldValues = data.get(fieldId);
                if (fieldValues.containsKey(revisionDate)) {
                    /*
                     * a new value was set for this long text field as of this
                     * revision
                     */
                    final Object newValue = fieldValues.get(revisionDate);

                    /*
                     * set the new value into the revision
                     */
                    revisionField.setNewValue(newValue);

                    /*
                     * update the current values map
                     */
                    currentValues.put(fieldId, newValue);
                } else if (fieldId.intValue() == WorkItemFieldIDs.HISTORY) {
                    /*
                     * the history field is treated specially
                     */
                    revisionField.setNewValue(null);
                    currentValues.put(fieldId, null);
                } else {
                    /*
                     * for this revision, the old value for the long text field
                     * is the same as the name value (field was unchanged)
                     */
                    revisionField.setNewValue(value);
                }
            }
        }

        /*
         * at this point all of the current values need to be put into the work
         * item itself
         */
        for (final Iterator<Integer> it = fieldIds.iterator(); it.hasNext();) {
            final Integer fieldId = it.next();
            final String value = (String) currentValues.get(fieldId);
            getWorkItem().getFieldsInternal().addOriginalFieldValueFromServer(fieldId.intValue(), value, false);
        }
    }
}
