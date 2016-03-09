// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.html;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Javascript-callable function called when the mouse leaves the region over a
 * link.
 * </p>
 * <h2>WARNING</h2>
 * <p>
 * This class's base class, {@link BrowserFunction}, is only available in
 * Eclipse 3.5 and later. To prevent class load errors, make sure your
 * references to this class are guarded against execution in a too-old Eclipse
 * environment.
 * </p>
 *
 * @threadsafety unknown
 */
final class MouseLinkExitFunction extends BrowserFunction {
    private final HTMLEditor editor;

    public MouseLinkExitFunction(final Browser browser, final String name, final HTMLEditor editor) {
        super(browser, name);

        Check.notNull(editor, "editor"); //$NON-NLS-1$
        this.editor = editor;
    }

    /**
     * Handles a mouse move out of the region over a link.
     *
     * @param arguments
     *        ignored
     * @return null
     */
    @Override
    public Object function(final Object[] arguments) {
        editor.onMouseLinkExit();

        return null;
    }
}