// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.sizing;

import org.eclipse.swt.graphics.Point;

/**
 * <p>
 * {@link Alignable} is an interface that can be implemented by related controls
 * that can be aligned with each other when they are hosted in the same
 * container.
 * </p>
 *
 * <p>
 * An {@link Alignable} is a typically a control that has a single
 * variable-sized subcontrol (all other subcontrols either stretch to fill or
 * are the same fixed size across all related controls).
 * {@link #getPreferredAlignSize()} returns the preferred size of the
 * variable-sized subcontrol in isolation. Once the align size of each of the
 * related controls is computed, {@link #setAlignSize(Point)} is called on each
 * control with the maximum align size. This allows each related control to set
 * a size hint so that its variable-sized subcontrol is sized as large as the
 * largest size needed by any of the subcontrols.
 * </p>
 *
 * <p>
 * Use {@link ControlSize#align(Alignable[])} to align a group of
 * {@link Alignable}s.
 * </p>
 */
public interface Alignable {
    /**
     * Compute the preferred align size of this {@link Alignable} in isolation.
     * See the {@link Alignable} javadoc for an explanation.
     *
     * @return the preferred align size for this {@link Alignable} (must not be
     *         <code>null</code>)
     */
    Point getPreferredAlignSize();

    /**
     * Sets the align size hint for this {@link Alignable}. See the
     * {@link Alignable} javadoc for an explanation.
     *
     * @param size
     *        the align size to use for this {@link Alignable} (never
     *        <code>null</code>)
     */
    void setAlignSize(Point size);
}
