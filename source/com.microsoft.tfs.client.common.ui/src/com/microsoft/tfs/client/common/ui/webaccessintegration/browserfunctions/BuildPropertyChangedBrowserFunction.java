// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.util.Check;

public class BuildPropertyChangedBrowserFunction extends BrowserFunction {
    public BuildPropertyChangedBrowserFunction(final Browser browser, final String name) {
        super(browser, name);
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 3, "arguments length"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "argument[0]"); //$NON-NLS-1$
        Check.isTrue(arguments[1] instanceof String, "argument[1]"); //$NON-NLS-1$

        final String buildUri = (String) arguments[0];
        final String buildProperty = (String) arguments[1];
        TFSCommonUIClientPlugin.getDefault().getBuildManager().fireBuildPropertyChangedEvent(
            this,
            buildUri,
            buildProperty);
        return null;
    }
}
