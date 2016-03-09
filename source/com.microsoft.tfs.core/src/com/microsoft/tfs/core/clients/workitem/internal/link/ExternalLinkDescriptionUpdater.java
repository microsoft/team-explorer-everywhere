// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.linking.LinkingClient;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;

import ms.tfs.services.linking._03._Artifact;

public class ExternalLinkDescriptionUpdater implements LinkDescriptionUpdater {
    private static final Log log = LogFactory.getLog(ExternalLinkDescriptionUpdater.class);

    private final DescriptionUpdateErrorCallback errorCallback;
    private final WITContext witContext;
    private final Map urisToLinks = new HashMap();

    public ExternalLinkDescriptionUpdater(
        final DescriptionUpdateErrorCallback errorCallback,
        final WITContext witContext) {
        this.errorCallback = errorCallback;
        this.witContext = witContext;
    }

    @Override
    public void addLinkToBeUpdated(final LinkImpl link) {
        final String uri = ((ExternalLink) link).getURI();
        urisToLinks.put(uri, link);
    }

    @Override
    public void run() {
        if (urisToLinks.size() == 0) {
            /*
             * nothing to do
             */
            return;
        }

        try {
            final LinkingClient linkingClient =
                (LinkingClient) witContext.getConnection().getClient(LinkingClient.class);

            final String[] artifactUriList = (String[]) urisToLinks.keySet().toArray(new String[] {});

            final _Artifact[] artifacts = linkingClient.getArtifacts(artifactUriList);

            for (int i = 0; i < artifacts.length; i++) {
                if (artifacts[i] != null) {
                    final LinkImpl link = (LinkImpl) urisToLinks.get(artifacts[i].getUri());
                    if (link != null) {
                        link.setDescription(artifacts[i].getArtifactTitle());
                    }
                }
            }
        } catch (final Throwable t) {
            if (errorCallback != null) {
                errorCallback.onDescriptionUpdateError(t);
            } else {
                log.error(t);
            }
        }
    }
}
