// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataCategory;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderWrapper;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;

public class OptionalExportsControl extends Composite {
    private final CheckboxTreeViewer treeViewer;

    public OptionalExportsControl(
        final Composite parent,
        final int style,
        final DataProviderCollection dataProviderCollection,
        final DataProviderTreeControl dataProviderTreeControl) {
        super(parent, style);

        final GridLayout layout = new GridLayout(1, false);
        setLayout(layout);

        final Label label = new Label(this, SWT.WRAP);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        label.setText(Messages.getString("OptionalExportsControl.SummaryLabelText")); //$NON-NLS-1$

        treeViewer = new CheckboxTreeViewer(this, SWT.BORDER);
        treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        treeViewer.setContentProvider(new ContentProvider());
        treeViewer.setLabelProvider(new LabelProvider());

        treeViewer.setInput(dataProviderCollection);

        treeViewer.setExpandedElements(dataProviderCollection.getSortedCategoriesWithOptionalExportProviders());

        final List checkedElements = new ArrayList();
        final DataCategory[] categories = dataProviderCollection.getSortedCategoriesWithOptionalExportProviders();
        for (int i = 0; i < categories.length; i++) {
            final DataProviderWrapper[] dataProviders =
                dataProviderCollection.getSortedOptionalExportProvidersForCategory(categories[i]);
            for (int j = 0; j < dataProviders.length; j++) {
                if (dataProviders[j].isShouldExport()) {
                    checkedElements.add(dataProviders[j]);
                }
            }
        }

        treeViewer.setCheckedElements(checkedElements.toArray());

        treeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                if (event.getElement() instanceof DataCategory) {
                    treeViewer.getTree().setRedraw(false);

                    final DataCategory category = (DataCategory) event.getElement();
                    final DataProviderWrapper[] children =
                        dataProviderCollection.getSortedOptionalExportProvidersForCategory(category);
                    for (int i = 0; i < children.length; i++) {
                        children[i].setShouldExport(event.getChecked());
                        treeViewer.setChecked(children[i], event.getChecked());
                    }

                    treeViewer.getTree().setRedraw(true);
                } else {
                    final DataProviderWrapper dataProvider = (DataProviderWrapper) event.getElement();
                    dataProvider.setShouldExport(event.getChecked());

                    final DataCategory parentCategory = dataProvider.getDataProviderInfo().getCategory();
                    final DataProviderWrapper[] siblings =
                        dataProviderCollection.getSortedOptionalExportProvidersForCategory(parentCategory);
                    boolean allChildrenChecked = true;
                    for (int i = 0; i < siblings.length; i++) {
                        if (!siblings[i].isShouldExport()) {
                            allChildrenChecked = false;
                            break;
                        }
                    }
                    treeViewer.setChecked(parentCategory, allChildrenChecked);
                }

                dataProviderTreeControl.refresh();
            }
        });
    }

    private static class ContentProvider extends ContentProviderAdapter implements ITreeContentProvider {
        private DataProviderCollection dataProviderCollection;

        @Override
        public Object getParent(final Object element) {
            return null;
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            dataProviderCollection = (DataProviderCollection) inputElement;
            return dataProviderCollection.getSortedCategoriesWithOptionalExportProviders();
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            return dataProviderCollection.getSortedOptionalExportProvidersForCategory((DataCategory) parentElement);
        }

        @Override
        public boolean hasChildren(final Object element) {
            return (element instanceof DataCategory);
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
                return dataProvider.getDataProviderInfo().getLabel();
            }
            return null;
        }
    }
}
