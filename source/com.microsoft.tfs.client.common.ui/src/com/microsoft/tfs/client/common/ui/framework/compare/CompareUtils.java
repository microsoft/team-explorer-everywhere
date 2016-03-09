// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.io.File;
import java.nio.charset.Charset;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.util.Adapters;
import com.microsoft.tfs.util.Check;

/**
 * {@link CompareUtils} contains convenience methods for working with the
 * Eclipse and Microsoft compare framework.
 */
public class CompareUtils {
    /**
     * <p>
     * Creates a compare element that represents the specified local path. If
     * the local path corresponds to a resource in the Eclipse workspace, the
     * returned compare element will represent that resource. Otherwise, the
     * returned compare element will not be associated with the Eclipse
     * workspace in any way.
     * </p>
     *
     * <p>
     * This method does not specify a {@link ResourceType}. If the expected
     * resource type of the local path is known, you should instead call one of
     * the overloads that does take a {@link ResourceType}.
     * </p>
     *
     * <p>
     * If the local path does correspond to a resource, no filtering will be
     * done when the compare element enumerates its children. If you want
     * filtering, you should call one of the overloads that takes a
     * {@link ResourceFilter}.
     * </p>
     *
     * @param localPath
     *        the local path to create a compare element for (must not be
     *        <code>null</code>)
     * @return a compare element that corresponds to the local path (never
     *         <code>null</code>)
     */
    public static ITypedElement createCompareElementForLocalPath(final String localPath) {
        return createCompareElementForLocalPath(localPath, null, ResourceType.ANY, null);
    }

    /**
     * <p>
     * Creates a compare element that represents the specified local path. If
     * the local path corresponds to a resource in the Eclipse workspace, the
     * returned compare element will represent that resource. Otherwise, the
     * returned compare element will not be associated with the Eclipse
     * workspace in any way.
     * </p>
     *
     * <p>
     * This method does not specify a {@link ResourceType}. If the expected
     * resource type of the local path is known, you should instead call one of
     * the overloads that does take a {@link ResourceType}.
     * </p>
     *
     * <p>
     * If the local path does correspond to a resource, the
     * {@link ResourceFilter} will be used when the returned compare element is
     * enumerating children.
     * </p>
     *
     * @param localPath
     *        the local path to create a compare element for (must not be
     *        <code>null</code>)
     * @param resourceFilter
     *        a {@link ResourceFilter} to use when enumerating children if the
     *        local path corresponds to a resource, or <code>null</code> to not
     *        filter children in this case
     * @return a compare element that corresponds to the local path (never
     *         <code>null</code>)
     */
    public static ITypedElement createCompareElementForLocalPath(
        final String localPath,
        final ResourceFilter resourceFilter) {
        return createCompareElementForLocalPath(localPath, null, ResourceType.ANY, resourceFilter);
    }

    /**
     * <p>
     * Creates a compare element that represents the specified local path. If
     * the local path corresponds to a resource in the Eclipse workspace, the
     * returned compare element will represent that resource. Otherwise, the
     * returned compare element will not be associated with the Eclipse
     * workspace in any way.
     * </p>
     *
     * <p>
     * The {@link ResourceType} specifies what kind of resource the local path
     * would correspond to, if in fact the local path corresponds to a resource.
     * If this information is not known, {@link ResourceType#ANY} can be passed
     * instead. Performance will be better if an explicit {@link ResourceType}
     * is specified, however.
     * </p>
     *
     * <p>
     * If the local path does correspond to a resource, no filtering will be
     * done when the compare element enumerates its children. If you want
     * filtering, you should call one of the overloads that takes a
     * {@link ResourceFilter}.
     * </p>
     *
     * @param localPath
     *        the local path to create a compare element for (must not be
     *        <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType} that the local path is expected to
     *        correspond to, or {@link ResourceType#ANY} if this information is
     *        not known (must not be <code>null</code>)
     * @return a compare element that corresponds to the local path (never
     *         <code>null</code>)
     */
    public static ITypedElement createCompareElementForLocalPath(
        final String localPath,
        final ResourceType resourceType) {
        return createCompareElementForLocalPath(localPath, null, resourceType, null);
    }

    /**
     * @deprecated use {@link createCompareElementForLocalPath(String, Charset,
     *             ResourceType, ResourceFilter)}
     */
    @Deprecated
    public static ITypedElement createCompareElementForLocalPath(
        final String localPath,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return createCompareElementForLocalPath(localPath, null, resourceType, resourceFilter);
    }

    /**
     * <p>
     * Creates a compare element that represents the specified local path. If
     * the local path corresponds to a resource in the Eclipse workspace, the
     * returned compare element will represent that resource. Otherwise, the
     * returned compare element will not be associated with the Eclipse
     * workspace in any way.
     * </p>
     *
     * <p>
     * The {@link ResourceType} specifies what kind of resource the local path
     * would correspond to, if in fact the local path corresponds to a resource.
     * If this information is not known, {@link ResourceType#ANY} can be passed
     * instead. Performance will be better if an explicit {@link ResourceType}
     * is specified, however.
     * </p>
     *
     * <p>
     * If the local path does correspond to a resource, the
     * {@link ResourceFilter} will be used when the returned compare element is
     * enumerating children.
     * </p>
     *
     * @param localPath
     *        the local path to create a compare element for (must not be
     *        <code>null</code>)
     * @param charset
     *        the charset encoding for this compare element (may be
     *        <code>null</code>, in which case the default platform charset is
     *        used.) Note that this may be overridden by the Eclipse-configured
     *        resource charset if a resource is found in the workspace
     * @param resourceType
     *        the {@link ResourceType} that the local path is expected to
     *        correspond to, or {@link ResourceType#ANY} if this information is
     *        not known (must not be <code>null</code>)
     * @param resourceFilter
     *        a {@link ResourceFilter} to use when enumerating children if the
     *        local path corresponds to a resource, or <code>null</code> to not
     *        filter children in this case
     * @return a compare element that corresponds to the local path (never
     *         <code>null</code>)
     */
    public static ITypedElement createCompareElementForLocalPath(
        final String localPath,
        final Charset charset,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        final IResource resource = Resources.getResourceForLocation(localPath, resourceType, true);

        if (resource != null) {
            return createCompareElementForResource(resource, resourceFilter);
        }

        return new NonWorkspaceFileNode(new File(localPath), charset);
    }

    /**
     * Creates a compare element that represents the specified resource. The
     * returned element will perform no filtering when enumerating children.
     *
     * @param resource
     *        the {@link IResource} to create a compare element for (must not be
     *        <code>null</code>)
     * @return a compare element that represents the resource (never
     *         <code>null</code>)
     */
    public static ITypedElement createCompareElementForResource(final IResource resource) {
        return createCompareElementForResource(resource, null);
    }

    /**
     * Creates a compare element that represents the specified resource. The
     * returned element will use the specified {@link ResourceFilter} when
     * enumerating children.
     *
     * @param resource
     *        the {@link IResource} to create a compare element for (must not be
     *        <code>null</code>)
     * @param resourceFilter
     *        a {@link ResourceFilter} to use when enumerating children, or
     *        <code>null</code> to not filter children
     * @return a compare element that represents the resource (never
     *         <code>null</code>)
     */
    public static ITypedElement createCompareElementForResource(
        final IResource resource,
        final ResourceFilter resourceFilter) {
        return new CustomResourceNode(resource, resourceFilter);
    }

    /**
     * Called by {@link ITypedElement} implementations to compute a suitable
     * return value for the {@link ITypedElement#getType()} method given a file
     * name.
     *
     * @param filename
     *        a filename to compute a type for (must not be <code>null</code>)
     * @return a suitable type for the given filename
     */
    public static String computeTypeFromFilename(final String filename) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        final int index = filename.lastIndexOf('.');

        /*
         * No extension case: return UNKNOWN_TYPE
         */
        if (index == -1) {
            return ITypedElement.UNKNOWN_TYPE;
        }

        /*
         * Filename ends in period case: return the empty string, just like what
         * ResourceNode does
         */
        if (index == (filename.length() - 1)) {
            return ""; //$NON-NLS-1$
        }

        /* Get the file extension */
        final String fileType = filename.substring(index + 1);

        /*
         * Ensure the file extension isn't "FOLDER" and doesn't overlap with the
         * internal "FOLDER" type. This would obviously be problematic.
         */
        if (fileType.equalsIgnoreCase(ITypedElement.FOLDER_TYPE)) {
            return ITypedElement.UNKNOWN_TYPE;
        }

        /*
         * Common case: return the extension
         */
        return fileType;
    }

    /**
     * Gets an image for the specified compare input object. This method tries
     * several ways of getting an image.
     *
     * @param inputObject
     *        the compare input object or <code>null</code>
     * @return an image for the compare input object, or <code>null</code> if no
     *         image could be determined
     */
    public static Image getImage(final Object inputObject) {
        if (inputObject instanceof ITypedElement) {
            final ITypedElement typedElement = (ITypedElement) inputObject;
            final Image image = typedElement.getImage();
            if (image != null) {
                return image;
            }
        }

        if (inputObject instanceof IAdaptable) {
            final IAdaptable adaptable = (IAdaptable) inputObject;
            final Image image = CompareUI.getImage(adaptable);
            if (image != null) {
                return image;
            }
        }

        return null;
    }

    /**
     * Gets a label for the specified compare input object. This method tries
     * several ways of getting a label, including checking whether the object
     * implements the {@link ILabeledCompareElement} interface.
     *
     * @param inputObject
     *        the compare input object or <code>null</code>
     * @return a label for the compare input object, or <code>null</code> if no
     *         label could be determined
     */
    public static String getLabel(final Object inputObject) {
        if (inputObject instanceof ILabeledCompareElement) {
            final ILabeledCompareElement labeledElement = (ILabeledCompareElement) inputObject;
            final String label = labeledElement.getLabel();
            if (label != null) {
                return label;
            }
        }

        final IWorkbenchAdapter adapter = (IWorkbenchAdapter) Adapters.getAdapter(inputObject, IWorkbenchAdapter.class);
        if (adapter != null) {
            final String label = adapter.getLabel(inputObject);
            if (label != null) {
                return label;
            }
        }

        if (inputObject instanceof ITypedElement) {
            final ITypedElement typedElement = (ITypedElement) inputObject;
            final String label = typedElement.getName();
            if (label != null) {
                return label;
            }
        }

        return null;
    }
}
