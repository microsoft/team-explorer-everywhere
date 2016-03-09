// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessWorkItemEditor;

public class DiscardNewWorkItemBrowserFunction extends BrowserFunction {
    private final WebAccessWorkItemEditor editor;

    public DiscardNewWorkItemBrowserFunction(
        final Browser browser,
        final String name,
        final WebAccessWorkItemEditor editor) {
        super(browser, name);
        this.editor = editor;
    }

    @Override
    public Object function(final Object[] arguments) {
        WebAccessWorkItemEditor.closeEditor(editor);
        return null;
    }
}
