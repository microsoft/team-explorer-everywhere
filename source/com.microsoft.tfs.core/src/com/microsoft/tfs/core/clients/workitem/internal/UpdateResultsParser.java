// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UpdateResultsParser {
    public static void parseUpdateResults(final WorkItemImpl workItem, final Element messageElement) {
        final Element updateWorkItemElement = (Element) messageElement.getFirstChild();

        Integer.parseInt(updateWorkItemElement.getAttribute("Revision")); //$NON-NLS-1$

        final Element computedColumnsElement = (Element) updateWorkItemElement.getFirstChild();
        final NodeList columns = computedColumnsElement.getElementsByTagName("ComputedColumn"); //$NON-NLS-1$
        for (int i = 0; i < columns.getLength(); i++) {
            final Element computedColumnElement = (Element) columns.item(i);
            computedColumnElement.getAttribute("Column"); //$NON-NLS-1$
            final Node valueTextNode = computedColumnElement.getFirstChild().getFirstChild();
            if (valueTextNode.getNodeType() == Node.TEXT_NODE) {
                valueTextNode.getNodeValue();
            }
        }
    }
}
