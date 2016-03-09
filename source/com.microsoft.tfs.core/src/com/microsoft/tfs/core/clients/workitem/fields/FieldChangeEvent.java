// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

/**
 * Event fired when a {@link Field}'s value changes. The {@link #source} object
 * may be used to resolve the original UI control which caused the change.
 *
 * @since TEE-SDK-10.1
 */
public class FieldChangeEvent {
    /**
     * The object which caused the field to change. May be <code>null</code>.
     */
    public Object source;

    /**
     * The field that changed. Never <code>null</code>.
     */
    public Field field;
}
