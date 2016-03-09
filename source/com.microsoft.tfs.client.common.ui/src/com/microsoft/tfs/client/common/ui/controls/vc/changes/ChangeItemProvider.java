// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public abstract class ChangeItemProvider {
    private final TFSRepository repository;
    private final SingleListenerFacade listeners = new SingleListenerFacade(ChangeItemProviderListener.class);

    protected ChangeItemProvider(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;
    }

    public abstract ChangeItem[] getChangeItems();

    public abstract ChangeItemType getType();

    public final void addListener(final ChangeItemProviderListener listener) {
        listeners.addListener(listener);
    }

    public final void removeListener(final ChangeItemProviderListener listener) {
        listeners.removeListener(listener);
    }

    public void dispose() {

    }

    protected final void notifyOfUpdatedChangeItems() {
        final ChangeItemProviderEvent event = new ChangeItemProviderEvent(this);
        final ChangeItemProviderListener listener = (ChangeItemProviderListener) listeners.getListener();
        listener.onChangeItemsUpdated(event);
    }

    protected final TFSRepository getRepository() {
        return repository;
    }
}
