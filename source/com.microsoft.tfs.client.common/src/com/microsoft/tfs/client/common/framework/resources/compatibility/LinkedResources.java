// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.compatibility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.util.Check;

/**
 * {@link LinkedResources} is a class containing static utility methods for
 * working with linked resources. Currently, this class contains a method for
 * detection of linked resources that is designed to work in both modern and
 * legacy Eclipse environments. If support for pre-Eclipse-3.2 environments is
 * not needed, this class should not be used and the relevant methods on
 * {@link IResource} should be called directly.
 */
public class LinkedResources {
    /**
     * Tracks whether the non-legacy linked resource detection has failed. If
     * <code>true</code>, the legacy detection method should be used instead.
     */
    private static volatile boolean useLegacyMethod = false;

    /**
     * <p>
     * Tests whether an IResource is a linked resource. Note that unlike
     * IResource.isLinked(), this method will return true for both linked
     * resources and children of linked resources (to any level). If you simply
     * want to test whether a resource is explicitly / directly linked and not
     * consider whether it is a descendant of a linked resource, use
     * IResource.isLinked() instead.
     * </p>
     *
     * <p>
     * See the class level documentation for some caveats. If your plugin
     * doesn't need to support pre-Eclipse-3.2 environments, you shouldn't be
     * calling this method.
     * </p>
     *
     * @param resource
     *        an IResource to test (must not be <code>null</code>)
     * @return true if the resource is linked or is a descendant of a linked
     *         resource
     */
    public static boolean isLinked(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        if (!useLegacyMethod) {
            try {
                /*
                 * This is a lot of code to do
                 * resource.isLinked(IResource.CHECK_ANCESTORS);
                 *
                 * Neither IResource.CHECK_ANCESTORS nor IResource.isLinked(int)
                 * exist on Eclipse 3.0, so these need to be accessed
                 * reflectively on new platforms, or else we can't compile our
                 * class on Eclipse 3.0.
                 *
                 * TODO: if we ever dump Eclipse 3.0 support, change this to
                 * resource.isLinked(IResource.CHECK_ANCESTORS);
                 */
                final Field checkAncestorField = IResource.class.getField("CHECK_ANCESTORS"); //$NON-NLS-1$
                final Integer IResource_CHECK_ANCESTORS = (Integer) checkAncestorField.get(null);

                final Method isLinkedMethod = IResource.class.getMethod("isLinked", new Class[] //$NON-NLS-1$
                {
                    int.class
                });

                final Boolean isLinked = (Boolean) isLinkedMethod.invoke(resource, new Object[] {
                    IResource_CHECK_ANCESTORS
                });

                return isLinked.booleanValue();
            } catch (final Exception e) {
                useLegacyMethod = true;
            }
        }

        return isLinkedLegacy(resource);
    }

    private static boolean isLinkedLegacy(final IResource resource) {
        /*
         * Try the direct link test first, since this is fast.
         */
        if (resource.isLinked()) {
            return true;
        }

        /*
         * Projects and the workspace root can't be links.
         */
        final int type = resource.getType();
        if (IResource.PROJECT == type || IResource.ROOT == type) {
            return false;
        }

        /*
         * Standard pre-Eclipse-3.2 way of checking for linked resources. Before
         * Eclipse 3.2, all direct links must be immediate children of a project
         * resource.
         */
        final String linkedParentName = resource.getProjectRelativePath().segment(0);
        final IFolder linkedParent = resource.getProject().getFolder(linkedParentName);
        return linkedParent.isLinked();
    }
}
