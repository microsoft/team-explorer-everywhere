// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProjectDescription;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * A class that holds the SelectedPaths to be imported.
 */
public class ImportFolderCollection extends ImportItemCollectionBase {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String ROOT_ELEMENT_NAME = "TFSImportSelectionSet"; //$NON-NLS-1$
    private static final String FOLDER_PATHS_ELEMENT_NAME = "ServerFolderPaths"; //$NON-NLS-1$
    private static final String PATH_ELEMENT_NAME = "Path"; //$NON-NLS-1$

    /*
     * The import options associated with this selection set
     */
    private final ImportOptions importOptions;

    /**
     * Creates a SelectionSet using the given ImportOptions.
     *
     * @param importOptions
     *        the ImportOptions to use.
     */
    public ImportFolderCollection(final ImportOptions importOptions) {
        super();
        this.importOptions = importOptions;
    }

    public ImportFolderCollection(final ImportOptions importOptions, final List<TypedServerItem> items) {
        this(importOptions);
        setItems(items);
    }

    @Override
    protected ImportFolderValidation validate(final ImportItemBase selectedPath) {
        final ImportFolderValidation validation = super.validate(selectedPath);

        if (validation.getStatus() != ImportFolderValidationStatus.OK) {
            return validation;
        } else {
            return importOptions.getFolderValidator().validate(selectedPath.getFullPath());
        }
    }

    @Override
    protected ImportItemBase getImportItem(final String folderPath) {
        // Determine if the specified server folder path has a mapping and how
        // it is mapped. Possibilities are:
        //
        // 1) mapped by a ancestor with RecursiveType.FULL
        // 2) mapped by a parent with RecursiveType.ONE
        // 3) mapped by itself at RecursiveType.FULL or RecursiveType.ONE
        // 4) not mapped
        //
        // We want to create a new default mapping for this folder in cases 2
        // and 4 above. For case number 2, translateServerPathToLocalPath will
        // return RecursiveType.NONE to indicate the path is mapped by a
        // RecursiveType.ONE parent.

        final PathTranslation translation = importOptions.getTFSWorkspace().translateServerPathToLocalPath(folderPath);
        ImportFolder selectedPath;

        // Test for cases 2 and 4 above.
        if (translation == null || translation.getRecursionType() == RecursionType.NONE) {
            // A new mapping will be created for this folder path.
            selectedPath = new ImportFolder(folderPath);
        } else {
            // A new mapping will not be created for this folder path.
            selectedPath = new ImportFolder(folderPath, translation.getTranslatedPath());
        }

        return selectedPath;
    }

    @Override
    protected ImportItemBase getImportItem(final TypedServerItem item) {
        return getImportItem(item.getServerPath());
    }

    /**
     *
     * WARNING: the return value is non-localized and should be used for logging
     * or debugging purposes only, never displayed directly in the UI.
     *
     * @return some text describing the import plan for this SelectionSet
     *         (non-localized)
     */
    public String getImportPlan() {
        final ImportTask[] tasks = makeImportTasks();
        final StringBuffer sb = new StringBuffer();

        sb.append("Import Plan for " + tasks.length + " import task"); //$NON-NLS-1$ //$NON-NLS-2$
        if (tasks.length != 1) {
            sb.append("s"); //$NON-NLS-1$
        }
        sb.append(NEWLINE).append(NEWLINE);

        for (int i = 0; i < tasks.length; i++) {
            sb.append(tasks[i].getImportPlan());
            sb.append(NEWLINE);
        }

        return sb.toString();
    }

    /**
     * Creates an array of ImportTasks that contains a task for each
     * SelectedPath in this SelectionSet.
     *
     * @return an array of ImportTasks
     */
    public ImportTask[] makeImportTasks() {
        final ImportTask[] importTasks = new ImportTask[size()];
        int ix = 0;

        /*
         * TODO do we want some kind of sorting here so that the tasks get
         * executed in a specific order?
         */

        for (final ImportItemBase item : getItems()) {
            final ImportFolder selectedPath = (ImportFolder) item;
            importTasks[ix++] = ImportTaskFactory.createImportTask(selectedPath, importOptions);
        }

        return importTasks;
    }

    /**
     * Performs a single server query for .project files corresponding to the
     * SelectedPaths in this SelectionSet. The data from the query is pushed
     * into the SelectedPaths by calling the serProjectMetadataFileServerItem()
     * method on SelectedPath.
     */
    public void queryForProjectMetadataFiles() {
        /*
         * clear out any old project metadata file references
         */
        for (final ImportItemBase item : getItems()) {
            final ImportFolder selectedPath = (ImportFolder) item;
            selectedPath.setProjectMetadataFileServerItem(null);
            selectedPath.setProjectMetadataFileServerExtendedItem(null);
        }

        /* Do a query items (for DURL) */

        final ItemSpec[] itemSpecs = new ItemSpec[size()];

        int ix = 0;
        for (final String folder : getFolders()) {
            final String queryPath = folder + "/" + IProjectDescription.DESCRIPTION_FILE_NAME; //$NON-NLS-1$
            itemSpecs[ix++] = new ItemSpec(queryPath, RecursionType.NONE);
        }

        final ItemSet[] itemResultSet = importOptions.getTFSWorkspace().getClient().getItems(
            itemSpecs,
            LatestVersionSpec.INSTANCE,
            DeletedState.NON_DELETED,
            ItemType.FILE,
            true);

        for (int i = 0; i < itemResultSet.length; i++) {
            final Item[] queryResults = itemResultSet[i].getItems();
            for (int j = 0; j < queryResults.length; j++) {
                final String dotProjectFilePath = queryResults[j].getServerItem();

                final ImportFolder selectedPath = (ImportFolder) get(ServerPath.getParent(dotProjectFilePath));

                selectedPath.setProjectMetadataFileServerItem(queryResults[j]);
            }
        }

        /* Do a query items extended (for local workspace information) */
        final ExtendedItem[][] extendedItemResults =
            importOptions.getTFSWorkspace().getExtendedItems(itemSpecs, DeletedState.NON_DELETED, ItemType.FILE);

        for (int i = 0; i < extendedItemResults.length; i++) {
            for (int j = 0; j < extendedItemResults[i].length; j++) {
                final String dotProjectFilePath = extendedItemResults[i][j].getSourceServerItem() != null
                    ? extendedItemResults[i][j].getSourceServerItem() : extendedItemResults[i][j].getTargetServerItem();

                if (dotProjectFilePath == null) {
                    continue;
                }

                final ImportFolder selectedPath = (ImportFolder) get(ServerPath.getParent(dotProjectFilePath));

                if (selectedPath != null) {
                    selectedPath.setProjectMetadataFileServerExtendedItem(extendedItemResults[i][j]);
                }
            }
        }
    }

    private List<String> makeItemPathList() {
        return Arrays.asList(getFolders());
    }

    /**
     * Serializes this SelectionSet out to a file.
     *
     * @param file
     *        the output file
     */
    public void toFile(final File file) {
        final List<String> sortedPaths = makeItemPathList();
        Collections.sort(sortedPaths, ServerPath.TOP_DOWN_COMPARATOR);

        final Document document = DOMCreateUtils.newDocument(ROOT_ELEMENT_NAME);
        final Element root = document.getDocumentElement();
        final Element element = DOMUtils.appendChild(root, FOLDER_PATHS_ELEMENT_NAME);

        for (final Iterator<String> it = sortedPaths.iterator(); it.hasNext();) {
            final String path = it.next();
            DOMUtils.appendChildWithText(element, PATH_ELEMENT_NAME, path);
        }

        DOMSerializeUtils.serializeToFile(document, file, DOMSerializeUtils.ENCODING_UTF8, DOMSerializeUtils.INDENT);
    }

    /**
     * Deserializes a file, populating this SelectionSet. Any SelectedPaths
     * already held in the set are first removed.
     *
     * @param file
     *        input file
     */
    public void fromFile(final File file) {
        final Document document = DOMCreateUtils.parseFile(file, DOMCreateUtils.ENCODING_UTF8);

        clear();

        final Node root = document.getFirstChild();
        if (root.getNodeType() == Node.ELEMENT_NODE && ROOT_ELEMENT_NAME.equals(root.getNodeName())) {
            NodeList list = ((Element) root).getElementsByTagName(FOLDER_PATHS_ELEMENT_NAME);
            if (list.getLength() > 0) {
                list = ((Element) list.item(0)).getElementsByTagName(PATH_ELEMENT_NAME);
                final int count = list.getLength();
                for (int i = 0; i < count; i++) {
                    final Element pathElement = (Element) list.item(i);
                    final Node child = pathElement.getFirstChild();
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        final String path = child.getNodeValue();
                        addFolder(path);
                    }
                }
            }
        }
    }
}