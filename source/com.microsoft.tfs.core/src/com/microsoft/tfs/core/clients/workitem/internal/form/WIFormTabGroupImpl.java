// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormPaddingAttribute;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTab;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTabGroup;

public class WIFormTabGroupImpl extends WIFormElementImpl implements WIFormTabGroup {
    private WIFormPaddingAttributeImpl padding;
    private WIFormPaddingAttributeImpl margin;
    private WIFormTab[] tabs;

    /**
     * Process the attributes from the "TabGroup" element.
     *
     * Attributes: - Padding: optional - Margin: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        padding = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_PADDING);
        margin = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_MARGIN);
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "TabGroup" element.
     *
     * Child elements: (sequence, minimum=1, maximum=unbounded) - Tab
     */
    @Override
    void endLoading() {
        tabs = (WIFormTab[]) childrenToArray(new WIFormTab[] {});
    }

    /**
     * Corresponds to the "Padding" attribute in XML use: optional
     */
    @Override
    public WIFormPaddingAttribute getPadding() {
        return padding;
    }

    /**
     * Corresponds to the "Margin" attribute in XML use: optional
     */
    @Override
    public WIFormPaddingAttribute getMargin() {
        return margin;
    }

    /**
     * Corresponds to "Tab" child elements in XML minOccurs: 1 maxOccurs:
     * unbounded
     */
    @Override
    public WIFormTab[] getTabChildren() {
        return tabs;
    }
}
