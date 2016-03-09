// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.microsoft.tfs.client.common.ui.Messages;

public class CollectionRunnable implements IRunnableWithProgress {
    private final List dataProviderWrappers = new ArrayList();
    private boolean cancelled;

    public CollectionRunnable(final DataProviderInfo[] dataProviders) {
        for (int i = 0; i < dataProviders.length; i++) {
            dataProviderWrappers.add(new DataProviderWrapper(dataProviders[i]));
        }
    }

    @Override
    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        cancelled = false;
        monitor.beginTask(
            Messages.getString("CollectionRunnable.CollectiongDataProgressText"), //$NON-NLS-1$
            dataProviderWrappers.size());

        for (final Iterator it = dataProviderWrappers.iterator(); it.hasNext();) {
            if (monitor.isCanceled()) {
                cancelled = true;
                break;
            }

            final DataProviderWrapper wrapper = (DataProviderWrapper) it.next();
            monitor.subTask(messageForDataProvider(wrapper));
            wrapper.populate();

            if (!wrapper.isAvailable()) {
                it.remove();
            }

            // Thread.sleep(200);

            monitor.worked(1);
        }

        monitor.done();
    }

    private String messageForDataProvider(final DataProviderWrapper wrapper) {
        final String messageFormat = Messages.getString("CollectionRunnable.ProviderDescriptionFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            wrapper.getDataProviderInfo().getCategory().getLabel(),
            wrapper.getDataProviderInfo().getLabel());
    }

    public DataProviderWrapper[] getDataProviderWrappers() {
        return (DataProviderWrapper[]) dataProviderWrappers.toArray(
            new DataProviderWrapper[dataProviderWrappers.size()]);
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
