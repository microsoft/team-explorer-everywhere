// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @since TEE-SDK-10.1
 */
public class SortFieldCollection {
    private final List<SortField> list = new ArrayList<SortField>();
    private final Map<String, SortField> fieldNameToSortField = new HashMap<String, SortField>();

    public SortFieldCollection() {

    }

    public SortFieldCollection(final SortFieldCollection sfc) {
        for (final Iterator<SortField> it = sfc.iterator(); it.hasNext();) {
            final SortField sortField = it.next();
            add(new SortField(sortField.getFieldName(), sortField.isAscending()));
        }
    }

    public boolean contains(final String fieldName) {
        return fieldNameToSortField.containsKey(fieldName);
    }

    public SortField[] toArray() {
        return list.toArray(new SortField[list.size()]);
    }

    public Iterator<SortField> iterator() {
        return list.iterator();
    }

    public void add(final SortField item) {
        list.add(item);
        fieldNameToSortField.put(item.getFieldName(), item);
    }

    public void insert(final int index, final SortField item) {
        list.add(index, item);
        fieldNameToSortField.put(item.getFieldName(), item);
    }

    public void remove(final SortField item) {
        list.remove(item);
        fieldNameToSortField.remove(item.getFieldName());
    }

    public void removeAt(final int index) {
        final SortField sortField = list.remove(index);
        fieldNameToSortField.remove(sortField.getFieldName());
    }

    public int indexOf(final SortField item) {
        return list.indexOf(item);
    }

    public int getCount() {
        return list.size();
    }

    public SortField get(final int index) {
        return list.get(index);
    }

    public void clear() {
        list.clear();
        fieldNameToSortField.clear();
    }
}
