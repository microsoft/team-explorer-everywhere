// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.annotate;

import java.io.FileInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

import com.microsoft.tfs.client.common.commands.vc.GetVersionedItemToTempLocationCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;

public class TFSItemReferenceProvider implements IQuickDiffReferenceProvider {

    public static final String ID = "com.microsoft.tfs.client.common.ui.commands.annotate.TfsItemReferenceProvider"; //$NON-NLS-1$

    private String id;

    private IDocument reference = null;

    private IFile editorFile = null;

    public TFSItemReferenceProvider() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IDocument getReference(final IProgressMonitor monitor) throws CoreException {
        if (reference == null) {
            try {
                getWorkspaceItem(monitor);
            } catch (final Exception e) {
                throw new CoreException(
                    new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getMessage(), e));
            }
        }
        return reference;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setActiveEditor(final ITextEditor editor) {
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            final IFileEditorInput f = (IFileEditorInput) editor.getEditorInput();
            editorFile = f.getFile();
        }

    }

    @Override
    public void setId(final String id) {
        this.id = id;
    }

    private void getWorkspaceItem(final IProgressMonitor monitor) throws Exception {
        final TFSRepository repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        final GetVersionedItemToTempLocationCommand cmd = new GetVersionedItemToTempLocationCommand(
            repository,
            editorFile.getLocation().toOSString(),
            new WorkspaceVersionSpec(repository.getWorkspace()));

        final IStatus status = cmd.run(monitor);
        if (status != null && status.isOK()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(cmd.getTempLocation());
                final byte[] contents = new byte[in.available()];
                in.read(contents);
                reference = new Document(new String(contents));
            } finally {
                in.close();
            }
        }
    }

}
