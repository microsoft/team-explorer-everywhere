// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

public class ServerItemControlUtils {
    private final static Log log = LogFactory.getLog(ServerItemControlUtils.class);

    /**
     * Sets the initial selection of the given ServerItemControl to the given
     * server path. If the given path is not found on the server, will set the
     * selection to the nearest parent path. (ie, if you provide
     * $/proj/a/example.txt as the selection path and $/proj/a does not exist,
     * then $/proj will be given selection and expanded.)
     *
     * @param path
     *        The server path to set the control selection to
     * @param control
     *        The {@link ServerItemControl} to set selection on
     * @return true if the selection was set to the given path or a parent,
     *         false otherwise
     */
    public static boolean setInitialSelection(String path, final ServerItemControl control) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        if (path == null) {
            return false;
        }

        path = path.trim();

        if (path.length() == 0) {
            return false;
        }

        /*
         * There is ambiguity here. The path (which could come from user input)
         * may refer to a folder or a file. First we'll try it as a folder.
         */

        try {
            path = ServerPath.canonicalize(path);
        } catch (final ServerPathFormatException e) {
            log.warn(MessageFormat.format("Asked to set path to non server path {0}", path)); //$NON-NLS-1$
            return false;
        }

        TypedServerItem item = new TypedServerItem(path, ServerItemType.FOLDER);
        control.setSelectedItem(item);
        if (control.getSelectedItem() != null) {
            return true;
        }

        /*
         * It wasn't a folder, so next try it as a file.
         */
        item = new TypedServerItem(path, ServerItemType.FILE);
        control.setSelectedItem(item);
        if (control.getSelectedItem() != null) {
            return true;
        }

        /*
         * There was no item in the tree at the specified path. Try to find a
         * close item by successively trying the item's ancestors. (Note: open
         * the children in this case to match the Visual Studio behavior when
         * the exact path is not found.)
         */
        item = item.getParent();
        while (item != null && item.getType() != ServerItemType.ROOT) {
            control.setSelectedItem(item);

            final TypedServerItem selectedItem = control.getSelectedItem();
            if (selectedItem != null) {
                if (ServerItemType.isFolder(selectedItem.getType()) && control instanceof ServerItemTreeControl) {
                    ((ServerItemTreeControl) control).expandItem(selectedItem, 1);
                }

                return true;
            }
            item = item.getParent();
        }

        /*
         * Nothing left to do. Neither the path nor any of the path's parents
         * were in the tree.
         */
        return false;
    }
}
