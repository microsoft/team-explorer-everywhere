// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportGetStrategy;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportLocalPathStrategy;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportMapStrategy;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportOpenProjectStrategy;

/**
 * The ImportTaskFactory creates ImportTasks.
 *
 * The logic for figuring out which concrete strategies to use lives here - this
 * logic is needed to build ImportTasks.
 */
public class ImportTaskFactory {
    /**
     * Creates an ImportTask capable of importing the given SelectedPath using
     * the given ImportOptions.
     *
     * @param selectedPath
     *        the server path to import
     * @param importOptions
     *        the import options
     * @return an ImportTask, ready to run
     */
    public static ImportTask createImportTask(final ImportFolder selectedPath, final ImportOptions importOptions) {
        ImportLocalPathStrategy localPathStrategy;
        ImportMapStrategy mapStrategy;
        ImportGetStrategy getStrategy;
        ImportOpenProjectStrategy openProjectStrategy;

        /*
         * determine the local path strategy
         */

        if (selectedPath.hasExistingMapping()) {
            localPathStrategy = new ImportLocalPathStrategy.ExistingMapping();
        } else if (selectedPath.projectMetadataFileExistsOnServer()) {
            localPathStrategy = new ImportLocalPathStrategy.ExistingProjectMetadataFile();
        } else {
            if (importOptions.isUseNewProjectWizard()) {
                localPathStrategy = new ImportLocalPathStrategy.NewProjectWizard(importOptions.getNewProjectAction());
            } else {
                localPathStrategy = new ImportLocalPathStrategy.FromServerPath();
            }
        }

        /*
         * determine the map strategy
         */

        if (selectedPath.hasExistingMapping()) {
            mapStrategy = new ImportMapStrategy.ExistingMapping();
        } else {
            mapStrategy = new ImportMapStrategy.Default();
        }

        /*
         * determine the get strategy
         */

        /* User checked the force get option */
        if (importOptions.isForceGet()) {
            getStrategy = new ImportGetStrategy.ForceFullRecursive();
        }
        /*
         * If there's already a mapping and the user has the .project file,
         * don't do a get at all. (Assume they're at whatever version they want
         * to be at.)
         */
        else if (selectedPath.hasExistingMapping()
            && selectedPath.projectMetadataFileExistsOnServer()
            && selectedPath.projectMetadataFileExistsLocally()) {
            getStrategy = new ImportGetStrategy.Null();
        }
        /*
         * If the server expects the .project file to exist but it does not (ie,
         * the user has deleted it or deleted a workfold w/o doing a get of C0)
         * then we need to force a get.
         */
        else if (selectedPath.projectMetadataFileShouldExistLocally()) {
            getStrategy = new ImportGetStrategy.ForceFullRecursive();
        }
        /* Otherwise, do a full get latest recursive */
        else {
            getStrategy = new ImportGetStrategy.FullRecursive();
        }

        /*
         * determine the open project strategy
         */

        if (selectedPath.hasExistingMapping()) {
            if (selectedPath.eclipseProjectAlreadyOpen(importOptions.getEclipseWorkspace())) {
                openProjectStrategy = new ImportOpenProjectStrategy.AlreadyOpen();
            } else if (!selectedPath.projectMetadataFileExistsLocally()
                && !selectedPath.projectMetadataFileExistsOnServer()) {
                openProjectStrategy = new ImportOpenProjectStrategy.NewSimpleProject();
            } else {
                openProjectStrategy = new ImportOpenProjectStrategy.ExistingProjectMetadataFile();
            }
        } else {
            if (selectedPath.projectMetadataFileExistsOnServer()) {
                openProjectStrategy = new ImportOpenProjectStrategy.ExistingProjectMetadataFile();
            } else if (importOptions.isUseNewProjectWizard()) {
                openProjectStrategy = new ImportOpenProjectStrategy.AlreadyOpen();
            } else {
                openProjectStrategy = new ImportOpenProjectStrategy.NewSimpleProject();
            }
        }

        /*
         * create an ImportTask, using the SelectedPath, the ImportOptions, and
         * the determined strategies
         */

        return new ImportTask(
            selectedPath,
            importOptions,
            localPathStrategy,
            mapStrategy,
            getStrategy,
            openProjectStrategy);
    }
}