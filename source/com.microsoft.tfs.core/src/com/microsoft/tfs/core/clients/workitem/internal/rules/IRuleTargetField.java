// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.internal.fields.ServerComputedFieldType;

public interface IRuleTargetField {
    public boolean isNewValueSet();

    public boolean isEditable();

    public Object getOriginalValue();

    public Object getValue();

    public void unsetNewValue();

    /**
     * Called by the rule engine to set the value of this field. The set must
     * not recursively trigger the rules engine.
     *
     * @param value
     *        the new value to set
     */
    public void setValueFromRule(Object value);

    public void setHelpText(String helpText);

    public void setServerComputed(ServerComputedFieldType serverComputedType);

    public ServerComputedFieldType getServerComputedType();

    public IFieldPickListSupport getPickListSupport();

    public void setStatus(FieldStatus status);

    public void setReadOnly(boolean readOnly);

    public void postProcessAfterRuleRun();
}
