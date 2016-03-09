// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import org.w3c.dom.Element;

/**
 * A document in a Sharepoint installation.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class WSSDocument extends WSSNode {
    private String iconType;

    public WSSDocument() {
        super();
    }

    public WSSDocument(final Element element) {
        super(element);
        iconType = WSSUtils.decodeWSSString(element.getAttribute("ows_DocIcon")); //$NON-NLS-1$
    }

    /**
     * @return Returns the iconType.
     */
    public String getIconType() {
        return iconType;
    }

    /**
     * @param iconType
     *        The iconType to set.
     */
    public void setIconType(final String iconType) {
        this.iconType = iconType;
    }
}
