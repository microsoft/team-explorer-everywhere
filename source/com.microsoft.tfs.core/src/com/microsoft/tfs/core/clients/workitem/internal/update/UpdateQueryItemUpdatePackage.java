// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.xml.DOMUtils;

public class UpdateQueryItemUpdatePackage extends BaseUpdatePackage {
    public UpdateQueryItemUpdatePackage(final WITContext context, final QueryItem queryItem) {
        super(context);
        populate(getRoot(), queryItem);
    }

    static final Element populate(final Element parent, final QueryItem queryItem) {
        final Element queryElement = DOMUtils.appendChild(parent, UpdateXMLConstants.ELEMENT_NAME_UPDATE_QUERY_ITEM);

        /* Note: WIT web services only accept guid strings without dashes. */
        queryElement.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_ID,
            queryItem.getID().getGUIDString(GUIDStringFormat.NONE));

        if (queryItem.getParent() != queryItem.getOriginalParent()) {
            queryElement.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_PARENT_ID,
                queryItem.getParent().getID().getGUIDString(GUIDStringFormat.NONE));
        }

        if (!queryItem.getName().equals(queryItem.getOriginalName())) {
            DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_NAME, queryItem.getName());
        }

        final IdentityDescriptor owner = queryItem.getOwnerDescriptor();
        final IdentityDescriptor originalOwner = queryItem.getOriginalOwnerDescriptor();

        if ((owner != null && !owner.equals(originalOwner)) || (owner == null && originalOwner != null)) {
            final String ownerIdentifier = (owner == null) ? "" : owner.getIdentifier(); //$NON-NLS-1$
            final String ownerIdentityType = (owner == null) ? "" : owner.getIdentityType(); //$NON-NLS-1$

            DOMUtils.appendChildWithText(
                queryElement,
                UpdateXMLConstants.ELEMENT_NAME_OWNER_IDENTIFIER,
                ownerIdentifier);
            DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_OWNER_TYPE, ownerIdentityType);
        }

        if (queryItem instanceof QueryDefinition
            && !((QueryDefinition) queryItem).getQueryText().equals(
                ((QueryDefinition) queryItem).getOriginalQueryText())) {
            DOMUtils.appendChildWithText(
                queryElement,
                UpdateXMLConstants.ELEMENT_NAME_QUERY_TEXT,
                ((QueryDefinition) queryItem).getQueryText());
        }

        return queryElement;
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
    }
}
