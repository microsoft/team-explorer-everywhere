// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.selection;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * A {@link SwitchingSelectionProvider} is used when a control has multiple
 * selection providers, only one of which is active at a time, and wants to
 * present the facade of a single selection provider to clients. The control
 * presents a {@link SwitchingSelectionProvider} as that facade. When the actual
 * selection provider changes, the control calls
 * {@link #setSelectionProvider(ISelectionProvider)}.
 * </p>
 *
 * <p>
 * It is possible for a {@link SwitchingSelectionProvider} to have no current
 * selection provider. This is the case when a
 * {@link SwitchingSelectionProvider} is first created or when you pass
 * <code>null</code> to {@link #setSelectionProvider(ISelectionProvider)}.
 * </p>
 *
 * <p>
 * {@link SwitchingSelectionProvider} is currently oriented toward
 * {@link IStructuredSelection}-based selection providers. When it has no
 * current selection provider, {@link StructuredSelection#EMPTY} will be used as
 * the current selection.
 * </p>
 *
 * <p>
 * This class is useful for implementing a view that has multiple child content
 * areas, each with a different selection provider. A view must register only a
 * single selection provider with the view site, and should not change that
 * selection provider once registered.
 * </p>
 *
 * @see ISelectionProvider
 * @see IStructuredSelection
 */
public class SwitchingSelectionProvider implements IPostSelectionProvider {
    private final BroadcastingSelectionChangedListener selectionChangedListener;
    private final BroadcastingSelectionChangedListener postSelectionChangedListener;

    private ISelectionProvider currentWrappedProvider;
    private final Object currentWrappedProviderLock = new Object();

    /**
     * Creates a new {@link SwitchingSelectionProvider} that initially has no
     * selection provider. To set a selection provider, call
     * {@link #setSelectionProvider(ISelectionProvider)}.
     */
    public SwitchingSelectionProvider() {
        selectionChangedListener = new BroadcastingSelectionChangedListener(this);
        postSelectionChangedListener = new BroadcastingSelectionChangedListener(this);
    }

    /**
     * Sets a new {@link ISelectionProvider} for this
     * {@link SwitchingSelectionProvider}. Any {@link ISelectionChangedListener}
     * s that are registered with this {@link SwitchingSelectionProvider} will
     * receive {@link SelectionChangedEvent}s. The new
     * {@link ISelectionProvider} may be <code>null</code>.
     *
     * @param selectionProvider
     *        a new {@link ISelectionProvider} to use (may be <code>null</code>)
     */
    public void setSelectionProvider(final ISelectionProvider selectionProvider) {
        synchronized (currentWrappedProviderLock) {
            if (currentWrappedProvider != null) {
                currentWrappedProvider.removeSelectionChangedListener(selectionChangedListener);
                if (currentWrappedProvider instanceof IPostSelectionProvider) {
                    ((IPostSelectionProvider) currentWrappedProvider).removePostSelectionChangedListener(
                        postSelectionChangedListener);
                }
            }

            currentWrappedProvider = selectionProvider;

            if (selectionProvider != null) {
                selectionProvider.addSelectionChangedListener(selectionChangedListener);
                if (selectionProvider instanceof IPostSelectionProvider) {
                    ((IPostSelectionProvider) selectionProvider).addPostSelectionChangedListener(
                        postSelectionChangedListener);
                }
            }

            final ISelection selection =
                (selectionProvider != null ? selectionProvider.getSelection() : StructuredSelection.EMPTY);
            selectionChangedListener.fireChildListeners(selection);
            postSelectionChangedListener.fireChildListeners(selection);
        }
    }

    /*
     * BEGIN: IPostSelectionProvider implementation
     */

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jface.viewers.IPostSelectionProvider#
     * addPostSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
        postSelectionChangedListener.addChildListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jface.viewers.IPostSelectionProvider#
     * removePostSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
        postSelectionChangedListener.removeChildListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListener.addChildListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#
     * removeSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListener.removeChildListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    @Override
    public ISelection getSelection() {
        synchronized (currentWrappedProviderLock) {
            if (currentWrappedProvider != null) {
                return currentWrappedProvider.getSelection();
            }
        }

        return StructuredSelection.EMPTY;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse
     * .jface.viewers.ISelection)
     */
    @Override
    public void setSelection(final ISelection selection) {
        synchronized (currentWrappedProviderLock) {
            if (currentWrappedProvider != null) {
                currentWrappedProvider.setSelection(selection);
            }
        }
    }

    /*
     * END: IPostSelectionProvider implementation
     */

    /**
     * A {@link BroadcastingSelectionChangedListener} serves a few purposes:
     * <ul>
     * <li>First, it is an {@link ISelectionChangedListener} and is added to the
     * current delegate {@link ISelectionProvider} that this
     * {@link SwitchingSelectionProvider} wraps.</li>
     * <li>Secondly, it holds any {@link ISelectionProvider}s that are
     * externally registered with this {@link SwitchingSelectionProvider}.</li>
     * <li>Finally, when it receives a {@link SelectionChangedEvent}, it
     * broadcasts a new {@link SelectionChangedEvent} to any child
     * {@link ISelectionChangedListener}s registered with it.</li>
     * </ul>
     */
    private static class BroadcastingSelectionChangedListener implements ISelectionChangedListener {
        private final SingleListenerFacade broadcastTargets;
        private final ISelectionProvider provider;

        public BroadcastingSelectionChangedListener(final ISelectionProvider provider) {
            this.provider = provider;
            broadcastTargets = new SingleListenerFacade(ISelectionChangedListener.class);
        }

        public void addChildListener(final ISelectionChangedListener listener) {
            broadcastTargets.addListener(listener);
        }

        public void removeChildListener(final ISelectionChangedListener listener) {
            broadcastTargets.removeListener(listener);
        }

        @Override
        public void selectionChanged(final SelectionChangedEvent event) {
            fireChildListeners(event.getSelection());
        }

        public void fireChildListeners(final ISelection selection) {
            final ISelectionChangedListener listener = (ISelectionChangedListener) broadcastTargets.getListener();
            listener.selectionChanged(new SelectionChangedEvent(provider, selection));
        }
    }
}
