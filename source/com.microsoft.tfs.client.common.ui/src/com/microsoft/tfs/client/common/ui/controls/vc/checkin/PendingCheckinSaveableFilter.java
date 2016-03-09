// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.ui.helpers.WorkbenchPartSaveableFilter;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditor;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInput;
import com.microsoft.tfs.client.common.ui.wit.query.BaseQueryDocumentEditor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinWorkItems;

/**
 * A callback filter for "IWorkBench.saveAll" to determine which dirty editors
 * should be saved. We exclude work items and queries from the automatic save
 * when initiating a check-in except if a dirty work item is part of the
 * check-in. We exclude IResources from the automatic save except if the
 * resource is included in the check-in.
 *
 * @threadsafety unknown
 */
public class PendingCheckinSaveableFilter implements WorkbenchPartSaveableFilter {
    // Contains the ids of work items contained in the check-in.
    final Set<Integer> mapWorkItemIds = new HashSet<Integer>();
    final Set<File> checkinFiles = new HashSet<File>();

    public PendingCheckinSaveableFilter(final PendingCheckin pendingCheckin) {
        // Determine which work items that are part of the check-in.
        final PendingCheckinWorkItems workItems = pendingCheckin.getWorkItems();
        if (workItems != null) {
            final WorkItemCheckinInfo[] infos = workItems.getCheckedWorkItems();
            if (infos != null) {
                for (int i = 0; i < infos.length; i++) {
                    final WorkItemCheckinInfo info = infos[i];
                    mapWorkItemIds.add(new Integer(info.getWorkItem().getID()));
                }
            }
        }

        // Determine which files are part of the check-in
        final PendingCheckinPendingChanges pendingChanges = pendingCheckin.getPendingChanges();
        if (pendingChanges != null) {
            final PendingChange[] checkedChanges = pendingChanges.getCheckedPendingChanges();

            if (checkedChanges != null) {
                for (int i = 0; i < checkedChanges.length; i++) {
                    if (checkedChanges[i].getLocalItem() != null) {
                        checkinFiles.add(new File(checkedChanges[i].getLocalItem()));
                    }
                }
            }
        }
    }

    @Override
    public boolean select(final IWorkbenchPart[] containingParts) {
        if (containingParts.length > 0) {
            final IWorkbenchPart part = containingParts[0];
            if (part instanceof BaseQueryDocumentEditor) {
                // Don't save dirty query edits.
                return false;
            } else if (part instanceof WorkItemEditor) {
                // Save the dirty work item only if it's in the check-in.
                final WorkItemEditor editor = (WorkItemEditor) part;
                final WorkItemEditorInput input = (WorkItemEditorInput) editor.getEditorInput();

                if (input != null && input.getWorkItem() != null) {
                    final int workItemId = input.getWorkItem().getID();
                    if (mapWorkItemIds.contains(new Integer(workItemId))) {
                        return true;
                    }
                }

                // Don't save work item that isn't part of the check-in.
                return false;
            } else if (part instanceof IEditorPart) {
                // Try to get an IResource for this editor part
                final IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
                IResource resource = null;

                if (editorInput instanceof IResource) {
                    resource = (IResource) editorInput;
                } else if (editorInput instanceof IAdaptable) {
                    resource = (IResource) ((IAdaptable) editorInput).getAdapter(IResource.class);
                }

                // Only save this file if it's being checked in
                if (resource != null && resource.getLocation() != null) {
                    return checkinFiles.contains(resource.getLocation().toFile());
                }
            }
        }

        // Unknown item, don't filter it out, allow the platform to prompt for
        // save.
        return true;
    }
}
