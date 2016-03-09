// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldListAssociationSupport;

public class RuleInfluencedFieldAttributes {
    private final FieldListAssociationSupport listAssociations = new FieldListAssociationSupport();

    private boolean required = false;
    private boolean readonly = false;
    private boolean mustBeEmpty = false;

    public FieldListAssociationSupport getFieldListAssociationSupport() {
        return listAssociations;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    public boolean isMustBeEmpty() {
        return mustBeEmpty;
    }

    public void setMustBeEmpty(final boolean mustBeEmpty) {
        this.mustBeEmpty = mustBeEmpty;
    }

    /*
     * public void addProhibitedValues(Collection values); public void
     * addAllowedValues(Collection values); public void
     * addSuggestedValues(Collection values);
     *
     *
     * public void setServerComputed(ServerComputedFieldType type); public
     * boolean isServerComputed(); public ServerComputedFieldType
     * getServerComputedFieldType();
     *
     * public void setEditable(boolean editable);
     *
     *
     * public void beginRuleBatch(); public void endRuleBatch();
     */

}
