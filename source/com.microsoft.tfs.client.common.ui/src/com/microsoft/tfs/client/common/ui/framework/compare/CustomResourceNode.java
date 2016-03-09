// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;

/**
 * <p>
 * {@link CustomResourceNode} is a saveable compare element implementation that
 * represents an Eclipse workspace resource. This class extends the
 * {@link ResourceNode} implementation that is part of the Eclipse compare
 * framework and adds saveability.
 * </p>
 *
 * <p>
 * {@link CustomResourceNode} implements the {@link IStructureComparator}
 * interface. If the associated resource is a container, the container's
 * children can be turned into child compare elements by the
 * {@link #getChildren()} method. Optionally, a {@link CustomResourceNode} can
 * have a {@link ResourceFilter} that filters the children and selects a subset
 * of them. This can be useful for ignoring resources such as derived resources
 * on one side of a compare.
 * </p>
 */
public class CustomResourceNode extends ResourceNode
    implements ISaveableCompareElement, ILabeledCompareElement, ExternalComparable {
    private final ResourceFilter resourceFilter;
    private List children;

    /**
     * Creates a new {@link CustomResourceNode} that represents the specified
     * resource and performs no filtering when enumerating children.
     *
     * @param resource
     *        the {@link IResource} that this compare element represents (must
     *        not be <code>null</code>)
     */
    public CustomResourceNode(final IResource resource) {
        this(resource, null);
    }

    /**
     * Creates a new {@link CustomResourceNode} that represents the specified
     * resource. When enumerating children, the specified {@link ResourceFilter}
     * is used to select a subset of all children.
     *
     * @param resource
     *        the {@link IResource} that this compare element represents (must
     *        not be <code>null</code>)
     * @param resourceFilter
     *        a filter used when enumerating children, or <code>null</code> to
     *        not filter children during enumeration
     */
    public CustomResourceNode(final IResource resource, final ResourceFilter resourceFilter) {
        super(resource);
        this.resourceFilter = resourceFilter;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getResource().toString();
    }

    /*
     * START: ResourceNode overrides
     */

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.ResourceNode#getChildren()
     */
    @Override
    public Object[] getChildren() {
        if (resourceFilter == null) {
            return super.getChildren();
        }

        if (children == null) {
            children = new ArrayList();
            if (getResource() instanceof IContainer) {
                try {
                    final IResource[] members =
                        Resources.getFilteredMembers((IContainer) getResource(), resourceFilter);

                    for (int i = 0; i < members.length; i++) {
                        final IStructureComparator child = createChild(members[i], resourceFilter);
                        if (child != null) {
                            children.add(child);
                        }
                    }
                } catch (final CoreException ex) {
                    // NeedWork
                }
            }
        }
        return children.toArray();
    }

    /*
     * END: ResourceNode overrides
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
        String label = getResource().getFullPath().toString();
        if (label.charAt(0) == IPath.SEPARATOR) {
            label = label.substring(1);
        }
        return label;
    }

    @Override
    public String getLabelNOLOC() {
        return getLabel();
    }

    /*
     * END: ILabeledCompareElement
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
        final IResource resource = getResource();
        if (resource.getType() != IResource.FILE) {
            return;
        }
        final IFile file = (IFile) resource;

        if (file.isReadOnly()) {
            final IStatus status = file.getWorkspace().validateEdit(new IFile[] {
                file
            }, null);

            if (!status.isOK()) {
                throw new CoreException(status);
            }
        }

        final InputStream is = getContents();
        try {
            if (file.exists()) {
                file.setContents(is, false, true, monitor);
            } else {
                file.create(is, false, monitor);
            }
        } finally {
            try {
                is.close();
            } catch (final IOException e) {
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
        return getResource().getLocation().toFile();
    }

    /*
     * END: ExternalComparable
     */

    /**
     * Subclasses must override this method to instantiate the proper type of
     * object when creating child nodes.
     */
    protected IStructureComparator createChild(final IResource child, final ResourceFilter resourceFilter) {
        return new CustomResourceNode(child, resourceFilter);
    }
}
