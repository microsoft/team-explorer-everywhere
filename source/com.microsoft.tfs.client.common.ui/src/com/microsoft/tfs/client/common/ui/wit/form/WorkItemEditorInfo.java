// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

/**
 * Contains information about a registered work item editor.
 *
 *
 * @threadsafety unknown
 */
public class WorkItemEditorInfo {
    private final String editorID;
    private final String displayName;

    public WorkItemEditorInfo(final String editorID, final String displayName) {
        this.editorID = editorID;
        this.displayName = displayName;
    }

    public String getEditorID() {
        return editorID;
    }

    public String getDisplayName() {
        return displayName;
    }
}
