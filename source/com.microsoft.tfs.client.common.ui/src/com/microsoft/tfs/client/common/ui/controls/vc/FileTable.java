// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.io.File;
import java.text.Collator;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.viewer.FolderFileLabelProvider;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.CollatorFactory;
import com.microsoft.tfs.util.Platform;

public class FileTable extends TableControl {
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$
    private static final String DATE_COLUMN_ID = "date"; //$NON-NLS-1$
    private static final String TYPE_COLUMN_ID = "type"; //$NON-NLS-1$
    private static final String SIZE_COLUMN_ID = "size"; //$NON-NLS-1$

    public FileTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public FileTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, File.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("AddFilesTable.ColumnHeaderNameText"), 150, 0.3f, NAME_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("AddFilesTable.ColumnHeaderDateText"), 150, 0.3f, DATE_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("AddFilesTable.ColumnHeaderTypeText"), 100, 0.2f, TYPE_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("AddFilesTable.ColumnHeaderSizeText"), 100, 0.2f, SIZE_COLUMN_ID) //$NON-NLS-1$
        };
        setupTable(true, false, columnData);

        setUseViewerDefaults();
        getViewer().setLabelProvider(new FileLabelProvider());

        ((TableViewerSorter) getViewer().getSorter()).setComparator(NAME_COLUMN_ID, new FileComparator());
    }

    public void setFiles(final File[] files) {
        setElements(files);
    }

    public File[] getFiles() {
        return (File[]) getElements();
    }

    public void setSelectedFiles(final File[] files) {
        setSelectedElements(files);
    }

    public void setSelectedFile(final File file) {
        setSelectedElement(file);
    }

    public File[] getSelectedFiles() {
        return (File[]) getSelectedElements();
    }

    public File getSelectedFile() {
        return (File) getSelectedElement();
    }

    private static class FileLabelProvider extends FolderFileLabelProvider implements ITableLabelProvider {
        private static HashMap<String, Boolean> symbolicLinkMap = new HashMap<String, Boolean>();

        private final DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

        private boolean isSymbolicLink(final String filename) {
            Boolean cached;
            boolean isSymbolicLink = false;
            FileSystemAttributes attributes;

            if (!Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
                return false;
            }

            if ((cached = symbolicLinkMap.get(filename)) != null) {
                return cached.booleanValue();
            }

            try {
                attributes = FileSystemUtils.getInstance().getAttributes(filename);
                isSymbolicLink = attributes.isSymbolicLink();
            } catch (final Exception e) {
                /* Assume permissions problem */
            }

            symbolicLinkMap.put(filename, isSymbolicLink);

            return isSymbolicLink;
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIdx) {
            if (columnIdx == 0) {
                final File file = (File) element;

                if (file.isDirectory()) {
                    return getImageForFolder();
                }

                return getImageForFile(file.getName());
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIdx) {
            final File file = (File) element;

            switch (columnIdx) {
                case 0:
                    return file.getName();
                case 1:
                    return dateFormat.format(new Date(file.lastModified()));
                case 2:
                    if (isSymbolicLink(file.getAbsolutePath())) {
                        return getSymbolicLinkDescription();
                    }

                    if (file.isDirectory()) {
                        return getFolderDescription();
                    }

                    return getFileTypeDescription(file.getName());
                case 3:
                    if (!file.isDirectory()) {
                        return getFileSize(file.length());
                    }

                    return ""; //$NON-NLS-1$
            }

            return ""; //$NON-NLS-1$
        }
    }

    private static class FileComparator implements Comparator<File> {
        private final Collator collator = CollatorFactory.getCaseInsensitiveCollator();

        @Override
        public int compare(final File a, final File b) {
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            }

            return collator.compare(a.getName(), b.getName());
        }
    }
}
