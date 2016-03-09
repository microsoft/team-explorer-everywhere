// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLink;
import com.microsoft.tfs.core.clients.workitem.form.WIFormText;

public class WIFormTextImpl extends WIFormElementImpl implements WIFormText {
    private String innerText;
    private WIFormLink link;

    /**
     * Process the attributes from the "Text" element.
     *
     * Attributes: - None
     */
    @Override
    void startLoading(final Attributes attributes) {
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "Text" element.
     *
     * Child elements: (sequence, minimum=0, maximum=1) - LINK element
     */
    @Override
    void endLoading() {
        final WIFormElement[] childElements = getChildElements();
        if (childElements.length == 1 && childElements[0] instanceof WIFormLink) {
            link = (WIFormLink) childElements[0];
        }
    }

    /**
     * The inner-text of the "Text" element may parsed as separate fragments and
     * should be concatenated to form a single string of content. This method is
     * called while the XML element is being parsed.
     *
     * @param text
     *        - a fragment of inner-text content.
     */
    public void appendInnerText(final String text) {
        if (innerText == null) {
            innerText = text;
        } else {
            innerText = innerText + text;
        }
    }

    /**
     * Corresponds to the inner-text content of the "Text" element.
     */
    @Override
    public String getInnerText() {
        return innerText;
    }

    /**
     * Corresponds to the "Link" child of the "Text" element.
     */
    @Override
    public WIFormLink getLink() {
        return link;
    }
}
