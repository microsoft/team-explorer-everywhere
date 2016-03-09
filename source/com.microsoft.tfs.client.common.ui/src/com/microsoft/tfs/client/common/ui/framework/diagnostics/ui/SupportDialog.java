// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.ui;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.InternalSupportUtils;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderWrapper;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportProvider;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;

public class SupportDialog extends BaseDialog {
    private static final int EXPORT_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int REFRESH_ID = IDialogConstants.CLIENT_ID + 2;

    private final SupportProvider supportProvider;
    private DataProviderCollection dataProviderCollection;
    private TabFolder tabFolder;

    private DataProviderTreeControl dataProviderTreeControl;

    public SupportDialog(
        final Shell parentShell,
        final SupportProvider supportProvider,
        final DataProviderCollection dataProviderCollection,
        final ClassLoader classLoader,
        final Map contextObjects) {
        super(parentShell);

        this.supportProvider = supportProvider;
        this.dataProviderCollection = dataProviderCollection;
        setOptionIncludeDefaultButtons(false);
        addButtonDescription(EXPORT_ID, Messages.getString("SupportDialog.ExportButtonText"), false); //$NON-NLS-1$
        addButtonDescription(REFRESH_ID, Messages.getString("SupportDialog.RefreshButtonText"), false); //$NON-NLS-1$
        addButtonDescription(IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Point defaultComputeInitialSize() {
        getParentShell();
        final Rectangle parentBounds = getParentShell().getBounds();

        final int width = Math.max(((int) .75 * parentBounds.width), 650);
        return new Point(width, (int) (.75 * parentBounds.height));
    }

    public void refresh() {
        final DataProviderCollection newCollection = InternalSupportUtils.createDataProviderCollection(getShell());
        if (newCollection == null) {
            /*
             * cancelled
             */
            return;
        }

        dataProviderCollection = newCollection;

        final TabItem[] items = tabFolder.getItems();
        for (int i = 0; i < items.length; i++) {
            items[i].dispose();
        }

        createUI();
    }

    private void createUI() {
        createSupportTab();
        createDataProviderTreeTab();
        createOptionalExportsTab();
        createDataProviderTabs();
        createDirectoriesTab();
    }

    private void createDataProviderTabs() {
        final DataProviderWrapper[] dataProviders = dataProviderCollection.getOwnTabDataProviders();
        for (int i = 0; i < dataProviders.length; i++) {
            createTabForDataProvider(dataProviders[i]);
        }
    }

    @Override
    protected String provideDialogTitle() {
        return supportProvider.getDialogTitle();
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == EXPORT_ID) {
            export();
        } else if (buttonId == REFRESH_ID) {
            refresh();
        }
    }

    private void export() {
        InternalSupportUtils.promptAndPerformExport(getShell(), dataProviderCollection);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        dialogArea.setLayout(layout);

        tabFolder = new TabFolder(dialogArea, SWT.NONE);

        createUI();
    }

    private void createTabForDataProvider(final DataProviderWrapper dataProvider) {
        final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(dataProvider.getDataProviderInfo().getLabel());

        final Composite outputComposite = new Composite(tabFolder, SWT.NONE);

        final FillLayout outputLayout = new FillLayout();
        outputLayout.marginWidth = getHorizontalMargin();
        outputLayout.marginHeight = getVerticalMargin();
        outputComposite.setLayout(outputLayout);

        final DataProviderOutputControl outputControl = new DataProviderOutputControl(outputComposite, SWT.NONE);
        outputControl.setData(dataProvider);

        tabItem.setControl(outputComposite);
    }

    private void createDirectoriesTab() {
        final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(Messages.getString("SupportDialog.DirectoryTabItemText")); //$NON-NLS-1$

        final Composite directoriesComposite = new Composite(tabFolder, SWT.NONE);

        final FillLayout directoriesLayout = new FillLayout();
        directoriesLayout.marginWidth = getHorizontalMargin();
        directoriesLayout.marginHeight = getVerticalMargin();
        directoriesComposite.setLayout(directoriesLayout);

        new DirectoriesControl(directoriesComposite, SWT.NONE, dataProviderCollection);
        tabItem.setControl(directoriesComposite);
    }

    private void createDataProviderTreeTab() {
        final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(Messages.getString("SupportDialog.DataTabItemText")); //$NON-NLS-1$

        final Composite dataProviderComposite = new Composite(tabFolder, SWT.NONE);

        final FillLayout dataProviderLayout = new FillLayout();
        dataProviderLayout.marginWidth = getHorizontalMargin();
        dataProviderLayout.marginHeight = getVerticalMargin();
        dataProviderComposite.setLayout(dataProviderLayout);

        dataProviderTreeControl = new DataProviderTreeControl(dataProviderComposite, SWT.NONE, dataProviderCollection);
        tabItem.setControl(dataProviderComposite);
    }

    private void createOptionalExportsTab() {
        if (dataProviderCollection.hasOptionalExportDataProviders()) {
            final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
            tabItem.setText(Messages.getString("SupportDialog.OptionalExportsTablItemText")); //$NON-NLS-1$

            final Composite optionalComposite = new Composite(tabFolder, SWT.NONE);

            final FillLayout optionalLayout = new FillLayout();
            optionalLayout.marginWidth = getHorizontalMargin();
            optionalLayout.marginHeight = getVerticalMargin();
            optionalComposite.setLayout(optionalLayout);

            new OptionalExportsControl(optionalComposite, SWT.NONE, dataProviderCollection, dataProviderTreeControl);
            tabItem.setControl(optionalComposite);
        }
    }

    private void createSupportTab() {
        final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(Messages.getString("SupportDialog.SupportTabItemText")); //$NON-NLS-1$

        final Composite supportComposite = new Composite(tabFolder, SWT.NONE);

        final FillLayout supportLayout = new FillLayout();
        supportLayout.marginWidth = getHorizontalMargin();
        supportLayout.marginHeight = getVerticalMargin();
        supportComposite.setLayout(supportLayout);

        new SupportControl(supportComposite, SWT.NONE, supportProvider);
        tabItem.setControl(supportComposite);
    }
}
