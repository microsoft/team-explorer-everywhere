// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.List;

import com.microsoft.tfs.core.clients.build.flags.InformationFields;
import com.microsoft.tfs.core.clients.build.flags.InformationTypes;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetail;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildInformationNode;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServer;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.CommonInformationHelper;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.InformationAddRequest;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.InformationChangeRequest;

public class InformationNodeConverters {
    /**
     * Gets a changesetId from the build
     *
     *
     * @param buildInformation
     * @return
     */
    public static int getChangesetID(final IBuildInformation buildInformation) {
        return getChangesetID(buildInformation, -1);
    }

    /**
     * Gets a changesetId from the build for a particular request
     *
     *
     * @param buildInformation
     * @param requestId
     * @return
     */
    public static int getChangesetID(final IBuildInformation buildInformation, final int requestId) {
        return getChangesetID(buildInformation, requestId, true);
    }

    /**
     * Gets a changesetId from the build for a particular request, if
     * returnFirstIfNotFound is specified, it returns the first changeset in the
     * list if the one specified is not present.
     *
     *
     * @param buildInformation
     * @param requestId
     * @param returnFirstIfNotFound
     * @return
     */
    public static int getChangesetID(
        final IBuildInformation buildInformation,
        final int requestId,
        final boolean returnFirstIfNotFound) {
        final IBuildInformationNode[] nodes = buildInformation.getNodesByType(InformationTypes.CHECK_IN_OUTCOME, true);
        if (nodes.length == 0) {
            return -1;
        } else {
            int changesetId = -1, requestId2;

            for (final IBuildInformationNode node : nodes) {
                // Get the request id out of the information node (if it's
                // there) If this node matches the request passed in, return the
                // changeset id
                requestId2 = CommonInformationHelper.getInt(node.getFields(), InformationFields.REQUEST_ID, -1);
                if (requestId2 != -1 && requestId2 == requestId) {
                    changesetId = CommonInformationHelper.getInt(node.getFields(), InformationFields.CHANGESET_ID, -1);
                    return changesetId;
                }
            }

            if (returnFirstIfNotFound) {
                // We didn't find the right changeset id, so try to return the
                // first one in the list
                changesetId = CommonInformationHelper.getInt(nodes[0].getFields(), InformationFields.CHANGESET_ID, -1);
            }

            return changesetId;
        }
    }

    public static void bulkUpdateInformationNodes(
        final BuildDetail build,
        final List<InformationChangeRequest> requests) {
        if (requests.size() > 0) {
            BuildInformationNode[] outNodes;
            final InformationChangeRequest[] requestsArray = new InformationChangeRequest[requests.size()];
            final BuildServer buildServer = (BuildServer) build.getBuildServer();

            if (buildServer.getBuildServerVersion().isV2()) {
                outNodes = buildServer.getBuild2008Helper().updateBuildInformation(requests.toArray(requestsArray));
            } else if (buildServer.getBuildServerVersion().isV3()) {
                outNodes = buildServer.getBuild2010Helper().updateBuildInformation(requests.toArray(requestsArray));
            } else {
                outNodes = buildServer.getBuildService().updateBuildInformation(requests.toArray(requestsArray));
            }

            for (int i = 0; i < requestsArray.length; i++) {
                if (requestsArray[i] instanceof InformationAddRequest) {
                    // Update the IDs of all added nodes.
                    ((InformationAddRequest) requests.get(i)).getNode().setID(outNodes[i].getNodeID());
                }
            }
        }
    }
}