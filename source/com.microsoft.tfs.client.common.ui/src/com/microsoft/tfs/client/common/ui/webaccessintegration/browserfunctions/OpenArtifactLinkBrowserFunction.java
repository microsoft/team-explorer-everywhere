// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ArtifactLinkHelpers;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.util.Check;

public class OpenArtifactLinkBrowserFunction extends BrowserFunction {
    public OpenArtifactLinkBrowserFunction(final Browser browser, final String name) {
        super(browser, name);
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(
            arguments.length == 1 && arguments[0] instanceof String,
            "arguments.length == 1 && arguments[0] instanceof String"); //$NON-NLS-1$

        final Shell shell = ShellUtils.getParentShell(getBrowser());
        final ArtifactID artifactID = new ArtifactID((String) arguments[0]);

        return ArtifactLinkHelpers.openArtifact(shell, artifactID);
    }
}
