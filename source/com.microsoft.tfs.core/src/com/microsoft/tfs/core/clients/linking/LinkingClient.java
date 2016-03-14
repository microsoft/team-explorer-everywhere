// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.linking;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.MalformedURIException;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.linking.exceptions.LinkingException;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.clients.registration.RegistrationExtendedAttribute;
import com.microsoft.tfs.core.clients.registration.ServiceInterface;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.util.StringUtil;

import ms.tfs.services.linking._03._Artifact;
import ms.tfs.services.linking._03._IntegrationServiceSoap;

/**
 * @since TEE-SDK-10.1
 */
public class LinkingClient {
    private final TFSTeamProjectCollection connection;

    private static final String EXTERNAL_URL_HARDCODED_PART = ".aspx?artifactMoniker="; //$NON-NLS-1$

    /*
     * Map from String (tool id) -> LinkingSoap
     */
    private Map<String, _IntegrationServiceSoap> artifactProviders;

    public LinkingClient(final TFSTeamProjectCollection connection) {
        this.connection = connection;
    }

    private _IntegrationServiceSoap getLinkingSoap(final String toolId) {
        synchronized (this) {
            if (artifactProviders == null) {
                artifactProviders = new HashMap<String, _IntegrationServiceSoap>();
                final RegistrationEntry[] allRegistrationEntries = getRegistrationClient().getRegistrationEntries();
                for (int i = 0; i < allRegistrationEntries.length; i++) {
                    String urlString = null;
                    final ServiceInterface[] serviceInterfaces = allRegistrationEntries[i].getServiceInterfaces();
                    for (int j = 0; j < serviceInterfaces.length; j++) {
                        if (ServiceInterfaceNames.LINKING.equalsIgnoreCase(serviceInterfaces[j].getName())) {
                            urlString = serviceInterfaces[j].getRelativeURL();
                            break;
                        }
                    }
                    if (!StringUtil.isNullOrEmpty(urlString)) {
                        final _IntegrationServiceSoap linkingService = connection.getLinkingWebService(urlString);
                        artifactProviders.put(allRegistrationEntries[i].getType(), linkingService);
                    }
                }
            }
        }

        return artifactProviders.get(toolId);
    }

    public _Artifact[] getArtifacts(final String[] artifactUriList) throws MalformedURIException {
        if (artifactUriList == null) {
            return null;
        }

        for (int i = 0; i < artifactUriList.length; i++) {
            ArtifactID.checkURIIsWellFormed(artifactUriList[i]);
        }

        return getArtifactsProcess(artifactUriList);
    }

    private _Artifact[] getArtifactsProcess(final String[] artifactUriList) {
        final Map<String, List<ArtifactID>> toolIdToArtifactIds = new HashMap<String, List<ArtifactID>>();

        for (int i = 0; i < artifactUriList.length; i++) {
            final ArtifactID artifactId = new ArtifactID(artifactUriList[i]);

            List<ArtifactID> artifactIds = toolIdToArtifactIds.get(artifactId.getTool());
            if (artifactIds == null) {
                artifactIds = new ArrayList<ArtifactID>();
                toolIdToArtifactIds.put(artifactId.getTool(), artifactIds);
            }
            artifactIds.add(artifactId);
        }

        final Map<String, _Artifact> artifactUriToArtifact = new HashMap<String, _Artifact>();

        for (final String toolId : toolIdToArtifactIds.keySet()) {
            final List<ArtifactID> artifactIdList = toolIdToArtifactIds.get(toolId);

            final ArtifactID[] artifactIds = artifactIdList.toArray(new ArtifactID[artifactIdList.size()]);

            final _Artifact[] artifacts = getArtifactsProcess(toolId, artifactIds);

            if (artifacts == null || artifacts.length != artifactIds.length) {
                throw new LinkingException();
            }

            for (int i = 0; i < artifactIds.length; i++) {
                artifactUriToArtifact.put(artifactIds[i].encodeURI(), artifacts[i]);
            }
        }

        final _Artifact[] returnValue = new _Artifact[artifactUriList.length];
        for (int i = 0; i < artifactUriList.length; i++) {
            returnValue[i] = artifactUriToArtifact.get(artifactUriList[i]);
        }

        return returnValue;
    }

    private _Artifact[] getArtifactsProcess(final String tool, final ArtifactID[] artifactIds) {
        _Artifact[] artifacts = null;

        /*
         * The requirements tool (new in Dev11) does not implement a server side
         * linking service. Construct user friendly display names here on the
         * client.
         */
        if (tool.equalsIgnoreCase(ToolNames.REQUIREMENTS)) {
            artifacts = new _Artifact[artifactIds.length];
            for (int i = 0; i < artifactIds.length; i++) {
                final ArtifactID artifactId = artifactIds[i];
                artifacts[i] = new _Artifact();
                artifacts[i].setUri(artifactId.encodeURI());

                final String format = Messages.getString("LinkingClient.StoryboardLinkDescriptionFormat"); //$NON-NLS-1$
                artifacts[i].setArtifactTitle(MessageFormat.format(format, artifactId.getToolSpecificID()));
            }
            return artifacts;
        }

        /*
         * Ask the server for the artifacts.
         */
        final _IntegrationServiceSoap linkingService = getLinkingSoap(tool);
        if (linkingService != null) {
            final String[] uris = new String[artifactIds.length];
            for (int i = 0; i < artifactIds.length; i++) {
                uris[i] = artifactIds[i].encodeURI();
            }
            artifacts = linkingService.getArtifacts(uris);
        }
        return artifacts;
    }

    public String getArtifactURLExternal(final ArtifactID id) {
        String displaySegment = null;

        /*
         * attempt to locate a display segment for the artifact using the
         * registration service
         */
        final RegistrationEntry entry = getRegistrationClient().getRegistrationEntry(id.getTool());
        final RegistrationExtendedAttribute[] attributes = entry.getRegistrationExtendedAttributes();
        if (attributes != null) {
            for (int j = 0; j < attributes.length && StringUtil.isNullOrEmpty(displaySegment); j++) {
                if (attributes[j].getName().equals("ArtifactDisplayUrl")) //$NON-NLS-1$
                {
                    displaySegment = attributes[j].getValue();
                }
            }
        }

        if (displaySegment != null) {
            displaySegment = displaySegment.trim();
        }

        final StringBuffer url = new StringBuffer();
        url.append(connection.getURL());

        if (!StringUtil.isNullOrEmpty(displaySegment) && displaySegment.startsWith(ArtifactID.URI_SEPARATOR)) {
            url.append(displaySegment);
        }

        if (!url.toString().endsWith(ArtifactID.URI_SEPARATOR)) {
            url.append(ArtifactID.URI_SEPARATOR);
        }

        url.append(id.getTool());
        url.append(ArtifactID.URI_SEPARATOR);
        url.append(id.getArtifactType());
        url.append(EXTERNAL_URL_HARDCODED_PART);
        url.append(id.getToolSpecificID());

        return url.toString();
    }

    public String getArtifactURLExternal(final String uri) {
        return getArtifactURLExternal(new ArtifactID(uri));
    }

    private com.microsoft.tfs.core.clients.registration.RegistrationClient getRegistrationClient() {
        return connection.getRegistrationClient();
    }
}
