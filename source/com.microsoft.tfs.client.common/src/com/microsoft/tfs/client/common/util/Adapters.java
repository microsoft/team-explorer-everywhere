// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.util.Check;

/**
 * {@link Adapters} is a convenience class for working with the
 * {@link IAdaptable} interface and the adaption mechansism in Eclipse. This
 * class consists of static utility methods.
 */
public class Adapters {
    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * getAdapter(inputObject, targetType, false)
     * </pre>
     *
     * @see #getAdapter(Object, Class, boolean)
     */
    public static Object getAdapter(final Object inputObject, final Class targetType) {
        return getAdapter(inputObject, targetType, false);
    }

    /**
     * <p>
     * Attempts to adapt an input {@link Object} to a specified type. The
     * adaption algorithm works as follows:
     * <ul>
     * <li>If the input {@link Object} is <code>null</code>, <code>null</code>
     * is returned</li>
     * <li>If the input {@link Object} is castable to the type specified by
     * <code>targetType</code>, the input {@link Object} is returned</li>
     * <li>If the input {@link Object} implements the {@link IAdaptable}
     * interface, {@link IAdaptable#getAdapter(Class)} is invoked with the
     * <code>targetType</code> argument to attempt to get an adapter</li>
     * <li>If all of the above fails, the {@link Platform}'s
     * {@link IAdapterManager} is used to attempt to get an adapter of the
     * specified type</li>
     * <li>If none of the above methods can produce an adapter of the requested
     * type, then <code>null</code> is returned</li>
     * </ul>
     * </p>
     *
     * <p>
     * If this method returns a non-<code>null</code> result, that result can
     * safely be cast to the specified <code>targetType</code>.
     * </p>
     *
     * <p>
     * The <code>forceLoad</code> parameter specifies the behavior if the
     * {@link Platform}'s {@link IAdapterManager} must be invoked. If
     * <code>forceLoad</code> is <code>true</code>, the
     * {@link IAdapterManager#loadAdapter(Object, String)} will be invoked. If
     * it is <code>false</code>, then
     * {@link IAdapterManager#getAdapter(Object, Class)} is invoked. The
     * documentation for these {@link IAdapterManager} methods explains the
     * differences between the two.
     * </p>
     *
     * @param inputObject
     *        an input {@link Object} to adapt
     * @param targetType
     *        the target {@link Class} to adapt to (must not be
     *        <code>null</code>)
     * @param forceLoad
     *        <code>true</code> to force loading of plugins in the
     *        {@link Platform}'s {@link IAdapterManager} is used (see above)
     * @return <code>null</code> if the input {@link Object} was
     *         <code>null</code> or could not be adapted to the specified type,
     *         or a non-<code>null</code> value if the input {@link Object} was
     *         successfully adapted
     */
    public static Object getAdapter(final Object inputObject, final Class targetType, final boolean forceLoad) {
        Check.notNull(targetType, "targetType"); //$NON-NLS-1$

        if (inputObject == null) {
            return null;
        }

        if (targetType.isInstance(inputObject)) {
            return inputObject;
        }

        if (inputObject instanceof IAdaptable) {
            final IAdaptable adaptable = (IAdaptable) inputObject;
            final Object targetObject = adaptable.getAdapter(targetType);
            if (targetObject != null) {
                return targetObject;
            }
        }

        if (forceLoad) {
            return Platform.getAdapterManager().loadAdapter(inputObject, targetType.getName());
        } else {
            return Platform.getAdapterManager().getAdapter(inputObject, targetType);
        }
    }
}
