// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;

public class WorkItemListLabelProvider extends LabelProvider implements ITableLabelProvider {
    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        return null;
    }

    // this method supports sorting
    // public String getText(Object element)
    // {
    // return ((AFileType) element).getName();
    // }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        final WorkItemCheckinInfo workItemInfo = (WorkItemCheckinInfo) element;
        switch (columnIndex) {
            case 0:
                return workItemInfo.getWorkItem().getType().getName();
            case 1:
                return String.valueOf(workItemInfo.getWorkItem().getFields().getID());
            case 2:
                return (String) workItemInfo.getWorkItem().getFields().getField(
                    CoreFieldReferenceNames.TITLE).getValue();
            case 3:
                return (String) workItemInfo.getWorkItem().getFields().getField(
                    CoreFieldReferenceNames.STATE).getValue();
            case 4:
                return workItemInfo.getActionString();
        }
        return null;
    }
}
