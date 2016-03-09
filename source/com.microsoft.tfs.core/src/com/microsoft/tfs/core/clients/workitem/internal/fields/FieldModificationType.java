// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

public class FieldModificationType {
    /**
     * Indicates a modification made by an end-user, through the public object
     * model (often through the user interface built on top of the public object
     * model).
     */
    public static final FieldModificationType USER = new FieldModificationType("USER"); //$NON-NLS-1$

    /**
     * Indicates a modification made by the rule engine.
     */
    public static final FieldModificationType RULE = new FieldModificationType("RULE"); //$NON-NLS-1$

    /**
     * Indicates a modification made by the server. These kind of modifications
     * are triggered by "server computed value" rules. Important: this
     * FieldModificationType should only be used if the value being set is a
     * String. By definition, all modification made by data coming from the
     * server will be character data.
     */
    public static final FieldModificationType SERVER = new FieldModificationType("SERVER"); //$NON-NLS-1$

    /**
     * Indicates a modification made when creating a new work item.
     */
    public static final FieldModificationType NEW = new FieldModificationType("NEW"); //$NON-NLS-1$

    /**
     * Indicates a modification made by the internal work item model - for
     * example, setting the System.ChangedBy field to the current user.
     */
    public static final FieldModificationType INTERNAL_MODEL = new FieldModificationType("NEW"); //$NON-NLS-1$

    private final String type;

    private FieldModificationType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
