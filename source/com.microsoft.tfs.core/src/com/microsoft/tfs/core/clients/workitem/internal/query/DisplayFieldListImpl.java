// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.query.DisplayFieldList;

public class DisplayFieldListImpl implements DisplayFieldList {
    private final WITContext context;
    private final List<FieldDefinition> displayFields = new ArrayList<FieldDefinition>();

    public DisplayFieldListImpl(final WITContext context) {
        this.context = context;
    }

    /*
     * ************************************************************************
     * START of implementation of DisplayFieldList interface
     * ***********************************************************************
     */

    @Override
    public void add(final FieldDefinition fieldDefinition) {
        if (fieldDefinition == null) {
            throw new IllegalArgumentException("fieldDefinition must not be null"); //$NON-NLS-1$
        }

        if (!fieldDefinition.isQueryable()) {
            throw new IllegalArgumentException(MessageFormat.format(
                "fieldDefinition [{0}] is not queryable", //$NON-NLS-1$
                fieldDefinition));
        }

        if (!displayFields.contains(fieldDefinition)) {
            displayFields.add(fieldDefinition);
        }
    }

    @Override
    public void add(final String fieldName) {
        add(context.getClient().getFieldDefinitions().get(fieldName));
    }

    @Override
    public int getSize() {
        return displayFields.size();
    }

    @Override
    public FieldDefinition getField(final int index) {
        return displayFields.get(index);
    }

    @Override
    public int indexOf(final FieldDefinition fieldDefinition) {
        return displayFields.indexOf(fieldDefinition);
    }

    @Override
    public void clear() {
        displayFields.clear();
    }

    /*
     * ************************************************************************
     * END of implementation of DisplayFieldList interface
     * ***********************************************************************
     */
}
