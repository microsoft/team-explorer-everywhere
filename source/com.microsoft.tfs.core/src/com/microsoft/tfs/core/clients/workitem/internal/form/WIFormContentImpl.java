// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormContent;

public class WIFormContentImpl extends WIFormElementImpl implements WIFormContent {
    private String content;

    /**
     * Process the attributes of the "Content" element.
     *
     * Attributes: - None
     */
    @Override
    void startLoading(final Attributes attributes) {
        setAttributes(attributes);
    }

    /**
     * Called when the inner-text of a "Content" element is parsed.
     *
     * @param value
     *        The inner-text of a "Content" element.
     */
    void setContent(final String value) {
        content = value;
    }

    /**
     * Get the inner-text content of the "Content" element.
     */
    @Override
    public String getContent() {
        return content;
    }
}
