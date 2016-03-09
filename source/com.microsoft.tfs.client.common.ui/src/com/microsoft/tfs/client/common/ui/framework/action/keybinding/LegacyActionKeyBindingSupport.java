// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action.keybinding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.ActionHandler;
import org.eclipse.ui.commands.HandlerSubmission;
import org.eclipse.ui.commands.IHandler;
import org.eclipse.ui.commands.IWorkbenchCommandSupport;
import org.eclipse.ui.commands.Priority;

import com.microsoft.tfs.util.Check;

public class LegacyActionKeyBindingSupport implements ActionKeyBindingSupport {
    private final IWorkbenchCommandSupport workbenchCommandSupport;
    private final Shell shell;
    private final IWorkbenchPartSite site;
    private final List handlerSubmissions = new ArrayList();

    public LegacyActionKeyBindingSupport(final IWorkbench workbench, final IWorkbenchPart part) {
        this(workbench, part.getSite().getShell(), part.getSite());
    }

    public LegacyActionKeyBindingSupport(final IWorkbench workbench, final Shell shell) {
        this(workbench, shell, null);
    }

    private LegacyActionKeyBindingSupport(
        final IWorkbench workbench,
        final Shell shell,
        final IWorkbenchPartSite site) {
        Check.notNull(workbench, "workbench"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        workbenchCommandSupport = workbench.getCommandSupport();
        this.shell = shell;
        this.site = site;
    }

    @Override
    public void addAction(final IAction action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        final String commandId = action.getActionDefinitionId();

        if (commandId == null) {
            throw new IllegalArgumentException("action does not have an action definition ID set"); //$NON-NLS-1$
        }

        final IHandler handler = new ActionHandler(action);
        final HandlerSubmission handlerSubmission =
            new HandlerSubmission(null, shell, site, commandId, handler, Priority.MEDIUM);

        workbenchCommandSupport.addHandlerSubmission(handlerSubmission);
        handlerSubmissions.add(handlerSubmission);
    }

    @Override
    public void dispose() {
        for (final Iterator it = handlerSubmissions.iterator(); it.hasNext();) {
            final HandlerSubmission handlerSubmission = (HandlerSubmission) it.next();
            workbenchCommandSupport.removeHandlerSubmission(handlerSubmission);
            handlerSubmission.getHandler().dispose();
        }
        handlerSubmissions.clear();
    }
}
