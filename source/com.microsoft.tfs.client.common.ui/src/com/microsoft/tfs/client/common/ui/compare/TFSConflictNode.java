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
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;
import com.microsoft.tfs.util.temp.TempStorageService;

public class TFSConflictNode
    implements ITypedElement, IEncodedStreamContentAccessor, ILabeledCompareElement, ExternalComparable {
    private final VersionControlClient vcClient;
    private final Conflict conflict;
    private final TFSConflictNodeType nodeType;

    private String label;
    private String labelNOLOC;

    private File downloadedFile;
    private final Object downloadedFileLock = new Object();

    public TFSConflictNode(
        final VersionControlClient vcClient,
        final Conflict conflict,
        final TFSConflictNodeType nodeType) {
        Check.notNull(conflict, "vcClient"); //$NON-NLS-1$
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(nodeType, "nodeType"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.conflict = conflict;
        this.nodeType = nodeType;
    }

    public VersionControlClient getVersionControlClient() {
        return vcClient;
    }

    public Conflict getConflict() {
        return conflict;
    }

    public TFSConflictNodeType getNodeType() {
        return nodeType;
    }

    public String getFilename() {
        if (TFSConflictNodeType.BASE.equals(nodeType) && conflict.getBaseServerItem() != null) {
            return ServerPath.getFileName(conflict.getBaseServerItem());
        } else if (TFSConflictNodeType.YOURS.equals(nodeType) && conflict.getYourServerItem() != null) {
            return ServerPath.getFileName(conflict.getYourServerItem());
        } else if (TFSConflictNodeType.THEIRS.equals(nodeType) && conflict.getTheirServerItem() != null) {
            return ServerPath.getFileName(conflict.getTheirServerItem());
        }

        return conflict.getFileName();
    }

    @Override
    public String getCharset() {
        if (TFSConflictNodeType.BASE.equals(nodeType)) {
            return CodePageMapping.getEncoding(conflict.getBaseEncoding().getCodePage(), false, false);
        } else if (TFSConflictNodeType.YOURS.equals(nodeType)) {
            return CodePageMapping.getEncoding(conflict.getYourEncoding().getCodePage(), false, false);
        } else if (TFSConflictNodeType.THEIRS.equals(nodeType)) {
            return CodePageMapping.getEncoding(conflict.getTheirEncoding().getCodePage(), false, false);
        } else {
            throw new IllegalArgumentException("Unknown compare node type"); //$NON-NLS-1$
        }
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label != null ? label : conflict.getFileName();
    }

    public void setLabelNOLOC(final String labelNOLOC) {
        this.labelNOLOC = labelNOLOC;
    }

    @Override
    public String getLabelNOLOC() {
        return labelNOLOC != null ? labelNOLOC : conflict.getFileName();
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
        return getFilename();
    }

    @Override
    public String getType() {
        return CompareUtils.computeTypeFromFilename(getFilename());
    }

    @Override
    public InputStream getContents() throws CoreException {
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
            monitor.beginTask(Messages.getString("TFSConflictNode.ProgressPrepareCompare"), 100); //$NON-NLS-1$
            final File file = getDownloadedFile(monitor);
            monitor.worked(100);

            return file;
        } finally {
            monitor.done();
        }
    }

    private File getDownloadedFile(final IProgressMonitor monitor) throws IOException, InterruptedException {
        synchronized (downloadedFileLock) {
            if (downloadedFile == null) {
                final File tempFile =
                    TempStorageService.getInstance().createTempFile(getType() == null ? null : "." + getType()); //$NON-NLS-1$

                final String message =
                    MessageFormat.format(Messages.getString("TFSConflictNode.DownloadingFileFormat"), getLabel()); //$NON-NLS-1$
                monitor.setTaskName(message);

                if (TFSConflictNodeType.BASE.equals(nodeType)) {
                    conflict.downloadBaseFile(vcClient, tempFile.getAbsolutePath());
                } else if (TFSConflictNodeType.YOURS.equals(nodeType)) {
                    conflict.downloadYourFile(vcClient, tempFile.getAbsolutePath());
                } else if (TFSConflictNodeType.THEIRS.equals(nodeType)) {
                    conflict.downloadTheirFile(vcClient, tempFile.getAbsolutePath());
                } else {
                    throw new IllegalArgumentException("Unknown compare node type"); //$NON-NLS-1$
                }

                downloadedFile = tempFile;
            }

            return downloadedFile;
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof TFSConflictNode) {
            return vcClient.equals(((TFSConflictNode) other).getVersionControlClient())
                && conflict.equals(((TFSConflictNode) other).getConflict())
                && nodeType.equals(((TFSConflictNode) other).getNodeType());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + vcClient.hashCode();
        result = 37 * result + conflict.hashCode();
        result = 37 * result + nodeType.getValue();

        return result;
    }

    public static class TFSConflictNodeType extends TypesafeEnum {
        public static final TFSConflictNodeType BASE = new TFSConflictNodeType(0);
        public static final TFSConflictNodeType YOURS = new TFSConflictNodeType(1);
        public static final TFSConflictNodeType THEIRS = new TFSConflictNodeType(2);

        private TFSConflictNodeType(final int value) {
            super(value);
        }
    }
}
