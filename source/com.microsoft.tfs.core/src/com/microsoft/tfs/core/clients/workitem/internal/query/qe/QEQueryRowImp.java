// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRow;

public class QEQueryRowImp implements QEQueryRow {
    private String logicalOperator;
    private String fieldName;
    private String operator;
    private String value;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Override
    public String getLogicalOperator() {
        return logicalOperator;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setLogicalOperator(final String logicalOperator) {
        final String old = this.logicalOperator;
        this.logicalOperator = logicalOperator;
        propertyChangeSupport.firePropertyChange("logicalOperator", old, logicalOperator); //$NON-NLS-1$
    }

    @Override
    public void setFieldName(final String fieldName) {
        final String old = this.fieldName;
        this.fieldName = fieldName;
        propertyChangeSupport.firePropertyChange("fieldName", old, fieldName); //$NON-NLS-1$
    }

    @Override
    public void setOperator(final String operator) {
        final String old = this.operator;
        this.operator = operator;
        propertyChangeSupport.firePropertyChange("operator", old, operator); //$NON-NLS-1$
    }

    @Override
    public void setValue(final String value) {
        final String old = this.value;
        this.value = value;
        propertyChangeSupport.firePropertyChange("value", old, value); //$NON-NLS-1$
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
