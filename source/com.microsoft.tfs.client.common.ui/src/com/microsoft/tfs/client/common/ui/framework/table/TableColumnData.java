// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * <p>
 * {@link TableColumnData} is used to represent the properties of a
 * {@link TableColumn}. {@link TableColumnData}s are created to represent
 * {@link TableColumn}s when a {@link Table} is being set up.
 * </p>
 *
 * <p>
 * An array of {@link TableColumnData} can be passed to either
 * {@link TableUtils#setupTable(Table, boolean, boolean, String, TableColumnData[])}
 * or
 * {@link TableViewerUtils#setupTableViewer(TableViewer, boolean, boolean, String, TableColumnData[])}
 * to initialize an SWT {@link Table} or a JFace {@link TableViewer}. An array
 * of {@link TableColumnData} can also be created by {@link TableControl}
 * subclasses and passed to
 * {@link TableControl#setupTable(boolean, boolean, TableColumnData[])} as a
 * convenient way to initialize a {@link TableControl}'s {@link TableViewer}.
 * </p>
 *
 * @see TableUtils
 * @see TableViewerUtils
 * @see TableControl
 */
public class TableColumnData {
    /**
     * Specifies the SWT style to use when creating the {@link TableColumn} that
     * this {@link TableColumnData} represents. Defaults to
     * <code>SWT.NONE</code>.
     */
    public int style = SWT.NONE;

    /**
     * Specifies the text to set on the {@link TableColumn} that this
     * {@link TableColumnData} represents (the value to pass to
     * {@link TableColumn#setText(String)}). If <code>null</code>, no text will
     * be set on the {@link TableColumn}. Defaults to <code>null</code>.
     */
    public String text;

    /**
     * Specifies the {@link Image} to set on the {@link TableColumn} that this
     * {@link TableColumnData} represents (the value to pass to
     * {@link TableColumn#setImage(Image)}). If <code>null</code>, no image will
     * be set on the {@link TableColumn}. Defaults to <code>null</code>.
     */
    public Image image;

    /**
     * Specifies whether the {@link TableColumn} that this
     * {@link TableColumnData} represents should be user-resizeable (the value
     * to pass to {@link TableColumn#setResizable(boolean)}). Defaults to
     * <code>true</code>.
     */
    public boolean resizeable = true;

    /**
     * Specifies the width to set on the {@link TableColumn} that this
     * {@link TableColumnData} represents (the value to pass to
     * {@link TableColumn#setWidth(int)}). If negative, the value of the
     * {@link #charWidth} field or {@link #percentWidth} field (if any) will be
     * used to set a width on the {@link TableColumn}. Defaults to
     * <code>-1</code>.
     */
    public int width = -1;

    /**
     * Specifies the width <b>in characters</b> to set on the
     * {@link TableColumn} that this {@link TableColumnData} represents. After
     * converting <code>charWidth</code> to a pixel value, the result will be
     * passed to {@link TableColumn#setWidth(int)}. <b>Important</b>: this field
     * will only be used if the {@link #width} field is set to its default value
     * of <code>-1</code>. If both {@link #width} and {@link #charWidth} are
     * negative, no width will be set on the {@link TableColumn}. Defaults to
     * <code>-1</code>.
     */
    public int charWidth = -1;

    /**
     * Specifies the width to set on the {@link TableColumn} that this
     * {@link TableColumnData} represents, as a percentage of the total width of
     * the table as a floating point number. (Ie, 0.33 = 33% of the width.)
     *
     * If there is no fixed width (from {@link #width} or {@link #charWidth}
     * fields, then this will be used to compute the width of the column. If
     * there is a fixed width, then any extra width (ie, whitespace in the
     * table) will be filled based on these percentages.
     */
    public float percentWidth = -1;

    /**
     * The persistence key that should be used to save and restore the column
     * width of the {@link TableColumn} that {@link TableColumnData} represents.
     * If specified and a previously saved column width exists, the previously
     * saved column width overrides any width set in the {@link #width} field.
     * If <code>null</code>, the {@link TableColumn} represented by this
     * {@link TableColumnData} will not have its column width automatically
     * persisted. Defaults to <code>null</code>.
     */
    public String persistenceKey;

    /**
     * The property name that should be used to refer to the {@link TableColumn}
     * that this {@link TableColumnData} represents when the {@link Table} is
     * being used in a JFace {@link TableViewer}. If <code>null</code>, the
     * {@link TableColumn} represented by this {@link TableColumnData} will not
     * automatically have a property name associated with it. Defaults to
     * <code>null</code>.
     */
    public String propertyName;

    /**
     * Creates a new {@link TableColumnData}. All of the fields will have their
     * documented default values.
     */
    public TableColumnData() {

    }

    /**
     * Creates a new {@link TableColumnData}.
     *
     * @param text
     *        the table column text, or <code>null</code> to not set text
     * @param width
     *        the width of the table column, or <code>-1</code> to not set a
     *        width
     * @param identifier
     *        will be used as both the persistence key and the column property
     *        name (can be <code>null</code>)
     */
    public TableColumnData(final String text, final int width, final String identifier) {
        this(text, width, -1, identifier);
    }

    /**
     * Creates a new {@link TableColumnData}.
     *
     * @param text
     *        the table column text, or <code>null</code> to not set text
     * @param percentWidth
     *        the width of the table column as a percentage of 1.00 (the width
     *        of the table), or <code>-1</code> to not set a width
     * @param identifier
     *        will be used as both the persistence key and the column property
     *        name (can be <code>null</code>)
     */
    public TableColumnData(final String text, final float percentWidth, final String identifier) {
        this(text, -1, percentWidth, identifier);
    }

    /**
     * Creates a new {@link TableColumnData}.
     *
     * @param text
     *        the table column text, or <code>null</code> to not set text
     * @param width
     *        the width of the table column, or <code>-1</code> to not set a
     *        width
     * @param percentWidth
     *        the width of the table column as a percentage of 1.00 (the width
     *        of the table), or <code>-1</code> to not set a width
     * @param identifier
     *        will be used as both the persistence key and the column property
     *        name (can be <code>null</code>)
     */
    public TableColumnData(final String text, final int width, final float percentWidth, final String identifier) {
        this.text = text;
        this.width = width;
        this.percentWidth = percentWidth;
        persistenceKey = identifier;
        propertyName = identifier;
    }

    /**
     * Creates a new {@link TableColumnData}.
     *
     * @param image
     *        the table column image, or <code>null</code> to not set an image
     * @param width
     *        the width of the table column, or <code>-1</code> to not set a
     *        width
     * @param identifier
     *        will be used as both the persistence key and the column property
     *        name (can be <code>null</code>)
     */
    public TableColumnData(final Image image, final int width, final String identifier) {
        this(image, width, -1, identifier);
    }

    /**
     * Creates a new {@link TableColumnData}.
     *
     * @param text
     *        the table column text, or <code>null</code> to not set text
     * @param percentWidth
     *        the width of the table column as a percentage of 1.00 (the width
     *        of the table), or <code>-1</code> to not set a width
     * @param identifier
     *        will be used as both the persistence key and the column property
     *        name (can be <code>null</code>)
     */
    public TableColumnData(final Image image, final float percentWidth, final String identifier) {
        this(image, -1, percentWidth, identifier);
    }

    /**
     * Creates a new {@link TableColumnData}.
     *
     * @param text
     *        the table column text, or <code>null</code> to not set text
     * @param width
     *        the width of the table column, or <code>-1</code> to not set a
     *        width
     * @param percentWidth
     *        the width of the table column as a percentage of 1.00 (the width
     *        of the table), or <code>-1</code> to not set a width
     * @param identifier
     *        will be used as both the persistence key and the column property
     *        name (can be <code>null</code>)
     */
    public TableColumnData(final Image image, final int width, final float percentWidth, final String identifier) {
        this.image = image;
        this.width = width;
        this.percentWidth = percentWidth;
        persistenceKey = identifier;
        propertyName = identifier;
    }

    /**
     * Sets the {@link #style} property of this {@link TableColumnData} (see the
     * {@link #style} field for documentation). Returns the receiver for method
     * chaining.
     *
     * @param style
     *        the style (<code>SWT.NONE</code> for the default style)
     * @return this instance
     */
    public TableColumnData setStyle(final int style) {
        this.style = style;
        return this;
    }

    /**
     * Sets the {@link #text} property of this {@link TableColumnData} (see the
     * {@link #text} field for documentation). Returns the receiver for method
     * chaining.
     *
     * @param text
     *        the text, or <code>null</code>
     * @return this instance
     */
    public TableColumnData setText(final String text) {
        this.text = text;
        return this;
    }

    /**
     * Sets the {@link #image} property of this {@link TableColumnData} (see the
     * {@link #image} field for documentation). Returns the receiver for method
     * chaining.
     *
     * @param image
     *        the image, or <code>null</code>
     * @return this instance
     */
    public TableColumnData setImage(final Image image) {
        this.image = image;
        return this;
    }

    /**
     * Sets the {@link #resizeable} property of this {@link TableColumnData}
     * (see the {@link #resizeable} field for documentation). Returns the
     * receiver for method chaining.
     *
     * @param resizeable
     *        the resizeable property
     * @return this instance
     */
    public TableColumnData setResizeable(final boolean resizeable) {
        this.resizeable = resizeable;
        return this;
    }

    /**
     * Sets the {@link #width} property of this {@link TableColumnData} (see the
     * {@link #width} field for documentation). Returns the receiver for method
     * chaining.
     *
     * @param width
     *        the width, or <code>-1</code>
     * @return this instance
     */
    public TableColumnData setWidth(final int width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the {@link #charWidth} property of this {@link TableColumnData} (see
     * the {@link #charWidth} field for documentation). Returns the receiever
     * for method chaining.
     *
     * @param charWidth
     *        the width <b>in characters</b>, or <code>-1</code>
     * @return this instance
     */
    public TableColumnData setCharWidth(final int charWidth) {
        this.charWidth = charWidth;
        return this;
    }

    /**
     * Sets the {@link #persistenceKey} property of this {@link TableColumnData}
     * (see the {@link #persistenceKey} field for documentation). Returns the
     * receiver for method chaining.
     *
     * @param persistenceKey
     *        the persistenceKey, or <code>null</code>
     * @return this instance
     */
    public TableColumnData setPersistenceKey(final String persistenceKey) {
        this.persistenceKey = persistenceKey;
        return this;
    }

    /**
     * Sets the {@link #propertyName} property of this {@link TableColumnData}
     * (see the {@link #propertyName} field for documentation). Returns the
     * receiver for method chaining.
     *
     * @param propertyName
     *        the propertyName, or <code>null</code>
     * @return this instance
     */
    public TableColumnData setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    /**
     * Sets both the {@link #persistenceKey} property and the
     * {@link #propertyName} property of this {@link TableColumnData} (see the
     * {@link #persistenceKey} and {@link #propertyName} fields for
     * documentation). Returns the receiver for method chaining.
     *
     * @param identifier
     *        the persistence key and property name, or <code>null</code>
     * @return this instance
     */
    public TableColumnData setIdentifier(final String identifier) {
        persistenceKey = identifier;
        propertyName = identifier;
        return this;
    }
}
