// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.ui;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.Adapters;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataCategory;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderWrapper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;

public class DirectoriesControl extends Composite {
    public DirectoriesControl(
        final Composite parent,
        final int style,
        final DataProviderCollection dataProviderCollection) {
        super(parent, style);

        final GridLayout layout = new GridLayout(1, false);
        setLayout(layout);

        final Map directoryProviders = dataProviderCollection.getDirectoryProvidersMap();

        final List categories = new ArrayList(directoryProviders.keySet());
        Collections.sort(categories);

        for (final Iterator it = categories.iterator(); it.hasNext();) {
            final DataCategory dataCategory = (DataCategory) it.next();

            final Composite group = createDirectoryGroup(dataCategory, directoryProviders);

            group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        }
    }

    private Composite createDirectoryGroup(final DataCategory dataCategory, final Map directoryProviders) {
        final Group group = new Group(this, SWT.NONE);
        group.setText(dataCategory.getLabel());

        final GridLayout layout = new GridLayout(4, false);
        group.setLayout(layout);

        final List list = (List) directoryProviders.get(dataCategory);
        Collections.sort(list);

        for (final Iterator providerIt = list.iterator(); providerIt.hasNext();) {
            final DataProviderWrapper dataProvider = (DataProviderWrapper) providerIt.next();

            final File directory = (File) Adapters.get(dataProvider.getData(), File.class);

            final String messageFormat = Messages.getString("DirectoriesControl.InfoLabelTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, dataProvider.getDataProviderInfo().getLabel());

            final Label providerLabel = new Label(group, SWT.NONE);
            providerLabel.setText(message);

            final Text text = new Text(group, SWT.BORDER);
            text.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            text.setEditable(false);
            text.setText(directory.getAbsolutePath());

            final Button copyButton = new Button(group, SWT.NONE);
            copyButton.setText(Messages.getString("DirectoriesControl.CopyClipboardButtonText")); //$NON-NLS-1$
            copyButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    UIHelpers.copyToClipboard(directory.getAbsolutePath());
                }
            });

            final Button openButton = new Button(group, SWT.NONE);
            openButton.setText(Messages.getString("DirectoriesControl.OpenButtonText")); //$NON-NLS-1$
            openButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    Launcher.launch(directory.getAbsolutePath());
                }
            });

            if (!directory.exists()) {
                SWTUtil.createHorizontalGridLayoutSpacer(group, 1);

                final Label errorLabel = new Label(group, SWT.NONE);
                errorLabel.setText(Messages.getString("DirectoriesControl.DirectoryDoesNotExist")); //$NON-NLS-1$

                SWTUtil.createHorizontalGridLayoutSpacer(group, 2);
            }
        }

        return group;
    }
}
