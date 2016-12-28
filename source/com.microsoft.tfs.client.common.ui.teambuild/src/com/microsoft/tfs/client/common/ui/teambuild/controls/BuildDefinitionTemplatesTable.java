// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionTemplate;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.util.StringUtil;

public class BuildDefinitionTemplatesTable extends TableControl {
    private static final String NAME_COLUMN_ID = "name"; //$NON-NLS-1$
    private static final String CATEGORY_COLUMN_ID = "category"; //$NON-NLS-1$
    private static final String DESCRIPTION_COLUMN_ID = "descr"; //$NON-NLS-1$

    private static final String EMPTY_TEMPLATE_ID = "blank"; //$NON-NLS-1$
    private static final int NAME_COLUMN_NUM = 0;
    private static final int CATEGORY_COLUMN_NUM = 1;
    private static final int DESCRIPTION_COLUMN_NUM = 2;
    private final TableColumnData[] columnData;

    public BuildDefinitionTemplatesTable(final Composite parent, final int style) {
        super(parent, (style | SWT.FULL_SELECTION) & ~SWT.MULTI & ~SWT.CHECK, BuildDefinitionTemplate.class, null);

        columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("BuildDefinitionTemplatesTable.NameColumn"), //$NON-NLS-1$
                20,
                0.10F,
                NAME_COLUMN_ID),
            new TableColumnData(
                Messages.getString("BuildDefinitionTemplatesTable.CategoryColumn"), //$NON-NLS-1$
                20,
                0.10F,
                CATEGORY_COLUMN_ID),
            new TableColumnData(
                Messages.getString("BuildDefinitionTemplatesTable.DescriptionColumn"), //$NON-NLS-1$
                80,
                0.80F,
                DESCRIPTION_COLUMN_ID),
        };

        setupTable(true, false, columnData);
        setUseViewerDefaults();
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final BuildDefinitionTemplate template = (BuildDefinitionTemplate) element;

        if (columnPropertyName.equals(NAME_COLUMN_ID)) {
            return template.getId().equals(EMPTY_TEMPLATE_ID) ? StringUtil.EMPTY : getColumnText(template.getName());
        } else if (columnPropertyName.equals(CATEGORY_COLUMN_ID)) {
            return getColumnText(template.getCategory());
        } else {
            return getColumnText(template.getDescription());
        }
    }

    private String getColumnText(final String s) {
        if (StringUtil.isNullOrEmpty(s)) {
            return StringUtil.EMPTY;
        } else {
            return s;
        }
    }

    /**
     * {@inheritDoc}
     */
    /*
     * @Override protected Image getColumnImage(Object element, String
     * columnPropertyName) { BuildDefinitionTemplate template =
     * (BuildDefinitionTemplate) element;
     *
     * if (columnPropertyName.equals(NAME_COLUMN_ID)) { final UUID iconTaskId =
     * template.getIconTaskId();
     *
     * if (iconTaskId == null) { return null; }
     *
     * final BufferedInputStream iconStream = new
     * BufferedInputStream(taskAgentClient.getTaskIcon(iconTaskId, null));
     *
     * if (iconStream == null) { return null; }
     *
     * try { ImageData imageData = new ImageData(iconStream); ImageDescriptor
     * imageDescriptor = ImageDescriptor.createFromImageData(imageData); return
     * imageDescriptor.createImage(false); } catch (SWTException e) { if (e.code
     * != SWT.ERROR_INVALID_IMAGE) { throw e; // fall through otherwise } }
     * finally { try { iconStream.close(); } catch (IOException e) { } } }
     *
     * return null; }
     */
    public void setTemplates(final List<BuildDefinitionTemplate> templates) {
        setElements(templates.toArray(new BuildDefinitionTemplate[templates.size()]));
        for (final BuildDefinitionTemplate template : templates) {
            if (template.getId().equals(EMPTY_TEMPLATE_ID)) {
                setSelectedElement(template);
                break;
            }
        }
    }

    public BuildDefinitionTemplate getSelectedTemplate() {
        final BuildDefinitionTemplate[] selection = (BuildDefinitionTemplate[]) getSelectedElements();
        return selection == null || selection.length == 0 ? null : selection[0];
    }

    public void setColumnWidth() {
        int maxNameWidth = 0; // getViewer().getTable().getColumn(NAME_COLUMN_NUM).getWidth();
        int maxCategoryWidth = 0; // getViewer().getTable().getColumn(CATEGORY_COLUMN_NUM).getWidth();
        int maxDescriptionWidth = 0; // getViewer().getTable().getColumn(DESCRIPTION_COLUMN_NUM).getWidth();

        final GC gc = new GC(getViewer().getTable());

        for (final Object element : getElements()) {
            maxNameWidth = Math.max(maxNameWidth, gc.stringExtent(getColumnText(element, NAME_COLUMN_ID)).x);
            maxCategoryWidth =
                Math.max(maxCategoryWidth, gc.stringExtent(getColumnText(element, CATEGORY_COLUMN_ID)).x);
            maxDescriptionWidth =
                Math.max(maxDescriptionWidth, gc.stringExtent(getColumnText(element, DESCRIPTION_COLUMN_ID)).x);
        }

        gc.dispose();

        getViewer().getTable().getColumn(NAME_COLUMN_NUM).setWidth(maxNameWidth + 30);
        getViewer().getTable().getColumn(CATEGORY_COLUMN_NUM).setWidth(maxCategoryWidth + 30);
        getViewer().getTable().getColumn(DESCRIPTION_COLUMN_NUM).setWidth(maxDescriptionWidth + 30);
        getViewer().getTable().layout();
    }
}
