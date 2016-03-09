// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.category;

/**
 * A work item category.
 *
 * @since TEE-SDK-10.1
 */
public interface Category {
    /**
     * @return the work item type category identifier.
     */
    int getID();

    /**
     * @return the work item type category name.
     */
    String getName();

    /**
     * @return the work item type category reference name.
     */
    String getReferenceName();

    /**
     * @return the work item type identifier of the default work item type for
     *         this category.
     */
    int getDefaultWorkItemTypeID();
}
