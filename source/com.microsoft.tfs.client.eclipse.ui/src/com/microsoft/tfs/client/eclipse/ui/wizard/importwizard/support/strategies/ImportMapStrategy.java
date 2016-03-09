// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies;

import java.io.IOException;

import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolder;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * Defines a strategy for performing a working folder mapping as part of an
 * import operation.
 */
public abstract class ImportMapStrategy {
    /**
     * Creates a working folder mapping.
     *
     * @param selectedPath
     *        the selected server path
     * @param localPath
     *        the local path
     * @param importOptions
     *        the import options
     */
    public abstract void map(ImportFolder selectedPath, String localPath, ImportOptions importOptions);

    /**
     * Cancels a working folder mapping previously created by this strategy.
     * This is called in response to a user cancellation of the import process.
     *
     * @param selectedPath
     * @param localPath
     * @param importOptions
     */
    public abstract void cancelMap(ImportFolder selectedPath, String localPath, ImportOptions importOptions);

    /**
     * WARNING: the return value is non-localized and should be used for logging
     * or debugging purposes only, never displayed directly in the UI.
     *
     * @return some text describing what this strategy will do (non-localized)
     */
    public abstract String getPlan();

    /**
     * The default MapStrategy. This is used when there is no existing working
     * folder mapping.
     */
    public static class Default extends ImportMapStrategy {
        @Override
        public String getPlan() {
            return "create mapping"; //$NON-NLS-1$
        }

        @Override
        public void map(final ImportFolder selectedPath, final String localPath, final ImportOptions importOptions) {
            final WorkingFolder wf = new WorkingFolder(selectedPath.getFullPath(), localPath, WorkingFolderType.MAP);
            importOptions.getTFSWorkspace().createWorkingFolder(wf, false);
        }

        @Override
        public void cancelMap(
            final ImportFolder selectedPath,
            final String localPath,
            final ImportOptions importOptions) {
            TaskMonitorService.getTaskMonitor().setCurrentWorkDescription(
                Messages.getString("ImportMapStrategy.RemovingWorkingFolderMapping")); //$NON-NLS-1$
            final WorkingFolder wf = importOptions.getTFSWorkspace().getExactMappingForLocalPath(localPath);
            if (wf != null) {
                try {
                    importOptions.getTFSWorkspace().deleteWorkingFolder(wf);
                } catch (final ServerPathFormatException e) {
                    throw new RuntimeException(e);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * A mapping strategy used when a working folder mapping already exists.
     */
    public static class ExistingMapping extends ImportMapStrategy {
        @Override
        public String getPlan() {
            return "use existing mapping"; //$NON-NLS-1$
        }

        @Override
        public void map(final ImportFolder selectedPath, final String localPath, final ImportOptions importOptions) {
            /*
             * do nothing, as the mapping already exists
             */
        }

        @Override
        public void cancelMap(
            final ImportFolder selectedPath,
            final String localPath,
            final ImportOptions importOptions) {
            /*
             * nothing to do, as this strategy doesn't create a mapping in the
             * first place
             */
        }
    }
}
