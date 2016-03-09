// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormPaddingAttribute;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTab;

public class WIFormTabImpl extends WIFormElementImpl implements WIFormTab {
    private String label;
    private WIFormPaddingAttributeImpl padding;
    private WIFormPaddingAttributeImpl margin;

    /**
     * Process the attributes from the "Tab" element.
     *
     * Attributes: - Label: required - Padding: optional - Margin: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        label = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_LABEL);
        padding = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_PADDING);
        margin = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_MARGIN);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "Label" attribute in XML use: required
     */
    @Override
    public String getLabel() {
        return label;
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
}
