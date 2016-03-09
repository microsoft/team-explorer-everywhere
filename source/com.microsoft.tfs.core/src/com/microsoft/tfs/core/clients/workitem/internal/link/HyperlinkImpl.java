// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateXMLConstants;
import com.microsoft.tfs.core.clients.workitem.link.Hyperlink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.util.xml.DOMUtils;

public class HyperlinkImpl extends LinkImpl implements Hyperlink {
    private final String location;

    public HyperlinkImpl(
        final String location,
        final String comment,
        final int extId,
        final boolean newComponent,
        final boolean readOnly) {
        super(new RegisteredLinkTypeImpl(RegisteredLinkTypeNames.HYPERLINK), comment, extId, newComponent, readOnly);

        if (location == null || location.trim().length() == 0) {
            throw new IllegalArgumentException("location must not be null or empty"); //$NON-NLS-1$
        }
        validateTextMaxLength(location.trim(), "location", LinkTextMaxLengths.HYPERLINK_LOCATION_MAX_LENGTH); //$NON-NLS-1$
        this.location = location.trim();

        setDescription(location);
    }

    @Override
    public LinkImpl cloneLink() {
        return new HyperlinkImpl(location, getComment(), -1, true, isReadOnly());
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public boolean isEquivalent(final Link link) {
        if (link instanceof HyperlinkImpl) {
            final HyperlinkImpl other = (HyperlinkImpl) link;
            return location.equalsIgnoreCase(other.location);
        }
        return false;
    }

    @Override
    protected void createXMLForAdd(final Element parentElement) {
        final Element element =
            DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_INSERT_RESOURCE_LINK);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FIELD_NAME, CoreFieldReferenceNames.LINKED_FILES);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_LOCATION, location);
        if (getComment() != null && getComment().trim().length() > 0) {
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_COMMENT, getComment().trim());
        }
    }

    @Override
    protected void createXMLForRemove(final Element parentElement) {
        final Element element =
            DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_REMOVE_RESOURCE_LINK);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_LINK_ID, String.valueOf(getExtID()));
    }

    @Override
    protected String getInsertTagName() {
        return UpdateXMLConstants.ELEMENT_NAME_INSERT_RESOURCE_LINK;
    }
}
