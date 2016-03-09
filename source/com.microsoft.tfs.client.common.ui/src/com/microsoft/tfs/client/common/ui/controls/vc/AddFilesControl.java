// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.io.File;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class AddFilesControl extends BaseControl {
    private final LocalPathCombo pathCombo;
    private final FileTable fileTable;
    private String filterPath;
    private ICommandExecutor commandExecutor;

    public AddFilesControl(final Composite parent, final int style) {
        this(parent, style, new CommandExecutor());
    }

    public AddFilesControl(final Composite parent, final int style, final ICommandExecutor executor) {
        super(parent, style);

        commandExecutor = executor;
        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        pathCombo = new LocalPathCombo(this, LocalPathCombo.REFRESH | LocalPathCombo.BROWSE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(pathCombo);

        pathCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setFilterPath(pathCombo.getPath());
            }
        });

        final Label tablePrompt = new Label(this, SWT.NONE);
        tablePrompt.setText(Messages.getString("AddFilesControl.TablePrompt")); //$NON-NLS-1$

        fileTable = new FileTable(this, SWT.MULTI | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().hHint(200).grab().fill().applyTo(fileTable);

        fileTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final File f = fileTable.getSelectedFile();

                if (f.isDirectory()) {
                    setFilterPath(f.getAbsolutePath());
                }
            }
        });
    }

    public void setFilterPath(final String path) {
        pathCombo.setPath(path);
        filterPath = path;
        final FilterItemsExistOnServerCommand command = new FilterItemsExistOnServerCommand(path, false);
        commandExecutor.execute(command);
        fileTable.setFiles(command.getLocalItems());
    }

    public void addPathChangeListener(final SelectionAdapter e) {
        pathCombo.addSelectionListener(e);
    }

    public void addDoubleClickListener(final IDoubleClickListener e) {
        fileTable.addDoubleClickListener(e);
    }

    public String getFilterPath() {
        return filterPath;
    }

    public String[] getSelectedFileNames() {
        final File[] files = fileTable.getSelectedFiles();
        final String[] names = new String[files.length];

        for (int i = 0; i < names.length; i++) {
            names[i] = files[i].getName();
        }
        return names;
    }

    public void addTableSelectionListener(final ISelectionChangedListener listener) {
        this.fileTable.getViewer().addSelectionChangedListener(listener);
    }

    public boolean fileSelected() {
        return fileTable.getSelectedElements().length > 0;
    }

    public void setCommandExecutor(final ICommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
}
