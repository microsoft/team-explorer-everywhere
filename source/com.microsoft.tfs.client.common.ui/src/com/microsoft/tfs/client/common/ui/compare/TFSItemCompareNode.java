// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.compare.ExternalComparable;
import com.microsoft.tfs.client.common.ui.framework.compare.ILabeledCompareElement;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.temp.TempStorageService;

public abstract class TFSItemCompareNode
    implements ITypedElement, IEncodedStreamContentAccessor, IStructureComparator, ILabeledCompareElement,
    ExternalComparable {
    private final VersionControlClient vcClient;

    private final List<TFSItemCompareNode> children = new ArrayList<TFSItemCompareNode>();

    private final Object downloadedFilePathLock = new Object();
    private String downloadedFilePath;

    public TFSItemCompareNode(final VersionControlClient vcClient) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        this.vcClient = vcClient;
    }

    public final void addChild(final TFSItemCompareNode child) {
        /*
         * Could check item.getItemType() here and validate that we're a folder.
         */

        synchronized (children) {
            children.add(child);
        }
    }

    /*
     * START: IStructureComparator
     */

    @Override
    public final Object[] getChildren() {
        synchronized (children) {
            return children.toArray();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof ITypedElement) {
            final String otherName = ((ITypedElement) other).getName();
            return getName().equals(otherName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /*
     * END: IStructureComparator
     */

    /*
     * START: IEncodedStreamContentAccessor
     */

    @Override
    public final InputStream getContents() throws CoreException {
        String path;

        try {
            path = getDownloadedFilePath(new NullProgressMonitor());
        } catch (final InterruptedException e) {
            throw new CoreException(Status.CANCEL_STATUS);
        } catch (final IOException e) {
            throw new CoreException(
                new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
        }

        if (path == null) {
            return null;
        }

        try {
            return new BufferedInputStream(new FileInputStream(path));
        } catch (final FileNotFoundException e) {
            throw new CoreException(
                new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
        }
    }

    /*
     * END: IEncodedStreamContentAccessor
     */

    /*
     * START: ExternalComparable
     */

    @Override
    public final File getExternalCompareFile(final IProgressMonitor monitor) throws IOException, InterruptedException {
        try {
            monitor.beginTask(Messages.getString("TFSItemCompareNode.ProgressPrepareCompare"), 100); //$NON-NLS-1$
            final String path = getDownloadedFilePath(monitor);
            monitor.worked(100);

            return path == null ? null : new File(path);
        } finally {
            monitor.done();
        }
    }

    /*
     * END: ExternalComparable
     */

    protected abstract Item getDownloadItem();

    protected String getDownloadedFilePath(final IProgressMonitor monitor) throws IOException, InterruptedException {
        synchronized (downloadedFilePathLock) {
            if (downloadedFilePath == null) {
                final Item downloadItem = getDownloadItem();

                if (downloadItem.getItemType() == ItemType.FOLDER) {
                    downloadedFilePath = TempStorageService.getInstance().createTempDirectory().getAbsolutePath();
                } else {
                    final String messageFormat = Messages.getString("TFSItemCompareNode.DowloadingFileFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, downloadItem.getServerItem());
                    monitor.setTaskName(message);

                    final String localFileName = ServerPath.getFileName(downloadItem.getServerItem());

                    downloadedFilePath =
                        downloadItem.downloadFileToTempLocation(vcClient, localFileName).getAbsolutePath();
                }
            }

            return downloadedFilePath;
        }
    }
}
