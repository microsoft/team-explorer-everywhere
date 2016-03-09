// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.ui;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Sash;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataCategory;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderWrapper;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;

public class DataProviderTreeControl extends Composite {
    private final TreeViewer treeViewer;
    private final DataProviderOutputControl outputControl;

    public DataProviderTreeControl(
        final Composite parent,
        final int style,
        final DataProviderCollection dataProviderCollection) {
        super(parent, style);

        final FormLayout layout = new FormLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        final Sash sash = new Sash(this, SWT.VERTICAL);
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(33, 0);
        sash.setLayoutData(data);

        sash.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ((FormData) sash.getLayoutData()).left = new FormAttachment(0, e.x);
                sash.getParent().layout();
            }
        });

        treeViewer = new TreeViewer(this, SWT.BORDER);
        data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.right = new FormAttachment(sash, 0, SWT.LEFT);
        treeViewer.getTree().setLayoutData(data);

        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (!selection.isEmpty() && selection.getFirstElement() instanceof DataProviderWrapper) {
                    final DataProviderWrapper dataProvider = (DataProviderWrapper) selection.getFirstElement();
                    outputControl.setData(dataProvider);
                } else {
                    outputControl.setData(null);
                }
            }
        });

        outputControl = new DataProviderOutputControl(this, SWT.NONE);
        data = new FormData();
        data.left = new FormAttachment(sash, 0, SWT.RIGHT);
        data.top = new FormAttachment(treeViewer.getTree(), 0, SWT.TOP);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        outputControl.setLayoutData(data);

        treeViewer.setContentProvider(new ContentProvider());
        treeViewer.setLabelProvider(new LabelProvider());

        treeViewer.setInput(dataProviderCollection);

        treeViewer.setExpandedElements(dataProviderCollection.getSortedCategories());
    }

    public void refresh() {
        treeViewer.refresh();
    }

    private static class ContentProvider extends ContentProviderAdapter implements ITreeContentProvider {
        private DataProviderCollection dataProviderCollection;

        @Override
        public Object[] getChildren(final Object parentElement) {
            if (parentElement instanceof DataCategory) {
                final DataCategory dataCategory = (DataCategory) parentElement;
                return dataProviderCollection.getSortedProvidersForCategory(dataCategory);
            }
            return null;
        }

        @Override
        public Object getParent(final Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(final Object element) {
            return (element instanceof DataCategory);
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            dataProviderCollection = (DataProviderCollection) inputElement;
            return dataProviderCollection.getSortedCategories();
        }
    }

    private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {
        @Override
        public String getText(final Object element) {
            if (element instanceof DataCategory) {
                return ((DataCategory) element).getLabel();
            }
            if (element instanceof DataProviderWrapper) {
                final DataProviderWrapper dataProvider = (DataProviderWrapper) element;
                if (!dataProvider.isShouldExport()) {
                    final String messageFormat = Messages.getString("DataProviderTreeControl.InfoLabelTextFormat"); //$NON-NLS-1$
                    return MessageFormat.format(messageFormat, dataProvider.getDataProviderInfo().getLabel());
                }
                return dataProvider.getDataProviderInfo().getLabel();
            }
            return null;
        }
    }
}
