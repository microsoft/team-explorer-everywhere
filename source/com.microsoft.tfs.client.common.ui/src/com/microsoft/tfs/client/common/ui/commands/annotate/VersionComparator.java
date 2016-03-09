// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.annotate;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public class VersionComparator {

    public static Block[] extractChangeBlocks(final Map tempCopyMap, final Shell shell) {
        final Changeset[] c = (Changeset[]) tempCopyMap.keySet().toArray(new Changeset[0]);
        final String[] t = (String[]) tempCopyMap.values().toArray(new String[0]);
        final ArrayList v = new ArrayList();
        for (int i = 0; i < c.length - 1; i++) {
            try {
                v.add(new Version(c[i], c[i + 1], t[i], t[i + 1]));
            } catch (final Exception e) {
                MessageDialog.openError(
                    shell,
                    Messages.getString("VersionComparator.ErrorDialogTitle"), //$NON-NLS-1$
                    e.getMessage());
            }
        }
        try {
            v.add(new Version(c[c.length - 1], t[c.length - 1]));
        } catch (final Exception e) {
            MessageDialog.openError(shell, Messages.getString("VersionComparator.ErrorDialogTitle"), e.getMessage()); //$NON-NLS-1$
        }
        for (int i = v.size() - 1; i > 0; i--) {
            ((Version) v.get(i - 1)).fold((Version) v.get(i));
        }

        return ((Block[]) ((Version) v.get(0)).blocks.toArray(new Block[0]));
    }

}
