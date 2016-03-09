// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.viewer;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import com.microsoft.tfs.client.common.commands.vc.LaunchExternalViewToolCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.helpers.FileViewer;
import com.microsoft.tfs.client.common.ui.prefs.ExternalToolPreferenceKey;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.MementoRepository;

public class EclipseFileViewer implements FileViewer {
    private static final Log log = LogFactory.getLog(EclipseFileViewer.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean viewFile(final String path, final IWorkbenchPage page, final boolean inModalContext) {
        /*
         * First see if the user has any plug-in-specific external tools
         * configured. These will always override the workbench's external
         * editors. These are the Eclipse-specific TFS external view
         * preferences.
         */

        final ExternalToolset pluginViewToolset = ExternalToolset.loadFromMemento(
            new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                ExternalToolPreferenceKey.VIEW_KEY));

        final ExternalTool pluginViewTool = pluginViewToolset.findTool(path);

        /*
         * A configured view tool works for a file or a folder.
         */
        if (pluginViewTool != null) {
            final String messageFormat =
                "Using (plug-in preferences) user-configured external tool {0} to view path {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, pluginViewTool, path);

            log.debug(message);

            final LaunchExternalViewToolCommand viewCommand =
                new LaunchExternalViewToolCommand(pluginViewTool, path, true);

            /*
             * These commands run at least as long as the external tool runs (if
             * it starts successfully), so use a job executor.
             */
            final JobOptions options = new JobOptions();
            options.setSystem(true);

            final ICommandExecutor executor =
                UICommandExecutorFactory.newUIJobCommandExecutor(page.getWorkbenchWindow().getShell(), options);
            executor.execute(viewCommand);
            return true;
        }

        /*
         * No Memento configured tools
         */
        final IEditorInput editorInput = getEditorInput(path);
        if (editorInput == null) {
            return false;
        }

        if (inModalContext) {
            /*
             * In a modal context, so only external programs make sense (or a
             * new-top-window internal view tool, which we currently do not
             * have). Ask Eclipse for a configured external editor.
             */
            if (PlatformUI.getWorkbench().getEditorRegistry().isSystemExternalEditorAvailable(editorInput.getName())) {
                try {
                    final String messageFormat = "Viewing path {0} with Eclipse external editor"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, path);

                    log.debug(message);

                    page.openEditor(editorInput, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
                    return true;
                } catch (final PartInitException e) {
                    final String messageFormat = "Error viewing path {0} with Eclipse external editor"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, path);
                    log.error(message, e);
                    throw new RuntimeException(e);
                }
            }
        } else {
            /*
             * In the non-modal case, see if Eclipse has an editor for this file
             * type.
             */
            IEditorDescriptor editorDesctriptor =
                PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(editorInput.getName());

            /*
             * Eclipse does not have a default editor so use the default text
             * editor.
             */
            if (editorDesctriptor == null) {
                editorDesctriptor =
                    PlatformUI.getWorkbench().getEditorRegistry().findEditor("org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
            }

            if (editorDesctriptor != null) {
                try {
                    final String messageFormat = "Viewing path {0} with Eclipse editor {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, path, editorDesctriptor.getId());
                    log.debug(message);

                    page.openEditor(editorInput, editorDesctriptor.getId());
                    return true;
                } catch (final PartInitException e) {
                    final String messageFormat = "Error viewing path {0} with registered Eclipse editor {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, path, editorDesctriptor.getId());
                    log.error(message, e);
                    throw new RuntimeException(e);
                }
            }
        }

        /*
         * No user-configured external tool or registered internal or external
         * editor.
         */
        return false;
    }

    private static IEditorInput getEditorInput(final String path) {
        final IFile file = Resources.getFileForLocation(path);
        if (file != null) {
            return new FileEditorInput(file);
        } else {
            // file is outside of workbench
            final IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(path));
            final IFileInfo fetchInfo = fileStore.fetchInfo();
            if (fetchInfo.isDirectory() || !fetchInfo.exists()) {
                return null; // ensure the file exists
            }
            return new FileStoreEditorInput(fileStore);
        }
    }

}
