// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.HashMap;
import java.util.Map;

public class RuleTargetStub implements IRuleTarget {
    private final int id;
    private final int areaId;
    private final Map fields = new HashMap();

    public RuleTargetStub(final int id, final int areaId) {
        this.id = id;
        this.areaId = areaId;
    }

    @Override
    public int getAreaID() {
        return areaId;
    }

    @Override
    public int getID() {
        return id;
    }

    public void addField(final RuleTargetFieldStub field) {
        fields.put(new Integer(field.getID()), field);
    }

    @Override
    public IRuleTargetField getRuleTargetField(final int fieldId) {
        final Integer key = new Integer(fieldId);

        if (!fields.containsKey(key)) {
            throw new IllegalArgumentException("the field id [" + fieldId + "] does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return (IRuleTargetField) fields.get(key);
    }
}
