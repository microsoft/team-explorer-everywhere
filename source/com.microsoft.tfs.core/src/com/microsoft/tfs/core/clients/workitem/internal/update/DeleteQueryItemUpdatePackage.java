// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.xml.DOMUtils;

public class DeleteQueryItemUpdatePackage extends BaseUpdatePackage {
    public DeleteQueryItemUpdatePackage(final WITContext context, final QueryItem queryItem) {
        super(context);

        populate(getRoot(), queryItem);
    }

    static final Element populate(final Element parent, final QueryItem queryItem) {
        final Element queryElement = DOMUtils.appendChild(parent, UpdateXMLConstants.ELEMENT_NAME_DELETE_QUERY_ITEM);

        /* Note: WIT web services only accept guid strings without dashes. */
        queryElement.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_ID,
            queryItem.getID().getGUIDString(GUIDStringFormat.NONE));

        return queryElement;
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
    }
}
