// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test;

import org.w3c.dom.Node;

public class Param {
    public String name = ""; //$NON-NLS-1$
    public String bind = ""; //$NON-NLS-1$

    public Param(final Node node) {
        String val = TestStepUtil.getAttr(node, "name"); //$NON-NLS-1$
        if (val != null) {
            name = val;
        }
        val = TestStepUtil.getAttr(node, "bind"); //$NON-NLS-1$
        if (val != null) {
            bind = val;
        }
    }

}