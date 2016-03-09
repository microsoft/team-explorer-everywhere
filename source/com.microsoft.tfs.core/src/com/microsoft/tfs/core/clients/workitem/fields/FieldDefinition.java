// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * Represents the attributes of a {@link Field}.
 *
 * @since TEE-SDK-10.1
 */
public interface FieldDefinition extends Comparable {
    /**
     * @return the friendly display name of this field definition.
     */
    public String getName();

    /**
     * @return the reference name of this field definition.
     */
    public String getReferenceName();

    /**
     * @return the ID of this field definition.
     */
    public int getID();

    /**
     * @return the data type that is stored by a {@link Field} that uses this
     *         field definition.
     */
    public FieldType getFieldType();

    /**
     * @return <code>true</code> if work items can be queried by a {@link Field}
     *         that uses this field definition, <code>false</code> otherwise.
     */
    public boolean isQueryable();

    /**
     * @return <code>true</code> if {@link WorkItem}s can be sorted by
     *         {@link Field}s that use this field definition.
     */
    public boolean isSortable();

    /**
     * @return the collection of valid values for a {@link Field} that uses this
     *         field definition.
     */
    public AllowedValuesCollection getAllowedValues();

    /**
     * @return <code>true</code> if the value of the {@link Field} that uses
     *         this field definition is computed.
     */
    public boolean isComputed();

    /**
     * @return the underlying {@link Class} which implements this field
     *         definition.
     */
    public Class getSystemType();

    /**
     * @return <code>true</code> if a Field that uses this field definition is
     *         indexed on the server.
     */
    public boolean isIndexed();

    /**
     * @return <code>true</code> if a Field that uses this field definition is a
     *         long text field type.
     */
    public boolean isLongText();

    /**
     * @return <code>true</code> if a Field that uses this field definition
     *         supports text queries.
     */
    public boolean supportsTextQuery();

    /**
     * @return the intended usage of this field definition.
     */
    public FieldUsages getUsage();

}
