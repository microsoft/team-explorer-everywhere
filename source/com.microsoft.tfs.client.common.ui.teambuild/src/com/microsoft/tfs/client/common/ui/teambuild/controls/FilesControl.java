// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.vc.FileTable;
import com.microsoft.tfs.client.common.ui.controls.vc.LocalPathCombo;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

/**
 * This is a reusable local file control, can pick either local file or folder
 */
public class FilesControl extends BaseControl {
    private final LocalPathCombo pathCombo;
    private final FileTable fileTable;
    private String filterPath;
    private final FilenameFilter filter;

    public FilesControl(final Composite parent, final int style, final FilenameFilter filter) {
        super(parent, style);

        this.filter = filter;
        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        pathCombo = new LocalPathCombo(this, LocalPathCombo.REFRESH);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(pathCombo);

        pathCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setFilterPath(pathCombo.getPath());
            }
        });

        fileTable = new FileTable(this, style);
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

    /**
     * Set initial filter path If retrictInPath is set to be true, only allow
     * user to select file under this path
     *
     * @param path
     * @param restrictInPath
     */
    public void setInitialPath(final String path, final boolean restrictInPath) {
        if (restrictInPath) {
            pathCombo.setRestrictingPath(path);
        }

        setFilterPath(path);
    }

    public void setFilterPath(final String path) {
        pathCombo.setPath(path);
        filterPath = path;

        if (filter != null) {
            fileTable.setFiles(new File(filterPath).listFiles(filter));
        } else {
            fileTable.setFiles(new File(filterPath).listFiles());
        }
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

    public File[] getSelectedFiles() {
        return fileTable.getSelectedFiles();
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
}
