// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditor;
import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;

/**
 * A saveable filter that limits itself to prompting for Team Foundation Server
 * related items.
 *
 * @threadsafety unknown
 */
public class TFSEditorSaveableFilter implements WorkbenchPartSaveableFilter {
    private final TFSEditorSaveableType saveableTypes;

    public TFSEditorSaveableFilter(final TFSEditorSaveableType saveableTypes) {
        Check.notNull(saveableTypes, "saveableTypes"); //$NON-NLS-1$

        this.saveableTypes = saveableTypes;
    }

    @Override
    public boolean select(final IWorkbenchPart[] workbenchParts) {
        if (workbenchParts.length > 0) {
            final IWorkbenchPart part = workbenchParts[0];

            if (part instanceof BaseQueryDocumentEditor) {
                return saveableTypes.contains(TFSEditorSaveableType.WORK_ITEM_QUERIES);
            }

            else if (part instanceof WorkItemEditor) {
                return saveableTypes.contains(TFSEditorSaveableType.WORK_ITEMS);
            }
        }

        return false;
    }

    public static final class TFSEditorSaveableType extends BitField {
        private static final long serialVersionUID = 7186322490480221970L;

        public TFSEditorSaveableType(final int flags) {
            super(flags);
        }

        public static final TFSEditorSaveableType WORK_ITEMS = new TFSEditorSaveableType(1);
        public static final TFSEditorSaveableType WORK_ITEM_QUERIES = new TFSEditorSaveableType(2);

        public static final TFSEditorSaveableType ALL = new TFSEditorSaveableType(BitField.combine(new BitField[] {
            WORK_ITEMS,
            WORK_ITEM_QUERIES
        }));

        public boolean contains(final TFSEditorSaveableType type) {
            return containsInternal(type);
        }
    }
}
