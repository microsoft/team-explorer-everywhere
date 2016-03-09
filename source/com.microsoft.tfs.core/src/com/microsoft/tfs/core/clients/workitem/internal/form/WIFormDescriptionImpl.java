// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormDescription;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLayout;

public class WIFormDescriptionImpl extends WIFormElementImpl implements WIFormDescription {
    private WIFormLayout[] layouts;

    /**
     * Process the attributes of the "Form" element.
     *
     * Attributes: - None
     */
    @Override
    void startLoading(final Attributes attributes) {
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "Form" element.
     *
     * Child elements: (sequence, minimum=1, maximum=unbounded) - Layout
     */
    @Override
    void endLoading() {
        layouts = (WIFormLayout[]) childrenToArray(new WIFormLayout[] {});
    }

    /**
     * Corresponds to "Layout" child elements in XML minOccurs: 1 maxOccurs:
     * unbounded
     */
    @Override
    public WIFormLayout[] getLayoutChildren() {
        return layouts;
    }
}
