// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action.keybinding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.expressions.ActivePartExpression;
import org.eclipse.ui.keys.IBindingService;

import com.microsoft.tfs.util.Check;

public class ModernActionKeyBindingSupport implements ActionKeyBindingSupport {
    private final IBindingService bindingService;
    private final IHandlerService handlerService;
    private final Expression expression;
    private final int sourcePriorities;
    private final List handlerActivations = new ArrayList();

    public ModernActionKeyBindingSupport(final IAdaptable serviceLocator, final Shell shell) {
        this(serviceLocator, new ActiveShellExpression(shell), ActiveShellExpression.SOURCES);
    }

    public ModernActionKeyBindingSupport(final IAdaptable serviceLocator, final IWorkbenchPart part) {
        this(serviceLocator, new ActivePartExpression(part), ActiveShellExpression.SOURCES);
    }

    private ModernActionKeyBindingSupport(
        final IAdaptable serviceLocator,
        final Expression expression,
        final int sourcePriorities) {
        Check.notNull(serviceLocator, "serviceLocator"); //$NON-NLS-1$
        Check.notNull(expression, "expression"); //$NON-NLS-1$

        bindingService = (IBindingService) serviceLocator.getAdapter(IBindingService.class);
        handlerService = (IHandlerService) serviceLocator.getAdapter(IHandlerService.class);

        if (bindingService == null || handlerService == null) {
            throw new IllegalArgumentException(
                "specified IAdapable could not provide IBindingService or IHandlerService"); //$NON-NLS-1$
        }

        this.expression = expression;
        this.sourcePriorities = sourcePriorities;
    }

    @Override
    public void addAction(final IAction action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        final String commandId = action.getActionDefinitionId();

        if (commandId == null) {
            throw new IllegalArgumentException("action does not have an action definition ID set"); //$NON-NLS-1$
        }

        final TriggerSequence[] bindings = bindingService.getActiveBindingsFor(commandId);

        if (bindings.length > 0) {
            final IHandler handler = new ActionHandler(action);
            /*
             * Call this deprecated overload for 3.1 support
             */
            final IHandlerActivation activation =
                handlerService.activateHandler(commandId, handler, expression, sourcePriorities);
            handlerActivations.add(activation);
        }
    }

    @Override
    public void dispose() {
        for (final Iterator it = handlerActivations.iterator(); it.hasNext();) {
            final IHandlerActivation activation = (IHandlerActivation) it.next();
            handlerService.deactivateHandler(activation);
            activation.getHandler().dispose();
        }
        handlerActivations.clear();
    }
}