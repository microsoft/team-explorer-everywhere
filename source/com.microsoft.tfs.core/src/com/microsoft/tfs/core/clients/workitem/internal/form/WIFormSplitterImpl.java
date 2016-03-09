// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormDockEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormSplitter;

public class WIFormSplitterImpl extends WIFormElementImpl implements WIFormSplitter {
    private WIFormDockEnum dock;

    /**
     * Process the attributes from the "Splitter" element.
     *
     * Attributes: - Dock: required
     */
    @Override
    void startLoading(final Attributes attributes) {
        dock = WIFormDockEnumFactory.fromType(attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_DOCK));
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "Dock" attribute in XML use: required
     */
    @Override
    public WIFormDockEnum getDock() {
        return dock;
    }

}
