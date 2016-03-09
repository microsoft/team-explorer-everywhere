// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormColumn;

public class WIFormColumnImpl extends WIFormElementImpl implements WIFormColumn {
    private Integer percentWidth;
    private Integer fixedWidth;

    /**
     * Process the attributes of the "Column" element.
     *
     * Attributes: - PercentWidth: optional - FixedWidth: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        percentWidth =
            WIFormParseHandler.readIntegerValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_PERCENT_WIDTH);
        fixedWidth = WIFormParseHandler.readIntegerValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_FIXED_WIDTH);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "PercentWidth" attribute in XML use: optional
     */
    @Override
    public Integer getPercentWidth() {
        return percentWidth;
    }

    /**
     * Corresponds to the "FixedWidth" attribute in XML use: optional
     */
    @Override
    public Integer getFixedWidth() {
        return fixedWidth;
    }
}
