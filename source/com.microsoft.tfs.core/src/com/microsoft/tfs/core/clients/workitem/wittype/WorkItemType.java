// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.wittype;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.form.WIFormDescription;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceURLInfo;

/**
 * Represents a specific type of {@link WorkItem}. To implement
 * {@link Comparable}, sorts by natural ordering of name ({@link #getName()}).
 *
 * @since TEE-SDK-10.1
 */
public interface WorkItemType extends Comparable<WorkItemType> {
    public String getName();

    public Project getProject();

    public int getID();

    /**
     * Gets the form description. Cached internally.
     */
    public WIFormDescription getFormDescription();

    public FieldDefinitionCollection getFieldDefinitions();

    public ProcessGuidanceURLInfo getProcessGuidanceURL();
}
