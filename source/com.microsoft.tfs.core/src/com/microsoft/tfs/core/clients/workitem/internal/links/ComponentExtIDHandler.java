// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.links;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.update.ElementHandler;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateXMLConstants;

public class ComponentExtIDHandler implements ElementHandler {
    private final WITComponent component;
    private final String elementName;

    public ComponentExtIDHandler(final WITComponent component, final String elementName) {
        this.component = component;
        this.elementName = elementName;
    }

    @Override
    public void handle(final Element element) {
        final String sId = element.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_ID);
        final int id = Integer.parseInt(sId);
        component.setExtID(id);
    }

    @Override
    public String getElementName() {
        return elementName;
    }
}
