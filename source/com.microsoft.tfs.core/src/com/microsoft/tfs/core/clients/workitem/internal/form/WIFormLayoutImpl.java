// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLayout;
import com.microsoft.tfs.core.clients.workitem.form.WIFormPaddingAttribute;
import com.microsoft.tfs.core.clients.workitem.form.WIFormSizeAttribute;

public class WIFormLayoutImpl extends WIFormElementImpl implements WIFormLayout {
    private String target;
    private WIFormSizeAttributeImpl minimumSize;
    private WIFormPaddingAttributeImpl padding;
    private WIFormPaddingAttributeImpl margin;

    /**
     * Process the attributes of the "Layout" element.
     *
     * Attributes: - Target: optional - MinimumSize: optional - Padding:
     * optional - Margin: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        target = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_TARGET);
        minimumSize =
            WIFormParseHandler.readSizeAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_MINIMUM_SIZE);
        padding = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_PADDING);
        margin = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_MARGIN);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "Target" attribute in XML use: optional
     */
    @Override
    public String getTarget() {
        return target;
    }

    /**
     * Corresponds to the "MinimumSize" attribute in XML use: optional
     */
    @Override
    public WIFormSizeAttribute getMinimumSize() {
        return minimumSize;
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
