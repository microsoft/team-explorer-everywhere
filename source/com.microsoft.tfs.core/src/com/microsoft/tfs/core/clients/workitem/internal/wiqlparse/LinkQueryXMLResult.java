// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import org.w3c.dom.Element;

/**
 * Wrapper class around the {@link LinkQueryXMLResult} and the associated
 * LinkGroup. Used by {@link WIQLAdapter} to return the link query XML while
 * passing the derrived LinkGroup. In the .NET OM this is achieved using an out
 * parameter.
 */
public class LinkQueryXMLResult {
    private final Element linkXml;
    private final NodeAndOperator linkGroup;

    public LinkQueryXMLResult(final Element linkXml, final NodeAndOperator linkGroup) {
        super();
        this.linkXml = linkXml;
        this.linkGroup = linkGroup;
    }

    public Element getLinkXML() {
        return linkXml;
    }

    public NodeAndOperator getLinkGroup() {
        return linkGroup;
    }

}
