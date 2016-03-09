// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessBaseEditor;
import com.microsoft.tfs.util.Check;

public class BuildDeletedBrowserFunction extends BrowserFunction {
    private final EditorPart editor;

    public BuildDeletedBrowserFunction(final Browser browser, final String name, final EditorPart editor) {
        super(browser, name);
        this.editor = editor;
    }

    @Override
    public Object function(final Object[] arguments) {
        WebAccessBaseEditor.closeEditor(editor);

        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 1, "arguments length"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "argument[0]"); //$NON-NLS-1$

        final String deletedBuildURI = (String) arguments[0];
        TFSCommonUIClientPlugin.getDefault().getBuildManager().fireBuildDeletedEvent(this, deletedBuildURI);
        return null;
    }
}
