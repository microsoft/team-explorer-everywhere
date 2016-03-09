// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * <p>
 * A utility class for creating label providers that behave safely under Eclipse
 * 3.0.
 * </p>
 * <p>
 * Under Eclipse 3.0, label providers that return <code>null</code> from a
 * <code>getText()</code> method will not work correctly with all viewers in all
 * situations. This flaw appears to have been corrected in later versions of
 * JFace.
 * </p>
 * <p>
 * Instead of having to write null guards into every label provider, you can
 * just call <code>SafeLabelProvider.wrap()</code> to wrap any label provider
 * with a safe version that will never return null from a <code>getText()</code>
 * method.
 * </p>
 */
public class SafeLabelProvider {
    /**
     * <p>
     * Wraps the given label provider with a delegating label provider that will
     * never return null from a <code>getText()</code> method.
     * </p>
     * <p>
     * The runtime type of the return of this method depends on the runtime type
     * of the argument to the method:
     * <ul>
     * <li>If the argument implements <code>ITableLabelProvider</code>, the
     * returned object will be castable to <code>ITableLabelProvider</code>,
     * otherwise</li>
     * <li>If the argument implements <code>ILabelProvider</code>, the returned
     * object will be castable to <code>ILabelProvider</code>, otherwise</li>
     * <li>The returned object will be of type <code>IBaseLabelProvider</code>
     * </li>
     * </ul>
     * </p>
     *
     * @param provider
     *        a label provider to wrap
     * @return an Eclipse 3.0-safe label provider
     */
    public static IBaseLabelProvider wrap(final IBaseLabelProvider provider) {
        if (provider instanceof ITableLabelProvider) {
            return new TableLabelProviderWrapper((ITableLabelProvider) provider);
        }
        if (provider instanceof ILabelProvider) {
            return new LabelProviderWrapper((ILabelProvider) provider);
        }
        return new BaseLabelProviderWrapper(provider);
    }

    private static class BaseLabelProviderWrapper implements IBaseLabelProvider {
        private final IBaseLabelProvider delegate;

        public BaseLabelProviderWrapper(final IBaseLabelProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public void addListener(final ILabelProviderListener listener) {
            delegate.addListener(listener);
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }

        @Override
        public boolean isLabelProperty(final Object element, final String property) {
            return delegate.isLabelProperty(element, property);
        }

        @Override
        public void removeListener(final ILabelProviderListener listener) {
            delegate.removeListener(listener);
        }
    }

    private static class LabelProviderWrapper extends BaseLabelProviderWrapper implements ILabelProvider {
        private final ILabelProvider delegate;

        public LabelProviderWrapper(final ILabelProvider delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public Image getImage(final Object element) {
            return delegate.getImage(element);
        }

        @Override
        public String getText(final Object element) {
            return makeTextSafe(delegate.getText(element));
        }
    }

    private static class TableLabelProviderWrapper extends BaseLabelProviderWrapper implements ITableLabelProvider {
        private final ITableLabelProvider delegate;

        public TableLabelProviderWrapper(final ITableLabelProvider delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return delegate.getColumnImage(element, columnIndex);
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            return makeTextSafe(delegate.getColumnText(element, columnIndex));
        }
    }

    private static String makeTextSafe(final String input) {
        return (input == null ? "" : input); //$NON-NLS-1$
    }
}
