// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.folder;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.helpers.SystemColor;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemLabelProvider;

public class FolderControlLabelProvider extends TFSItemLabelProvider {
    @Override
    protected Color getForegroundColorForTFSItem(final TFSRepository repository, final TFSItem item) {
        if (!item.isLocal()) {
            final Display display = Display.getCurrent();

            if (display != null) {
                return SystemColor.getDimmedWidgetForegroundColor(Display.getCurrent());
            }
        }

        return super.getForegroundColorForTFSItem(repository, item);
    }
}
