// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import java.net.URLEncoder;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLink;
import com.microsoft.tfs.core.clients.workitem.form.WIFormParam;
import com.microsoft.tfs.core.clients.workitem.internal.MacroHelpers;

public class WIFormLinkImpl extends WIFormElementImpl implements WIFormLink {
    private String urlRoot;
    private String urlPath;

    /**
     * Process the attributes of the "Link" element.
     *
     * Attributes: - UrlRoot: required - UrlPath: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        urlRoot = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_URLROOT);
        urlPath = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_URLPATH);
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "UrlRoot" attribute of the "Link" element.
     */
    public String getURLRoot() {
        return urlRoot;
    }

    /**
     * Corresponds to the "UrlPath" attribute of the "Link" element.
     */
    public String getURLPath() {
        return urlPath;
    }

    /**
     * Compute the URL for this link. Resolve any parameters or macros that may
     * be specified for this URL. The UrlRoot may contain a macro but no
     * parameters. The UrlPath may contain parameters but no macros.
     */
    @Override
    public String getURL(final WorkItem workItem) {
        // Start with the potentially parameterized URL.
        String resolvedRoot = urlRoot == null ? "" : urlRoot; //$NON-NLS-1$
        String resolvedPath = urlPath == null ? "" : urlPath; //$NON-NLS-1$

        // Apply the parameter substitutions to the URL path.
        final WIFormElement[] children = getChildElements();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof WIFormParam) {
                final WIFormParam param = (WIFormParam) children[i];
                final String token = param.getSubstitutionToken();
                String replacement = param.getSubstitutionValue(workItem);
                replacement = URLEncoder.encode(replacement);
                resolvedPath = resolvedPath.replaceAll(token, replacement);
            }
        }

        // Apply macro substitutions to the URL root.
        if (MacroHelpers.isMacro(resolvedRoot)) {
            resolvedRoot = MacroHelpers.resolveURIMacros(workItem, resolvedRoot);
        }

        // Return the fully resolved URL, ensuring there is a separator
        // between the root and the path.
        if (resolvedRoot.endsWith("/") || resolvedPath.startsWith("/")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return resolvedRoot + resolvedPath;
        } else {
            return resolvedRoot + "/" + resolvedPath; //$NON-NLS-1$
        }
    }
}
