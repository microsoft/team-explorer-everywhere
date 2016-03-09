// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCache;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.TeamViewerAction;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

/**
 * Methods to assist {@link TeamViewerAction}s in enabling themselves in reponse
 * to selection changed events.
 *
 */
public class ActionEnablementHelper {
    public static boolean selectionContainsRoot(final ISelection selection) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSFolder && ((TFSFolder) object).getItemPath().isRoot());
            }
        });
    }

    public static boolean selectionContainsLockedItem(
        final ISelection selection,
        final PendingChangeCache pendingChangeCache) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                if (object instanceof TFSItem) {
                    final PendingChange[] changes =
                        pendingChangeCache.getPendingChangesByServerPathRecursive(((TFSItem) object).getFullPath());

                    if (changes != null) {
                        for (int i = 0; i < changes.length; i++) {
                            if (changes[i].getChangeType().contains(ChangeType.LOCK)) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
        });
    }

    public static boolean selectionContainsProjectFolder(final ISelection selection) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSFolder
                    && ((TFSFolder) object).getItemPath().getParent() != null
                    && ((TFSFolder) object).getItemPath().getParent().isRoot());
            }
        });
    }

    public static boolean selectionContainsNonLocalItem(final ISelection selection) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSItem && ((TFSItem) object).isLocal() == false);
            }
        });
    }

    public static boolean selectionContainsDeletedItem(final ISelection selection) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSItem && ((TFSItem) object).getDeletionID() != 0);
            }
        });
    }

    public static boolean selectionContainsDeletedFolder(final ISelection selection) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSFolder && ((TFSFolder) object).isDeleted());
            }
        });
    }

    public static boolean selectionContainsNonDeletedItem(final ISelection selection) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSItem && ((TFSItem) object).getDeletionID() == 0);
            }
        });
    }

    public static boolean selectionContainsPendingChanges(final ISelection selection, final boolean recursive) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                return (object instanceof TFSItem && ((TFSItem) object).hasPendingChanges(recursive));
            }
        });
    }

    public static boolean selectionContainsPendingChangesOfAnyChangeType(
        final ISelection selection,
        final boolean recursive,
        final ChangeType changeType) {
        return selectionAnySearch(selection, new Matcher() {
            @Override
            public boolean matches(final Object object) {
                if (object instanceof TFSItem) {
                    final PendingChange[] pendingChanges = ((TFSItem) object).getPendingChanges(recursive);

                    for (int i = 0; i < pendingChanges.length; i++) {
                        if (pendingChanges[i].getChangeType().containsAny(changeType)) {
                            return true;
                        }
                    }
                }

                return false;
            }
        });
    }

    private static interface Matcher {
        public boolean matches(Object object);
    }

    /**
     * @return true if any item in the selection matches, false if no items
     *         match
     */
    private static boolean selectionAnySearch(final ISelection selection, final Matcher matcher) {
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            for (final Iterator it = structuredSelection.iterator(); it.hasNext();) {
                final Object selectionPart = it.next();
                if (matcher.matches(selectionPart)) {
                    return true;
                }
            }
        }
        return false;
    }
}
