// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionImpl;

/**
 * Handles the Revisions table that comes back from the GetWorkItem call.
 */
public class RevisionsRowSetHandler implements RowSetParseHandler {
    private final WorkItemImpl workItem;
    private List<String[]> rows;
    private List<String> columns;
    private int curColIx;
    private int revColIx;

    public RevisionsRowSetHandler(final WorkItemImpl workItem) {
        this.workItem = workItem;
    }

    private void createRevisions() {
        /*
         * create the first historical revision based off the current work item
         * values
         */
        RevisionImpl revision = RevisionImpl.createFromFieldCollection(
            workItem.getFieldsInternal(),
            workItem.getContext(),
            rows.size(),
            workItem.getRevisionsInternal());

        /*
         * sort the rows collection so the revision rows are from most recent
         * historical revision to earliest historical revision
         */
        Collections.sort(rows, new RowComparator());

        /*
         * loop over each Revisions table row
         */
        for (final Iterator<String[]> rowIt = rows.iterator(); rowIt.hasNext();) {
            final String[] rowValues = rowIt.next();

            for (int i = 0; i < rowValues.length; i++) {
                if (rowValues[i] != null) {
                    final String value = rowValues[i].trim();
                    if (value.length() > 0) {
                        /*
                         * we've found a non-null, non-whitespace row value this
                         * indicates a field change
                         */
                        final String fieldName = columns.get(i);
                        revision.getFieldInternal(fieldName).setOldValueFromString(value);
                    }
                }
            }

            /*
             * add the revision to the work item's revision collection
             */
            workItem.getRevisionsInternal().addRevisionToStart(revision);

            /*
             * create a copy to use for the next pass through the loop
             */
            revision = revision.createCopyForPreviousRevision();
        }

        /*
         * convert the revision into the first revision
         */
        revision.convertToInitialRevision();

        /*
         * add the initial revision to the work item's revision collection
         */
        workItem.getRevisionsInternal().addRevisionToStart(revision);
    }

    @Override
    public void handleBeginParsing() {
        rows = new ArrayList<String[]>();
        columns = new ArrayList<String>();
        curColIx = 0;
        revColIx = -1;
    }

    @Override
    public void handleColumn(final String name, final String type) {
        columns.add(name);
        if (CoreFieldReferenceNames.REVISION.equals(name)) {
            revColIx = curColIx;
        }
        ++curColIx;
    }

    @Override
    public void handleEndParsing() {
        createRevisions();
    }

    @Override
    public void handleFinishedColumns() {
        if (revColIx == -1) {
            throw new IllegalStateException(MessageFormat.format("columns did not contain rev: {0}", columns)); //$NON-NLS-1$
        }
    }

    @Override
    public void handleRow(final String[] rowValues) {
        rows.add(rowValues);
    }

    @Override
    public void handleTableName(final String tableName) {
    }

    private class RowComparator implements Comparator<String[]> {
        @Override
        public int compare(final String[] row1, final String[] row2) {
            final int rev1 = Integer.parseInt(row1[revColIx]);
            final int rev2 = Integer.parseInt(row2[revColIx]);

            return rev2 - rev1; // higher rev rows should come first
        }
    }
}
