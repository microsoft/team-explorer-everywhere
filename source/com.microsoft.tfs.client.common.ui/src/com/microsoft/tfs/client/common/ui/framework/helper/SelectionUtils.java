// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate;
import com.microsoft.tfs.client.common.ui.framework.action.SelectionProviderAction;
import com.microsoft.tfs.client.common.util.Adapters;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Static utility methods for working with {@link ISelection}s. This class is
 * currently only useful for {@link ISelection}s that are
 * {@link IStructuredSelection}s.
 * </p>
 *
 * <p>
 * One reason for this class is to have a place to hold selection utility
 * methods that are used by action-related classes that don't share a common
 * class hierarchy and can't have a base class in common. For example, both
 * {@link SelectionProviderAction} and {@link ObjectActionDelegate} make use of
 * this class to provide selection utilities to their subclasses.
 * </p>
 *
 * @see ISelection
 * @see IStructuredSelection
 */
public class SelectionUtils {
    /**
     * Obtains the size of the specified {@link ISelection}:
     * <ul>
     * <li>If the selection is <code>null</code>, <code>0</code> is returned
     * </li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>Otherwise, the size of the structured selection is returned</li>
     * </ul>
     *
     * @param selection
     *        the {@link ISelection} to get the size for (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}
     * @return the size of the structured selection
     */
    public static final int getSelectionSize(final ISelection selection) {
        final IStructuredSelection ss = getStructuredSelection(selection);
        return (ss == null ? 0 : ss.size());
    }

    /**
     * Returns the elements contained in the selection as an array. The runtime
     * type of the array is {@link Object}. The array is computed in the
     * following way:
     * <ul>
     * <li>If the selection is <code>null</code>, an empty array is returned
     * </li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>Otherwise, the selection's elements are converted to an array and
     * returned</li>
     * </ul>
     *
     * @param selection
     *        the {@link ISelection} to get the elements for (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}
     * @return the selection elements as an {@link Object} array (never
     *         <code>null</code>)
     */
    public static final Object[] selectionToArray(final ISelection selection) {
        final IStructuredSelection ss = getStructuredSelection(selection);
        return (ss == null ? new Object[0] : ss.toArray());
    }

    /**
     * <p>
     * Returns the elements contained in the selection as an array. The runtime
     * type of the array is specified by <code>targetType</code>. The array is
     * computed in the following way:
     * <ul>
     * <li>If the selection is <code>null</code>, an empty array (of the
     * specified target type) is returned</li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>Otherwise, the selection's elements are converted to an array (of the
     * specified target type) and returned</li>
     * </ul>
     * </p>
     *
     * <p>
     * You must not specify a primitive type as the <code>targetType</code>.
     * </p>
     *
     * @param selection
     *        the {@link ISelection} to get the elements for (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}, or if the specified
     *         <code>targetType</code> is a primitive type
     * @throws ArrayStoreException
     *         if any of the selection elements are not compatible with the
     *         specified <code>targetType</code>
     * @return the selection elements as an array of the target type (never
     *         <code>null</code>)
     */
    public static final Object[] selectionToArray(final ISelection selection, final Class targetType) {
        return selectionToArray(selection, targetType, false);
    }

    /**
     * <p>
     * Returns the elements contained in the selection as an array. The runtime
     * type of the array is specified by <code>targetType</code>. Each element
     * in the selection is adapted to the specified target type. The array is
     * computed in the following way:
     * <ul>
     * <li>If the selection is <code>null</code>, an empty array (of the
     * specified target type) is returned</li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>Otherwise, the selection's elements are adapted and converted to an
     * array (of the specified target type) and returned</li>
     * </ul>
     * </p>
     *
     * <p>
     * You must not specify a primitive type as the <code>targetType</code>.
     * </p>
     *
     * @param selection
     *        the {@link ISelection} to get the elements for (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}, or if the specified
     *         <code>targetType</code> is a primitive type, or if any of the
     *         selection elements could not be adapted to the target type
     * @throws ArrayStoreException
     *         if any of the adapted selection elements are not compatible with
     *         the specified <code>targetType</code>
     * @return the adapted selection elements as an array of the target type
     *         (never <code>null</code>)
     */
    public static final Object[] adaptSelectionToArray(final ISelection selection, final Class targetType) {
        return selectionToArray(selection, targetType, true);
    }

    /**
     * <p>
     * Returns the elements contained in the selection as an array. The runtime
     * type of the array is specified by <code>targetType</code>. If
     * <code>adapt</code> is <code>true</code>, each element in the selection is
     * adapted to the specified target type. The array is computed in the
     * following way:
     * <ul>
     * <li>If the selection is <code>null</code>, an empty array (of the
     * specified target type) is returned</li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>Otherwise, the selection's elements are optionally adapted and
     * converted to an array (of the specified target type) and returned</li>
     * </ul>
     * </p>
     *
     * <p>
     * You must not specify a primitive type as the <code>targetType</code>.
     * </p>
     *
     * @param selection
     *        the {@link ISelection} to get the elements for (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}, or if the specified
     *         <code>targetType</code> is a primitive type, or if
     *         <code>adapt</code> is <code>true</code> and any of the selection
     *         elements could not be adapted to the target type
     * @throws ArrayStoreException
     *         if any of the (possibly adapted) selection elements are not
     *         compatible with the specified <code>targetType</code>
     * @return the (possibly adapted) selection elements as an array of the
     *         target type (never <code>null</code>)
     */
    public static final Object[] selectionToArray(
        final ISelection selection,
        final Class targetType,
        final boolean adapt) {
        Check.notNull(targetType, "targetType"); //$NON-NLS-1$

        final IStructuredSelection structuredSelection = getStructuredSelection(selection);

        if (targetType.isPrimitive()) {
            throw new IllegalArgumentException("You must specify a non-primitive target type"); //$NON-NLS-1$
        }

        final Object[] result =
            (Object[]) Array.newInstance(targetType, (structuredSelection == null ? 0 : structuredSelection.size()));

        if (structuredSelection == null) {
            return result;
        }

        int ix = 0;
        for (final Iterator it = structuredSelection.iterator(); it.hasNext();) {
            final Object sourceElement = it.next();
            Object targetElement = sourceElement;

            if (adapt && sourceElement != null) {
                targetElement = Adapters.getAdapter(sourceElement, targetType);

                if (targetElement == null) {
                    final String messageFormat = "adaptSelectionToArray({0}): could not adapt [{1}]"; //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, targetType.getName(), sourceElement.getClass().getName());
                    throw new IllegalArgumentException(message);
                }
            }

            result[ix++] = targetElement;
        }

        return result;
    }

    /**
     * Gets the first element in the specified selection:
     * <ul>
     * <li>If the selection is <code>null</code>, <code>null</code> is returned
     * </li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>If the selection is an empty structured selection, <code>null</code>
     * is returned</li>
     * <li>Otherwise, the first element of the structured selection is returned
     * </li>
     * </ul>
     *
     * @param selection
     *        the {@link ISelection} to get the first element of (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}
     * @return the first selection element or <code>null</code>
     */
    public static final Object getSelectionFirstElement(final ISelection selection) {
        final IStructuredSelection ss = getStructuredSelection(selection);
        return (ss == null ? null : ss.getFirstElement());
    }

    /**
     * Gets the first element in the specified selection, adapting the element
     * the specified <code>targetType</code>:
     * <ul>
     * <li>If the selection is <code>null</code>, <code>null</code> is returned
     * </li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>If the selection is an empty structured selection, <code>null</code>
     * is returned</li>
     * <li>Otherwise, the first element of the structured selection is adapted
     * and returned</li>
     * </ul>
     *
     * @param selection
     *        the {@link ISelection} to get the first element of (can be
     *        <code>null</code> but if non-<code>null</code> must be an
     *        {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}, or if the first element
     *         was not adaptable to the target type
     * @return the first selection element (adapted to <code>targetType</code>)
     *         or <code>null</code>
     */
    public static final Object adaptSelectionFirstElement(final ISelection selection, final Class targetType) {
        final Object element = getSelectionFirstElement(selection);

        if (element == null) {
            return null;
        }

        final Object adapted = Adapters.getAdapter(element, targetType);

        if (adapted != null) {
            return adapted;
        }

        final String messageFormat = "adaptSelectionFirstElement({0}): could not adapt [{1}]"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, targetType.getName(), element.getClass().getName());
        throw new IllegalArgumentException(message);
    }

    /**
     * Gets the specified {@link ISelection} as an {@link IStructuredSelection}:
     * <ul>
     * <li>If the selection is <code>null</code>, <code>null</code> is returned
     * </li>
     * <li>If the selection is not an {@link IStructuredSelection}, an
     * {@link IllegalArgumentException} is thrown</li>
     * <li>Otherwise, the selection is returned as an
     * {@link IStructuredSelection}</li>
     * </ul>
     *
     * @param selection
     *        an {@link ISelection} (can be <code>null</code> but if non-
     *        <code>null</code> must be an {@link IStructuredSelection})
     * @throws IllegalArgumentException
     *         if the specified {@link ISelection} is not <code>null</code> and
     *         is not an {@link IStructuredSelection}
     * @return the selection as an {@link IStructuredSelection} or
     *         <code>null</code>
     */
    public static final IStructuredSelection getStructuredSelection(final ISelection selection) {
        if (selection == null) {
            return null;
        }

        if (selection instanceof IStructuredSelection) {
            return (IStructuredSelection) selection;
        }

        throw new IllegalArgumentException("the selection is not an IStructuredSelection"); //$NON-NLS-1$
    }
}
