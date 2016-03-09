// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;

import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.client.common.ui.wit.query.QueryDocumentEditorEnabledChangedListener;

public class QueryEditorActionContributor extends EditorActionBarContributor {
    private RunQueryAction runQueryAction;
    private ColumnOptionsAction columnOptionsAction;
    private QueryEditor activeEditor;
    private QueryDocumentEditorEnabledChangedListener listener;

    @Override
    public void contributeToToolBar(final IToolBarManager toolBarManager) {
        runQueryAction = new RunQueryAction();
        columnOptionsAction = new ColumnOptionsAction();
        toolBarManager.add(runQueryAction);
        toolBarManager.add(columnOptionsAction);

        listener = new QueryDocumentEditorEnabledChangedListener() {
            @Override
            public void onEnabledChanged(final BaseQueryDocumentEditor editor, final boolean enabled) {
                if (activeEditor == editor) {
                    setEnabled(enabled);
                }
            }
        };
    }

    @Override
    public void setActiveEditor(final IEditorPart targetEditor) {
        activeEditor = (QueryEditor) targetEditor;
        boolean isConnected = false;

        if (activeEditor != null) {
            activeEditor.setEnabledChangedListener(listener);
            isConnected = activeEditor.isConnected();
        }

        if (runQueryAction != null) {
            runQueryAction.setActiveEditor(activeEditor);
        }
        if (columnOptionsAction != null) {
            columnOptionsAction.setActiveEditor(activeEditor);
        }

        final IActionBars actionBars = getActionBars();
        actionBars.setGlobalActionHandler(
            ActionFactory.DELETE.getId(),
            activeEditor.getAction(ActionFactory.DELETE.getId()));

        actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), runQueryAction);
        actionBars.updateActionBars();

        setEnabled(isConnected);
    }

    public void setEnabled(final boolean enabled) {
        if (runQueryAction != null) {
            runQueryAction.setEnabled(enabled);
        }

        if (columnOptionsAction != null) {
            columnOptionsAction.setEnabled(enabled);
        }
    }
}
