// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LinkQueryResultParser {

    private final Element messageElement;

    public LinkQueryResultParser(final Element messageElement) {
        this.messageElement = messageElement;
    }

    public WorkItemRelation[] parse() {
        final List results = new ArrayList();

        // The valid pattern is to skip an attribute if it equals the last.
        int lastSourceID = WorkItemRelation.MISSING_ID;
        int lastTargetID = WorkItemRelation.MISSING_ID;
        int lastLinkType = WorkItemRelation.MISSING_ID;
        boolean lastEnumValue = false;

        final NodeList childNodes = messageElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Element child = (Element) childNodes.item(i);
            if (child.getNodeName().equalsIgnoreCase(LinkQueryResultXMLConstants.SOURCE_RUN)) {
                lastTargetID = WorkItemRelation.MISSING_ID;
                lastLinkType = WorkItemRelation.MISSING_ID;
                lastEnumValue = false;

                final String sAtt = child.getAttribute(LinkQueryResultXMLConstants.RUN_START);
                final String eAtt = child.getAttribute(LinkQueryResultXMLConstants.RUN_END);
                if (sAtt == null || sAtt.length() == 0) {
                    continue;
                }

                final int start = Integer.parseInt(sAtt);
                int end = start;
                if (eAtt != null && eAtt.length() > 0) {
                    end = Integer.parseInt(eAtt);
                }
                for (int curIx = start; curIx <= end; curIx++) {
                    results.add(createWorkItemRelation(curIx, lastTargetID, lastLinkType, lastEnumValue));
                }
                lastSourceID = end;
            } else if (child.getNodeName().equalsIgnoreCase(LinkQueryResultXMLConstants.TARGET_RUN)) {
                lastSourceID = WorkItemRelation.MISSING_ID;
                lastEnumValue = false;

                final String sAtt = child.getAttribute(LinkQueryResultXMLConstants.RUN_START);
                final String eAtt = child.getAttribute(LinkQueryResultXMLConstants.RUN_END);
                if (sAtt == null || sAtt.length() == 0) {
                    continue;
                }

                final int start = Integer.parseInt(sAtt);
                int end = start;
                if (eAtt != null && eAtt.length() > 0) {
                    end = Integer.parseInt(eAtt);
                }
                for (int curIx = start; curIx <= end; curIx++) {
                    results.add(createWorkItemRelation(lastSourceID, curIx, lastLinkType, lastEnumValue));
                }
                lastTargetID = end;
            } else {
                final String sourceId = child.getAttribute(LinkQueryResultXMLConstants.SOURCE_ID);
                if (sourceId != null && sourceId.length() > 0) {
                    lastSourceID = Integer.parseInt(sourceId);
                }
                final String targetId = child.getAttribute(LinkQueryResultXMLConstants.TARGET_ID);
                if (targetId != null && targetId.length() > 0) {
                    lastTargetID = Integer.parseInt(targetId);
                }
                final String linkType = child.getAttribute(LinkQueryResultXMLConstants.LINK_TYPE);
                if (linkType != null && linkType.length() > 0) {
                    lastLinkType = Integer.parseInt(linkType);
                }
                final String enumValue = child.getAttribute(LinkQueryResultXMLConstants.ENUM_VALUE);
                if (enumValue != null && enumValue.length() > 0) {
                    lastEnumValue = Integer.parseInt(enumValue) == 1;
                }

                results.add(createWorkItemRelation(lastSourceID, lastTargetID, lastLinkType, lastEnumValue));
            }
        }

        return (WorkItemRelation[]) results.toArray(new WorkItemRelation[results.size()]);

    }

    private WorkItemRelation createWorkItemRelation(
        final int sourceId,
        final int targetId,
        int linkType,
        boolean enumValue) {
        if (sourceId == WorkItemRelation.MISSING_ID || targetId == WorkItemRelation.MISSING_ID) {
            linkType = WorkItemRelation.MISSING_ID;
            enumValue = false;
        }
        return new WorkItemRelation(sourceId, targetId, linkType, enumValue);
    }

}
