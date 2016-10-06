// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.protocolhandler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Ignore;

import com.microsoft.tfs.util.URLEncode;
import com.microsoft.tfs.util.base64.Base64;

@Ignore
public class ProtocolHandlerTestData {

    private static final String handlerUrlTemplate = "vsoeclipse://checkout/?EncFormat=UTF8&tfslink={tfsLink}"; //$NON-NLS-1$
    private static final String tfsLinkTemplate =
        "serverUrl={configurationServerUrl}&cloneUrl={repoUrl}&collectionId={collectionId}&project={project}&repository={repoName}&ref={branchName}"; //$NON-NLS-1$

    public static String getHandlerUrl(
        final String serverUrl,
        final String repoUrl,
        final String collectionId,
        final String projectName,
        final String repoName,
        final String branchName) {

        final String tfsLink = tfsLinkTemplate //
            .replace("{configurationServerUrl}", urlEncode(serverUrl)) //$NON-NLS-1$
            .replace("{repoUrl}", urlEncode(repoUrl)) //$NON-NLS-1$
            .replace("{collectionId}", collectionId) //$NON-NLS-1$
            .replace("{project}", projectName) //$NON-NLS-1$
            .replace("{repoName}", repoName) //$NON-NLS-1$
            .replace("{branchName}", urlEncodeComponent(branchName)); //$NON-NLS-1$

        final String base64EncodedTfsLink = base64Encode(tfsLink);

        return handlerUrlTemplate.replace("{tfsLink}", base64EncodedTfsLink); //$NON-NLS-1$
    }

    private static String base64Encode(final String s) {
        try {
            return new String(Base64.encodeBase64(s.getBytes("UTF-8"))); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Incorrect test authoring", e); //$NON-NLS-1$
        }
    }

    private static String urlEncodeComponent(final String s) {
        return URLEncode.encode(s).replace("&", "%26"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String urlEncode(final String s) {
        try {
            return new URI(s).toASCIIString();
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Incorrect test authoring", ex); //$NON-NLS-1$
        }
    }
}
