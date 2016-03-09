// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;

/**
 * <p>
 * {@link SelectionProviderAction} is an base {@link Action} implementation. It
 * is intended for use by non-declarative actions that need to track the
 * selection provided by an {@link ISelectionProvider}.
 * </p>
 */
public abstract class SelectionProviderAction extends Action {
    /**
     * An {@link ISelectionChangedListener} that we attach and detach from our
     * {@link ISelectionProvider}.
     */
    private final ISelectionChangedListener selectionChangedListener;

    /**
     * The {@link ISelectionProvider} we're currently tracking or
     * <code>null</code>.
     */
    private ISelectionProvider selectionProvider;

    /**
     * The last {@link ISelection} from the {@link ISelectionProvider}.
     */
    private ISelection selection;

    /**
     * Creates a new {@link SelectionProviderAction}, supplying an initial
     * {@link ISelectionProvider} to track. This {@link SelectionProviderAction}
     * is initially disabled. If subclasses wish to perform immediate
     * enablement, they must manually call {@link #selectionChanged(ISelection)}
     * in the subclass constructor.
     *
     * @param selectionProvider
     *        initial {@link ISelectionProvider} or <code>null</code>
     */
    protected SelectionProviderAction(final ISelectionProvider selectionProvider) {
        /*
         * create a selection changed listener
         */
        selectionChangedListener = new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                SelectionProviderAction.this.selectionChanged(event);
            }
        };

        /*
         * start tracking the specified provider
         */
        setSelectionProvider(selectionProvider);
    }

    /**
     * Provides a new {@link ISelectionProvider} for this
     * {@link SelectionProviderAction} to track. If there is an existing
     * {@link ISelectionProvider}, it will be disassociated from this
     * {@link SelectionProviderAction}. This method will trigger a call to
     * {@link #selectionChanged(ISelection)}.
     *
     * @param selectionProvider
     *        a new {@link ISelectionProvider} to track or <code>null</code>
     */
    public void setSelectionProvider(final ISelectionProvider selectionProvider) {
        dispose();
        this.selectionProvider = selectionProvider;
        if (selectionProvider != null) {
            selectionProvider.addSelectionChangedListener(selectionChangedListener);
            selectionChanged(selectionProvider.getSelection());
        } else {
            selectionChanged(StructuredSelection.EMPTY);
        }
    }

    /**
     * Should be called when this action is no longer needed. If this
     * {@link SelectionProviderAction} is tracking an {@link ISelectionProvider}
     * , this method removes the {@link ISelectionChangedListener} from the
     * provider and stops tracking the provider.
     */
    public void dispose() {
        if (selectionProvider != null) {
            selectionProvider.removeSelectionChangedListener(selectionChangedListener);
            selectionProvider = null;
        }
    }

    /**
     * Called by this {@link SelectionProviderAction} whenever the
     * {@link ISelectionProvider} being tracked publishes a
     * {@link SelectionChangedEvent}. Subclasses may overrride. The base class
     * calls the {@link #selectionChanged(ISelection)} method with the event's
     * {@link ISelection}.
     *
     * @param event
     *        a {@link SelectionChangedEvent} (must not be <code>null</code>)
     */
    protected void selectionChanged(final SelectionChangedEvent event) {
        selectionChanged(event.getSelection());
    }

    /**
     * Called by this {@link SelectionProviderAction} whenever the selection has
     * changed - either because a new {@link ISelectionProvider} has been set or
     * because a {@link SelectionChangedEvent} has been received. Subclasses may
     * override. The base class caches the selection, calls one of the
     * <code>computeEnablement</code> overrides, and sets enablement on this
     * action.
     *
     * @param selection
     *        the new {@link ISelection} (must not be <code>null</code>)
     */
    protected void selectionChanged(final ISelection selection) {
        this.selection = selection;
        if (selection instanceof IStructuredSelection) {
            setEnabled(computeEnablement((IStructuredSelection) selection));
        } else {
            setEnabled(computeEnablement(selection));
        }
    }

    /**
     * Called by this {@link SelectionProviderAction} whenever the selection has
     * changed and the new {@link ISelection} is not an
     * {@link IStructuredSelection}. The return value is used to set a new
     * enablement for this action. Subclasses may override. The base class just
     * returns <code>false</code>.
     *
     * @param selection
     *        the new {@link ISelection} (must not be <code>null</code>)
     * @return action enablement based on the new selection
     */
    protected boolean computeEnablement(final ISelection selection) {
        return false;
    }

    /**
     * Called by this {@link SelectionProviderAction} whenever the selection has
     * changed and the new {@link ISelection} is an {@link IStructuredSelection}
     * . The return value is used to set a new enablement for this action.
     * Subclasses may override. The base class just returns <code>false</code>.
     *
     * @param selection
     *        the new {@link IStructuredSelection} (must not be
     *        <code>null</code>)
     * @return action enablement based on the new selection
     */
    protected boolean computeEnablement(final IStructuredSelection selection) {
        return false;
    }

    /**
     * Obtains the current selection size. If there is no current selection,
     * returns <code>0</code>. If the current selection is not an
     * {@link IStructuredSelection}, throws an exception.
     *
     * @return the current selection size
     */
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

    /**
     * Obtains the current {@link IStructuredSelection}. If there is no current
     * selection, returns <code>null</code>. If the current selection is not an
     * {@link IStructuredSelection}, throws an exception.
     *
     * @return the current {@link IStructuredSelection} or <code>null</code> if
     *         there is no current selection
     */
    protected final IStructuredSelection getStructuredSelection() {
        return SelectionUtils.getStructuredSelection(selection);
    }

    /**
     * Obtains the current {@link ISelection}. If there is no current selection,
     * returns <code>null</code>.
     *
     * @return the current {@link ISelection} or <code>null</code> if there is
     *         no current selection
     */
    protected final ISelection getSelection() {
        return selection;
    }

    /**
     * Obtains the {@link ISelectionProvider} currently being tracked by this
     * {@link SelectionProviderAction}. If there is no current
     * {@link ISelectionProvider}, returns <code>null</code>.
     *
     * @return the current {@link ISelectionProvider} or <code>null</code> if
     *         there is no current {@link ISelectionProvider}
     */
    protected final ISelectionProvider getSelectionProvider() {
        return selectionProvider;
    }

    @Override
    public final void run() {
        ClientTelemetryHelper.sendRunActionEvent(this);
        doRun();
    }

    public abstract void doRun();
}
