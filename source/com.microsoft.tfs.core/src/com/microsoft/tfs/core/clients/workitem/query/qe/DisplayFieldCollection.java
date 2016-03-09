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
public class DisplayFieldCollection {
    private final List<DisplayField> list = new ArrayList<DisplayField>();
    private final Map<String, DisplayField> fieldNameToDisplayField = new HashMap<String, DisplayField>();

    public DisplayFieldCollection() {

    }

    public DisplayFieldCollection(final DisplayFieldCollection dfc) {
        for (final Iterator<DisplayField> it = dfc.iterator(); it.hasNext();) {
            final DisplayField displayField = it.next();
            add(new DisplayField(displayField.getFieldName(), displayField.getWidth()));
        }
    }

    public boolean contains(final String fieldName) {
        return fieldNameToDisplayField.containsKey(fieldName);
    }

    public DisplayField get(final String fieldName) {
        return fieldNameToDisplayField.get(fieldName);
    }

    public DisplayField[] toArray() {
        return list.toArray(new DisplayField[list.size()]);
    }

    public Iterator<DisplayField> iterator() {
        return list.iterator();
    }

    public void add(final DisplayField displayField) {
        list.add(displayField);
        fieldNameToDisplayField.put(displayField.getFieldName(), displayField);
    }

    public void insert(final int index, final DisplayField item) {
        list.add(index, item);
        fieldNameToDisplayField.put(item.getFieldName(), item);
    }

    public void remove(final DisplayField item) {
        list.remove(item);
        fieldNameToDisplayField.remove(item.getFieldName());
    }

    public void removeAt(final int index) {
        final DisplayField displayField = list.remove(index);
        fieldNameToDisplayField.remove(displayField.getFieldName());
    }

    public int indexOf(final DisplayField displayField) {
        return list.indexOf(displayField);
    }

    public int getCount() {
        return list.size();
    }

    public DisplayField get(final int index) {
        return list.get(index);
    }

    public void clear() {
        list.clear();
        fieldNameToDisplayField.clear();
    }
}
