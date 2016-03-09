// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlFilterOnEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilter;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilterEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilters;

public class WIFormLinksControlWILinkFiltersImpl extends WIFormElementImpl implements WIFormLinksControlWILinkFilters {
    private WIFormLinksControlWILinkFilter[] filters;
    private WIFormLinksControlWILinkFilterEnum filterType;

    /**
     * Process the attributes of the "WorkItemLinkFilters" element.
     *
     * Attributes: - FilterType: required.
     */
    @Override
    void startLoading(final Attributes attributes) {
        filterType = WIFormLinksControlWILinkFilterEnumFactory.fromType(
            attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_FILTERTYPE));
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "WorkItemLinkFilters" element.
     *
     * Child elements: (sequence, minimum=0, maximum=unbounded) - Filter
     */
    @Override
    void endLoading() {
        filters = (WIFormLinksControlWILinkFilter[]) childrenToArray(new WIFormLinksControlWILinkFilter[] {});
    }

    /**
     * Corresponds to the "Filter" child elements of the "WorkItemLinkFilters"
     * element.
     */
    @Override
    public WIFormLinksControlWILinkFilter[] getFilters() {
        return filters;
    }

    /**
     * Corresponds to the "FilterType" attribute of the "WorkItemLinkFilters"
     * element.
     */
    @Override
    public WIFormLinksControlWILinkFilterEnum getFilterType() {
        return filterType;
    }

    /**
     * Returns true if the filters include a link with the specified reference
     * name and directions. It is possible for a link to be both forward and
     * reverse.
     *
     * @param linkReferenceName
     *        The link reference name.
     *
     * @param isForward
     *        Indicates the link is a forward link.
     *
     * @param isReverse
     *        Indicates the link is a reverse link.
     *
     * @return Returns true if the filters include the specified link in the
     *         specified direction.
     */
    @Override
    public boolean includes(final String linkReferenceName, final boolean isForward, final boolean isReverse) {
        if (filterType.equals(WIFormLinksControlWILinkFilterEnum.INCLUDEALL)) {
            return true;
        } else if (filterType.equals(WIFormLinksControlWILinkFilterEnum.EXCLUDEALL)) {
            return false;
        } else if (filterType.equals(WIFormLinksControlWILinkFilterEnum.EXCLUDE)) {
            // The filters are for EXCLUDED types. Test for a filter of this
            // link reference name.
            final WIFormLinksControlWILinkFilter filter = getFilterForReferenceName(linkReferenceName);

            if (filter == null) {
                // No filter entry exists, so this is link is not EXCLUDED.
                return true;
            } else {
                // See if the the filter is narrowed by the link direction.
                final WIFormLinksControlFilterOnEnum filterOn = filter.getLinksControlFilterOn();

                if (filterOn == null) {
                    // No directional filter was specified, which means EXCLUDE
                    // both directions.
                    return false;
                } else if (filterOn == WIFormLinksControlFilterOnEnum.FORWARDNAME) {
                    // The filter specifies only the forward links of this type
                    // should be EXCLUDED.
                    return !isForward;
                } else {
                    // The filter specifies only the reverse links of this type
                    // should be EXCLUDED.
                    return !isReverse;
                }
            }
        } else {
            // The filters are for INCLUDED types. Test for a filter of this
            // link reference name.
            final WIFormLinksControlWILinkFilter filter = getFilterForReferenceName(linkReferenceName);

            if (filter != null) {
                // See if the the filter is narrowed by the link direction.
                final WIFormLinksControlFilterOnEnum filterOn = filter.getLinksControlFilterOn();

                if (filterOn == null) {
                    // No directional filter was specified, which means INCLUDE
                    // both directions.
                    return true;
                } else if (filterOn == WIFormLinksControlFilterOnEnum.FORWARDNAME) {
                    // The filter specifies only the forward links of this type
                    // should be INCLUDED.
                    return isForward;
                } else {
                    // The filter specifies only the reverse links of this type
                    // should be INCLUDED.
                    return isReverse;
                }
            } else {
                // No filter entry exists, so this is link is not INCLUDED.
                return false;
            }
        }
    }

    /**
     * Look for a filter matching the specified link reference name.
     *
     * @param referenceName
     *        The link reference name to search for.
     *
     * @return The matching filter or null if no match was found.
     */
    private WIFormLinksControlWILinkFilter getFilterForReferenceName(final String referenceName) {
        for (int i = 0; i < filters.length; i++) {
            if (referenceName.equalsIgnoreCase(filters[i].getLinkType())) {
                return filters[i];
            }
        }
        return null;
    }
}
