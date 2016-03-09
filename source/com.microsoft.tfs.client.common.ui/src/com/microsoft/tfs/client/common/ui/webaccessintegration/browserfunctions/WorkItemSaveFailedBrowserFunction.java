// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.util.Check;

public class WorkItemSaveFailedBrowserFunction extends BrowserFunction {
    public WorkItemSaveFailedBrowserFunction(final Browser browser, final String name) {
        super(browser, name);
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 1, "arguments.length == 1"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "arguments[0] instanceof String"); //$NON-NLS-1$

        final String title = Messages.getString("WorkItemEditor.ErrorDialogTitle"); //$NON-NLS-1$
        final String message = (String) arguments[0];

        MessageDialog.openError(getBrowser().getShell(), title, message);
        return null;
    }
}
