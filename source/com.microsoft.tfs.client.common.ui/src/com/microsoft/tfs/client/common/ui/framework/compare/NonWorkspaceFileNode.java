// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;

/**
 * {@link NonWorkspaceFileNode} is a saveable compare element implementation
 * that represents a file on disk. Normally, this compare element is only used
 * if the file is not in the Eclipse workspace. If the file is in the Eclipse
 * workspace, a resource-oriented compare element such as
 * {@link CustomResourceNode} should be used instead.
 */
public class NonWorkspaceFileNode extends BufferedContent
    implements IEncodedStreamContentAccessor, IStructureComparator, ITypedElement, IEditableContent, IModificationDate,
    ISaveableCompareElement, ILabeledCompareElement, ExternalComparable {
    private static final Log log = LogFactory.getLog(NonWorkspaceFileNode.class);

    private final File file;
    private Charset charset;

    private List children;

    /**
     * Creates a new {@link NonWorkspaceFileNode} to represent the specified
     * file.
     *
     * @param file
     *        the file that this compare element represents (must not be
     *        <code>null</code>)
     */
    private NonWorkspaceFileNode(final File file) {
        this(file, null);
    }

    /**
     * Creates a new {@link NonWorkspaceFileNode} to represent the specified
     * file.
     *
     * @param file
     *        the file that this compare element represents (must not be
     *        <code>null</code>)
     * @param charset
     *        the charset that this compare element represents (may be
     *        <code>null</code>)
     */
    public NonWorkspaceFileNode(final File file, final Charset charset) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        this.file = file;
        this.charset = charset;
    }

    /**
     * @return the {@link File} that this compare element represents
     */
    public File getFile() {
        return file;
    }

    /*
     * START: IEncodedStreamContentAccessor
     */

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.BufferedContent#createStream()
     */
    @Override
    protected InputStream createStream() throws CoreException {
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getMessage(), e));
        }
        return new BufferedInputStream(fileInputStream);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.IEncodedStreamContentAccessor#getCharset()
     */
    @Override
    public String getCharset() throws CoreException {
        /*
         * We try to detect the charset if none was given. (We do this here as
         * we're being run off the UI thread, before getCharset() is called.)
         */
        if (charset == null && file.exists() && file.isDirectory() == false) {
            final TFSRepository repository =
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

            /*
             * If we're online, query the pending changes cache for a pending
             * type change.
             */
            if (repository != null) {
                final PendingChange pendingChange =
                    repository.getPendingChangeCache().getPendingChangeByLocalPath(file.getAbsolutePath());

                /* Use the pending encoding change if it exists. */
                if (pendingChange != null && pendingChange.getChangeType().contains(ChangeType.ENCODING)) {
                    charset = CodePageMapping.getCharset(pendingChange.getEncoding(), false);
                }
                /*
                 * Adds and branches should always have encoding changes (this
                 * test should always be true).
                 */
                else if (pendingChange == null
                    || (!pendingChange.getChangeType().contains(ChangeType.ADD)
                        && !pendingChange.getChangeType().contains(ChangeType.BRANCH))) {
                    /*
                     * Make sure this file is in a mapping. (Avoid querying the
                     * server unnecessarily.)
                     */
                    final String serverPath = repository.getWorkspace().getMappedServerPath(file.getAbsolutePath());

                    if (serverPath != null) {
                        /*
                         * See if we can query this item at the workspace
                         * version.
                         */
                        final Item item = repository.getWorkspace().getClient().getItem(
                            serverPath,
                            new WorkspaceVersionSpec(repository.getWorkspace()));

                        if (item != null) {
                            charset = CodePageMapping.getCharset(item.getEncoding().getCodePage(), false);
                        }
                    }
                }
            }
        }

        if (charset != null) {
            return charset.name();
        }

        return ResourcesPlugin.getEncoding();
    }

    /*
     * END: IEncodedStreamContentAccessor
     */

    /*
     * START: IStructureComparator
     */

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.compare.structuremergeviewer.IStructureComparator#getChildren
     * ()
     */
    @Override
    public Object[] getChildren() {
        if (children == null) {
            children = new ArrayList();

            if (file.isDirectory()) {
                final File[] fileChildren = file.listFiles();

                for (int i = 0; i < fileChildren.length; i++) {
                    final IStructureComparator child = createChild(fileChildren[i]);
                    if (child != null) {
                        children.add(child);
                    }
                }
            }
        }

        return children.toArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ITypedElement) {
            final String otherName = ((ITypedElement) other).getName();
            return getName().equals(otherName);
        }
        return super.equals(other);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /*
     * END: IStructureComparator
     */

    /*
     * START: ILabeledCompareElement
     */

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.compare.ILabeledCompareElement#
     * getLabel ()
     */
    @Override
    public String getLabel() {
        return file.getAbsolutePath();
    }

    @Override
    public String getLabelNOLOC() {
        return file.getAbsolutePath();
    }

    /*
     * END: ILabeledCompareElement
     */

    /*
     * START: ITypedElement
     */

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.ITypedElement#getImage()
     */
    @Override
    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.ITypedElement#getName()
     */
    @Override
    public String getName() {
        return file.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.ITypedElement#getType()
     */
    @Override
    public String getType() {
        if (file.isDirectory()) {
            return ITypedElement.FOLDER_TYPE;
        }

        return CompareUtils.computeTypeFromFilename(getName());
    }

    /*
     * END: ITypedElement
     */

    /*
     * START: IEditableContent
     */

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.IEditableContent#isEditable()
     */
    @Override
    public boolean isEditable() {
        return file.canWrite();
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.compare.IEditableContent#replace(org.eclipse.compare.
     * ITypedElement, org.eclipse.compare.ITypedElement)
     */
    @Override
    public ITypedElement replace(final ITypedElement dest, final ITypedElement src) {
        /*
         * same as ResourceNode implementation, may need to do something more
         * sophisticated e.g. something like what BufferedResourceNode does
         */
        return dest;
    }

    /*
     * END: IEditableContent
     */

    /*
     * START: IModificationDate
     */

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.IModificationDate#getModificationDate()
     */
    @Override
    public long getModificationDate() {
        return file.lastModified();
    }

    /*
     * END: IModificationDate
     */

    /*
     * START: ISaveableCompareElement
     */

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.compare.ISaveableCompareElement#save
     * (org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void save(final IProgressMonitor monitor) throws CoreException {
        if (!file.isFile()) {
            return;
        }
        final InputStream in = getContents();
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out = new BufferedOutputStream(out);
            writeInToOut(in, out);
        } catch (final IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getMessage(), e));
        } finally {
            try {
                in.close();
            } catch (final IOException e) {
            }
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                }
            }
        }
    }

    /*
     * END: ISaveableCompareElement
     */

    /*
     * START: ExternalComparable
     */

    @Override
    public File getExternalCompareFile(final IProgressMonitor monitor) {
        return file;
    }

    /*
     * END: ExternalComparable
     */

    /**
     * Subclasses must override this method to instantiate the proper type of
     * object when creating child nodes.
     */
    protected IStructureComparator createChild(final File child) {
        return new NonWorkspaceFileNode(child);
    }

    private void writeInToOut(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[2048];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }
}
