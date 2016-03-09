// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.internal.link.ExternalLinkImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.HyperlinkImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.RelatedLinkImpl;
import com.microsoft.tfs.util.Check;

/**
 * @since TEE-SDK-10.1
 */
public class LinkFactory {
    private static int RELATED_LINKTYPES_ID_V2 = 0;
    private static String RELATED_LINKTYPES_REFNAME_V3 = "System.LinkTypes.Related-Forward"; //$NON-NLS-1$

    public static RelatedLink newRelatedLink(
        final WorkItem sourceWorkItem,
        final WorkItem targetWorkItem,
        final String comment,
        final boolean readOnly) {
        final WorkItemClient client = sourceWorkItem.getClient();
        int linkId = RELATED_LINKTYPES_ID_V2;

        if (client.supportsWorkItemLinkTypes()) {
            final WorkItemLinkTypeEndCollection ends = client.getLinkTypes().getLinkTypeEnds();
            final WorkItemLinkTypeEnd related = ends.get(RELATED_LINKTYPES_REFNAME_V3);
            Check.notNull(related, "related"); //$NON-NLS-1$
            linkId = related.getID();
        }

        return newRelatedLink(sourceWorkItem, targetWorkItem, linkId, comment, readOnly);
    }

    public static RelatedLink newRelatedLink(
        final WorkItem sourceWorkItem,
        final WorkItem targetWorkItem,
        final int linkTypeId,
        final String comment,
        final boolean readOnly) {
        final RelatedLinkImpl relatedLink = new RelatedLinkImpl(
            sourceWorkItem,
            targetWorkItem.getFields().getID(),
            linkTypeId,
            comment,
            true,
            readOnly);

        relatedLink.setDescription(WorkItemLinkUtils.buildDescriptionFromWorkItem(targetWorkItem));
        relatedLink.setWorkItem(targetWorkItem);
        return relatedLink;
    }

    public static Hyperlink newHyperlink(final String location, final String comment, final boolean readOnly) {
        return new HyperlinkImpl(location, comment, -1, true, readOnly);
    }

    public static ExternalLink newExternalLink(
        final RegisteredLinkType linkType,
        final String artifactUri,
        final String comment,
        final boolean readOnly) {
        ArtifactID.checkURIIsWellFormed(artifactUri);

        return new ExternalLinkImpl(linkType, artifactUri, comment, -1, true, readOnly);
    }

    public static ExternalLink newExternalLink(
        final RegisteredLinkType linkType,
        final ArtifactID artifactId,
        final String comment,
        final boolean readOnly) {
        return new ExternalLinkImpl(linkType, artifactId.encodeURI(), comment, -1, true, readOnly);
    }
}
