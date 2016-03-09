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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.framework.compare.ExternalComparable;
import com.microsoft.tfs.client.common.ui.framework.compare.ILabeledCompareElement;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class TFSShelvedChangeNode
    implements ITypedElement, IEncodedStreamContentAccessor, ILabeledCompareElement, ExternalComparable {
    private static final Log log = LogFactory.getLog(TFSShelvedChangeNode.class);

    private final PendingChange pendingChange;
    private final String shelvesetName;
    private final TFSRepository repository;
    private final Object downloadedFilePathLock = new Object();
    private String downloadedFilePath;

    public TFSShelvedChangeNode(
        final PendingChange pendingChange,
        final String shelvesetName,
        final String shelvesetOwner,
        final TFSRepository repository) {
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$
        Check.notNull(shelvesetName, "shelvesetName"); //$NON-NLS-1$
        Check.notNull(shelvesetOwner, "shelvesetOwner"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.pendingChange = pendingChange;
        this.shelvesetName = shelvesetName;
        this.repository = repository;
    }

    @Override
    public String toString() {
        final String messageFormat = Messages.getString("TFSShelvedChangeNode.ToStringFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            pendingChange.getServerItem(),
            Integer.toString(pendingChange.getVersion()));
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
        return ServerPath.getFileName(pendingChange.getServerItem());
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

    @Override
    public InputStream getContents() throws CoreException {
        if (pendingChange.getItemType() == ItemType.FOLDER) {
            return null;
        }

        synchronized (downloadedFilePathLock) {
            if (downloadedFilePath == null) {
                final String localFileName = ServerPath.getFileName(pendingChange.getServerItem());

                downloadedFilePath = pendingChange.downloadShelvedFileToTempLocation(
                    repository.getVersionControlClient(),
                    localFileName).getAbsolutePath();
            }

            try {
                return new BufferedInputStream(new FileInputStream(downloadedFilePath));
            } catch (final FileNotFoundException e) {
                throw new CoreException(
                    new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
            }
        }
    }

    /*
     * END: IEncodedStreamContentAccessor
     */

    /*
     * START: ILabeledCompareElement
     */

    @Override
    public String getLabel() {
        final String messageFormat = Messages.getString("TFSShelvedChangeNode.LabelFormat"); //$NON-NLS-1$

        return MessageFormat.format(
            messageFormat,
            pendingChange.getServerItem(),
            Integer.toString(pendingChange.getVersion()),
            shelvesetName);
    }

    @Override
    public String getLabelNOLOC() {
        final String messageFormat = Messages.getString("TFSShelvedChangeNode.LabelFormat", LocaleUtil.ROOT); //$NON-NLS-1$

        return MessageFormat.format(
            messageFormat,
            pendingChange.getServerItem(),
            Integer.toString(pendingChange.getVersion()),
            shelvesetName);
    }

    /*
     * END: ILabeledCompareElement
     */

    /*
     * START: ExternalComparable
     */

    @Override
    public File getExternalCompareFile(final IProgressMonitor monitor) throws IOException {
        try {
            monitor.beginTask(Messages.getString("TFSShelvedChangeNode.ProgressPreareCompare"), 100); //$NON-NLS-1$
            final InputStream stream = getContents();
            if (stream != null) {
                stream.close();
            }
            monitor.worked(100);
        } catch (final CoreException e) {
            log.error("Error getting external compare file: ", e); //$NON-NLS-1$
            throw new IOException(e.getMessage());
        } finally {
            monitor.done();
        }

        return downloadedFilePath == null ? null : new File(downloadedFilePath);
    }

    /*
     * END: ExternalComparable
     */
}
