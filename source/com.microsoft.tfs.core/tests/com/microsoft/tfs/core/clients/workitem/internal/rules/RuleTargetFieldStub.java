// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.internal.fields.ServerComputedFieldType;

public class RuleTargetFieldStub implements IRuleTargetField {
    private Object originalValue;
    private Object newValue;
    private boolean newValueSet = false;
    private ServerComputedFieldType serverComputedFieldType;
    private FieldStatus fieldStatus;
    private final FieldPickListSupport fieldPickListSupport = new FieldPickListSupport(0, "notused"); //$NON-NLS-1$
    private final int id;

    public RuleTargetFieldStub(final int id) {
        this.id = id;
    }

    public void setOriginalValue(final Object value) {
        this.originalValue = value;
    }

    public void setValue(final Object value) {
        this.newValue = value;
        this.newValueSet = true;
    }

    public int getID() {
        return id;
    }

    public FieldStatus getStatus() {
        return fieldStatus;
    }

    @Override
    public Object getOriginalValue() {
        return originalValue;
    }

    @Override
    public IFieldPickListSupport getPickListSupport() {
        return fieldPickListSupport;
    }

    @Override
    public ServerComputedFieldType getServerComputedType() {
        return serverComputedFieldType;
    }

    @Override
    public Object getValue() {
        if (newValueSet) {
            return newValue;
        } else {
            return originalValue;
        }
    }

    @Override
    public boolean isNewValueSet() {
        return newValueSet;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public void postProcessAfterRuleRun() {
    }

    @Override
    public void setHelpText(final String helpText) {
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
    }

    @Override
    public void setServerComputed(final ServerComputedFieldType serverComputedType) {
        this.serverComputedFieldType = serverComputedType;
        this.newValueSet = true;
    }

    @Override
    public void setStatus(final FieldStatus status) {
        this.fieldStatus = status;
    }

    @Override
    public void setValueFromRule(final Object value) {
        this.newValue = value;
        newValueSet = true;
    }

    @Override
    public void unsetNewValue() {
        this.newValue = null;
        newValueSet = false;
    }
}
