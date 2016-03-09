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

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.framework.compare.ExternalComparable;
import com.microsoft.tfs.client.common.ui.framework.compare.ILabeledCompareElement;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.util.Check;

public class TFSDownloadURLNode
    implements ITypedElement, IEncodedStreamContentAccessor, ILabeledCompareElement, ExternalComparable {
    private final VersionControlClient vcClient;
    private final String downloadURL;
    private final String filename;
    private final String charset;

    private String label;
    private String labelNOLOC;

    private File downloadedFile;
    private final Object downloadedFilePathLock = new Object();

    public TFSDownloadURLNode(
        final VersionControlClient vcClient,
        final String downloadURL,
        final String filename,
        final String charset) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(downloadURL, "downloadURL"); //$NON-NLS-1$
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.downloadURL = downloadURL;
        this.filename = filename;
        this.charset = charset;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label != null ? label : filename;
    }

    public void setLabelNOLOC(final String labelNOLOC) {
        this.labelNOLOC = labelNOLOC;
    }

    @Override
    public String getLabelNOLOC() {
        return labelNOLOC != null ? labelNOLOC : filename;
    }

    public final Object[] getChildren() {
        return new Object[0];
    }

    @Override
    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public String getType() {
        return CompareUtils.computeTypeFromFilename(filename);
    }

    @Override
    public final InputStream getContents() throws CoreException {
        File file;

        try {
            file = getDownloadedFile(new NullProgressMonitor());
        } catch (final InterruptedException e) {
            throw new CoreException(Status.CANCEL_STATUS);
        } catch (final IOException e) {
            throw new CoreException(
                new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
        }

        if (file == null) {
            return null;
        }

        try {
            return new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
        } catch (final FileNotFoundException e) {
            throw new CoreException(
                new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
        }
    }

    @Override
    public final File getExternalCompareFile(final IProgressMonitor monitor) throws IOException, InterruptedException {
        try {
            monitor.beginTask(Messages.getString("TFSDownloadURLNode.ProgressPrepareCompare"), 100); //$NON-NLS-1$
            final File file = getDownloadedFile(monitor);
            monitor.worked(100);

            return file;
        } finally {
            monitor.done();
        }
    }

    private File getDownloadedFile(final IProgressMonitor monitor) throws IOException, InterruptedException {
        synchronized (downloadedFilePathLock) {
            if (downloadedFile == null) {
                final String message =
                    MessageFormat.format(Messages.getString("TFSDownloadURLNode.DownloadingFileFormat"), getLabel()); //$NON-NLS-1$
                monitor.setTaskName(message);

                downloadedFile = vcClient.downloadFileToTempLocation(new DownloadSpec(downloadURL), filename);
            }

            return downloadedFile;
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof TFSDownloadURLNode) {
            return downloadURL.equals(((TFSDownloadURLNode) other).getDownloadURL());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return downloadURL.hashCode();
    }
}
