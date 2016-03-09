// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.runnable;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class BusyIndicatorRunnableContext implements IRunnableContext {
    private final Display display;

    public BusyIndicatorRunnableContext(final Display display) {
        this.display = display;
    }

    @Override
    public void run(final boolean fork, final boolean cancelable, final IRunnableWithProgress runnable)
        throws InvocationTargetException,
            InterruptedException {
        BusyIndicator.showWhile(display, new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run(new NullProgressMonitor());
                } catch (final InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
