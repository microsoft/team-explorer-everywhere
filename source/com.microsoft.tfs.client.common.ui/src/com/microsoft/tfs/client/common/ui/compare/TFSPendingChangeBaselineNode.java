// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * A source of compare content that reads a pending change's "workspace" content
 * from the local workspace baseline file. It falls back to contacting the
 * server if the baseline file is missing or it's a server workspace.
 */
public class TFSPendingChangeBaselineNode extends TFSItemCompareNode {
    private final Workspace workspace;
    private final PendingChange pendingChange;

    private final Object downloadedFilePathLock = new Object();
    private String downloadedFilePath;

    public TFSPendingChangeBaselineNode(final Workspace workspace, final PendingChange pendingChange) {
        super(workspace.getClient());

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$

        this.workspace = workspace;
        this.pendingChange = pendingChange;
    }

    @Override
    public String toString() {
        return getPendingChangeServerOrLocalPath() + ";" + new WorkspaceVersionSpec(workspace).toString(); //$NON-NLS-1$
    }

    /*
     * START: ITypedElement
     */

    @Override
    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    @Override
    public String getName() {
        final String path = getPendingChangeServerOrLocalPath();
        if (ServerPath.isServerPath(path)) {
            return ServerPath.getFileName(path);
        } else {
            return LocalPath.getFileName(path);
        }
    }

    @Override
    public String getType() {
        if (pendingChange.getItemType() == ItemType.FOLDER) {
            return ITypedElement.FOLDER_TYPE;
        }

        return CompareUtils.computeTypeFromFilename(getName());
    }

    /*
     * END: ITypedElement
     */

    /*
     * START: IEncodedStreamContentAccessor
     */

    @Override
    public String getCharset() throws CoreException {
        final int codePage = pendingChange.getEncoding();
        return CodePageMapping.getEncoding(codePage, false, false);
    }

    /*
     * END: IEncodedStreamContentAccessor
     */

    /*
     * START: ILabeledCompareElement
     */

    @Override
    public String getLabel() {
        return MessageFormat.format(
            Messages.getString("TFSPendingChangeBaselineNode.CompareElementLabelFormat"), //$NON-NLS-1$
            getPendingChangeServerOrLocalPath(),
            new WorkspaceVersionSpec(workspace).toString());
    }

    @Override
    public String getLabelNOLOC() {
        return MessageFormat.format(
            Messages.getString("TFSPendingChangeBaselineNode.CompareElementLabelFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            getPendingChangeServerOrLocalPath(),
            new WorkspaceVersionSpec(workspace).toString());
    }

    /*
     * END: ILabeledCompareElement
     */

    /*
     * BEGIN: TFSCompareNode
     */

    @Override
    protected Item getDownloadItem() {
        // Return null because we can't get an Item without querying the server,
        // and we're overriding getDownloadedFilePath anyway.
        return null;
    }

    @Override
    protected String getDownloadedFilePath(final IProgressMonitor monitor) throws IOException, InterruptedException {
        synchronized (downloadedFilePathLock) {
            if (downloadedFilePath == null) {
                if (pendingChange.getItemType() == ItemType.FOLDER) {
                    throw new IOException(Messages.getString("TFSPendingChangeBaselineNode.FoldersNotSupported")); //$NON-NLS-1$
                } else {
                    // Handles contacting the server for server workspaces
                    downloadedFilePath =
                        pendingChange.downloadBaseFileToTempLocation(workspace.getClient(), getName()).getPath();
                }
            }

            return downloadedFilePath;
        }
    }

    private String getPendingChangeServerOrLocalPath() {
        String path = pendingChange.getServerItem();
        if (path == null) {
            path = pendingChange.getLocalItem();
        }
        if (path == null) {
            path = pendingChange.getSourceServerItem();
        }
        if (path == null) {
            path = pendingChange.getSourceLocalItem();
        }

        return path;
    }
}
