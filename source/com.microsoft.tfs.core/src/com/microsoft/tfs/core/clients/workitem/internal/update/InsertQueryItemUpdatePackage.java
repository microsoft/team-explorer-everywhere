// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import java.util.Date;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy.QueryItemImpl;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.xml.DOMUtils;

public class InsertQueryItemUpdatePackage extends BaseUpdatePackage {
    private final QueryItem queryItem;

    public InsertQueryItemUpdatePackage(final QueryItem queryItem, final WITContext context) {
        super(context);
        this.queryItem = queryItem;

        populate(getRoot(), queryItem);
    }

    static final Element populate(final Element parent, final QueryItem queryItem) {
        final Element queryElement = DOMUtils.appendChild(parent, UpdateXMLConstants.ELEMENT_NAME_INSERT_QUERY_ITEM);

        /* Note: WIT web services only accept guid strings without dashes. */
        queryElement.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_ID,
            queryItem.getID().getGUIDString(GUIDStringFormat.NONE));

        queryElement.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_PARENT_ID,
            queryItem.getParent().getID().getGUIDString(GUIDStringFormat.NONE));

        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_NAME, queryItem.getName());

        if (queryItem.getOwnerDescriptor() != null) {
            DOMUtils.appendChildWithText(
                queryElement,
                UpdateXMLConstants.ELEMENT_NAME_OWNER_IDENTIFIER,
                queryItem.getOwnerDescriptor().getIdentifier());
            DOMUtils.appendChildWithText(
                queryElement,
                UpdateXMLConstants.ELEMENT_NAME_OWNER_TYPE,
                queryItem.getOwnerDescriptor().getIdentityType());
        }

        if (queryItem instanceof QueryDefinition) {
            DOMUtils.appendChildWithText(
                queryElement,
                UpdateXMLConstants.ELEMENT_NAME_QUERY_TEXT,
                ((QueryDefinition) queryItem).getQueryText());
        }

        return queryElement;
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
        final Element responseElement = (Element) response.getElements()[0]. // <UpdateResults>
        getElementsByTagName(UpdateXMLConstants.ELEMENT_NAME_INSERT_QUERY_ITEM).item(0); // <InsertQueryItem>

        final Date updateTime = parseDate(responseElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_UPDATE_TIME));

        if (queryItem instanceof QueryItemImpl) {
            ((QueryItemImpl) queryItem).updateAfterUpdate(updateTime);
        }
    }
}
