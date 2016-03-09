// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.util.Check;

/**
 * @since TEE-SDK-10.1
 */
public class DisplayField {
    public static String getInvariantFieldName(
        final String fieldName,
        final FieldDefinitionCollection fieldDefinitions) {
        if (fieldDefinitions != null) {
            return fieldDefinitions.get(fieldName).getReferenceName();
        }
        return fieldName;
    }

    public static String getLocalizedFieldName(
        final String fieldName,
        final FieldDefinitionCollection fieldDefinitions) {
        if (fieldDefinitions != null) {
            return fieldDefinitions.get(fieldName).getName();
        }
        return fieldName;
    }

    private String fieldName;
    private int width;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public DisplayField(final String fieldName, final int width) {
        this.fieldName = fieldName;
        this.width = width;
    }

    @Override
    public String toString() {
        return fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        Check.notNull(fieldName, "fieldName"); //$NON-NLS-1$

        final String old = this.fieldName;

        this.fieldName = fieldName;

        propertyChangeSupport.firePropertyChange("fieldName", old, fieldName); //$NON-NLS-1$
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        Check.isTrue(width >= 0, "width out of range"); //$NON-NLS-1$

        final int old = this.width;

        this.width = width;

        propertyChangeSupport.firePropertyChange("width", old, width); //$NON-NLS-1$
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
