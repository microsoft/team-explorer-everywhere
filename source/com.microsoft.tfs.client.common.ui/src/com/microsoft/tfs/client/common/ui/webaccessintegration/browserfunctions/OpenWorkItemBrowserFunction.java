// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.util.Check;

public class OpenWorkItemBrowserFunction extends BrowserFunction {
    private final TFSServer server;

    public OpenWorkItemBrowserFunction(final Browser browser, final String name, final TFSServer server) {
        super(browser, name);
        this.server = server;
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$

        int workItemID = 0;
        if (arguments[0] instanceof Double) {
            workItemID = ((Double) arguments[0]).intValue();
        } else if (arguments[0] instanceof String) {
            workItemID = Integer.parseInt((String) arguments[0]);
        }

        Check.isTrue(workItemID != 0, "workItemID != 0"); //$NON-NLS-1$
        WorkItemEditorHelper.openEditor(server, workItemID);
        return true;
    }
}
