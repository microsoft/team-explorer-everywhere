// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test;

import org.w3c.dom.Node;

import com.microsoft.tfs.util.xml.DOMUtils;

public class ParamDataTable {
    private final String[] columnValues;
    private final String[] columnNames;

    public ParamDataTable(final Node n) {
        final Node[] nodes = DOMUtils.getChildElements(n);
        columnNames = new String[nodes.length];
        columnValues = new String[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            columnNames[i] = nodes[i].getNodeName();
            columnValues[i] = DOMUtils.getText(nodes[i]);
        }
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public String[] getColumnValues() {
        return columnValues;
    }
}