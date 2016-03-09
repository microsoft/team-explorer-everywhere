// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

/**
 * A class that holds the SelectedPaths to be imported.
 */
public abstract class ImportItemCollectionBase {
    private static final String ANCESTOR_OF_OTHER_SELECTION_MESSAGE =
        Messages.getString("ImportFolderCollection.SelectedPathIsAncestorFormat"); //$NON-NLS-1$

    /*
     * from ItemPath -> SelectedPath
     */
    private final Map<String, ImportItemBase> itemMap = new HashMap<String, ImportItemBase>();

    /*
     * tracks validation state for the selection set
     */
    private boolean valid = true;

    /*
     * holds an error message when the selection set is invalid
     */
    private String invalidMessage;

    public ImportItemCollectionBase() {
    }

    /**
     * Drops internal caches. This should be called when the ImportOptions have
     * changed internally.
     */
    public void dropCache() {
    }

    /**
     * @return the error message if this SelectionSet is invalid
     */
    public String getInvalidMessage() {
        return invalidMessage;
    }

    /**
     * @return true if this selection set is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return the number of SelectedPaths in this SelectionSet
     */
    public int size() {
        return itemMap.size();
    }

    protected void addFolder(final String itemPath) {
        final ImportItemBase importItem = getImportItem(itemPath);
        itemMap.put(itemPath, importItem);
    }

    protected void addItem(final TypedServerItem serverItem) {
        final ImportItemBase importItem = getImportItem(serverItem);
        itemMap.put(serverItem.getServerPath(), importItem);
    }

    public String[] getFolders() {
        final Set<String> folderSet = itemMap.keySet();

        return folderSet.toArray(new String[folderSet.size()]);
    }

    public ImportItemBase[] getItems() {
        return itemMap.values().toArray(new ImportItemBase[size()]);
    }

    public ImportItemBase get(final String itemName) {
        return itemMap.get(itemName);
    }

    public void clear() {
        itemMap.clear();
    }

    /**
     * Populates this SelectionSet from the given ISelection. Any SelectedPaths
     * currently held in the set are first removed. The ISelection is assumed to
     * be an IStructuredSelection containing TFSFolder objects.
     *
     * @param selection
     *        the ISelection to use to populate this SelectionSet
     */
    public void setFolders(final String[] folders) {
        itemMap.clear();

        if (folders != null) {
            for (int i = 0; i < folders.length; i++) {
                addFolder(folders[i]);
            }
        }

        validate();
    }

    public void setItems(final TypedServerItem[] items) {
        itemMap.clear();

        if (items != null) {
            for (final TypedServerItem item : items) {
                addItem(item);
            }
        }

        validate();
    }

    protected void setItems(final List<TypedServerItem> items) {
        itemMap.clear();

        if (items != null) {
            for (final TypedServerItem item : items) {
                addItem(item);
            }
        }

        validate();
    }

    private void validate() {
        valid = false;

        for (final Iterator<ImportItemBase> selectedPathIt = itemMap.values().iterator(); selectedPathIt.hasNext();) {
            final ImportItemBase currentSelectedPath = selectedPathIt.next();

            final ImportFolderValidation validation = validate(currentSelectedPath);

            if (validation.getStatus() != ImportFolderValidationStatus.OK) {
                invalidMessage = validation.getMessage();
                return;
            }
        }

        valid = true;
    }

    protected ImportFolderValidation validate(final ImportItemBase selectedPath) {
        /*
         * Ensure that the selected path is not an ancestor of another selected
         * path.
         */
        for (final Iterator<ImportItemBase> otherSelectedPathsIt =
            itemMap.values().iterator(); otherSelectedPathsIt.hasNext();) {
            final ImportItemBase otherSelectedPath = otherSelectedPathsIt.next();

            if (selectedPath == otherSelectedPath) {
                continue;
            }

            if (ServerPath.isChild(selectedPath.getFullPath(), otherSelectedPath.getFullPath())) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.ERROR,
                    MessageFormat.format(ANCESTOR_OF_OTHER_SELECTION_MESSAGE, new Object[] {
                        selectedPath.getFullPath(),
                        otherSelectedPath.getFullPath()
                }));
            }
        }

        return new ImportFolderValidation(ImportFolderValidationStatus.OK, null);
    }

    protected abstract ImportItemBase getImportItem(final String itemPath);

    protected abstract ImportItemBase getImportItem(final TypedServerItem item);
}