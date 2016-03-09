// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinkColumn;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinkColumns;

public class WIFormLinkColumnsImpl extends WIFormElementImpl implements WIFormLinkColumns {
    private WIFormLinkColumn[] linkColumns;

    /**
     * Process the attributes of the "LinkColumns" element.
     *
     * Attributes: - None
     */
    @Override
    void startLoading(final Attributes attributes) {
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "LinkColumns" element.
     *
     * Child elements: (sequence, minimum=1, maximum=unbounded) - LinkColumn
     */
    @Override
    void endLoading() {
        linkColumns = (WIFormLinkColumn[]) childrenToArray(new WIFormLinkColumn[] {});
    }

    /**
     * Corresponds to the "Columns" child elements of the "LinkColumns" element.
     */
    @Override
    public WIFormLinkColumn[] getLinkColumns() {
        return linkColumns;
    }
}
