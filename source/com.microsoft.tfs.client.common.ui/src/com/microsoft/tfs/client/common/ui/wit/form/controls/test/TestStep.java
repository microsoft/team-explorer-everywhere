// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test;

import org.w3c.dom.Node;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.util.xml.DOMUtils;

public class TestStep {

    private final Node node;

    public String type = ""; //$NON-NLS-1$

    public String[] parameterizedStrings;

    public int noOfAttachments = 0;

    public TestStep(final Node node, final WorkItem wi) {
        this.node = node;
        initParameterizedStrings();
        initNoOfAttachments(wi);
        initType();
    }

    private void initNoOfAttachments(final WorkItem wi) {
        final Node idNode = node.getAttributes().getNamedItem("id"); //$NON-NLS-1$
        if (idNode != null) {
            final String id = idNode.getNodeValue();
            for (final Attachment attachment : wi.getAttachments()) {
                if (attachment.getComment() != null && attachment.getComment().startsWith("[TestStep=" + id + "]:")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    noOfAttachments++;
                }
            }
        }
    }

    private void initType() {
        final Node typeNode = node.getAttributes().getNamedItem("type"); //$NON-NLS-1$
        if (typeNode != null) {
            type = typeNode.getNodeValue();
        }
    }

    private void initParameterizedStrings() {
        final Node[] nodes = DOMUtils.getChildElements(node);
        parameterizedStrings = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            parameterizedStrings[i] = TestStepUtil.getParameterizedString(nodes[i]);
        }
    }
}