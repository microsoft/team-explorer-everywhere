// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessWorkItemEditor;
import com.microsoft.tfs.util.Check;

public class WorkItemDirtyChangedBrowserFunction extends BrowserFunction {
    private final WebAccessWorkItemEditor editor;

    public WorkItemDirtyChangedBrowserFunction(
        final Browser browser,
        final String name,
        final WebAccessWorkItemEditor editor) {
        super(browser, name);
        this.editor = editor;
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 1, "arguments length"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "argument[0]"); //$NON-NLS-1$

        editor.onDirtyStateChanged();
        return null;
    }
}
