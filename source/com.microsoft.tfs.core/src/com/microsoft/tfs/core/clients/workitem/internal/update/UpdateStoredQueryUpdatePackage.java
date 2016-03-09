// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import java.util.Date;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.xml.DOMUtils;

public class UpdateStoredQueryUpdatePackage extends BaseUpdatePackage {
    private final StoredQueryImpl query;

    public UpdateStoredQueryUpdatePackage(final StoredQueryImpl query, final WITContext context) {
        super(context);
        this.query = query;

        final Element queryElement = DOMUtils.appendChild(getRoot(), UpdateXMLConstants.ELEMENT_NAME_UPDATE_QUERY);
        queryElement.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_ID,
            query.getQueryGUID().getGUIDString(GUIDStringFormat.NONE));

        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_NAME, query.getName());
        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_QUERY_TEXT, query.getQueryText());
        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_DESCRIPTION, query.getDescription());
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
        final Element responseElement = (Element) response.getElements()[0]. // <UpdateResults>
        getElementsByTagName(UpdateXMLConstants.ELEMENT_NAME_UPDATE_QUERY).item(0); // <UpdateQuery>

        final Date updateTime = parseDate(responseElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_UPDATE_TIME));

        query.updateAfterUpdate(updateTime);
    }
}
