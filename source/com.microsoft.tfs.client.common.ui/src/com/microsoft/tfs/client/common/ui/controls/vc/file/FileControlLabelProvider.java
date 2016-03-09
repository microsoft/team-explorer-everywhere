// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.file;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.helpers.SystemColor;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemLabelProvider;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderComparatorType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.util.Check;

public class FileControlLabelProvider extends TFSItemLabelProvider {
    private final FileControlPendingChangesCache pendingChangesCache;

    public FileControlLabelProvider(final FileControlPendingChangesCache pendingChangesCache) {
        this.pendingChangesCache = pendingChangesCache;
    }

    @Override
    protected Color getBackgroundColorForTFSItem(final TFSRepository repository, final TFSItem item) {
        return super.getBackgroundColorForTFSItem(repository, item);
    }

    @Override
    protected Color getForegroundColorForTFSItem(final TFSRepository repository, final TFSItem item) {
        if (!item.isLocal()) {
            final Display display = Display.getCurrent();

            if (display != null) {
                return SystemColor.getDimmedWidgetForegroundColor(Display.getCurrent());
            }
        }

        return super.getForegroundColorForTFSItem(repository, item);
    }

    @Override
    protected String getColumnTextForTFSItem(
        final TFSRepository repository,
        final TFSItem item,
        final int columnIndex) {
        switch (columnIndex) {
            case 0:
                return item.getName();
            case 1:
                return getPendingChanges(repository, item);
            case 2:
                return getPendingChangeUsers(repository, item);
            case 3:
                return getLatestDescription(repository, item);
            case 4:
                return getLastCheckinDate(repository, item);
            default:
                return null;
        }
    }

    private String getLastCheckinDate(final TFSRepository repository, final TFSItem item) {
        if (item.getExtendedItem() == null
            || item.getExtendedItem().getCheckinDate() == null
            || item.getExtendedItem().getCheckinDate().get(Calendar.YEAR) == 1) {
            // Note the "year = 1" test is how the Microsoft code does the check
            // to see if a value is present that should be displayed. See
            //
            // Microsoft.TeamFoundation.VersionControl.Controls.
            // ListViewExplorer.FillWithPath(String)

            return ""; //$NON-NLS-1$
        }
        return SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(
            item.getExtendedItem().getCheckinDate().getTime());
    }

    /**
     * Work out the Latest description.
     */
    private String getLatestDescription(final TFSRepository repository, final TFSItem item) {
        /*
         * Note :-
         *
         * Visual Studio's SCE looks to see whether the item (ExtendedItem) indicates
         * that the file is in the workspace (LocalItem is not null). If it is,
         * then it's either latest or not (latest = VersionLocal ==
         * VersionLatest). If it is not in the workspace, it gets the
         * WorkingFolder for the item's TargetServerItem (we cache the working
         * folder mappings for the workspace so that there's one server call
         * regardless of the number of working folders being computed for the
         * view). At that point, it's either not mapped, cloaked, deleted
         * (DeletionId is non-zero), or it hasn't been gotten in the workspace."
         */

        /*
         * Extended item is only null if this is an implicitly added folder.
         * (Ie, a pended add exists beneath this folder, but no pended add
         * actually exists for this.)
         */
        if (item.getExtendedItem() == null) {
            return Messages.getString("FileControlLabelProvider.Yes"); //$NON-NLS-1$
        } else if (item.getExtendedItem().getLocalItem() == null) {
            // Item is not local, now we need to figure out why.
            return getNonLocalDescription(repository, item);
        } else {
            // Item is local, but is it the latest version.

            return (item.isLatest() ? Messages.getString("FileControlLabelProvider.Yes") //$NON-NLS-1$
                : Messages.getString("FileControlLabelProvider.No")); //$NON-NLS-1$
        }
    }

    /**
     * Work out the description of the non local item.
     */
    private String getNonLocalDescription(final TFSRepository repository, final TFSItem item) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(item, "item"); //$NON-NLS-1$

        final String serverPath = item.getFullPath();
        if (serverPath == null) {
            return Messages.getString("FileControlLabelProvider.Unknown"); //$NON-NLS-1$
        }

        try {
            // Check to see if it is mapped to a working folder.
            final WorkingFolder[] workingFolders = repository.getWorkspace().getFolders();

            // Sort in reverse order to guarantee we find the most precise match
            // first.
            Arrays.sort(workingFolders, new WorkingFolderComparator(WorkingFolderComparatorType.SERVER_PATH_REVERSE));

            for (int i = 0; i < workingFolders.length; i++) {
                final WorkingFolder baseWorkingFolder = workingFolders[i];
                final boolean isEqual = ServerPath.equals(baseWorkingFolder.getServerItem(), serverPath);

                if (isEqual || ServerPath.isChild(baseWorkingFolder.getServerItem(), serverPath)) {
                    if (baseWorkingFolder.getType() == WorkingFolderType.CLOAK) {
                        return Messages.getString("FileControlLabelProvider.Cloaked"); //$NON-NLS-1$
                    } else if (repository.getWorkspace().isServerPathMapped(serverPath)) {
                        if (item.getItemSpec() != null && item.getItemSpec().getDeletionID() > 0) {
                            return Messages.getString("FileControlLabelProvider.Deleted"); //$NON-NLS-1$
                        } else {
                            return Messages.getString("FileControlLabelProvider.NotDownloaded"); //$NON-NLS-1$
                        }
                    } else {
                        return Messages.getString("FileControlLabelProvider.NotMapped"); //$NON-NLS-1$
                    }
                }
            }
        } catch (final PathTooLongException e) {
            // Ignore so "not mapped" is used
        } catch (final ServerPathFormatException e) {
            return Messages.getString("FileControlLabelProvider.Unknown"); //$NON-NLS-1$
        }

        return Messages.getString("FileControlLabelProvider.NotMapped"); //$NON-NLS-1$
    }

    private String getPendingChanges(final TFSRepository repository, final TFSItem item) {
        /* May not necessarily have an extended item for this item */
        if (item.getExtendedItem() == null) {
            return ""; //$NON-NLS-1$
        }

        /*
         * If another user has a pending change for this item, we have to query
         * the teamviewer's pending change cache because the other peoples'
         * change information is not in the extended item.
         */
        if (item.getExtendedItem().hasOtherPendingChange()
            && pendingChangesCache.getChangesForItem(item) != null
            && pendingChangesCache.getChangesForItem(item).length > 0) {
            ChangeType compositeChangeType = ChangeType.NONE;
            final PendingChange[] pendingChanges = pendingChangesCache.getChangesForItem(item);

            for (int i = 0; i < pendingChanges.length; i++) {
                compositeChangeType = compositeChangeType.combine(pendingChanges[i].getChangeType());
            }

            return compositeChangeType.toUIString(true, item.getExtendedItem());
        } else {
            final ChangeType changeType = item.getExtendedItem().getPendingChange();
            return changeType.toUIString(true, item.getExtendedItem());
        }
    }

    private String getPendingChangeUsers(final TFSRepository repository, final TFSItem item) {
        if (item.getExtendedItem() == null) {
            return ""; //$NON-NLS-1$
        }

        if (item.getExtendedItem().hasOtherPendingChange()
            && pendingChangesCache.getPendingSetsForItem(item) != null
            && pendingChangesCache.getPendingSetsForItem(item).length > 0) {
            final PendingSet[] pendingSets = pendingChangesCache.getPendingSetsForItem(item);

            if (pendingSets.length > 0) {
                final TFSTeamProjectCollection tfsCollection = repository.getVersionControlClient().getConnection();
                final TeamFoundationIdentity authorizedIdentity = tfsCollection.getAuthorizedIdentity();

                String ownerDisplayName = pendingSets[0].getOwnerDisplayName();
                final String ownerUniqueName = pendingSets[0].getOwnerName();

                /* There are multiple other users editing this file */
                if (pendingSets.length > 1) {
                    /* See if any of them are us */
                    for (int i = 0; i < pendingSets.length; i++) {
                        if (pendingSets[i].getOwnerName().equalsIgnoreCase(authorizedIdentity.getUniqueName())) {
                            ownerDisplayName = pendingSets[i].getOwnerDisplayName();
                            break;
                        }
                    }

                    final String messageFormat =
                        Messages.getString("FileControlLabelProvider.MultiPendingChangeUserFormat"); //$NON-NLS-1$
                    return MessageFormat.format(messageFormat, ownerDisplayName);
                }
                /* This is us on another computer or workspace */
                else if (ownerUniqueName.equalsIgnoreCase(authorizedIdentity.getUniqueName())) {
                    return MessageFormat.format(
                        Messages.getString("FileControlLabelProvider.SinglePendingChangeUserFormat"), //$NON-NLS-1$
                        ownerDisplayName,
                        pendingSets[0].getName());
                }
                /* This is simply another user */
                else {
                    return ownerDisplayName;
                }
            }

            return ""; //$NON-NLS-1$
        } else {
            if (item.getExtendedItem().hasLocalChange()) {
                // local pending changes - so just display the current user
                return repository.getWorkspace().getOwnerDisplayName();
            }
            return ""; //$NON-NLS-1$
        }
    }
}
