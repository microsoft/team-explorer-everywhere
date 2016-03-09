// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.CoreFields;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.internal.QueryPackageNames;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.query.SortField;
import com.microsoft.tfs.core.clients.workitem.query.SortFieldList;
import com.microsoft.tfs.core.clients.workitem.query.SortType;

import ms.tfs.workitemtracking.clientservices._03._QuerySortOrderEntry;

public class SortFieldListImpl implements SortFieldList {
    private final WITContext context;
    private final List<SortField> sortFields = new ArrayList<SortField>();
    private final List<Integer> ids = new ArrayList<Integer>();

    public SortFieldListImpl(final WITContext context) {
        this.context = context;
    }

    /*
     * ************************************************************************
     * START of implementation of SortFieldList interface
     * ***********************************************************************
     */

    @Override
    public void clear() {
        sortFields.clear();
        ids.clear();
    }

    @Override
    public boolean add(final FieldDefinition fieldDefinition, final SortType sortType) {
        if (fieldDefinition == null) {
            throw new IllegalArgumentException("fieldDefinition must not be null"); //$NON-NLS-1$
        }

        if (!fieldDefinition.isSortable()) {
            throw new IllegalArgumentException(MessageFormat.format(
                "fieldDefinition [{0}] is not sortable", //$NON-NLS-1$
                fieldDefinition));
        }

        if (!ids.contains(new Integer(fieldDefinition.getID()))) {
            sortFields.add(new SortField(fieldDefinition, sortType));
            ids.add(new Integer(fieldDefinition.getID()));
            return true;
        }

        return false;
    }

    @Override
    public boolean add(final String fieldName, final SortType sortType) {
        return add(context.getClient().getFieldDefinitions().get(fieldName), sortType);
    }

    @Override
    public SortField get(final int index) {
        return sortFields.get(index);
    }

    @Override
    public int indexOf(final FieldDefinition fieldDefinition) {
        return ids.indexOf(new Integer(fieldDefinition.getID()));
    }

    @Override
    public boolean remove(final FieldDefinition fieldDefinition) {
        final int ix = indexOf(fieldDefinition);
        if (ix != -1) {
            ids.remove(ix);
            sortFields.remove(ix);
            return true;
        }
        return false;
    }

    @Override
    public void insert(final int ix, final FieldDefinition fieldDefinition, final SortType sortType) {
        if (fieldDefinition == null) {
            throw new IllegalArgumentException("fieldDefinition must not be null"); //$NON-NLS-1$
        }

        if (indexOf(fieldDefinition) != -1) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "field definition [{0}] already exists in this SortFieldList", //$NON-NLS-1$
                    Integer.toString(fieldDefinition.getID())));
        }

        if (!fieldDefinition.isSortable()) {
            throw new IllegalArgumentException(MessageFormat.format(
                "fieldDefinition [{0}] is not sortable", //$NON-NLS-1$
                fieldDefinition));
        }

        sortFields.add(ix, new SortField(fieldDefinition, sortType));
        ids.add(ix, new Integer(fieldDefinition.getID()));
    }

    @Override
    public int getSize() {
        return sortFields.size();
    }

    /*
     * ************************************************************************
     * END of implementation of SortFieldList interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of internal (SortFieldListImpl) methods
     * ***********************************************************************
     */

    public _QuerySortOrderEntry[] getSortOrderEntries() {
        final _QuerySortOrderEntry[] entries = new _QuerySortOrderEntry[sortFields.size()];

        for (int i = 0; i < sortFields.size(); i++) {
            final SortField sortField = sortFields.get(i);
            entries[i] = new _QuerySortOrderEntry(
                sortField.getFieldDefinition().getReferenceName(),
                sortField.getSortType() == SortType.ASCENDING);
        }

        return entries;
    }

    public _QuerySortOrderEntry[] getLinksSortOrder(final boolean isTreeQuery) {
        final List<_QuerySortOrderEntry> list = new ArrayList<_QuerySortOrderEntry>();

        // There are 4 states that we want to handle on the client
        // 1. No sort crieria - do not have the server sort on anything and have
        // the client sort on ID
        // 2. Sorting on ID only - do not have the server sort on ID and have
        // the client sort on ID
        // 3. Sorting on a set of fields that does not include ID - Add ID to
        // the sort list that goes to the server (not the client)
        // 4. Sorting on a set of fields that includes ID - No special case

        // Handle cases 1 and 2
        if ((sortFields.size() == 0)
            || ((sortFields.size() == 1)
                && ((sortFields.get(0)).getFieldDefinition().getID() == CoreFields.ID)
                && (!isTreeQuery || (sortFields.get(0)).getSortType() == SortType.ASCENDING))) {
            return new _QuerySortOrderEntry[0];
        }

        if (isTreeQuery) {
            return getSortOrderEntries();
        }

        boolean includesId = false;

        // first left-hand side
        for (final Iterator<SortField> it = sortFields.iterator(); it.hasNext();) {
            final SortField field = it.next();

            if (field.getFieldDefinition().getUsage() == FieldUsages.WORK_ITEM) {
                list.add(getLinkSortOrder(field, QueryPackageNames.QUERY_LINKS_LEFT_QUERY_PREFIX));
            }
            // Check for System.ID
            if (field.getFieldDefinition().getID() == CoreFields.ID) {
                includesId = true;
            }
        }

        if (!includesId) {
            // This is for case 3. Sorting on a set of field that does not
            // include ID,
            // need to add it to the sort list before sending to server.
            // This is LHS Check - RHS check later on.
            final String columnName = "[" //$NON-NLS-1$
                + QueryPackageNames.QUERY_LINKS_LEFT_QUERY_PREFIX
                + "].[" //$NON-NLS-1$
                + CoreFieldReferenceNames.ID
                + "]"; //$NON-NLS-1$
            list.add(new _QuerySortOrderEntry(columnName, true));
        }

        // then the right hand side
        for (final Iterator<SortField> it = sortFields.iterator(); it.hasNext();) {
            final SortField field = it.next();
            final FieldDefinition fd = field.getFieldDefinition();
            if (fd.getUsage() == FieldUsages.WORK_ITEM) {
                list.add(getLinkSortOrder(field, QueryPackageNames.QUERY_LINKS_RIGHT_QUERY_PREFIX));
            } else if (fd.getUsage() == FieldUsages.WORK_ITEM_LINK) {
                list.add(getLinkSortOrder(field, QueryPackageNames.QUERY_LINKS_LINK_QUERY_PREFIX));
            }
        }

        if (!includesId) {
            // For case 3, RHS Check (must be done in this order)
            final String columnName = "[" //$NON-NLS-1$
                + QueryPackageNames.QUERY_LINKS_RIGHT_QUERY_PREFIX
                + "].[" //$NON-NLS-1$
                + CoreFieldReferenceNames.ID
                + "]"; //$NON-NLS-1$
            list.add(new _QuerySortOrderEntry(columnName, true));
        }

        return list.toArray(new _QuerySortOrderEntry[list.size()]);
    }

    private _QuerySortOrderEntry getLinkSortOrder(final SortField field, final String prefix) {
        final String columnName = "[" + prefix + "].[" + field.getFieldDefinition().getReferenceName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return new _QuerySortOrderEntry(columnName, field.getSortType() == SortType.ASCENDING);
    }

    /*
     * ************************************************************************
     * END of internal (SortFieldListImpl) methods
     * ***********************************************************************
     */
}
