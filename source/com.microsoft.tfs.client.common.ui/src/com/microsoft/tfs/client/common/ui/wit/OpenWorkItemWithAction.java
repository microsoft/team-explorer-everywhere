// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit;

import org.eclipse.jface.action.Action;

/**
 * Action to open a work item with a specific work item editor.
 *
 *
 * @threadsafety unknown
 */
public class OpenWorkItemWithAction extends Action {
    private final String editorID;

    public OpenWorkItemWithAction(final String editorDisplayName, final String editorID) {
        this.editorID = editorID;
        this.setText(editorDisplayName);
    }

    public String getEditorID() {
        return editorID;
    }
}
