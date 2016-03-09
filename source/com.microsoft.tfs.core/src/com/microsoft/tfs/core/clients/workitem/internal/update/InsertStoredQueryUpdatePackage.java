// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import java.util.Date;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.xml.DOMUtils;

public class InsertStoredQueryUpdatePackage extends BaseUpdatePackage {
    private final StoredQueryImpl query;

    public InsertStoredQueryUpdatePackage(final StoredQueryImpl query, final WITContext context) {
        super(context);
        this.query = query;

        final Element queryElement = DOMUtils.appendChild(getRoot(), UpdateXMLConstants.ELEMENT_NAME_INSERT_QUERY);
        DOMUtils.appendChildWithText(
            queryElement,
            UpdateXMLConstants.ELEMENT_NAME_PROJECT_ID,
            String.valueOf(query.getProjectID()));
        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_NAME, query.getName());
        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_QUERY_TEXT, query.getQueryText());
        DOMUtils.appendChildWithText(queryElement, UpdateXMLConstants.ELEMENT_NAME_DESCRIPTION, query.getDescription());
        DOMUtils.appendChildWithText(
            queryElement,
            UpdateXMLConstants.ELEMENT_NAME_IS_PUBLIC,
            (query.getQueryScope() == QueryScope.PUBLIC ? "1" : "0")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
        final Element responseElement =
            (Element) response.getElements()[0].getElementsByTagName(UpdateXMLConstants.ELEMENT_NAME_INSERT_QUERY).item(
                0);

        final String guid = responseElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_ID);
        final Date updateTime = parseDate(responseElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_UPDATE_TIME));

        query.updateAfterSave(new GUID(guid), updateTime);
    }
}
