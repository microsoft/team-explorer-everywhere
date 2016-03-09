// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilter;

public class WIFormLinksControlWITypeFilterImpl extends WIFormElementImpl implements WIFormLinksControlWITypeFilter {
    private String workItemType;

    /**
     * Process the attributes of the "Filter" element (when nested in a
     * "WorkItemTypeFilters" element)
     *
     * Attributes: - WorkItemType: required.
     */
    @Override
    void startLoading(final Attributes attributes) {
        workItemType = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_WORKITEMTYPE);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "WorkItemType" attribute of the "Filter" element.
     */
    @Override
    public String getWorkItemType() {
        return workItemType;
    }
}
