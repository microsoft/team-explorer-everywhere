// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.update.ElementHandler;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateXMLConstants;

public class ExtIDElementHandler implements ElementHandler {
    private final LinkImpl link;

    public ExtIDElementHandler(final LinkImpl link) {
        this.link = link;
    }

    @Override
    public void handle(final Element element) {
        final String sId = element.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_ID);
        final int id = Integer.parseInt(sId);
        link.setExtID(id);
    }

    @Override
    public String getElementName() {
        return UpdateXMLConstants.ELEMENT_NAME_INSERT_RESOURCE_LINK;
    }
}
