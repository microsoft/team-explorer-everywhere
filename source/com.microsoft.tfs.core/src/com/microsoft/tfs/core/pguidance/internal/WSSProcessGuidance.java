// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pguidance.internal;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProcessGuidanceEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.pguidance.IProcessGuidance;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceURLInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class WSSProcessGuidance implements IProcessGuidance {
    private static final String RESOLVE_PROCESS_GUIDANCE_URI_PROPERTY = "teamexplorer.resolveProcessGuidance"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(WSSProcessGuidance.class);

    private final TFSTeamProjectCollection connection;
    private final boolean doResolve;

    public WSSProcessGuidance(final TFSTeamProjectCollection connection) {
        this.connection = connection;

        doResolve = (!"false".equals(System.getProperty(RESOLVE_PROCESS_GUIDANCE_URI_PROPERTY))); //$NON-NLS-1$
    }

    /**
     * Queries the catalog service to determine if process guidance is enabled
     * for this team project
     *
     */
    @Override
    public boolean isEnabled(final ProjectInfo projectInfo) {
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        final TeamProjectEntity teamProject =
            connection.getTeamProjectCollectionEntity(false).getTeamProject(new GUID(projectInfo.getGUID()));

        final ProcessGuidanceEntity processGuidance = (teamProject == null) ? null : teamProject.getProcessGuidance();

        if (processGuidance != null) {
            return true;
        }

        return false;
    }

    /**
     * Attempts to retrieve a URL to the given process guidance document beneath
     * the given project. If those documents fail to open AND the process
     * guidance site is an external website, then the fully qualified url of the
     * external website will be returned.
     *
     * @param projectInfo
     *        The team project information (not <code>null</code>)
     * @param documentPath
     *        The document file name to return (all components must be URL
     *        encoded)
     * @return a {@link ProcessGuidanceReference} describing the validity of the
     *         process guidance document and its url if it is valid (never
     *         <code>null</code>)
     */
    @Override
    public ProcessGuidanceURLInfo getProcessGuidanceURL(final ProjectInfo projectInfo, final String documentPath) {
        return getProcessGuidanceURL(projectInfo, documentPath, null);
    }

    /**
     * Attempts to retrieve a URL to the given process guidance document beneath
     * the given project. If the given document fails to open, the alternate
     * document paths will be tried in order. If those documents fail to open
     * AND the process guidance site is an external website, then the fully
     * qualified url of the external website will be returned.
     *
     *
     * @param projectInfo
     *        The team project information (not <code>null</code>)
     * @param documentPath
     *        The document file name to return (all components must be URL
     *        encoded)
     * @param alternateDocumentPaths
     *        alternate document paths to try if the primary document path fails
     *        (may be <code>null</code>)
     * @return a {@link ProcessGuidanceReference} describing the validity of the
     *         process guidance document and its url if it is valid (never
     *         <code>null</code>)
     */
    @Override
    public ProcessGuidanceURLInfo getProcessGuidanceURL(
        final ProjectInfo projectInfo,
        final String documentPath,
        final String[] alternateDocumentPaths) {
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$

        boolean valid = false;
        String url;
        String invalidMessage = null;

        final TeamProjectEntity teamProject =
            connection.getTeamProjectCollectionEntity(false).getTeamProject(new GUID(projectInfo.getGUID()));

        final ProcessGuidanceEntity processGuidance = (teamProject == null) ? null : teamProject.getProcessGuidance();

        if (processGuidance == null) {
            return new ProcessGuidanceURLInfoImpl(
                null,
                false,
                MessageFormat.format(
                    Messages.getString("WssProcessGuidance.ProcessGuidanceNotConfiguredFormat"), //$NON-NLS-1$
                    projectInfo.getName()));
        }

        final String processGuidanceUrl = processGuidance.getFullyQualifiedURL();

        url = URIUtils.combinePaths(processGuidanceUrl, documentPath);
        valid = isValidURL(url);

        if (!valid && alternateDocumentPaths != null) {
            int ix = 0;
            while (!valid && ix < alternateDocumentPaths.length) {
                url = URIUtils.combinePaths(processGuidanceUrl, alternateDocumentPaths[ix++]);
                valid = isValidURL(url);
            }
        }

        /*
         * For website (non-WSS) process guidance installations, we give up and
         * return the process guidance site.
         */
        if (!valid && "WebSite".equals(processGuidance.getResourceSubType())) //$NON-NLS-1$
        {
            url = processGuidanceUrl;
            valid = isValidURL(url);
        }

        if (!valid) {
            url = null;
            invalidMessage =
                MessageFormat.format(
                    Messages.getString("WssProcessGuidance.ProcessGuidanceNotFoundFormat"), //$NON-NLS-1$
                    projectInfo.getName(),
                    documentPath);
        }

        return new ProcessGuidanceURLInfoImpl(url, valid, invalidMessage);
    }

    private boolean isValidURL(String url) {
        /* Users can override validity checking. */
        if (!doResolve) {
            return true;
        }

        final String originalUrl = url;

        HttpClient httpClient = connection.newHTTPClient(URIUtils.newURI(url));

        GetMethod getMethod = new GetMethod(url);

        int redirects = 0;

        try {
            while (redirects < 8) {
                final int code = httpClient.executeMethod(getMethod);

                if (code == HttpStatus.SC_OK) {
                    return true;
                }

                /*
                 * We need to handle redirects. The default behavior on agile
                 * templates in 2010 is to redirect users to MSDN.
                 */
                if (code == HttpStatus.SC_MOVED_PERMANENTLY || code == HttpStatus.SC_MOVED_TEMPORARILY) {
                    final Header[] responseHeaders = getMethod.getResponseHeaders("Location"); //$NON-NLS-1$

                    if (responseHeaders.length != 1) {
                        return false;
                    }

                    url = responseHeaders[0].getValue();

                    getMethod.releaseConnection();

                    httpClient = connection.newHTTPClient(URIUtils.newURI(url));
                    getMethod = new GetMethod(url);

                    redirects++;
                } else {
                    log.info(MessageFormat.format(
                        "Could not open process guidance page {0}: {1}", //$NON-NLS-1$
                        url,
                        Integer.toString(code)));
                    return false;
                }
            }

            log.warn(MessageFormat.format("Too many redirects trying to open {0}", originalUrl)); //$NON-NLS-1$
            return false;
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Error validating process guidance url: {0}", originalUrl), e); //$NON-NLS-1$
            return false;
        } finally {
            getMethod.releaseConnection();
        }
    }
}
