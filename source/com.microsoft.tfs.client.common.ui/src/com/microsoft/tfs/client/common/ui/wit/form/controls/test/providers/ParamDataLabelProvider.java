// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.wit.form.controls.test.ParamDataTable;

public class ParamDataLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        return null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        if (element instanceof ParamDataTable) {
            final ParamDataTable p = (ParamDataTable) element;
            final String[] values = p.getColumnValues();

            if (values.length > columnIndex && values[columnIndex] != null) {
                return values[columnIndex];
            }
        }

        return null;
    }

}