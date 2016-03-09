// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class TFSItemUtils {
    /**
     * Obtain an array of Strings - the full paths of the array of TFSItems
     * passed in. This is a convenience method for a common operation. See also
     * getItemSpecs() if recursion is an issue.
     *
     * @param items
     *        TFSItems to get the full paths of
     * @return the full paths of the items
     */
    public static String[] getFullPaths(final TFSItem[] items) {
        final String[] s = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            s[i] = items[i].getFullPath();
        }
        return s;
    }

    /**
     * Obtain an array of Strings - the full paths of the array of TFSITem
     * objects passed in. This is a convenience method for a common operation.
     * See also getItemSpecs() if recursion is an issue.
     *
     * @param tfsItems
     *        TFSItems to get the full paths of
     * @return the full paths of the items
     */
    public static String[] getFullPaths(final Object[] tfsItems) {
        final String[] s = new String[tfsItems.length];
        for (int i = 0; i < tfsItems.length; i++) {
            s[i] = ((TFSItem) tfsItems[i]).getFullPath();
        }
        return s;
    }

    /**
     * Obtain an array of {@link ItemSpec} from TFSItems. Obeys Recursion (Full
     * for folders, None for files).
     *
     * @param items
     *        TFSItems to get {@link ItemSpec} for
     * @return the resultant {@link ItemSpec}
     */
    public static ItemSpec[] getItemSpecs(final TFSItem[] items) {
        final ItemSpec[] itemSpecs = new ItemSpec[items.length];

        for (int i = 0; i < items.length; i++) {
            itemSpecs[i] = new ItemSpec(
                items[i].getFullPath(),
                items[i] instanceof TFSFolder ? RecursionType.FULL : RecursionType.NONE,
                0);
        }

        return itemSpecs;
    }

    /**
     * Obtain an array of {@link ItemSpec} from TFSItems. Obeys Recursion (Full
     * for folders, None for files).
     *
     * @param items
     *        TFSItems to get {@link ItemSpec} for
     * @return the resultant {@link ItemSpec}
     */
    public static TypedItemSpec[] getTypedItemSpecs(final TFSItem[] items) {
        final TypedItemSpec[] itemSpecs = new TypedItemSpec[items.length];

        for (int i = 0; i < items.length; i++) {
            itemSpecs[i] = new TypedItemSpec(
                items[i].getFullPath(),
                items[i] instanceof TFSFolder ? RecursionType.FULL : RecursionType.NONE,
                0,
                items[i] instanceof TFSFile ? ItemType.FILE : ItemType.FOLDER);
        }

        return itemSpecs;
    }
}
