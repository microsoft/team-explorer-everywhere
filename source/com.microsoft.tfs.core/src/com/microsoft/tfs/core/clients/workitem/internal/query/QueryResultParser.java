// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class QueryResultParser {
    private final Element messageElement;

    public QueryResultParser(final Element messageElement) {
        this.messageElement = messageElement;
    }

    public int[] parseIDs() {
        final List<Integer> ids = new ArrayList<Integer>();

        final NodeList idsElements = messageElement.getElementsByTagName("id"); //$NON-NLS-1$

        for (int i = 0; i < idsElements.getLength(); i++) {
            final Element idElement = (Element) idsElements.item(i);
            final String sAtt = idElement.getAttribute("s"); //$NON-NLS-1$
            final String eAtt = idElement.getAttribute("e"); //$NON-NLS-1$
            if (sAtt == null || sAtt.length() == 0) {
                continue;
            }

            final int id = Integer.parseInt(sAtt);

            if (eAtt == null || eAtt.length() == 0) {
                ids.add(new Integer(id));
            } else {
                final int endIx = Integer.parseInt(eAtt);
                for (int curIx = id; curIx <= endIx; curIx++) {
                    ids.add(new Integer(curIx));
                }
            }
        }

        final int[] parsedIds = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            parsedIds[i] = ids.get(i).intValue();
        }

        return parsedIds;
    }
}
