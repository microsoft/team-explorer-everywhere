// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelText;
import com.microsoft.tfs.core.clients.workitem.form.WIFormText;

public class WIFormLabelTextImpl extends WIFormElementImpl implements WIFormLabelText {
    private WIFormText[] textElements;

    /**
     * Process the attributes of the "LabelText" element.
     *
     * Attributes: - None.
     */
    @Override
    void startLoading(final Attributes attributes) {
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "LabelText" element.
     *
     * Child elements: (sequence, minimum=1, maximum=unbounded) - Text
     */
    @Override
    void endLoading() {
        textElements = (WIFormText[]) childrenToArray(new WIFormText[] {});
    }

    /**
     * Corresponds to the "Text" child elements of the "LabelText" element.
     */
    @Override
    public WIFormText[] getTextElements() {
        return textElements;
    }
}
