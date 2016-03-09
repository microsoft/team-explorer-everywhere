// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.layout;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A builder class for concisely creating {@link GridData} instances. Instead of
 * creating {@link GridData} instances by handing over several lines, a
 * {@link GridDataBuilder} can create one in a much less verbose and more
 * readable way, often using only a single line.
 * </p>
 *
 * <p>
 * Each {@link GridDataBuilder} instance keeps an internal {@link GridData}
 * instance. {@link GridDataBuilder} has a number of public methods that modify
 * the internal {@link GridData} instance, and return the
 * {@link GridDataBuilder} instance that the method was invoked on. This allows
 * method calls to be chained.
 * </p>
 *
 * <p>
 * Two methods allow access to the {@link GridData} being built. The
 * {@link #gridData()} method returns a {@link GridData}, while the
 * {@link #applyTo(Control)} method takes a {@link Control} and sets the
 * control's layout data. In both cases, a <b>copy</b> of the internal
 * {@link GridData} instance is made. This allows a single instance of
 * {@link GridDataBuilder} to be reused for multiple controls (SWT requires that
 * each control must have a unique layout data object).
 * </p>
 *
 * <p>
 * An example that shows method chaining:
 *
 * <pre>
 * GridDataBuilder.newInstance().fill().grab().applyTo(myControl);
 * </pre>
 *
 * </p>
 *
 * <p>
 * If you can take a dependency on Eclipse 3.2, you may want to consider using
 * <code>org.eclipse.jface.layout.GridDataFactory</code> instead of this class.
 * <code>GridDataFactory</code> has a similar design and functionality.
 * </p>
 */
public class GridDataBuilder {
    private static final Log log = LogFactory.getLog(GridDataBuilder.class);

    /**
     * Creates a new {@link GridDataBuilder} instance. This static method is
     * fully equivalent to the public no-args constructor of this class.
     *
     * @return a new {@link GridDataBuilder}
     */
    public static GridDataBuilder newInstance() {
        return new GridDataBuilder();
    }

    /**
     * The private {@link GridData} instance. This field should never be exposed
     * to clients of this class. Any methods that need to expose it should first
     * make a copy using the static {@link #copy(GridData)} method.
     */
    private final GridData gridData;

    /**
     * Creates a new {@link GridDataBuilder}. The default values are the same as
     * when creating a new {@link GridData} instance using the no-args
     * constructor on {@link GridData}.
     */
    public GridDataBuilder() {
        gridData = new GridData();
    }

    /**
     * Creates a new {@link GridDataBuilder} which has initial values specified
     * by an existing {@link GridData} instance. No reference to the specified
     * {@link GridData} instance is held after this constructor returns.
     *
     * @param gridData
     *        a {@link GridData} to get initial values from (must not be
     *        <code>null</code>)
     */
    public GridDataBuilder(final GridData gridData) {
        Check.notNull(gridData, "gridData"); //$NON-NLS-1$

        this.gridData = copy(gridData);
    }

    /**
     * Creates a new {@link GridDataBuilder} which has initial values specified
     * by an existing {@link GridDataBuilder} instance. No reference to the
     * specified {@link GridDataBuilder} is held after this constructor returns.
     *
     * @param gridDataBuilder
     *        a {@link GridDataBuilder} to get initial values from (must not be
     *        <code>null</code>)
     */
    public GridDataBuilder(final GridDataBuilder gridDataBuilder) {
        Check.notNull(gridDataBuilder, "gridDataBuilder"); //$NON-NLS-1$

        gridData = gridDataBuilder.getGridData();
    }

    public GridData getGridData() {
        return copy(gridData);
    }

    public GridDataBuilder vAlign(final int verticalAlignment) {
        gridData.verticalAlignment = verticalAlignment;
        return this;
    }

    public GridDataBuilder vAlignTop() {
        return vAlign(SWT.TOP);
    }

    public GridDataBuilder vAlignCenter() {
        return vAlign(SWT.CENTER);
    }

    public GridDataBuilder vAlignBottom() {
        return vAlign(SWT.BOTTOM);
    }

    public GridDataBuilder vAlignFill() {
        return vAlign(SWT.FILL);
    }

    public GridDataBuilder vFill() {
        return vAlign(SWT.FILL);
    }

    public GridDataBuilder hAlign(final int horizontalAlignment) {
        gridData.horizontalAlignment = horizontalAlignment;
        return this;
    }

    public GridDataBuilder hAlignPrompt() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return hAlignRight();
        }

        return hAlignLeft();
    }

    public GridDataBuilder hAlignLeft() {
        return hAlign(SWT.LEFT);
    }

    public GridDataBuilder hAlignCenter() {
        return hAlign(SWT.CENTER);
    }

    public GridDataBuilder hAlignRight() {
        return hAlign(SWT.RIGHT);
    }

    public GridDataBuilder hAlignFill() {
        return hAlign(SWT.FILL);
    }

    public GridDataBuilder hFill() {
        return hAlign(SWT.FILL);
    }

    public GridDataBuilder fill() {
        hFill();
        vFill();
        return this;
    }

    public GridDataBuilder align(final int horizontalAlignment, final int verticalAlignment) {
        hAlign(horizontalAlignment);
        vAlign(verticalAlignment);
        return this;
    }

    public GridDataBuilder wHint(final int widthHint) {
        gridData.widthHint = widthHint;
        return this;
    }

    public GridDataBuilder wCHint(final Control control, final int charWidth) {
        gridData.widthHint = ControlSize.convertCharWidthToPixels(control, charWidth);
        return this;
    }

    public GridDataBuilder wButtonHint(final Control control) {
        final int widthHint =
            Dialog.convertHorizontalDLUsToPixels(ControlSize.getFontMetrics(control), IDialogConstants.BUTTON_WIDTH);
        final Point minSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        gridData.widthHint = Math.max(widthHint, minSize.x);

        return this;
    }

    public GridDataBuilder hHint(final int heightHint) {
        gridData.heightHint = heightHint;
        return this;
    }

    public GridDataBuilder hCHint(final Control control, final int charHeight) {
        gridData.heightHint = ControlSize.convertCharHeightToPixels(control, charHeight);
        return this;
    }

    public GridDataBuilder hint(final int widthHint, final int heightHint) {
        wHint(widthHint);
        hHint(heightHint);
        return this;
    }

    public GridDataBuilder hint(final Point point) {
        wHint(point.x);
        hHint(point.y);
        return this;
    }

    public GridDataBuilder cHint(final Control control, final int charWidth, final int charHeight) {
        final Point point = ControlSize.convertCharSizeToPixels(control, charWidth, charHeight);
        wHint(point.x);
        hHint(point.y);
        return this;
    }

    public GridDataBuilder hIndent(final int horizontalIndent) {
        gridData.horizontalIndent = horizontalIndent;
        return this;
    }

    public GridDataBuilder vIndent(final int verticalIndent) {
        try {
            final Class gdClass = gridData.getClass();
            final Field viField = gdClass.getField("verticalIndent"); //$NON-NLS-1$

            viField.setInt(gridData, verticalIndent);
        } catch (final Exception e) {
            // PRE SWT 3.1, ignore
        }

        return this;
    }

    public GridDataBuilder hSpan(final int horizontalSpan) {
        gridData.horizontalSpan = horizontalSpan;
        return this;
    }

    public GridDataBuilder hSpan(final GridLayout layout) {
        return hSpan(layout.numColumns);
    }

    public GridDataBuilder vSpan(final int verticalSpan) {
        gridData.verticalSpan = verticalSpan;
        return this;
    }

    public GridDataBuilder span(final int horizontalSpan, final int verticalSpan) {
        hSpan(horizontalSpan);
        vSpan(verticalSpan);
        return this;
    }

    public GridDataBuilder hGrab() {
        return hGrab(true);
    }

    public GridDataBuilder hGrab(final boolean grabExcessHorizontalSpace) {
        gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
        return this;
    }

    public GridDataBuilder vGrab() {
        return vGrab(true);
    }

    public GridDataBuilder vGrab(final boolean grabExcessVerticalSpace) {
        gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
        return this;
    }

    public GridDataBuilder grab() {
        hGrab();
        vGrab();
        return this;
    }

    public GridDataBuilder grab(final boolean grabExcessHorizontalSpace, final boolean grabExcessVerticalSpace) {
        hGrab(grabExcessHorizontalSpace);
        vGrab(grabExcessVerticalSpace);
        return this;
    }

    public GridDataBuilder minWidth(final int width) {
        try {
            final Field field = gridData.getClass().getField("minimumWidth"); //$NON-NLS-1$

            if (field != null) {
                field.set(gridData, new Integer(width));
            }
        } catch (final Exception e) {
            /* Suppress, not available in many versions of Eclipse */
        }

        return this;
    }

    public GridDataBuilder minHeight(final int height) {
        try {
            final Field field = gridData.getClass().getField("minimumHeight"); //$NON-NLS-1$

            if (field != null) {
                field.set(gridData, new Integer(height));
            }
        } catch (final Exception e) {
            /* Suppress, not available in many versions of Eclipse */
        }

        return this;
    }

    public GridDataBuilder min(final int width, final int height) {
        minWidth(width);
        minHeight(height);
        return this;
    }

    @Override
    public String toString() {
        return gridData.toString();
    }

    public GridDataBuilder applyTo(final Control control) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        return applyTo(control, false);
    }

    public GridDataBuilder applyTo(final Control control, final boolean overwrite) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        if (overwrite == false && control.getLayoutData() != null) {
            final String messageFormat =
                "control [{0}] (parent [{1}]) already has a layout data object set - this is most likely a programming error"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, control, control.getParent());
            log.warn(message);
        }

        control.setLayoutData(getGridData());
        return this;
    }

    /**
     * Performs a copy operation on the specified {@link GridData}, returning a
     * new {@link GridData} instance that has all fields set to the same value
     * as the input {@link GridData}.
     *
     * @param in
     *        an input {@link GridData} (must not be <code>null</code>)
     * @return a copy of the input
     */
    private static GridData copy(final GridData in) {
        final GridData out = new GridData();

        /*
         * The copy is done reflectively instead of directly. The GridData class
         * has added lots of fields in different versions of Eclipse since 3.0 -
         * this is the easiest way to get compatibility with all those versions.
         */

        /*
         * PERF: consider caching the final Field[] array.
         */

        final Field[] fields = GridData.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers()) || Modifier.isFinal(fields[i].getModifiers())) {
                /*
                 * skip static and final fields
                 */
                continue;
            }

            fields[i].setAccessible(true);
            try {
                final Object valueToCopy = fields[i].get(in);
                fields[i].set(out, valueToCopy);
            } catch (final IllegalArgumentException e) {
                final String messageFormat = "field [{0}]: {1}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, fields[i].getName(), e.getMessage());
                throw new RuntimeException(message, e);
            } catch (final IllegalAccessException e) {
                final String messageFormat = "field [{0}]: {1}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, fields[i].getName(), e.getMessage());
                throw new RuntimeException(message, e);
            }
        }

        return out;
    }
}
