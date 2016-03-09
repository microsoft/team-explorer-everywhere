// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.text.MessageFormat;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateXMLConstants;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.util.xml.DOMUtils;

public class ExternalLinkImpl extends LinkImpl implements ExternalLink {
    private final String uri;

    public ExternalLinkImpl(
        final RegisteredLinkType linkType,
        final String uri,
        final String comment,
        final int extId,
        final boolean newComponent,
        final boolean readOnly) {
        super(linkType, comment, extId, newComponent, readOnly);

        if (RegisteredLinkTypeNames.WORKITEM.equals(linkType.getName())
            || RegisteredLinkTypeNames.HYPERLINK.equals(linkType.getName())) {
            throw new IllegalArgumentException(
                MessageFormat.format("the link type [{0}] is not valid for ExternalLinks", linkType.getName())); //$NON-NLS-1$
        }

        if (uri == null || uri.trim().length() == 0) {
            throw new IllegalArgumentException("uri must not be null or empty"); //$NON-NLS-1$
        }

        this.uri = uri.trim();
    }

    @Override
    public LinkImpl cloneLink() {
        return new ExternalLinkImpl(getLinkType(), uri, getComment(), -1, true, isReadOnly());
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public boolean isEquivalent(final Link link) {
        if (link instanceof ExternalLinkImpl) {
            final ExternalLinkImpl other = (ExternalLinkImpl) link;
            return uri.equalsIgnoreCase(other.uri);
        }
        return false;
    }

    @Override
    public ArtifactID getArtifactID() {
        return new ArtifactID(uri);
    }

    @Override
    protected String getFallbackDescription() {
        final ArtifactID id = getArtifactID();
        final StringBuilder description = new StringBuilder(id.getArtifactType());
        description.append(" "); //$NON-NLS-1$
        if (id.getArtifactType().equalsIgnoreCase("commit")) //$NON-NLS-1$
        {
            final String toolSpecificId = id.getToolSpecificID();
            final String commitId = toolSpecificId.substring(toolSpecificId.lastIndexOf('/') + 1);
            description.append(commitId);
        } else {
            description.append(id.getToolSpecificID());
        }
        return description.toString();
    }

    @Override
    protected void createXMLForAdd(final Element parentElement) {
        final Element element =
            DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_INSERT_RESOURCE_LINK);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FIELD_NAME, CoreFieldReferenceNames.BIS_LINKS);
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_LINK_TYPE, getLinkType().getName());
        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_LOCATION, uri);
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
