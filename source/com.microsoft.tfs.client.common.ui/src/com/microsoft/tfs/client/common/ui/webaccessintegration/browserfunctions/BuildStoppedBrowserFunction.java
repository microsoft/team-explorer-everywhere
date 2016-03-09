// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.util.Check;

public class BuildStoppedBrowserFunction extends BrowserFunction {
    public BuildStoppedBrowserFunction(final Browser browser, final String name) {
        super(browser, name);
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 1, "arguments length"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "argument[0]"); //$NON-NLS-1$

        final String stoppedBuildURI = (String) arguments[0];
        TFSCommonUIClientPlugin.getDefault().getBuildManager().fireBuildStoppedEvent(this, stoppedBuildURI);

        return null;
    }
}
