// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilter;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilterEnum;

public class WIFormLinksControlExternalLinkFiltersImpl extends WIFormElementImpl
    implements WIFormLinksControlExternalLinkFilters {
    private WIFormLinksControlExternalLinkFilter[] filters;
    private WIFormLinksControlWILinkFilterEnum filterType;

    /**
     * Process the attributes of the "ExternalLinkFilters" element.
     *
     * Attributes: - FilterType: required
     */
    @Override
    void startLoading(final Attributes attributes) {
        filterType = WIFormLinksControlWILinkFilterEnumFactory.fromType(
            attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_FILTERTYPE));
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "ExternalLinkFilters" element.
     *
     * Child elements: (sequence, minimum=0, maximum=unbounded) - Filter
     */
    @Override
    void endLoading() {
        filters =
            (WIFormLinksControlExternalLinkFilter[]) childrenToArray(new WIFormLinksControlExternalLinkFilter[] {});
    }

    /**
     * Corresponds to the "Filter" child elements of the "ExternalLinkFilters"
     * element.
     */
    @Override
    public WIFormLinksControlExternalLinkFilter[] getFilters() {
        return filters;
    }

    /**
     * Corresponds to the "FilterType" attribute of the "ExternalLinkFilters"
     * element.
     */
    @Override
    public WIFormLinksControlWILinkFilterEnum getFilterType() {
        return filterType;
    }

    @Override
    public boolean includes(final String externalLinkName) {
        if (filterType.equals(WIFormLinksControlWILinkFilterEnum.INCLUDEALL)) {
            return true;
        } else if (filterType.equals(WIFormLinksControlWILinkFilterEnum.EXCLUDEALL)) {
            return false;
        } else if (filterType.equals(WIFormLinksControlWILinkFilterEnum.EXCLUDE)) {
            return !contains(externalLinkName);
        } else {
            return contains(externalLinkName);
        }
    }

    private boolean contains(final String linkType) {
        for (int i = 0; i < filters.length; i++) {
            if (linkType.equalsIgnoreCase(filters[i].getLinkType())) {
                return true;
            }
        }
        return false;
    }
}
