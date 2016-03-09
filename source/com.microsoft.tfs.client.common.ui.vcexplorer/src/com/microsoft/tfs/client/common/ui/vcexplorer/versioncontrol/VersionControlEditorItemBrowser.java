// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.helpers.ServerItemBrowser;
import com.microsoft.tfs.util.Check;

public class VersionControlEditorItemBrowser implements ServerItemBrowser {
    private static final Log log = LogFactory.getLog(VersionControlEditorItemBrowser.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean browse(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        VersionControlEditor editor;

        /* Try to find the version control editor */
        try {
            /**
             * Opening an editor using the {@link VersionControlEditorInput}
             * will guarantee that we will not open multiple source control
             * explorers.
             */
            final IEditorPart editorPart =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
                    new VersionControlEditorInput(),
                    VersionControlEditor.ID);

            if (!(editorPart instanceof VersionControlEditor)) {
                log.warn(
                    MessageFormat.format("Opened editor {0} but received a {1}", VersionControlEditor.ID, editorPart)); //$NON-NLS-1$
                return false;
            }

            editor = (VersionControlEditor) editorPart;
        } catch (final PartInitException e) {
            log.warn(MessageFormat.format("Could not open version control editor for item {0}", serverPath), e); //$NON-NLS-1$
            return false;
        }

        editor.setSelectedFolder(new ServerItemPath(serverPath));

        return true;
    }
}
