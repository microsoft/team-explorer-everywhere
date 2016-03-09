// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormLinkColumn;

public class WIFormLinkColumnImpl extends WIFormElementImpl implements WIFormLinkColumn {
    private String linkAttribute;
    private String refName;

    /**
     * Process attributes of the "LinkColumn" element.
     *
     * Attributes: - LinkAttribute: optional - RefName: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        linkAttribute =
            WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_LINKATTRIBUTE);
        refName = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_REFNAME);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "LinkAttribute" attribute of the "LinkColumn" element.
     */
    @Override
    public String getLinkAttribute() {
        return linkAttribute;
    }

    /**
     * Corresponds to the "RefName" attribute of the "LinkColumn" element.
     */
    @Override
    public String getRefName() {
        return refName;
    }
}
