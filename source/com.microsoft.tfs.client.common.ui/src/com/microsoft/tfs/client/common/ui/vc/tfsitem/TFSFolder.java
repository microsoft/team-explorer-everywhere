// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IActionFilter;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.CollatorFactory;

/**
 * A representation of a TFS folder.
 */
public class TFSFolder extends TFSItem implements IActionFilter {
    private Map<String, TFSItem> children;

    private final Log log = LogFactory.getLog(TFSFolder.class);

    /**
     * Creates a new TFSFolder using the given AExtendedItem.
     *
     * @param extendedItem
     *        contains the data used in this TFSFolder
     */
    public TFSFolder(final ExtendedItem extendedItem) {
        this(extendedItem, null);
    }

    public TFSFolder(final ExtendedItem extendedItem, final TFSRepository repository) {
        super(extendedItem, repository);
    }

    public TFSFolder(final ServerItemPath path) {
        this(path, null);
    }

    public TFSFolder(final ServerItemPath path, final TFSRepository repository) {
        super(path, repository);
    }

    /**
     * Returns the set of children (TFSItem objects) of this TFSFolder. Only
     * immediate children - not all ancestors - are returned. The set may be
     * empty, but never null. If the children for this TFSFolder have already
     * been computed, no server lookup is done. If the children have not yet
     * been computed, a server call will be done when this method is called to
     * determine the children.
     *
     * @return the set of children of this folder
     */
    public Set<TFSItem> getChildren() {
        return getChildren(false);
    }

    public Set<TFSItem> getChildren(final boolean showDeletedItems) {
        if (children == null) {
            computeChildren(showDeletedItems);
        }

        return new HashSet<TFSItem>(children.values());
    }

    /**
     * Modify the computed set of this folder's children to be empty. Any stored
     * cache of children is discarded, and future calls to getChildren() will
     * not trigger a server call.
     */
    public void setChildrenEmpty() {
        children = new TreeMap<String, TFSItem>(CollatorFactory.getCaseInsensitiveCollator());
    }

    private void computeChildren(final boolean showDeletedItems) {
        setChildrenEmpty();

        final DeletedState deletedState = showDeletedItems ? DeletedState.ANY : DeletedState.NON_DELETED;
        GetItemsOptions itemsOptions = GetItemsOptions.INCLUDE_BRANCH_INFO;

        if (showDeletedItems) {
            itemsOptions = itemsOptions.combine(GetItemsOptions.INCLUDE_SOURCE_RENAMES);
        }

        /*
         * Make sure to include branch information in this query.
         */
        final QueryItemsExtendedCommand queryCommand = new QueryItemsExtendedCommand(
            getRepository(),
            getFullPath(),
            ItemType.ANY,
            deletedState,
            RecursionType.ONE_LEVEL,
            itemsOptions);

        final IStatus status = new CommandExecutor().execute(queryCommand);

        if (!status.isOK()) {
            final String messageFormat = "Could not query extended items for {0}: {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getFullPath(), status.getMessage());
            log.error(message, status.getException());
            return;
        }

        final ExtendedItem[][] childExtendedItems = queryCommand.getItems();

        if (childExtendedItems.length != 1) {
            return;
        }

        for (int i = 0; i < childExtendedItems[0].length; i++) {
            final TFSItem childItem = TFSItemFactory.getItem(childExtendedItems[0][i]);

            /*
             * never add this folder as its own child - the query above can
             * return this.
             */
            if (equals(childItem)) {
                continue;
            }

            addChild(childItem);
        }

        /*
         * Add in pending changes. We could have an add for a file well beneath
         * us that implicitly adds a folder at this level (but there is no
         * pending add for the folder itself). In this case, this would not be
         * caught by the QueryItemsExtended call, so we need to add it in from
         * the pending change cache.
         */
        final TFSItem[] implicitChildren = TFSItemFactory.getImplicitAdds(getRepository(), this);

        for (int i = 0; i < implicitChildren.length; i++) {
            addChild(implicitChildren[i]);
        }
    }

    /**
     * Behaves like getChildren(), but only returns children that are folders
     * (TFSFolder objects).
     *
     * @return set of folder children of this folder
     */
    public Set<TFSItem> getFolderChildren() {
        return getFolderChildren(false);
    }

    public Set<TFSItem> getFolderChildren(final boolean includeDeletedItems) {
        final Set<TFSItem> folderChildren = new HashSet<TFSItem>(getChildren(includeDeletedItems));
        for (final Iterator<TFSItem> i = folderChildren.iterator(); i.hasNext();) {
            if (!(i.next() instanceof TFSFolder)) {
                i.remove();
            }
        }
        return folderChildren;
    }

    /**
     * Clear any cache held in this object of its children. Subsequent calls to
     * getChildren() will recompute the set of children of this folder by making
     * a server call.
     */
    public void clearCachedChildren() {
        children = null;
    }

    /**
     * Adds a child item to this folder. Any existing children will not be
     * removed - however, calling this method will populate a cache of children
     * so that future calls to getChildren() will not make a server call.
     *
     * @param child
     *        TFSItem child to add to this folder
     */
    public void addChild(final TFSItem child) {
        if (children == null) {
            setChildrenEmpty();
        }

        final String childFullPath = child.getFullPath();

        if (child.getDeletionID() != 0) {
            /*
             * The goal is to ensure that we have only one child for each unique
             * server path. If we are including deleted items, multiple children
             * could have the same server path. If one of the children is
             * non-deleted, we show it. Otherwise, we show the most recently
             * deleted child (that is, the child with the highest deletion ID).
             */

            final TFSItem existingChild = children.get(childFullPath);

            if (existingChild != null) {
                if (existingChild.getDeletionID() == 0) {
                    /*
                     * We already have a non-deleted child at this path, so
                     * don't add the new child.
                     */
                    return;
                }

                if (existingChild.getDeletionID() > child.getDeletionID()) {
                    /*
                     * We already have a deleted child at this path with a
                     * higher deletion ID, so don't add the new child.
                     */
                    return;
                }
            }
        }

        children.put(childFullPath, child);
    }

    /**
     * Return the TFSItem corresponding to one of this folder's direct children
     * with the given full path. Subfolders will not be searched. If the
     * children for this folder have not been cached yet, they will be computed
     * during this method, which will result in a server query.
     *
     * @param fullPathToChild
     * @return match or null
     */
    public TFSItem getChildByFullPath(final String fullPathToChild) {
        if (children == null) {
            computeChildren(false);
        }
        return children.get(fullPathToChild);
    }

    public TFSItem getDescendantByFullPath(final String fullPathToChild) {
        // First, hope that it's one of this folder's direct
        // children.
        final TFSItem returnme = getChildByFullPath(fullPathToChild);
        if (returnme == null) {
            final int indexOfChildFinishingSlash = fullPathToChild.indexOf('/', getFullPath().length() + 1);
            if (indexOfChildFinishingSlash == -1) {
                return null;
            }
            final String nextChildFullPath = fullPathToChild.substring(0, indexOfChildFinishingSlash);
            final TFSItem directChild = getChildByFullPath(nextChildFullPath);
            if (directChild == null || (directChild instanceof TFSFolder) == false) {
                return null;
            }
            return ((TFSFolder) directChild).getDescendantByFullPath(fullPathToChild);

        }

        return returnme;
    }

    /**
     * @return true if this TFSFolder has cached its children
     */
    public boolean areChildrenCached() {
        return children != null;
    }

    /**
     * @return true if this TFSFolder has children
     */
    public boolean hasChildren() {
        return getChildren().size() > 0;
    }

    /**
     * @return true if this TFSFolder has children
     */
    public boolean hasFolderChildren() {
        return getFolderChildren().size() > 0;
    }

    /**
     * Implementation for IActionFilter. This method is called by the platform
     * when constructing a context menu for the TFVC Source Control Explorer
     * view. This is called when TFSFolder items are selected and is used to
     * determine the visibility of actions associated with the TFSFolder
     * contribution objects.
     */
    @Override
    public boolean testAttribute(final Object target, final String name, final String value) {
        if (target instanceof TFSFolder) {
            final TFSFolder folder = (TFSFolder) target;
            final String serverPath = folder.getFullPath();

            WorkingFolder exactFolder;
            boolean isMapped;

            try {
                final Workspace workspace = getRepository().getWorkspace();
                exactFolder = workspace.getExactMappingForServerPath(serverPath);
                isMapped = workspace.isServerPathMapped(serverPath);
            } catch (final PathTooLongException e) {
                exactFolder = null;
                isMapped = false;
            }

            if (name.equals("MappingState")) //$NON-NLS-1$
            {
                if (value.equals("Unmapped")) //$NON-NLS-1$
                {
                    return isMapped == false && exactFolder == null;
                } else if (value.equals("ExactMapping")) //$NON-NLS-1$
                {
                    return exactFolder != null && exactFolder.isCloaked() == false;
                } else if (value.equals("MappedByParent")) //$NON-NLS-1$
                {
                    return isMapped == true && exactFolder == null;
                } else if (value.equals("Cloaked")) //$NON-NLS-1$
                {
                    return exactFolder != null && exactFolder.isCloaked() == true;
                }
            }
        }

        return false;
    }
}