// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

public class PSQuery {
    private final Element root;
    private Element currentElement;

    public PSQuery(final WIQLContext context) {
        final Document document = DOMCreateUtils.newDocument("Query"); //$NON-NLS-1$

        root = document.getDocumentElement();
        root.setAttribute("Product", context.getProduct()); //$NON-NLS-1$

        currentElement = root;
    }

    public void startElement(final String elementName) {
        currentElement = DOMUtils.appendChild(currentElement, elementName);
    }

    public void endElement() {
        currentElement = (Element) currentElement.getParentNode();
    }

    public void setElementAttribute(final String name, final String value) {
        currentElement.setAttribute(name, value);
    }

    public void setElementValue(final String value) {
        DOMUtils.appendText(currentElement, value);
    }

    public DOMAnyContentType getQuery() {
        return new DOMAnyContentType(new Element[] {
            root
        });
    }
}
