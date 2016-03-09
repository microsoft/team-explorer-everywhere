// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

/**
 * The results of a compare operation
 */
public class CompareResult {
    private final boolean contentIdentical;
    private final boolean okPressed;
    private final boolean contentSaved;

    CompareResult(final boolean contentIdentical, final boolean okPressed, final boolean contentSaved) {
        this.contentIdentical = contentIdentical;
        this.okPressed = okPressed;
        this.contentSaved = contentSaved;
    }

    /**
     * Queries whether the inputs to the compare engine were all identical
     * (modified, original and, if specified, ancestor.) Note that this does NOT
     * return true when modified and original are identical, but were both
     * modified in common from a differing ancestor.
     *
     * @return <code>true</code> if all inputs to the compare engine are
     *         identical, <code>false</code> otherwise
     */
    public boolean isContentIdentical() {
        return contentIdentical;
    }

    /**
     * Returns true if the compare was displayed in a dialog and the OK button
     * was pressed. If the compare was displayed in an editor, this value always
     * returns false
     *
     * @return <code>true</code> if the compare was displayed in a dialog and
     *         the OK button was pressed, <code>false</code> otherwise
     */
    public boolean wasOKPressed() {
        return okPressed;
    }

    /**
     * Returns true if the contents of any of the compare inputs were saved
     * during the comparison, false otherwise. Note that this value is only
     * updated for synchronous compare UIs (such as when using a dialog), for
     * asynchronous compare UIs, you should use a saved listener on the
     * {@link CustomompareEditorInput}.
     *
     * @return <code>true</code> if the contents of any editor were saved,
     *         <code>false</code> otherwise
     */
    public boolean isContentSaved() {
        return contentSaved;
    }
}
