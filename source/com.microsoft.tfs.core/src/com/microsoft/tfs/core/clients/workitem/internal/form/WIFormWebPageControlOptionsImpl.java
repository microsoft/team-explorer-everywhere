// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormContent;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLink;
import com.microsoft.tfs.core.clients.workitem.form.WIFormWebPageControlOptions;

public class WIFormWebPageControlOptionsImpl extends WIFormElementImpl implements WIFormWebPageControlOptions {
    private boolean allowScript;
    private boolean reloadOnParamChange;

    private WIFormContent content;
    private WIFormLink link;

    /**
     * Process the attributes from the "WebpageControlOptions" element.
     *
     * Attributes: - AllowScript: optional - ReloadOnParamChange: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        allowScript =
            WIFormParseHandler.readBooleanValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_ALLOWSCRIPT, false);
        reloadOnParamChange = WIFormParseHandler.readBooleanValue(
            attributes,
            WIFormParseConstants.ATTRIBUTE_NAME_RELOADONPARAMCHANGE,
            false);
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "WebpageControlOptions" element.
     *
     * Child elements: (choice, minimum=1, maximum=1) - LINK element - CONTENT
     * element.
     */
    @Override
    void endLoading() {
        final WIFormElement[] children = getChildElements();
        for (int i = 0; i < children.length; i++) {
            final WIFormElement child = children[i];
            if (child instanceof WIFormContent) {
                content = (WIFormContent) child;
            } else if (child instanceof WIFormLink) {
                link = (WIFormLink) child;
            }
        }
    }

    /**
     * Corresponds to the AllowScript attribute on the "WebpageControlOptions"
     * element.
     */
    @Override
    public boolean getAllowScript() {
        return allowScript;
    }

    /**
     * Corresponds to the ReloadOnParamChange attribute on the
     * "WebpageControlOptions" element.
     */
    @Override
    public boolean getReloadOnParamChange() {
        return reloadOnParamChange;
    }

    /**
     * Corresponds to the CONTENT child element on the "WebpageControlOptions"
     * element.
     */
    @Override
    public WIFormContent getContent() {
        return content;
    }

    /**
     * Corresponds to the LINK child element on the "WebpageControlOptions"
     * element.
     */
    @Override
    public WIFormLink getLink() {
        return link;
    }

    /**
     * True if content of the "WebpageControlOptions" element is a LINK.
     */
    @Override
    public boolean isLink() {
        return link != null;
    }
}
