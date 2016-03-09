// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinkColumns;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;

public class WIFormLinksControlOptionsImpl extends WIFormElementImpl implements WIFormLinksControlOptions {
    private WIFormLinkColumns linkColumns;
    private WIFormLinksControlExternalLinkFilters externalLinkFilters;
    private WIFormLinksControlWILinkFilters workItemLinkFilters;
    private WIFormLinksControlWITypeFilters workItemTypeFilters;

    /**
     * Process the attributes of the "LinksControlOptions" element.
     *
     * Attributes: - None
     */
    @Override
    void startLoading(final Attributes attributes) {
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "LinksControlOptions" element.
     *
     * Child elements: (all -- any order) - LinkColumns: (minimum=0, maximum=1)
     * - WorkItemLinkFilters: (minimum=0, maximum=1) - ExternalLinkFilters:
     * (minimum=0, maximum=1) - WorkItemTypeFilters: (minimum=0, maximum=1)
     */
    @Override
    void endLoading() {
        final WIFormElement[] children = getChildElements();

        for (int i = 0; i < children.length; i++) {
            final WIFormElement child = children[i];
            if (child instanceof WIFormLinksControlWITypeFilters) {
                workItemTypeFilters = (WIFormLinksControlWITypeFilters) child;
            } else if (child instanceof WIFormLinksControlWILinkFilters) {
                workItemLinkFilters = (WIFormLinksControlWILinkFilters) child;
            } else if (child instanceof WIFormLinksControlExternalLinkFilters) {
                externalLinkFilters = (WIFormLinksControlExternalLinkFilters) child;
            } else if (child instanceof WIFormLinkColumns) {
                linkColumns = (WIFormLinkColumns) child;
            }
        }
    }

    /**
     * Corresponds to the "ExternalLinkFilters" child of the
     * "LinksControlOptions" element.
     */
    @Override
    public WIFormLinksControlExternalLinkFilters getExternalLinkFilters() {
        return externalLinkFilters;
    }

    /**
     * Corresponds to the "LinkColumns" child of the "LinksControlOptions"
     * element.
     */
    @Override
    public WIFormLinkColumns getLinkColumns() {
        return linkColumns;
    }

    /**
     * Corresponds to the "WorkItemLinkFilters" child of the
     * "LinksControlOptions" element.
     */
    @Override
    public WIFormLinksControlWILinkFilters getWorkItemLinkFilters() {
        return workItemLinkFilters;
    }

    /**
     * Corresponds to the "WorkItemTypeFilters" child fo the
     * "LinksControlOptions" element.
     */
    @Override
    public WIFormLinksControlWITypeFilters getWorkItemTypeFilters() {
        return workItemTypeFilters;
    }
}
