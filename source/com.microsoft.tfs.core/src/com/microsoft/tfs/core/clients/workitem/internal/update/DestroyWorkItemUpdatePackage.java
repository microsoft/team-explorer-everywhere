// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.xml.DOMUtils;

public class DestroyWorkItemUpdatePackage extends BaseUpdatePackage {
    public DestroyWorkItemUpdatePackage(final int workItemId, final WITContext context) {
        super(context);

        final Element queryElement = DOMUtils.appendChild(getRoot(), UpdateXMLConstants.ELEMENT_NAME_DESTROY_WORK_ITEM);
        queryElement.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_WORK_ITEM_ID, String.valueOf(workItemId));
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
    }
}
