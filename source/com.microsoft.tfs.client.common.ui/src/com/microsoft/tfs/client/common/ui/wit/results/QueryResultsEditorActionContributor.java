// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;

import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.client.common.ui.wit.query.QueryDocumentEditorEnabledChangedListener;

public class QueryResultsEditorActionContributor extends EditorActionBarContributor {
    private RunQueryAction runQueryAction;
    private ViewQueryAction viewQueryAction;
    private ColumnOptionsAction columnOptionsAction;
    private QueryResultsEditor activeEditor;
    private QueryDocumentEditorEnabledChangedListener listener;

    @Override
    public void contributeToToolBar(final IToolBarManager toolBarManager) {
        runQueryAction = new RunQueryAction();
        viewQueryAction = new ViewQueryAction();
        columnOptionsAction = new ColumnOptionsAction();
        toolBarManager.add(runQueryAction);
        toolBarManager.add(viewQueryAction);
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
        // Get the new active editor and setup a listener.
        activeEditor = (QueryResultsEditor) targetEditor;
        boolean isConnected = false;

        if (activeEditor != null) {
            activeEditor.setEnabledChangedListener(listener);
            isConnected = activeEditor.isConnected();
        }

        // Enable the actions appropriately.
        if (runQueryAction != null) {
            runQueryAction.setActiveEditor(activeEditor);

            final IActionBars actionBars = getActionBars();
            actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), runQueryAction);
            actionBars.updateActionBars();
        }
        if (viewQueryAction != null) {
            viewQueryAction.setActiveEditor(activeEditor);
        }
        if (columnOptionsAction != null) {
            columnOptionsAction.setActiveEditor(activeEditor);
        }

        setEnabled(isConnected);
    }

    public void setEnabled(final boolean enabled) {
        if (runQueryAction != null) {
            runQueryAction.setEnabled(enabled);
        }

        if (viewQueryAction != null) {
            viewQueryAction.setEnabled(enabled);
        }

        if (columnOptionsAction != null) {
            columnOptionsAction.setEnabled(enabled);
        }
    }
}
