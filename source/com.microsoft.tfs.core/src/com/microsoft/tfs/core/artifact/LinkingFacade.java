// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.artifact;

import java.net.URI;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.linking.LinkingClient;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;

/**
 * Static methods for getting linking information from a {@link WorkItem} and
 * {@link TFSTeamProjectCollection}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class LinkingFacade {
    public static String getExternalURL(final WorkItem workItem, final TFSTeamProjectCollection connection) {
        try {
            final TSWAHyperlinkBuilder builder = new TSWAHyperlinkBuilder(workItem.getClient().getConnection());
            final URI uri = builder.getWorkItemEditorURL(workItem.getID());
            if (uri != null) {
                return uri.toString();
            }
        } catch (final Exception e) {
            // Ignore exception raised by doing it the new way - fall back
            // to
            // old mechanism
        }

        final LinkingClient linkingClient = (LinkingClient) connection.getClient(LinkingClient.class);
        return linkingClient.getArtifactURLExternal(workItem.getURI());
    }
}
