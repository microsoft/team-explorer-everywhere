// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.xml.DOMUtils;

public class DeleteStoredQueryUpdatePackage extends BaseUpdatePackage {
    public DeleteStoredQueryUpdatePackage(final StoredQueryImpl query, final WITContext context) {
        super(context);

        final Element queryElement = DOMUtils.appendChild(getRoot(), UpdateXMLConstants.ELEMENT_NAME_DELETE_QUERY);
        queryElement.setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_QUERY_ID,
            query.getQueryGUID().getGUIDString(GUIDStringFormat.NONE));
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
    }
}
