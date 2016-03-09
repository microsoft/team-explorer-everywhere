// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;

public abstract class ObjectActionDelegate implements IObjectActionDelegate {
    private IAction action;
    private IWorkbenchPart targetPart;
    private ISelection selection;

    @Override
    public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
        this.action = action;
        this.targetPart = targetPart;
    }

    @Override
    public final void selectionChanged(final IAction action, final ISelection selection) {
        this.selection = selection;
        onSelectionChanged(action, selection);
    }

    protected void onSelectionChanged(final IAction action, final ISelection selection) {

    }

    protected final IWorkbenchPart getTargetPart() {
        return targetPart;
    }

    protected final Shell getShell() {
        if (targetPart != null) {
            return targetPart.getSite().getShell();
        }

        return Display.getCurrent().getActiveShell();
    }

    protected final IWorkbenchWindow getWorkbenchWindow() {
        return targetPart.getSite().getWorkbenchWindow();
    }

    protected final IAction getAction() {
        return action;
    }

    protected final ISelection getSelection() {
        return selection;
    }

    protected final int getSelectionSize() {
        return SelectionUtils.getSelectionSize(selection);
    }

    protected final Object[] selectionToArray() {
        return SelectionUtils.selectionToArray(selection);
    }

    protected final Object[] selectionToArray(final Class targetType) {
        return SelectionUtils.selectionToArray(selection, targetType);
    }

    protected final Object[] adaptSelectionToArray(final Class targetType) {
        return SelectionUtils.adaptSelectionToArray(selection, targetType);
    }

    protected final Object[] selectionToArray(final Class targetType, final boolean adapt) {
        return SelectionUtils.selectionToArray(selection, targetType, adapt);
    }

    protected final Object getSelectionFirstElement() {
        return SelectionUtils.getSelectionFirstElement(selection);
    }

    protected final Object adaptSelectionFirstElement(final Class targetType) {
        return SelectionUtils.adaptSelectionFirstElement(selection, targetType);
    }

    protected final IStructuredSelection getStructuredSelection() {
        return SelectionUtils.getStructuredSelection(selection);
    }

    protected final ICommandExecutor getCommandExecutor() {
        return getCommandExecutor(false);
    }

    protected final ICommandExecutor getCommandExecutor(final boolean async) {
        if (async) {
            /*
             * Since this is an action, make the Job a user job. This is what
             * the end user would normally expect.
             */
            final JobOptions jobOptions = new JobOptions().setUser(true);

            return UICommandExecutorFactory.newUIJobCommandExecutor(getShell(), jobOptions);
        } else {
            return UICommandExecutorFactory.newUICommandExecutor(getShell());
        }
    }

    protected final IStatus execute(final ICommand command) {
        return execute(command, false);
    }

    protected final IStatus execute(final ICommand command, final boolean async) {
        final IStatus status = getCommandExecutor(async).execute(command);

        ClientTelemetryHelper.sendCommandFinishedEvent(command, status);

        return status;
    }
}
