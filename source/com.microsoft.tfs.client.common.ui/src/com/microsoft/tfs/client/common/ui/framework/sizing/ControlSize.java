// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.sizing;

import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

import com.microsoft.tfs.util.Check;

/**
 * {@link ControlSize} is a utility class containing static helper methods
 * dealing with control sizing in SWT.
 */
public class ControlSize {
    /**
     * Obtains a {@link FontMetrics} object corresponding to the specified
     * {@link Control}. This method creates and disposes a {@link GC} object to
     * get {@link FontMetrics}.
     *
     * @param control
     *        a {@link Control} to get {@link FontMetrics} for (must not be
     *        <code>null</code>)
     * @return a {@link FontMetrics} object (never <code>null</code>)
     */
    public static FontMetrics getFontMetrics(final Control control) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        /*
         * TODO A possible performance enhancement would be to cache the
         * FontMetrics on a Control (control.setData). This would allow multiple
         * calls to this method for the same control to avoid the overhead of
         * creating and disposing a GC each time. We should do this only if
         * profiling indicates a performance problem. If we do this, we should
         * provide an overload of this method that takes a boolean that controls
         * whether the cache is used or not - useful when the client changes the
         * font on a control in between calls, etc.
         */

        final GC gc = new GC(control);
        try {
            gc.setFont(control.getFont());
            return gc.getFontMetrics();
        } finally {
            gc.dispose();
        }
    }

    /**
     * <p>
     * Designed to be called from the
     * {@link Control#computeSize(int, int, boolean)} method. This helper method
     * is useful when a control wants to have a preferred size that is expressed
     * in terms of a static character width and height.
     * </p>
     *
     * <p>
     * If either of <code>pixelWidthHint</code> or <code>pixelHeightHint</code>
     * parameters is not {@link SWT#DEFAULT}, those values will be returned
     * as-is as per the <code>computeSize</code> contract. Otherwise, the
     * specified <code>preferredCharWidth</code> and/or
     * <code>preferredCharHeight</code> parameters will be used to compute a
     * pixel width and/or height based on the {@link Control}'s current font.
     * </p>
     *
     * <p>
     * The final size is returned as a {@link Point} object, suitable for
     * returning from a control's <code>computeSize</code> method.
     * </p>
     *
     * @param pixelWidthHint
     *        the <code>wHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param pixelHeightHint
     *        the <code>hHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param control
     *        the {@link Control} to compute size for (must not be
     *        <code>null</code>)
     * @param preferredCharWidth
     *        the desired width in characters
     * @param preferredCharHeight
     *        the desired height in characters
     * @return a pixel size expressed as a {@link Point} (never
     *         <code>null</code>)
     */
    public static Point computeCharSize(
        final int pixelWidthHint,
        final int pixelHeightHint,
        final Control control,
        final int preferredCharWidth,
        final int preferredCharHeight) {
        if (pixelWidthHint != SWT.DEFAULT && pixelHeightHint != SWT.DEFAULT) {
            return new Point(pixelWidthHint, pixelHeightHint);
        }

        final Point pt = convertCharSizeToPixels(control, preferredCharWidth, preferredCharHeight);

        final int width = pixelWidthHint != SWT.DEFAULT ? pixelWidthHint : pt.x;
        final int height = pixelHeightHint != SWT.DEFAULT ? pixelHeightHint : pt.y;

        return new Point(width, height);
    }

    /**
     * <p>
     * Designed to be called from the
     * {@link Control#computeSize(int, int, boolean)} method. This helper method
     * is useful when a control wants to have a preferred size that is partially
     * expressed in terms of a static character width.
     * </p>
     *
     * <p>
     * If the <code>pixelWidthHint</code> parameter is not {@link SWT#DEFAULT},
     * that value will be returned as-is as per the <code>computeSize</code>
     * contract. Otherwise, the specified <code>preferredCharWidth</code>
     * parameter will be used to compute a pixel width based on the
     * {@link Control}'s current font.
     * </p>
     *
     * @param pixelWidthHint
     *        the <code>wHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param control
     *        the {@link Control} to compute size for (must not be
     *        <code>null</code>)
     * @param preferredCharWidth
     *        the desired width in characters
     * @return a pixel width
     */
    public static int computeCharWidth(final int pixelWidthHint, final Control control, final int preferredCharWidth) {
        return pixelWidthHint != SWT.DEFAULT ? pixelWidthHint : convertCharWidthToPixels(control, preferredCharWidth);
    }

    /**
     * <p>
     * Designed to be called from the
     * {@link Control#computeSize(int, int, boolean)} method. This helper method
     * is useful when a control wants to have a preferred size that is partially
     * expressed in terms of a static character height.
     * </p>
     *
     * <p>
     * If the <code>pixelHeightHint</code> parameter is not {@link SWT#DEFAULT},
     * that value will be returned as-is as per the <code>computeSize</code>
     * contract. Otherwise, the specified <code>preferredCharHeight</code>
     * parameter will be used to compute a pixel width based on the
     * {@link Control}'s current font.
     * </p>
     *
     * @param pixelHeightHint
     *        the <code>hHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param control
     *        the {@link Control} to compute size for (must not be
     *        <code>null</code>)
     * @param preferredCharHeight
     *        the desired height in characters
     * @return a pixel height
     */
    public static int computeCharHeight(
        final int pixelHeightHint,
        final Control control,
        final int preferredCharHeight) {
        return pixelHeightHint != SWT.DEFAULT ? pixelHeightHint
            : convertCharHeightToPixels(control, preferredCharHeight);
    }

    /**
     * <p>
     * Designed to be called from the
     * {@link Control#computeSize(int, int, boolean)} method. This helper method
     * is useful when a control wants to have a preferred size that is expressed
     * in terms of a static pixel width and height.
     * </p>
     *
     * <p>
     * If either of <code>pixelWidthHint</code> or <code>pixelHeightHint</code>
     * parameters is not {@link SWT#DEFAULT}, those values will be returned
     * as-is as per the <code>computeSize</code> contract. Otherwise, the
     * specified <code>preferredPixelWidth</code> and/or
     * <code>preferredPixelHeight</code> parameters will be returned.
     * </p>
     *
     * <p>
     * The final size is returned as a {@link Point} object, suitable for
     * returning from a control's <code>computeSize</code> method.
     * </p>
     *
     * @param pixelWidthHint
     *        the <code>wHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param pixelHeightHint
     *        the <code>hHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param preferredPixelWidth
     *        the desired width in pixels
     * @param preferredPixelHeight
     *        the desired height in pixels
     * @return a pixel size expressed as a {@link Point} (never
     *         <code>null</code>)
     */
    public static Point computePixelSize(
        final int pixelWidthHint,
        final int pixelHeightHint,
        final int preferredPixelWidth,
        final int preferredPixelHeight) {
        final int width = pixelWidthHint != SWT.DEFAULT ? pixelWidthHint : preferredPixelWidth;
        final int height = pixelHeightHint != SWT.DEFAULT ? pixelHeightHint : preferredPixelHeight;
        return new Point(width, height);
    }

    /**
     * <p>
     * Designed to be called from the
     * {@link Control#computeSize(int, int, boolean)} method. This helper method
     * is useful when a control wants to have a preferred size that is partially
     * expressed in terms of a static character width.
     * </p>
     *
     * <p>
     * If the <code>pixelWidthHint</code> parameter is not {@link SWT#DEFAULT},
     * that value will be returned as-is as per the <code>computeSize</code>
     * contract. Otherwise, the specified <code>preferredPixelWidth</code>
     * parameter will be returned.
     * </p>
     *
     * @param pixelWidthHint
     *        the <code>wHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param preferredPixelWidth
     *        the desired width in pixels
     * @return a pixel width
     */
    public static int computePixelWidth(final int pixelWidthHint, final int preferredPixelWidth) {
        return pixelWidthHint != SWT.DEFAULT ? pixelWidthHint : preferredPixelWidth;
    }

    /**
     * <p>
     * Designed to be called from the
     * {@link Control#computeSize(int, int, boolean)} method. This helper method
     * is useful when a control wants to have a preferred size that is partially
     * expressed in terms of a static character height.
     * </p>
     *
     * <p>
     * If the <code>pixelHeightHint</code> parameter is not {@link SWT#DEFAULT},
     * that value will be returned as-is as per the <code>computeSize</code>
     * contract. Otherwise, the specified <code>preferredPixelHeight</code>
     * parameter will be returned.
     * </p>
     *
     * @param pixelHeightHint
     *        the <code>hHint</code> parameter passed to a control's
     *        <code>computeSize</code> method
     * @param preferredPixelHeight
     *        the desired height in pixels
     * @return a pixel height
     */
    public static int computePixelHeight(final int pixelHeightHint, final int preferredPixelHeight) {
        return pixelHeightHint != SWT.DEFAULT ? pixelHeightHint : preferredPixelHeight;
    }

    /**
     * Returns a size that is the largest preferred size (in both dimensions) of
     * all of the specified controls. That is, the returned size is large enough
     * in either dimension to accommodate the preferred size of any of the
     * specified controls in that dimension.
     *
     * @param controls
     *        the controls to compute size for (must not be <code>null</code>,
     *        must not contains any <code>null</code> elements, and must contain
     *        at least one element)
     * @return the max size (never <code>null</code>)
     */
    public static Point maxSize(final Control[] controls) {
        return maxSize(controls, SWT.DEFAULT, SWT.DEFAULT);
    }

    /**
     * Returns a size that is the largest computed size (in both dimensions) of
     * all of the specified controls. That is, the returned size is large enough
     * in either dimension to accommodate any of the specified controls in that
     * dimension.
     *
     * @param controls
     *        the controls to compute size for (must not be <code>null</code>,
     *        must not contains any <code>null</code> elements, and must contain
     *        at least one element)
     * @param wHint
     *        the <code>wHint</code> value to pass to
     *        {@link Control#computeSize(int, int)}
     * @param hHint
     *        the <code>hHint</code> value to pass to
     *        {@link Control#computeSize(int, int)}
     * @return the max size (never <code>null</code>)
     */
    public static Point maxSize(final Control[] controls, final int wHint, final int hHint) {
        Check.notNull(controls, "controls"); //$NON-NLS-1$

        if (controls.length == 0) {
            throw new IllegalArgumentException("the controls array must have at least one control"); //$NON-NLS-1$
        }

        if (controls[0] == null) {
            throw new IllegalArgumentException("controls[0] is null"); //$NON-NLS-1$
        }

        Point size = controls[0].computeSize(wHint, hHint);

        for (int i = 1; i < controls.length; i++) {
            if (controls[i] == null) {
                throw new IllegalArgumentException("controls[" + i + "] is null"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            size = Geometry.max(size, controls[i].computeSize(wHint, hHint));
        }

        return size;
    }

    /**
     * Aligns the specified group of {@link Alignable}s. See the
     * {@link Alignable} class javadoc for more information.
     *
     * @param alignables
     *        the {@link Alignable}s to align (must not be <code>null</code>),
     *        must contain at least one element, and must not contain any
     *        <code>null</code> elements)
     * @return the align size that was used for this alignment (never
     *         <code>null</code>)
     */
    public static Point align(final Alignable[] alignables) {
        Check.notNull(alignables, "alignables"); //$NON-NLS-1$

        if (alignables.length == 0) {
            throw new IllegalArgumentException("you must pass at least one Alignable"); //$NON-NLS-1$
        }

        if (alignables[0] == null) {
            throw new IllegalArgumentException("alignables[0] is null"); //$NON-NLS-1$
        }

        Point size = alignables[0].getPreferredAlignSize();

        for (int i = 1; i < alignables.length; i++) {
            if (alignables[i] == null) {
                throw new IllegalArgumentException("alignables[" + i + "] is null"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            size = Geometry.max(size, alignables[i].getPreferredAlignSize());
        }

        for (int i = 0; i < alignables.length; i++) {
            alignables[i].setAlignSize(size);
        }

        return size;
    }

    /**
     * Computes the string extent of the specified {@link String} for the
     * specified {@link Control}.
     *
     * @param text
     *        the {@link String} to compute an extent for (must not be
     *        <code>null</code>)
     * @param control
     *        the {@link Control} to compute an extent for (must not be
     *        <code>null</code>)
     * @return the extent as a {@link Point} (never <code>null</code>)
     */
    public static Point getStringExtent(final String text, final Control control) {
        Check.notNull(text, "text"); //$NON-NLS-1$
        Check.notNull(control, "control"); //$NON-NLS-1$

        final GC gc = new GC(control);
        try {
            gc.setFont(control.getFont());
            return gc.stringExtent(text);
        } finally {
            gc.dispose();
        }
    }

    /**
     * <p>
     * Attempts to set a width layout data hint for the specified
     * {@link Control}. The pixel value is computed using the specified
     * character width.
     * </p>
     *
     * <p>
     * If the control already has a layout data object set, and the layout data
     * type is {@link FormData}, {@link GridData}, or {@link RowData}, then that
     * layout data is modified with a hint value. If the control does not have a
     * layout data set, but the control has a parent with a {@link FormLayout},
     * {@link GridLayout}, or {@link RowLayout}, then a new layout data of the
     * corresponding type is set on the control and modified with a hint value.
     * If the control has an unsupported layout data or an unsupported parent
     * layout, an exception is thrown.
     * </p>
     *
     * <p>
     * If <code>charWidthHint</code> is {@link SWT#DEFAULT}, the corresponding
     * layout data member is not set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param charWidthHint
     *        the width hint, specified in characters, or {@link SWT#DEFAULT} to
     *        not set the corresponding layout data member
     */
    public static void setCharWidthHint(final Control control, final int charWidthHint) {
        setCharSizeHints(control, charWidthHint, SWT.DEFAULT);
    }

    /**
     * <p>
     * Attempts to set a height layout data hint for the specified
     * {@link Control}. The pixel value is computed using the specified
     * character height.
     * </p>
     *
     * <p>
     * If the control already has a layout data object set, and the layout data
     * type is {@link FormData}, {@link GridData}, or {@link RowData}, then that
     * layout data is modified with a hint value. If the control does not have a
     * layout data set, but the control has a parent with a {@link FormLayout},
     * {@link GridLayout}, or {@link RowLayout}, then a new layout data of the
     * corresponding type is set on the control and modified with a hint value.
     * If the control has an unsupported layout data or an unsupported parent
     * layout, an exception is thrown.
     * </p>
     *
     * <p>
     * If <code>charHeightHint</code> is {@link SWT#DEFAULT}, the corresponding
     * layout data member is not set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param charHeightHint
     *        the height hint, specified in characters, or {@link SWT#DEFAULT}
     *        to not set the corresponding layout data member
     */
    public static void setCharHeightHint(final Control control, final int charHeightHint) {
        setCharSizeHints(control, SWT.DEFAULT, charHeightHint);
    }

    /**
     * <p>
     * Attempts to set width and height layout data hints for the specified
     * {@link Control}. The pixel values are computed using the specified
     * character width and height.
     * </p>
     *
     * <p>
     * If the control already has a layout data object set, and the layout data
     * type is {@link FormData}, {@link GridData}, or {@link RowData}, then that
     * layout data is modified with hint values. If the control does not have a
     * layout data set, but the control has a parent with a {@link FormLayout},
     * {@link GridLayout}, or {@link RowLayout}, then a new layout data of the
     * corresponding type is set on the control and modified with hint values.
     * If the control has an unsupported layout data or an unsupported parent
     * layout, an exception is thrown.
     * </p>
     *
     * <p>
     * If either <code>charWidthHint</code> or <code>charHeightHint</code> is
     * {@link SWT#DEFAULT}, the corresponding layout data member is not set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param charWidthHint
     *        the width hint, specified in characters, or {@link SWT#DEFAULT} to
     *        not set the corresponding layout data member
     * @param charHeightHint
     *        the height hint, specified in characters, or {@link SWT#DEFAULT}
     *        to not set the corresponding layout data member
     */
    public static void setCharSizeHints(final Control control, final int charWidthHint, final int charHeightHint) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        setSizeHints(control, convertCharSizeToPixels(control, charWidthHint, charHeightHint));
    }

    public static void setSizeHints(final Control control, final Point size) {
        setSizeHints(control, size.x, size.y);
    }

    /**
     * <p>
     * Attempts to set width and height (in pixels) layout data hints for the
     * specified {@link Control}.
     * </p>
     *
     * <p>
     * If the control already has a layout data object set, and the layout data
     * type is {@link FormData}, {@link GridData}, or {@link RowData}, then that
     * layout data is modified with hint values. If the control does not have a
     * layout data set, but the control has a parent with a {@link FormLayout},
     * {@link GridLayout}, or {@link RowLayout}, then a new layout data of the
     * corresponding type is set on the control and modified with hint values.
     * If the control has an unsupported layout data or an unsupported parent
     * layout, an exception is thrown.
     * </p>
     *
     * <p>
     * If either <code>widthHint</code> or <code>heightHint</code> is
     * {@link SWT#DEFAULT}, the corresponding layout data member is not set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param widthHint
     *        the width hint, specified in pixels, or {@link SWT#DEFAULT} to not
     *        set the corresponding layout data member
     * @param heightHint
     *        the height hint, specified in pixels, or {@link SWT#DEFAULT} to
     *        not set the corresponding layout data member
     */
    public static void setSizeHints(final Control control, final int widthHint, final int heightHint) {
        final Object layoutData = control.getLayoutData();

        if (layoutData instanceof FormData) {
            setSizeHints(control, (FormData) layoutData, widthHint, heightHint);
            return;
        }

        if (layoutData instanceof GridData) {
            setSizeHints(control, (GridData) layoutData, widthHint, heightHint);
            return;
        }

        if (layoutData instanceof RowData) {
            setSizeHints(control, (RowData) layoutData, widthHint, heightHint);
            return;
        }

        if (layoutData != null) {
            throw new IllegalArgumentException("The layout data type [" //$NON-NLS-1$
                + layoutData.getClass().getName()
                + "] is not supported by this method"); //$NON-NLS-1$
        }

        final Composite parent = control.getParent();

        if (parent == null) {
            throw new IllegalArgumentException("control does not have a layout data object and parent is null"); //$NON-NLS-1$
        }

        final Layout layout = parent.getLayout();

        if (layout == null) {
            throw new IllegalArgumentException(
                "control does not have a layout data object and parent does not have a layout"); //$NON-NLS-1$
        }

        if (layout instanceof FormLayout) {
            final FormData formData = new FormData();
            control.setLayoutData(formData);
            setSizeHints(control, formData, widthHint, heightHint);
            return;
        }

        if (layout instanceof GridLayout) {
            final GridData gridData = new GridData();
            control.setLayoutData(gridData);
            setSizeHints(control, gridData, widthHint, heightHint);
            return;
        }

        if (layout instanceof RowLayout) {
            final RowData rowData = new RowData();
            control.setLayoutData(rowData);
            setSizeHints(control, rowData, widthHint, heightHint);
            return;
        }

        throw new IllegalArgumentException(
            "control does not have a layout data object and parent layout is unsupported: [" //$NON-NLS-1$
                + layout.getClass().getName()
                + "]"); //$NON-NLS-1$
    }

    /**
     * <p>
     * Sets the <code>width</code> and <code>height</code> members on the
     * specified {@link FormData}.
     * </p>
     *
     * <p>
     * If either <code>widthHint</code> or <code>heightHint</code> is
     * {@link SWT#DEFAULT}, the corresponding {@link FormData} member is not
     * set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param formData
     *        the {@link FormData} instance to modify (must not be
     *        <code>null</code>)
     * @param widthHint
     *        the width hint, specified in pixels, or {@link SWT#DEFAULT} to not
     *        set the <code>width</code> member of the {@link FormData}
     * @param heightHint
     *        the height hint, specified in pixels, or {@link SWT#DEFAULT} to
     *        not set the <code>height</code> member of the {@link FormData}
     */
    private static void setSizeHints(
        final Control control,
        final FormData formData,
        final int widthHint,
        final int heightHint) {
        Check.notNull(control, "control"); //$NON-NLS-1$
        Check.notNull(formData, "formData"); //$NON-NLS-1$

        if (widthHint != SWT.DEFAULT) {
            formData.width = widthHint;
        }

        if (heightHint != SWT.DEFAULT) {
            formData.height = heightHint;
        }
    }

    /**
     * <p>
     * Sets the <code>widthHint</code> and <code>heightHint</code> members on
     * the specified {@link GridData}.
     * </p>
     *
     * <p>
     * If either <code>charWidthHint</code> or <code>charHeightHint</code> is
     * {@link SWT#DEFAULT}, the corresponding {@link GridData} member is not
     * set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param gridData
     *        the {@link GridData} instance to modify (must not be
     *        <code>null</code>)
     * @param widthHint
     *        the width hint, specified in pixels, or {@link SWT#DEFAULT} to not
     *        set the <code>widthHint</code> member of the {@link GridData}
     * @param heightHint
     *        the height hint, specified in pixels, or {@link SWT#DEFAULT} to
     *        not set the <code>heightHint</code> member of the {@link GridData}
     */
    private static void setSizeHints(
        final Control control,
        final GridData gridData,
        final int widthHint,
        final int heightHint) {
        Check.notNull(control, "control"); //$NON-NLS-1$
        Check.notNull(gridData, "gridData"); //$NON-NLS-1$

        if (widthHint != SWT.DEFAULT) {
            gridData.widthHint = widthHint;
        }

        if (heightHint != SWT.DEFAULT) {
            gridData.heightHint = heightHint;
        }
    }

    /**
     * <p>
     * Sets the <code>width</code> and <code>height</code> members on the
     * specified {@link RowData}.
     * </p>
     *
     * <p>
     * If either <code>widthHint</code> or <code>heightHint</code> is
     * {@link SWT#DEFAULT}, the corresponding {@link RowData} member is not set.
     * </p>
     *
     * @param control
     *        the control to compute sizes for (must not be <code>null</code>)
     * @param rowData
     *        the {@link RowData} instance to modify (must not be
     *        <code>null</code>)
     * @param charWidthHint
     *        the width hint, specified in pixels, or {@link SWT#DEFAULT} to not
     *        set the <code>width</code> member of the {@link RowData}
     * @param charHeightHint
     *        the height hint, specified in pixels, or {@link SWT#DEFAULT} to
     *        not set the <code>height</code> member of the {@link RowData}
     */
    private static void setSizeHints(
        final Control control,
        final RowData rowData,
        final int widthHint,
        final int heightHint) {
        Check.notNull(rowData, "rowData"); //$NON-NLS-1$

        if (widthHint != SWT.DEFAULT) {
            rowData.width = widthHint;
        }

        if (heightHint != SWT.DEFAULT) {
            rowData.height = heightHint;
        }
    }

    /**
     * <p>
     * Converts a width specified in characters to a size specified in pixels
     * for the specified {@link Control}.
     * </p>
     *
     * <p>
     * If <code>charWidth</code> is equal to {@link SWT#DEFAULT}, the value
     * {@link SWT#DEFAULT} is returned for the corresponding pixel value. This
     * can be useful when computing layout data hints, since most layout data
     * objects that supported width hints use the value {@link SWT#DEFAULT} to
     * indicate no hint - the default value.
     * </p>
     *
     * @param control
     *        the control to calculate pixel sizes for (must not be
     *        <code>null</code>)
     * @param charWidth
     *        the width, specified in characters, or {@link SWT#DEFAULT}
     * @return the computed size in pixels
     */
    public static int convertCharWidthToPixels(final Control control, final int charWidth) {
        return convertCharSizeToPixels(control, charWidth, SWT.DEFAULT).x;
    }

    /**
     * <p>
     * Converts a height specified in characters to a size specified in pixels
     * for the specified {@link Control}.
     * </p>
     *
     * <p>
     * If <code>charHeight</code> is equal to {@link SWT#DEFAULT}, the value
     * {@link SWT#DEFAULT} is returned for the corresponding pixel value. This
     * can be useful when computing layout data hints, since most layout data
     * objects that supported height hints use the value {@link SWT#DEFAULT} to
     * indicate no hint - the default value.
     * </p>
     *
     * @param control
     *        the control to calculate pixel sizes for (must not be
     *        <code>null</code>)
     * @param charHeight
     *        the height, specified in characters, or {@link SWT#DEFAULT}
     * @return the computed size in pixels
     */
    public static int convertCharHeightToPixels(final Control control, final int charHeight) {
        return convertCharSizeToPixels(control, SWT.DEFAULT, charHeight).y;
    }

    /**
     * <p>
     * Converts a width and height specified in characters to dimensions
     * specified in pixels for the specified {@link Control}.
     * </p>
     *
     * <p>
     * If either <code>charWidth</code> or <code>charHeight</code> are equal to
     * {@link SWT#DEFAULT}, the value {@link SWT#DEFAULT} is used for the
     * corresponding pixel value. This can be useful when computing layout data
     * hints, since most layout data objects that supported width or height
     * hints use the value {@link SWT#DEFAULT} to indicate no hint - the default
     * value.
     * </p>
     *
     * <p>
     * The pixel values are returned as a {@link Point} object, with the
     * calculated pixel width as the <code>x</code> value and the pixel height
     * as the <code>y</code> value.
     * </p>
     *
     * @param control
     *        the control to calculate pixel sizes for (must not be
     *        <code>null</code>)
     * @param charWidth
     *        the width, specified in characters, or {@link SWT#DEFAULT}
     * @param charHeight
     *        the height, specified in characters, or {@link SWT#DEFAULT}
     * @return the computed pixel dimensions as a {@link Point} (never
     *         <code>null</code>)
     */
    public static Point convertCharSizeToPixels(final Control control, final int charWidth, final int charHeight) {
        int pixelWidth = SWT.DEFAULT;
        int pixelHeight = SWT.DEFAULT;

        if (charWidth != SWT.DEFAULT || charHeight != SWT.DEFAULT) {
            final FontMetrics fontMetrics = getFontMetrics(control);

            if (charWidth != SWT.DEFAULT) {
                pixelWidth = charWidth * fontMetrics.getAverageCharWidth();
            }
            if (charHeight != SWT.DEFAULT) {
                pixelHeight = charHeight * fontMetrics.getHeight();
            }
        }

        return new Point(pixelWidth, pixelHeight);
    }
}
