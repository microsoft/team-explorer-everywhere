// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilter;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilterEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilterScopeEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;

public class WIFormLinksControlWITypeFiltersImpl extends WIFormElementImpl implements WIFormLinksControlWITypeFilters {
    private WIFormLinksControlWITypeFilter[] filters;
    private WIFormLinksControlWITypeFilterScopeEnum scope;
    private WIFormLinksControlWITypeFilterEnum filterType;

    /**
     * Process the attributes of the "WorkItemTypeFilters" element.
     *
     * Attributes: - Scope: optional - FilterType: required
     */
    @Override
    void startLoading(final Attributes attributes) {
        scope = WIFormLinksControlWITypeFilterScopeEnumFactory.fromType(
            attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_SCOPE));
        filterType = WIFormLinksControlWITypeFilterEnumFactory.fromType(
            attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_FILTERTYPE));
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "WorkItemTypeFilters" element.
     *
     * Child elements: (sequence, minimum=0, maximum=unbounded) - Filter
     */
    @Override
    void endLoading() {
        filters = (WIFormLinksControlWITypeFilter[]) childrenToArray(new WIFormLinksControlWITypeFilter[] {});
    }

    /**
     * Corresponds to the "Filter" children of the "WorkItemTypeFilters"
     * element.
     */
    @Override
    public WIFormLinksControlWITypeFilter[] getFilters() {
        return filters;
    }

    /**
     * Corresponds to the "Filter" attribute of the "WorkItemTypeFilters"
     * element.
     */
    @Override
    public WIFormLinksControlWITypeFilterEnum getFilter() {
        return filterType;
    }

    /**
     * Corresponds to the "Scope" attribute of the "WorkItemTypeFilters"
     * element.
     */
    @Override
    public WIFormLinksControlWITypeFilterScopeEnum getScope() {
        return scope;
    }

    /**
     * Create a WIQL query which queries for a subset of the candidate work-item
     * IDs that meet the criteria contained in this filter.
     *
     * @param A
     *        array of work item IDs to filter.
     *
     * @param The
     *        current project name.
     *
     * @returns A string containing the WIQL query.
     */
    @Override
    public String createFilterWIQLQuery(final int[] candidateWorkItemIds, final String projectName) {
        // Build a WIQL query to filter the work items.
        final StringBuffer sb = new StringBuffer();
        sb.append("SELECT [System.Id] FROM WorkItems WHERE [System.Id] IN ("); //$NON-NLS-1$

        for (int i = 0; i < candidateWorkItemIds.length; i++) {
            if (i > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(candidateWorkItemIds[i]);
        }

        sb.append(") AND [System.WorkItemType] "); //$NON-NLS-1$
        if (filterType == WIFormLinksControlWITypeFilterEnum.EXCLUDE) {
            sb.append("NOT "); //$NON-NLS-1$
        }
        sb.append("IN ("); //$NON-NLS-1$

        for (int i = 0; i < filters.length; i++) {
            if (i > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append("'"); //$NON-NLS-1$
            sb.append(filters[i].getWorkItemType());
            sb.append("'"); //$NON-NLS-1$
        }

        sb.append(")"); //$NON-NLS-1$

        if (scope == WIFormLinksControlWITypeFilterScopeEnum.PROJECT) {
            sb.append(" AND [System.TeamProject] = '"); //$NON-NLS-1$
            sb.append(projectName);
            sb.append("'"); //$NON-NLS-1$
        }

        return sb.toString();
    }

    /**
     * Returns true if the filters include a type with the specified work item
     * type name.
     *
     * @param workItemTypeName
     *        The work item type name.
     *
     * @return Returns true if the filters include the specified work item type
     *         name.
     */
    @Override
    public boolean includes(final String workItemTypeName) {
        if (filterType.equals(WIFormLinksControlWITypeFilterEnum.INCLUDEALL)) {
            return true;
        } else if (filterType.equals(WIFormLinksControlWITypeFilterEnum.EXCLUDE)) {
            // The filters are for EXCLUDED types. Test for a filter of this
            // type name.
            final WIFormLinksControlWITypeFilter filter = getFilterForTypeName(workItemTypeName);

            if (filter == null) {
                // No filter entry exists, so this type is not EXCLUDED.
                return true;
            } else {
                // Filter entry exists, so this type is EXCLUDED.
                return false;
            }
        } else {
            // The filters are for INCLUDED types. Test for a filter of this
            // type name.
            final WIFormLinksControlWITypeFilter filter = getFilterForTypeName(workItemTypeName);

            if (filter != null) {
                // A filter entry exists, so this type is INCLUDED.
                return true;
            } else {
                // No filter entry exists, so this is link is not INCLUDED.
                return false;
            }
        }
    }

    /**
     * Look for a filter matching the specified work item type name.
     *
     * @param workItemTypeName
     *        The work item type name to search for.
     *
     * @return The matching filter or null if no match was found.
     */
    private WIFormLinksControlWITypeFilter getFilterForTypeName(final String workItemTypeName) {
        for (int i = 0; i < filters.length; i++) {
            final WIFormLinksControlWITypeFilter filter = filters[i];
            if (filter.getWorkItemType().equalsIgnoreCase(workItemTypeName)) {
                return filter;
            }
        }
        return null;
    }
}
