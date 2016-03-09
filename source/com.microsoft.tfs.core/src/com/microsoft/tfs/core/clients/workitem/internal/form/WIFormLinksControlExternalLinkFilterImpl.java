// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilter;

public class WIFormLinksControlExternalLinkFilterImpl extends WIFormElementImpl
    implements WIFormLinksControlExternalLinkFilter {
    private String linkType;

    /**
     * Parse the attributes of the "Filter" element (when nested in the
     * "ExternalLinkFilters" element)
     *
     * Attributes: - LinkType: required
     */
    @Override
    void startLoading(final Attributes attributes) {
        linkType = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_LINKTYPE);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "LinkType" attribute of the "Filter" element.
     */
    @Override
    public String getLinkType() {
        return linkType;
    }
}
