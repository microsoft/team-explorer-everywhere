// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlFilterOnEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilter;

public class WIFormLinksControlWILinkFilterImpl extends WIFormElementImpl implements WIFormLinksControlWILinkFilter {
    private String linkType;
    private WIFormLinksControlFilterOnEnum filterOn;

    /**
     * Process the attributes of the "Filter" element (when nested in a
     * "WorkItemLinkFilters" element).
     *
     * Attributes: - LinkType: required - FilterOn: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        linkType = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_LINKTYPE);
        filterOn = WIFormLinksControlFilterOnEnumFactory.fromType(
            attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_FILTERON));
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "LinkType" attribute of the "Filter" element.
     */
    @Override
    public String getLinkType() {
        return linkType;
    }

    /**
     * Corresponds to the "FilterOn" attribute of the "Filter" element.
     */
    @Override
    public WIFormLinksControlFilterOnEnum getLinksControlFilterOn() {
        return filterOn;
    }
}
