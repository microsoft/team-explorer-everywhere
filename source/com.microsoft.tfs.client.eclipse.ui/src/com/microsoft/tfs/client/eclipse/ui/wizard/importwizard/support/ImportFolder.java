// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.io.File;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;

/**
 * A SelectedPath represents a single server folder path that has been selected
 * for import.
 *
 * SelectedPaths are placed into a SelectionSet, and a SelectionSet is handed
 * off to an ImportCommand to run an import.
 */
public class ImportFolder extends ImportItemBase {
    private final String existingWorkingFolderMapping;

    private Item projectMetadataFileServerItem;
    private ExtendedItem projectMetadataFileServerExtendedItem;

    /**
     * Creates a new SelectedPath. The caller is responsible for passing in any
     * existing working folder mapping that the server path is a part of.
     *
     * @param itemPath
     *        the server path
     * @param existingWorkingFolderMapping
     *        an existing working folder mapping or null
     */
    public ImportFolder(final String itemPath, final String existingWorkingFolderMapping) {
        super(itemPath);
        this.existingWorkingFolderMapping = existingWorkingFolderMapping;
    }

    public ImportFolder(final String itemPath) {
        this(itemPath, null);
    }

    /**
     * Called at certain times during the import to add additional state to a
     * SelectedPath. This method is called after querying the server for
     * existing .project files.
     *
     * @param projectMetadataFileServerItem
     *        the AItem corresponding to an existing .project file for this
     *        SelectedPath
     */
    public void setProjectMetadataFileServerItem(final Item projectMetadataFileServerItem) {
        this.projectMetadataFileServerItem = projectMetadataFileServerItem;
    }

    /**
     * Called at certain times during the import to add additional state to a
     * SelectedPath. This method is called after querying the server for
     * existing .project files.
     *
     * @param projectMetadataFileServerItem
     *        the AItem corresponding to an existing .project file for this
     *        SelectedPath
     */
    public void setProjectMetadataFileServerExtendedItem(final ExtendedItem projectMetadataFileServerExtendedItem) {
        this.projectMetadataFileServerExtendedItem = projectMetadataFileServerExtendedItem;
    }

    /**
     * @return true if this SelectedPath already has a working folder mapping
     */
    public boolean hasExistingMapping() {
        return existingWorkingFolderMapping != null;
    }

    /**
     * @return the existing working folder mapping for this SelectedPath, or
     *         null if none
     */
    public String getExistingWorkingFolderMapping() {
        return existingWorkingFolderMapping;
    }

    /**
     * @return true if this SelectedPath has an existing WF mapping and there is
     *         a .project file locally
     */
    public boolean projectMetadataFileExistsLocally() {
        /*
         * for this to be true, there must be an existing WF mapping and it must
         * not be cloaked
         */
        if (existingWorkingFolderMapping != null) {
            final File projectMetadataFile =
                new File(existingWorkingFolderMapping, IProjectDescription.DESCRIPTION_FILE_NAME);
            return projectMetadataFile.exists();
        }
        return false;
    }

    /**
     * @param eclipseWorkspace
     *        the Eclipse workspace to check
     * @return true if this SelectedPath has an existing WF mapping and a
     *         corresponding Eclipse project is already open
     */
    public boolean eclipseProjectAlreadyOpen(final IWorkspace eclipseWorkspace) {
        /*
         * for this to be true, there must be an existing WF mapping
         */
        if (existingWorkingFolderMapping != null) {
            final String projectName = new File(existingWorkingFolderMapping).getName();
            return eclipseWorkspace.getRoot().getProject(projectName).exists();
        }
        return false;
    }

    /**
     * @return true if the .project file exists on the server
     */
    public boolean projectMetadataFileExistsOnServer() {
        return projectMetadataFileServerItem != null;
    }

    /**
     * @return the .project {@link Item} if it exists on the server, or
     *         <code>null</code> if it does not exist
     */
    public Item getExistingProjectMetadataFileItem() {
        return projectMetadataFileServerItem;
    }

    public boolean projectMetadataFileShouldExistLocally() {
        return (projectMetadataFileServerExtendedItem != null
            && projectMetadataFileServerExtendedItem.getLocalItem() != null
            && projectMetadataFileServerExtendedItem.getLocalVersion() != 0);
    }
}