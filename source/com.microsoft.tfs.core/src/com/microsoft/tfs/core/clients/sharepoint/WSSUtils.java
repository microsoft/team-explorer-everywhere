// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectPortalEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * Utility class to help when talking to Windows Sharepoint Services.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public final class WSSUtils {
    private static final Log log = LogFactory.getLog(WSSUtils.class);

    private WSSUtils() {
    }

    public static boolean isWSSConfigured(final TFSTeamProjectCollection connection, final ProjectInfo projectInfo) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        final ProjectCollectionEntity projectCollectionEntity = connection.getTeamProjectCollectionEntity(false);

        if (projectCollectionEntity == null) {
            log.warn("Could not load team project collection catalog entity"); //$NON-NLS-1$
            return false;
        }

        final TeamProjectEntity teamProject = projectCollectionEntity.getTeamProject(new GUID(projectInfo.getGUID()));

        if (teamProject == null) {
            log.warn(MessageFormat.format(
                "Could not team project catalog entity for team project {0}", //$NON-NLS-1$
                projectInfo.getName()));
            return false;
        }

        final ProjectPortalEntity projectPortal = teamProject.getProjectPortal();

        if (projectPortal != null && projectPortal.getResourceSubType().equals("WssSite")) //$NON-NLS-1$
        {
            return true;
        }

        return false;
    }

    /**
     * The strings that sharepoint returns have an order followed by the actual
     * String I.e.:-
     *
     * 1;#Supporting Files 3;#1
     *
     * This method will return the bit after the #.
     */
    public static String decodeWSSString(final String valueToDecode) {
        return valueToDecode.substring(valueToDecode.lastIndexOf('#') + 1);
    }

    public static String getWSSURL(final TFSTeamProjectCollection connection, final ProjectInfo projectInfo) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        final TeamProjectEntity teamProject =
            connection.getTeamProjectCollectionEntity(false).getTeamProject(new GUID(projectInfo.getGUID()));

        if (teamProject == null) {
            log.warn(MessageFormat.format(
                "Could not load project collection catalog entity for team project {0}", //$NON-NLS-1$
                projectInfo.getName()));
            return null;
        }

        final ProjectPortalEntity projectPortal = teamProject.getProjectPortal();

        if (projectPortal != null) {
            return projectPortal.getFullItemPath();
        }

        return null;
    }

    public static URI getViewURI(final Hierarchical wssNode) throws URISyntaxException {
        String path = null;

        if (wssNode instanceof WSSDocumentLibrary) {
            path = ((WSSDocumentLibrary) wssNode).getDefaultViewURL();
        }
        if (wssNode instanceof WSSNode) {
            path = ((WSSNode) wssNode).getFullPath();
        }

        if (path == null) {
            return new URI(null, null, null, null);
        }

        // All paths are fully qualified, but getFullPath usually doesn't have a
        // leading slash.
        if (path.length() > 0 && path.charAt(0) != '/') {
            path = "/" + path; //$NON-NLS-1$
        }

        return new URI(null, null, path, null);
    }
}
