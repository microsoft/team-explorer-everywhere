// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelManager;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelProvider;
import com.microsoft.tfs.client.common.ui.framework.validation.CheckboxProviderValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.ElementProviderValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxProvider;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementProvider;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.valid.Validator;

/**
 * <p>
 * {@link TableControl} is a base class for table-based UI controls that use a
 * JFace {@link TableViewer}.
 * </p>
 *
 * <p>
 * The SWT table styles used by {@link TableControl} tables always include
 * {@link SWT#BORDER} (unless style is passed with
 * {@link TableControl#NO_BORDER}), {@link SWT#V_SCROLL}, and
 * {@link SWT#H_SCROLL}. Optionally, the client can pass in the styles
 * {@link SWT#FULL_SELECTION}, {@link SWT#HIDE_SELECTION}, {@link SWT#MULTI}, or
 * {@link SWT#CHECK} which will be used if specified.
 * </p>
 *
 * <p>
 * A {@link TableControl} tracks 3 collections of elements: all elements,
 * selected elements, and checked elements (see {@link ElementCollectionType}).
 * These collections are tracked across the lifetime of the {@link TableControl}
 * . Clients can retrieve the current collections, respond to changes in the
 * collections, and access the collections after the underlying {@link Table}
 * has been disposed. The element collections are strongly-typed - the runtime
 * type of the arrays available from this class is specified by an element type.
 * </p>
 *
 * <p>
 * The selected and checked element collections can be tracked automatically
 * without requiring coordination with the subclass. Unfortunately, the
 * all-elements collection can't. The subclass must call
 * {@link #computeElements()} any time the elements collection may have changed.
 * For instance, if the subclass calls {@link #getViewer()} and sets the input
 * manually, the subclass must follow that with a call to
 * {@link #computeElements()}. Convenience methods on this class to set the
 * input ({@link #setElements(Object[])},{@link #setInput(Object)}, etc.)
 * perform this housekeeping task automatically and should be used when
 * possible.
 * </p>
 *
 * <p>
 * Subclasses should usually provide convenience methods for accessing the
 * element collections that have strongly-typed return value types and argument
 * types. For example, if the element type displayed by a subclass is
 * <code>Name</code>, then the suggested convenience methods are:
 * <ul>
 * <li><code>setNames(Name[])</code>: delegates to {@link #setElements(Object[])}
 * </li>
 * <li><code>Name[] getNames()</code>: delegates to {@link #getElements()}</li>
 * <li><code>setSelectedNames(Name[])</code>: delegates to
 * {@link #setSelectedElements(Object[])}</li>
 * <li><code>setSelectedName(Name)</code>: delegates to
 * {@link #setSelectedElement(Object)}</li>
 * <li><code>Name[] getSelectedNames()</code>: delegates to
 * {@link #getSelectedElements()}</li>
 * <li><code>Name getSelectedName()</code>: delegates to
 * {@link #getSelectedElement()}</li>
 * <li><code>setCheckedNames(Name[])</code>: delegates to
 * {@link #setCheckedElements(Object[])}</li>
 * <li><code>Name[] getCheckedNames()</code>: delegates to
 * {@link #getCheckedElements()}</li>
 * </ul>
 * </p>
 *
 * @see TableViewer
 * @see IPostSelectionProvider
 * @see ElementProvider
 * @see CheckboxProvider
 */
public abstract class TableControl extends BaseControl
    implements IPostSelectionProvider, CheckboxProvider, ElementProvider, ICheckable, TableTooltipLabelProvider {
    /**
     * SWT table styles that will always be used.
     */
    private static int TABLE_STYLES = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;

    /**
     * Override the default border.
     */
    public static int NO_BORDER = 1 << 31;

    /**
     * SWT table styles that will only be used if the client passes them in.
     */
    private static int OPTIONAL_TABLE_STYLES = SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.HIDE_SELECTION;

    /**
     * The type of element managed by this {@link TableControl}. Methods that
     * return array data (for example, {@link #getSelectedElements()}) will
     * return arrays that have this component type.
     */
    private final Class elementType;

    /**
     * A key that can be used to persist view data (never <code>null</code>).
     */
    private final String viewDataKey;

    /**
     * The SWT table style that we used when the table was created.
     */
    private final int tableStyles;

    /**
     * The {@link TableViewer} this {@link TableControl} wraps.
     */
    private final TableViewer viewer;

    /**
     * The {@link MenuManager} attached to this {@link TableControl}'s
     * {@link Table}.
     */
    private final MenuManager contextMenu;

    /**
     * An optional text label prompt for the table.
     */
    private Label textLabel = null;

    /**
     * Tracks {@link ISelectionChangedListener}s registered as normal selection
     * changed listeners.
     */
    private final SingleListenerFacade selectionListener = new SingleListenerFacade(ISelectionChangedListener.class);

    /**
     * Tracks {@link ISelectionChangedListener}s registered as post-selection
     * changed listeners.
     */
    private final SingleListenerFacade postSelectionListener =
        new SingleListenerFacade(ISelectionChangedListener.class);

    /**
     * Tracks registered {@link CheckboxListener}s.
     */
    private final SingleListenerFacade checkboxListener = new SingleListenerFacade(CheckboxListener.class);

    /**
     * Tracks registered {@link ElementListener}s.
     */
    private final SingleListenerFacade elementListener = new SingleListenerFacade(ElementListener.class);

    /**
     * Tracks registered {@link ICheckStateListener}s.
     */
    private final SingleListenerFacade checkStateListener = new SingleListenerFacade(ICheckStateListener.class);

    /**
     * Tracks registered {@link IDoubleClickListener}s.
     */
    private final SingleListenerFacade doubleClickListener = new SingleListenerFacade(IDoubleClickListener.class);

    /**
     * The {@link DragSource} we create for the table. Never <code>null</code>
     * and disposed in the {@link #widgetDisposed(DisposeEvent)} method.
     */
    private final DragSource dragSource;

    /**
     * If <code>true</code>, we are using {@link #tooltipMouseTrackListener} to
     * provide per-element table tooltips.
     */
    private boolean enableTooltips;

    /**
     * The table tooltip listener.
     */
    private TableTooltipLabelManager tooltipManager;

    /**
     * If <code>true</code>, a check or uncheck should be propogated to all
     * elements in the current selection.
     */
    private boolean checksAffectSelection = true;

    /**
     * The current elements in this {@link TableControl}, or <code>null</code>
     * if there are no elements. The component type of this array is
     * {@link #elementType}.
     */
    private Object[] allElements;

    /**
     * The currently selected elements in this {@link TableControl}, or
     * <code>null</code> if there are no selected elements. The component type
     * of this array is {@link #elementType}.
     */
    private Object[] selectedElements;

    /**
     * The currently checked elements in this {@link TableControl}, or
     * <code>null</code> if there are no checked elements. The component type of
     * this array is {@link #elementType}.
     */
    private Object[] checkedElements;

    /**
     * The lazily created {@link Clipboard}. This field starts off as
     * <code>null</code>, and a clipboard is allocated if needed. This field
     * will be cleaned up in the {@link #widgetDisposed(DisposeEvent)} method,
     * if necessary.
     */
    private Clipboard clipboard;

    /**
     * The transfer types that will be used to copy data to the clipboard. If
     * <code>null</code> or a 0-length array, no data will be copied to the
     * clipboard.
     */
    private Transfer[] clipboardTransferTypes;

    private boolean persistGeometry = true;

    /**
     * <p>
     * Constructs a new {@link TableControl}. See the class documentation for
     * the supported style bits.
     * </p>
     *
     * <p>
     * Subclasses must indicate the runtime type of the element collections
     * managed by this base class by specifying a {@link Class} argument to this
     * constructor. This type is used to determine the component type of the
     * arrays available from public methods on this base class (for example,
     * {@link #getElements()}).
     * </p>
     *
     * <p>
     * Subclasses may optionally pass in a view data key that can be used any
     * time view data needs to be persisted by this {@link TableControl}. If
     * <code>null</code>, a default view data key will be used which is the
     * short class name of the subclass.
     * </p>
     *
     * @param parent
     *        parent {@link Composite}
     * @param style
     *        style bits as described in the class documentation
     * @param elementType
     *        the element type of this {@link TableControl} (must not be
     *        <code>null</code>)
     * @param viewDataKey
     *        a view data key, or <code>null</code> to compute a default key
     */
    protected TableControl(final Composite parent, final int style, final Class elementType, String viewDataKey) {
        super(parent, style);

        Check.notNull(elementType, "elementType"); //$NON-NLS-1$
        if (elementType.isPrimitive()) {
            final String messageFormat = "a TableControl can't be created with the primitive element type [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, elementType.getName());
            throw new IllegalArgumentException(message);
        }
        this.elementType = elementType;

        if (viewDataKey == null) {
            String className = getClass().getName();
            className = className.substring(className.lastIndexOf('.') + 1);

            String parentName = parent.getClass().getName();
            parentName = parentName.substring(parentName.lastIndexOf('.') + 1);

            String shellName = getShell().getText();
            shellName = shellName.replaceAll("[^a-zA-Z0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$

            viewDataKey = className + "#" + parentName + "#" + shellName; //$NON-NLS-1$ //$NON-NLS-2$
        }
        this.viewDataKey = viewDataKey;

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = getVerticalSpacing() / 2;
        setLayout(layout);

        tableStyles = ((style & NO_BORDER) == NO_BORDER)
            ? ((TABLE_STYLES & ~SWT.BORDER) | (OPTIONAL_TABLE_STYLES & (style & ~NO_BORDER)))
            : (TABLE_STYLES | (OPTIONAL_TABLE_STYLES & style));

        final Table table = new Table(this, tableStyles);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().applyTo(table);

        if (isCheckboxTable()) {
            viewer = new CheckboxTableViewer(table);
        } else {
            viewer = new TableViewer(table);
        }

        viewer.setUseHashlookup(true);

        contextMenu = createContextMenu(table);

        dragSource = new DragSource(viewer.getControl(), DND.DROP_COPY);
        dragSource.addDragListener(new DragSourceAdapter() {
            @Override
            public void dragSetData(final DragSourceEvent event) {
                TableControl.this.dragSetData(event);
            }
        });

        hookTable(table);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                TableControl.this.widgetDisposed(e);
            }
        });

        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(final Object element, final String property) {
                return TableControl.this.canModifyElement(element, property);
            }

            @Override
            public Object getValue(final Object element, final String property) {
                return TableControl.this.getValueToModify(element, property);
            }

            @Override
            public void modify(Object element, final String property, final Object value) {
                if (element instanceof TableItem) {
                    element = ((TableItem) element).getData();
                }

                TableControl.this.modifyElement(element, property, value);
            }
        });
        // Must default column properties in case subclass does not define any.
        viewer.setColumnProperties(new String[0]);
    }

    public void setText(final String text) {
        if (textLabel == null && text != null) {
            textLabel = new Label(this, SWT.NONE);
            textLabel.setText(text);
            textLabel.moveAbove(viewer.getTable());
        } else if (textLabel != null && text != null) {
            textLabel.setText(text);
        } else if (textLabel != null && text == null) {
            textLabel.setText(""); //$NON-NLS-1$
        }
    }

    public String getText() {
        if (textLabel == null) {
            return null;
        }

        return textLabel.getText();
    }

    /**
     * Gets the {@link MenuManager} that serves as the context menu for this
     * {@link TableControl}. The context menu is attached to the {@link Table}
     * control available by calling {@link #getTable()}.
     *
     * @return the context menu for this control (never <code>null</code>)
     */
    public MenuManager getContextMenu() {
        return contextMenu;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jface.viewers.IPostSelectionProvider#
     * addPostSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
        postSelectionListener.addListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jface.viewers.IPostSelectionProvider#
     * removePostSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
        postSelectionListener.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionListener.addListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#
     * removeSelectionChangedListener
     * (org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionListener.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    @Override
    public ISelection getSelection() {
        if (selectedElements == null) {
            return StructuredSelection.EMPTY;
        }

        /*
         * Note: StructuredSelection's constructor makes a copy of the passed-in
         * array.
         */
        return new StructuredSelection(selectedElements);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse
     * .jface.viewers.ISelection)
     */
    @Override
    public void setSelection(final ISelection selection) {
        viewer.setSelection(selection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCheckboxListener(final CheckboxListener listener) {
        throwIfNotCheckboxTable();
        checkboxListener.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCheckboxListener(final CheckboxListener listener) {
        throwIfNotCheckboxTable();
        checkboxListener.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addElementListener(final ElementListener listener) {
        elementListener.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeElementListener(final ElementListener listener) {
        elementListener.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.viewer.ElementProvider#
     * getElements ()
     */
    @Override
    public Object[] getElements() {
        return getElementCollection(ElementCollectionType.ALL_ELEMENTS);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ICheckable#addCheckStateListener(org.eclipse
     * .jface.viewers.ICheckStateListener)
     */
    @Override
    public void addCheckStateListener(final ICheckStateListener listener) {
        throwIfNotCheckboxTable();
        checkStateListener.addListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ICheckable#removeCheckStateListener(org.eclipse
     * .jface.viewers.ICheckStateListener)
     */
    @Override
    public void removeCheckStateListener(final ICheckStateListener listener) {
        throwIfNotCheckboxTable();
        checkStateListener.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ICheckable#getChecked(java.lang.Object)
     */
    @Override
    public boolean getChecked(final Object element) {
        throwIfNotCheckboxTable();
        return ((CheckboxTableViewer) viewer).getChecked(element);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ICheckable#setChecked(java.lang.Object,
     * boolean)
     */
    @Override
    public boolean setChecked(final Object element, final boolean state) {
        throwIfNotCheckboxTable();
        final boolean success = ((CheckboxTableViewer) viewer).setChecked(element, state);

        if (success) {
            /**
             * Programmatically setting check state
             * (CheckboxTableViewer#setChecked()) does not fire a
             * CheckStateChangedEvent. We need to manually trigger a recompute
             * of the checked element set.
             */
            computeCheckedElements(true);
        }

        return success;
    }

    /**
     * @return the number of elements currently contained in this control
     */
    public int getCount() {
        return getElementCollectionCount(ElementCollectionType.ALL_ELEMENTS);
    }

    /**
     * Obtains the currently selected elements in this {@link TableControl}. The
     * component type of the returned array is the element type that was
     * specified at construction time.
     *
     * @return the currently selected elements or an empty array if there are no
     *         currently selected elements (never returns <code>null</code>)
     */
    public Object[] getSelectedElements() {
        return getElementCollection(ElementCollectionType.SELECTED_ELEMENTS);
    }

    /**
     * Obtains the first of the currently selected elements in this
     * {@link TableControl}.
     *
     * @return the first selected element, or <code>null</code> if no elements
     *         are currently selected
     */
    public Object getSelectedElement() {
        if (selectedElements == null || selectedElements.length == 0) {
            return null;
        }
        return selectedElements[0];
    }

    /**
     * Sets the currently selected elements in this {@link TableControl}. Any
     * existing selection is discarded. Any changes in the selected elements
     * will be reported to registered {@link ISelectionChangedListener}s.
     *
     * @param elementsToSelect
     *        the elements that should be selected in this control, or
     *        <code>null</code> if no elements should be selected
     */
    public void setSelectedElements(final Object[] elementsToSelect) {
        ISelection selection;

        if (elementsToSelect == null) {
            selection = StructuredSelection.EMPTY;
        } else {
            selection = new StructuredSelection(elementsToSelect);
        }

        setSelection(selection);
    }

    /**
     * Sets the currently selected element in this {@link TableControl}. Any
     * existing selection is discarded. Any changes in the selected elements
     * will be reported to registered {@link ISelectionChangedListener}s.
     *
     * @param elementToSelect
     *        the element that should be selected in this control, or
     *        <code>null</code> if no element should be selected
     */
    public void setSelectedElement(final Object elementToSelect) {
        ISelection selection;

        if (elementToSelect == null) {
            selection = StructuredSelection.EMPTY;
        } else {
            selection = new StructuredSelection(elementToSelect);
        }

        setSelection(selection);
    }

    /**
     * Sets the currently selected element in this {@link TableControl} to be
     * the first element. Any existing selection is discarded. Any changes in
     * the selected elements will be reported to registered
     * {@link ISelectionChangedListener}s. If there are no elements currently in
     * this {@link TableControl}, this method does nothing.
     */
    public void selectFirst() {
        final Table table = getTable();
        if (table.getItemCount() == 0) {
            return;
        }
        final Object element = table.getItem(0).getData();
        setSelection(new StructuredSelection(element));
    }

    /**
     * Sets every element currently in this {@link TableControl} to be selected.
     * Any existing selection is discarded. Any changes in the selected elements
     * will be reported to registered {@link ISelectionChangedListener}s.
     */
    public void selectAll() {
        final Table table = getTable();

        final TableItem[] items = table.getItems();
        final List elements = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            final Object element = items[i].getData();
            if (!hideElementFromCollections(element)) {
                elements.add(element);
            }
        }
        setSelection(new StructuredSelection(elements));
    }

    /**
     * Unselects all elements in this {@link TableControl}. Any existing
     * selection is discarded. Any changes in the selected elements will be
     * reported to registered {@link ISelectionChangedListener}s.
     */
    public void unselectAll() {
        setSelection(StructuredSelection.EMPTY);
    }

    /**
     * @return the number of elements currently selected
     */
    public int getSelectionCount() {
        return getElementCollectionCount(ElementCollectionType.SELECTED_ELEMENTS);
    }

    /**
     * Obtains the currently checked elements in this {@link TableControl}. The
     * component type of the returned array is the element type that was
     * specified at construction time. If this {@link TableControl} was not
     * constructed with the {@link SWT#CHECK} style, an exception is thrown.
     *
     * @return the currently checked elements or an empty array if there are no
     *         currently checked elements (never returns <code>null</code>)
     */
    @Override
    public Object[] getCheckedElements() {
        return getElementCollection(ElementCollectionType.CHECKED_ELEMENTS);
    }

    /**
     * Sets the currently checked elements in this {@link TableControl}. Any
     * existing check state is discarded. Any changes in the checked elements
     * will be reported to registered {@link CheckboxListener}s. If this
     * {@link TableControl} was not constructed with the {@link SWT#CHECK}
     * style, an exception is thrown.
     *
     * @param elementsToCheck
     *        the elements that should be checked in this control, or
     *        <code>null</code> if no elements should be checked
     */
    public void setCheckedElements(Object[] elementsToCheck) {
        throwIfNotCheckboxTable();

        if (elementsToCheck == null) {
            elementsToCheck = new Object[0];
        }

        ((CheckboxTableViewer) viewer).setCheckedElements(elementsToCheck);

        /**
         * Programmatically setting check state
         * (CheckboxTableViewer#setCheckedElements()) does not fire a
         * CheckStateChangedEvent. We need to manually trigger a recompute of
         * the checked element set.
         */
        computeCheckedElements(true);
    }

    /**
     * Sets all elements currently in this {@link TableControl} to be checked.
     * Any existing check state is discarded. Any changes in the checked
     * elements will be reported to registered {@link CheckboxListener}s. If
     * this {@link TableControl} was not constructed with the {@link SWT#CHECK}
     * style, an exception is thrown.
     */
    public void checkAll() {
        throwIfNotCheckboxTable();
        ((CheckboxTableViewer) viewer).setAllChecked(true);

        /**
         * Programmatically setting check state
         * (CheckboxTableViewer#setAllChecked()) does not fire a
         * CheckStateChangedEvent. We need to manually trigger a recompute of
         * the checked element set.
         */
        computeCheckedElements(true);
    }

    /**
     * Unchecks all elements currently in this {@link TableControl}. Any
     * existing check state is discarded. Any changes in the checked elements
     * will be reported to registered {@link CheckboxListener}s. If this
     * {@link TableControl} was not constructed with the {@link SWT#CHECK}
     * style, an exception is thrown.
     */
    public void uncheckAll() {
        throwIfNotCheckboxTable();
        ((CheckboxTableViewer) viewer).setAllChecked(false);

        /**
         * Programmatically setting check state
         * (CheckboxTableViewer#setAllChecked()) does not fire a
         * CheckStateChangedEvent. We need to manually trigger a recompute of
         * the checked element set.
         */
        computeCheckedElements(true);
    }

    /**
     * Sets the check state of the specified element to checked. Any existing
     * check state of other elements in this {@link TableControl} is preserved.
     * Any changes in the checked elements will be reported to registered
     * {@link CheckboxListener}s. If this {@link TableControl} was not
     * constructed with the {@link SWT#CHECK} style, an exception is thrown.
     *
     * @param element
     *        an element to check (must not be <code>null</code>)
     * @return <code>true</code> if the checked state could be set, and
     *         <code>false</code> otherwise
     */
    public boolean setChecked(final Object element) {
        return setChecked(element, true);
    }

    /**
     * Sets the check state of the specified element to unchecked. Any existing
     * check state of other elements in this {@link TableControl} is preserved.
     * Any changes in the checked elements will be reported to registered
     * {@link CheckboxListener}s. If this {@link TableControl} was not
     * constructed with the {@link SWT#CHECK} style, an exception is thrown.
     *
     * @param element
     *        an element to uncheck (must not be <code>null</code>)
     * @return <code>true</code> if the checked state could be set, and
     *         <code>false</code> otherwise
     */
    public boolean setUnchecked(final Object element) {
        return setChecked(element, false);
    }

    /**
     * @return the number of elements currently checked in this control
     */
    public int getCheckedProjectsCount() {
        return getElementCollectionCount(ElementCollectionType.CHECKED_ELEMENTS);
    }

    /**
     * Adds an {@link IDoubleClickListener} that will be notified when an
     * element in this {@link TableControl} is double-clicked.
     *
     * @param listener
     *        an {@link IDoubleClickListener} to add (must not be
     *        <code>null</code>)
     */
    public void addDoubleClickListener(final IDoubleClickListener listener) {
        doubleClickListener.addListener(listener);
    }

    /**
     * Removes a previously-added {@link IDoubleClickListener} from this
     * {@link TableControl}.
     *
     * @param listener
     *        an {@link IDoubleClickListener} to remove (must not be
     *        <code>null</code>)
     */
    public void removeDoubleClickListener(final IDoubleClickListener listener) {
        doubleClickListener.removeListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
     */
    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        return ControlSize.computeCharSize(wHint, hHint, viewer.getControl(), 60, 5);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Composite#setFocus()
     */
    @Override
    public boolean setFocus() {
        return viewer.getControl().setFocus();
    }

    /**
     * Sets an {@link ILabelDecorator} for this control to use. Any previous
     * {@link ILabelDecorator} will be ignored. This method is intended for use
     * with the workbench label decoration mechanism.
     *
     * @param labelDecorator
     *        a new {@link ILabelDecorator} to use (must not be
     *        <code>null</code>)
     */
    public void setLabelDecorator(final ILabelDecorator labelDecorator) {
        Check.notNull(labelDecorator, "labelDecorator"); //$NON-NLS-1$

        ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider();

        if (labelProvider instanceof DecoratingLabelProvider) {
            final DecoratingLabelProvider decoratingLabelProvider = (DecoratingLabelProvider) labelProvider;
            labelProvider = decoratingLabelProvider.getLabelProvider();
        }

        labelProvider = new DecoratingLabelProvider(labelProvider, labelDecorator);
        viewer.setLabelProvider(labelDecorator);
    }

    /**
     * @return a {@link Validator} which validates that this
     *         {@link TableControl} contains at least one element
     */
    public Validator getElementsValidator() {
        return new ElementProviderValidator(this, NumericConstraint.ONE_OR_MORE, getElementsValidatorErrorMessage());
    }

    /**
     * @return a {@link Validator} which validates that this
     *         {@link TableControl} has at least one selected element
     */
    public Validator getSelectionValidator() {
        return new SelectionProviderValidator(this, NumericConstraint.ONE_OR_MORE, getSelectionValidatorErrorMessage());
    }

    /**
     * @return a {@link Validator} which validates that this
     *         {@link TableControl} has exactly one selected element
     */
    public Validator getSingleSelectionValidator() {
        return new SelectionProviderValidator(
            this,
            NumericConstraint.EXACTLY_ONE,
            getSingleSelectionValidatorErrorMessage());
    }

    /**
     * @return a {@link Validator} which validates that this
     *         {@link TableControl} has at least one checked element
     */
    public Validator getCheckboxValidator() {
        throwIfNotCheckboxTable();

        return new CheckboxProviderValidator(this, NumericConstraint.ONE_OR_MORE, getCheckboxValidatorErrorMessage());
    }

    /**
     * Calls {@link TableViewer#refresh()} on the underlying {@link TableViewer}
     * .
     */
    public void refresh() {
        viewer.refresh();
        computeElements();
    }

    /**
     * @return the SWT {@link Table} that this {@link TableControl} wraps
     */
    public final Table getTable() {
        return viewer.getTable();
    }

    /**
     * @return the JFace {@link TableViewer} that this {@link TableControl}
     *         wraps
     */
    public final TableViewer getViewer() {
        return viewer;
    }

    /**
     * Copies the currently selected elements to the clipboard. The exact format
     * of the clipboard data is specified by the {@link TableControl} subclass.
     * If there are no currently selected elements, or if the
     * {@link TableControl} subclass has not configured clipboard transfer, this
     * method does nothing.
     */
    public void copySelectionToClipboard() {
        final Object[] selectedElements = getSelectedElements();

        if (selectedElements.length == 0) {
            return;
        }

        final Transfer[] transferTypes = clipboardTransferTypes;

        if (transferTypes == null || transferTypes.length == 0) {
            return;
        }

        final Object[] transferData = new Object[transferTypes.length];
        for (int i = 0; i < transferTypes.length; i++) {
            transferData[i] = getTransferData(transferTypes[i], selectedElements);
        }

        getClipboard().setContents(transferData, transferTypes);
    }

    /**
     * Obtains one of the element collections from this {@link TableControl}.
     * The returned array is safe to use (it is a copy of internal data) and is
     * strongly typed to the element type of this {@link TableControl}.
     *
     * @param type
     *        the type of the element collection to return (must not be
     *        <code>null</code>)
     * @return the element collection specified by <code>type</code> (never
     *         <code>null</code>)
     */
    public Object[] getElementCollection(final ElementCollectionType type) {
        final Object[] elements = getElementCollectionInternal(type);

        if (elements == null) {
            return newEmptyArray();
        }

        return elements.clone();
    }

    /**
     * Obtains the count of one of the element collections of this
     * {@link TableControl}.
     *
     * @param type
     *        the type of the element collection to return the count of (must
     *        not be <code>null</code>)
     * @return the count of the element collection specified by
     *         <code>type</code>
     */
    public int getElementCollectionCount(final ElementCollectionType type) {
        final Object[] elements = getElementCollectionInternal(type);

        if (elements == null) {
            return 0;
        }

        return elements.length;
    }

    /**
     * @return the highest index of a selected item, or <code>-1</code> if there
     *         are no selected items
     */
    public int getMaxSelectionIndex() {
        return TableUtils.getMaxSelectionIndex(getTable());
    }

    /**
     * Obtains the element (model object) at the specified index.
     *
     * @throws IllegalArgumentException
     *         if the index is invalid
     *
     * @param index
     *        the index of the item to return
     * @return the element at the specified index
     */
    public Object getElement(final int index) {
        final Table table = getTable();
        final int count = table.getItemCount();
        if (index < 0 || index >= count) {
            final String messageFormat = "index [{0}] is out of range [0,{1}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(index), count);
            throw new IllegalArgumentException(message);
        }

        return table.getItem(index).getData();
    }

    /**
     * Initiates cell editing on the specified element. The column that is
     * edited is specified by property name.
     *
     * @param element
     *        the element to edit (must not be <code>null</code>)
     * @param columnPropertyName
     *        the property name of the column to edit (must not be
     *        <code>null</code>)
     */
    public void editElement(final Object element, final String columnPropertyName) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        viewer.editElement(element, columnIndex);
    }

    /**
     * Sets the input of this {@link TableControl} to the specified elements
     * array. Subclasses that call this method do not need to follow it with a
     * call to {@link #computeElements()} as this is done automatically. Any
     * existing elements are not preserved. Any changes to the element
     * collections are reported to registered listeners.
     *
     * @param tableElements
     *        an elements array to set as the input of this {@link TableControl}
     *        , or <code>null</code> to clear the table
     */
    protected final void setElements(final Object[] tableElements) {
        setInput(tableElements);
    }

    /**
     * Sets the input of this {@link TableControl}. Subclasses that call this
     * method do not need to follow it with a call to {@link #computeElements()}
     * as this is done automatically. Any existing elements are not preserved.
     * Any changes to the element collections are reported to registered
     * listeners.
     *
     * @param input
     *        a new input for this {@link TableControl}
     */
    protected final void setInput(final Object input) {
        getViewer().setInput(input);
        computeElements();
    }

    protected final void removeElements(final Object[] removeElements) {
        Check.notNull(removeElements, "removeElements"); //$NON-NLS-1$

        final List newElements = new ArrayList();

        for (int i = 0; i < allElements.length; i++) {
            boolean remove = false;

            for (int j = 0; j < removeElements.length; j++) {
                if (allElements[i].equals(removeElements[j])) {
                    remove = true;
                    break;
                }
            }

            if (!remove) {
                newElements.add(allElements[i]);
            }
        }

        setInput(newElements.toArray(new Object[newElements.size()]));
    }

    /**
     * <p>
     * Throws an {@link IllegalStateException} if this {@link TableControl} was
     * not created with the {@link SWT#CHECK} style bit.
     * </p>
     *
     * <p>
     * If this method does not throw, it is safe to cast the result of the
     * {@link #getViewer()} method to a {@link CheckboxTableViewer}.
     * </p>
     */
    protected final void throwIfNotCheckboxTable() {
        if (isCheckboxTable()) {
            return;
        }
        throw new IllegalStateException("This TableControl was not created with the SWT.CHECK style"); //$NON-NLS-1$
    }

    /**
     * Tests whether this {@link TableControl}'s table was created with the
     * {@link SWT#CHECK} style bit. If this method returns <code>true</code>, it
     * is safe to cast the result of the {@link #getViewer()} method to a
     * {@link CheckboxTableViewer}.
     *
     * @return <code>true</code> if this {@link TableControl} was created with
     *         the {@link SWT#CHECK} style bit
     */
    protected final boolean isCheckboxTable() {
        return (tableStyles & SWT.CHECK) != 0;
    }

    /**
     * @return the SWT table styles that were used to create the {@link Table}
     *         this {@link TableControl} wraps
     */
    protected final int getTableStyles() {
        return tableStyles;
    }

    /**
     * Sets whether to propagate checks to the entire selection. If
     * <code>true</code> (the default), when an element is checked or unchecked
     * all other elements in the current selection will also be checked or
     * unchecked.
     *
     * @param checksAffectSelection
     *        <code>true</code> to have the "checks affect entire selection"
     *        behavior
     */
    protected final void setChecksAffectSelection(final boolean checksAffectSelection) {
        this.checksAffectSelection = checksAffectSelection;
    }

    /**
     * @return <code>true</code> if the "checks affect selection" mode is
     *         currently on (see {@link #setChecksAffectSelection(boolean)})
     */
    protected final boolean isChecksAffectSelection() {
        return checksAffectSelection;
    }

    /**
     * A convenience method to set a JFace cell editor for a single column in
     * the {@link TableViewer} that backs this {@link TableControl}. This method
     * can be called multiple times: each call preserves existing cell editors
     * that were previously set for other columns. This method must not be
     * called until the underlying SWT {@link Table}'s columns have been
     * created.
     *
     * @param columnIndex
     *        the column index to set a {@link CellEditor} for
     * @param cellEditor
     *        the {@link CellEditor} to set for the column, or <code>null</code>
     *        for no cell editor
     */
    protected final void setCellEditor(final int columnIndex, final CellEditor cellEditor) {
        CellEditor[] cellEditors = getViewer().getCellEditors();

        if (cellEditors == null) {
            cellEditors = new CellEditor[getTable().getColumnCount()];
            getViewer().setCellEditors(cellEditors);
        }

        if (columnIndex < 0 || columnIndex >= cellEditors.length) {
            final String messageFormat = "the specified column index {0} is out of range"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(columnIndex));
            throw new IllegalArgumentException(message);
        }

        cellEditors[columnIndex] = cellEditor;
    }

    /**
     * A convenience method to get a JFace cell editor for a single column in
     * the {@link TableViewer} that backs this {@link TableControl}.
     *
     * @throws IllegalArgumentException
     *         if no cell editors have been set on the {@link TableViewer}, or
     *         if the specified column index is not valid
     *
     * @param columnIndex
     *        the column index to get a {@link CellEditor} for
     * @return the {@link CellEditor} for the column, or <code>null</code> if
     *         the column does not have a cell editor
     */
    protected final CellEditor getCellEditor(final int columnIndex) {
        final CellEditor[] cellEditors = getViewer().getCellEditors();

        if (cellEditors == null || columnIndex < 0 || columnIndex >= cellEditors.length) {
            final String messageFormat = "the specified column index {0} is out of range"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(columnIndex));
            throw new IllegalArgumentException(message);
        }

        return cellEditors[columnIndex];
    }

    /**
     * A convenience method to set a JFace cell editor for a single column in
     * the {@link TableViewer} that backs this {@link TableControl}. This method
     * can be called multiple times: each call preserves existing cell editors
     * that were previously set for other columns. This method must not be
     * called until the underlying SWT {@link Table}'s columns have been
     * created.
     *
     * @param columnPropertyName
     *        identifies the column to set a {@link CellEditor} for
     * @param cellEditor
     *        the {@link CellEditor} to set for the column, or <code>null</code>
     *        for no cell editor
     */
    protected final void setCellEditor(final String columnPropertyName, final CellEditor cellEditor) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, getViewer());
        setCellEditor(columnIndex, cellEditor);
    }

    /**
     * A convenience method to get a JFace cell editor for a single column in
     * the {@link TableViewer} that backs this {@link TableControl}.
     *
     * @throws IllegalArgumentException
     *         if no cell editors have been set on the {@link TableViewer}, or
     *         if the specified column property name is not valid
     *
     * @param columnPropertyName
     *        the column property name that identifies the column to get a
     *        {@link CellEditor} for
     * @return the {@link CellEditor} for the column, or <code>null</code> if
     *         the column does not have a cell editor
     */
    protected final CellEditor getCellEditor(final String columnPropertyName) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, getViewer());
        return getCellEditor(columnIndex);
    }

    /**
     * Sets up the JFace {@link TableViewer} used by this {@link TableControl}
     * to have a default label provider. The default label provider delegates to
     * the {@link #getColumnImage(Object, int)} and
     * {@link #getColumnText(Object, int)}. Subclasses should override those
     * methods if they are using the default label provider.
     */
    protected final void setUseDefaultLabelProvider() {
        viewer.setLabelProvider(new DefaultLabelProvider() {
            @Override
            public Image getColumnImage(final Object element, final int columnIndex) {
                return TableControl.this.getColumnImage(element, columnIndex);
            }

            @Override
            public String getColumnText(final Object element, final int columnIndex) {
                final String s = TableControl.this.getColumnText(element, columnIndex);
                return (s != null ? s : ""); //$NON-NLS-1$
            }

            @Override
            public Color getForeground(final Object element) {
                return TableControl.this.getForegroundColor(element);
            }

            @Override
            public Color getBackground(final Object element) {
                return TableControl.this.getBackgroundColor(element);
            }
        });
    }

    /**
     * Sets up the JFace {@link TableViewer} used by this {@link TableControl}
     * to have a default content provider. The default content provider assumes
     * that the input {@link Object} is an array. The elements returned from the
     * input {@link Object} is simply the input {@link Object} itself, cast to
     * an array.
     */
    protected final void setUseDefaultContentProvider() {
        viewer.setContentProvider(new ContentProviderAdapter() {
            @Override
            public Object[] getElements(final Object inputElement) {
                return (Object[]) inputElement;
            }
        });
    }

    /**
     * Sets up the JFace {@link TableViewer} used by this {@link TableControl}
     * to have a default sorter. The default sorter is an instance of
     * {@link TableViewerSorter}. This method should be called after the
     * underlying {@link Table} has been configured and has had
     * {@link TableColumn}s added to it.
     */
    protected final void setUseDefaultSorter() {
        viewer.setSorter(new TableViewerSorter(viewer));
    }

    /**
     * Sets up the JFace {@link TableViewer} used by this {@link TableControl}
     * to have a default label provider, content provider, and sorter. This
     * method should be called after the underlying {@link Table} has been
     * configured and has had {@link TableColumn}s added to it. This is a
     * convenience method and is equivalent to calling the following methods:
     * <ul>
     * <li>{@link #setUseDefaultLabelProvider()}</li>
     * <li>{@link #setUseDefaultContentProvider()}</li>
     * <li>{@link #setUseDefaultSorter()}</li>
     * </ul>
     */
    protected final void setUseViewerDefaults() {
        setUseDefaultLabelProvider();
        setUseDefaultContentProvider();
        setUseDefaultSorter();
    }

    /**
     * An optional convenience method method to configure the underlying SWT
     * {@link Table} for this {@link TableControl}.
     *
     * @param headerVisible
     *        <code>true</code> if the table's header should be visible
     * @param linesVisible
     *        <code>true</code> if the table's gridlines should be visible
     * @param columnData
     *        an array of {@link TableColumnData}, one for each column to be
     *        created (must not be <code>null</code>)
     */
    protected final void setupTable(
        final boolean headerVisible,
        final boolean linesVisible,
        final TableColumnData[] columnData) {
        TableViewerUtils.setupTableViewer(viewer, headerVisible, linesVisible, getViewDataKey(), columnData);
    }

    /**
     * Set the specified tooltips on the corresponding table column headers.
     *
     * @param tooltips
     *        The list of tooltips. Each item in the list corresponds to a
     *        column at the same index. Use a <code>null</code> value to
     *        indicate a tooltip should be omitted for the column. This list can
     *        be shorter than the corresponding number of columns, in which case
     *        tooltips are not defined for those columns.
     */
    protected final void setTableColumnHeaderTooltips(final String[] tooltips) {
        final TableColumn[] columns = viewer.getTable().getColumns();
        for (int i = 0; i < columns.length; i++) {
            if (i >= tooltips.length) {
                break;
            }

            if (tooltips[i] != null) {
                columns[i].setToolTipText(tooltips[i]);
            }
        }
    }

    /**
     * Sets whether to enable per-item tooltips for this {@link TableControl}
     * (default is <code>false</code>). If <code>true</code>, the
     * {@link #getTooltipText(Object, int)} method will be called to obtain new
     * tooltip text each time the mouse hovers over an element.
     *
     * @param enableTooltips
     *        <code>true</code> to enable the per-element tooltip behavior
     */
    protected final void setEnableTooltips(final boolean enableTooltips) {
        setEnableTooltips(enableTooltips, false);
    }

    /**
     * Sets whether to enable per-item or per-cell tooltips for this
     * {@link TableControl} (default is <code>false</code>). If
     * <code>true</code>, then {@link #getTooltipText(Object, int)} method will
     * be called to obtain new tooltip text each time the mouse hovers over an
     * element.
     *
     * @param enableTooltips
     *        <code>true</code> to enable the per-element tooltip behavior
     * @param supportCellTooltips
     *        <code>true</code> to enable the per-cell tooltip behavior
     */
    protected final void setEnableTooltips(final boolean enableTooltips, final boolean supportCellTooltips) {
        if (this.enableTooltips == enableTooltips) {
            return;
        }

        this.enableTooltips = enableTooltips;

        if (enableTooltips) {
            tooltipManager = new TableTooltipLabelManager(getTable(), this, supportCellTooltips);
            tooltipManager.addTooltipManager();
        } else {
            if (tooltipManager != null) {
                tooltipManager.removeTooltipManager();
                tooltipManager = null;
            }
        }
    }

    /**
     * Called to set the clipboard transfer types for this {@link TableControl}.
     * The clipboard transfer types are initially <code>null</code>. The
     * {@link #copySelectionToClipboard()} method does not do anything until at
     * least one clipboard transfer type has been specified by the subclass
     * calling this method. Once clipboard transfer types have been specified,
     * the {@link #copySelectionToClipboard()} method calls the
     * {@link #getTransferData(Transfer, Object)} method for each transfer type
     * and element in the selection. If you call this method to specify
     * clipboard transfer types, you should also override
     * {@link #getTransferData(Transfer, Object)} to return data for each type.
     *
     * @param transferTypes
     *        the clipboard transfer types to use, or <code>null</code> to
     *        disable clipboard support for this {@link TableControl}
     */
    protected final void setClipboardTransferTypes(final Transfer[] transferTypes) {
        clipboardTransferTypes = transferTypes;
    }

    /**
     * Called to set a single clipboard transfer type for this
     * {@link TableControl}. The clipboard transfer types are initially
     * <code>null</code>. The {@link #copySelectionToClipboard()} method does
     * not do anything until a clipboard transfer type has been specified by the
     * subclass calling this method. Once clipboard transfer types have been
     * specified, the {@link #copySelectionToClipboard()} method calls the
     * {@link #getTransferData(Transfer, Object)} method for each transfer type
     * and element in the selection. If you call this method to specify
     * clipboard transfer types, you should also override
     * {@link #getTransferData(Transfer, Object)} to return data for each type.
     *
     * @param transferType
     *        the clipboard transfer type to use, or <code>null</code> to
     *        disable clipboard support for this {@link TableControl}
     */
    protected final void setClipboardTransferType(final Transfer transferType) {
        setClipboardTransferTypes(new Transfer[] {
            transferType
        });
    }

    /**
     * Called to set the drag transfer types for this {@link TableControl}. The
     * drag transfer types are initially unset. Once drag types have been set,
     * drag operations will call {@link #getTransferData(Transfer, Object[])}
     * with a transfer type specified here and the selected elements. If you
     * call this method to specify drag transfer types, you should also override
     * {@link #getTransferData(Transfer, Object[])} to return data for each
     * type.
     *
     * @param transferTypes
     *        the drag transfer types to use, or <code>null</code> to disable
     *        drag support for this {@link TableControl}
     */
    protected final void setDragTransferTypes(Transfer[] transferTypes) {
        if (transferTypes == null) {
            transferTypes = new Transfer[0];
        }
        dragSource.setTransfer(transferTypes);
    }

    /**
     * Obtains a {@link Clipboard}. Subclasses must not dispose of the clipboard
     * - it is disposed by the {@link TableControl} base class.
     *
     * @return a {@link Clipboard} (never <code>null</code>)
     */
    protected final Clipboard getClipboard() {
        if (clipboard == null) {
            clipboard = new Clipboard(getDisplay());
        }

        return clipboard;
    }

    /**
     * <p>
     * This method computes the current elements contained in this
     * {@link TableControl} and caches them. The contained elements can be
     * retrieved by calling {@link #getElements()}. As a result of calling this
     * method, any registered {@link ElementListener}s are notified of a change
     * in the all-elements collection.
     * </p>
     *
     * <p>
     * This method <b>must</b> be called by subclasses whenever the subclass
     * performs an operation that may modify the set of elements contained in
     * this control. For example, if the subclass calls {@link #getViewer()} and
     * manually sets the input element, it must subsequently call this method.
     * </p>
     */
    protected final void computeElements() {
        final TableItem[] items = getTable().getItems();
        final Object[] elements = new Object[items.length];
        for (int i = 0; i < items.length; i++) {
            elements[i] = items[i].getData();
        }

        allElements = computeElementCollectionInternal(elements, ElementCollectionType.ALL_ELEMENTS);

        /*
         * In order that the selection and checked items are correctly
         * represented in this class when the element listener fires, we have to
         * compute them before we fire the event, but we don't want these other
         * computations to fire their events.
         */
        computeSelectedElements(false);

        if (isCheckboxTable()) {
            computeCheckedElements(false);
        }

        notifyElementListeners();
    }

    /**
     * <p>
     * This method computes the currently selected elements in this
     * {@link TableControl} and caches them. The selected elements can be
     * retrieved by calling {@link #getSelectedElements()}. As a result of
     * calling this method, any registered {@link ISelectionChangedListener}s
     * are notified of a change in the selected elements collection.
     * </p>
     *
     * <p>
     * Normally, there is no reason for subclasses to call this method. Changes
     * to the selected elements are detected automatically by this base class.
     * </p>
     */
    protected final void computeSelectedElements(final boolean fireEvent) {
        final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        final Object[] elements = selection.toArray();

        selectedElements = computeElementCollectionInternal(elements, ElementCollectionType.SELECTED_ELEMENTS);

        if (fireEvent) {
            notifySelectionChangedListeners();
        }
    }

    /**
     * <p>
     * This method computes the currently checked elements in this
     * {@link TableControl} and caches them. The checked elements can be
     * retrieved by calling {@link #getCheckedElements()}. As a result of
     * calling this method, any registered {@link CheckboxListener}s are
     * notified of a change in the checked elements collection.
     * </p>
     *
     * <p>
     * Normally, there is no reason for subclasses to call this method. Changes
     * to the checked elements are detected automatically by this base class.
     * </p>
     */
    protected final void computeCheckedElements(final boolean fireEvent) {
        final Object[] elements = ((CheckboxTableViewer) viewer).getCheckedElements();

        checkedElements = computeElementCollectionInternal(elements, ElementCollectionType.CHECKED_ELEMENTS);

        if (fireEvent) {
            notifyCheckboxListeners();
        }
    }

    /**
     * Called to compute an element collection. This method gives subclasses a
     * chance to filter or modify the element collections. The default behavior
     * is to call {@link #hideElementFromCollections(Object)} for each candidate
     * element. All candidates for which that method returns <code>false</code>
     * will be added to the element collection.
     *
     * @param candidates
     *        the candidate elements (never <code>null</code>)
     * @param type
     *        the element collection type that is being built (never
     *        <code>null</code>)
     * @return a {@link List} containing elements that should be in the
     *         collection (must not be <code>null</code>)
     */
    protected List computeElementCollection(final Object[] candidates, final ElementCollectionType type) {
        final List list = new ArrayList();
        for (int i = 0; i < candidates.length; i++) {
            if (!hideElementFromCollections(candidates[i])) {
                list.add(candidates[i]);
            }
        }
        return list;
    }

    /**
     * Called by the default implementation of
     * {@link #computeElementCollection(Object[], ElementCollectionType)} to
     * allow subclasses to filter certain elements from all element collections.
     * The default behavior is to return <code>false</code>, which means that no
     * elements will be filtered from the collections.
     *
     * @param element
     *        an element being considered for inclusion in an element collection
     *        (never <code>null</code>)
     * @return <code>true</code> if the element should be hidden from the
     *         collection
     */
    protected boolean hideElementFromCollections(final Object element) {
        return false;
    }

    /**
     * Notifies any registered {@link ElementListener}s of a change in the
     * all-elements collection. Normally, there is no reason for subclasses to
     * call this method.
     */
    protected final void notifyElementListeners() {
        final ElementEvent elementEvent = new ElementEvent(this, getElements());
        ((ElementListener) elementListener.getListener()).elementsChanged(elementEvent);
    }

    /**
     * Notifies any registered {@link ISelectionChangedListener}s of a change in
     * the selected elements collection. Normally, there is no reason for
     * subclasses to call this method.
     */
    protected final void notifySelectionChangedListeners() {
        final SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
        final ISelectionChangedListener listener = (ISelectionChangedListener) selectionListener.getListener();
        listener.selectionChanged(event);
    }

    /**
     * Notifies any registered post-{@link ISelectionChangedListener}s of a
     * change in the selected elements collection. Normally, there is no reason
     * for subclasses to call this method.
     */
    protected final void notifyPostSelectionChangedListeners() {
        final SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
        final ISelectionChangedListener listener = (ISelectionChangedListener) postSelectionListener.getListener();
        listener.selectionChanged(event);
    }

    /**
     * Notifies any registered {@link CheckboxListener}s of a change in the
     * checked elements collection. Normally, there is no reason for subclasses
     * to call this method.
     */
    protected final void notifyCheckboxListeners() {
        final CheckboxEvent checkboxEvent = new CheckboxEvent(this, getCheckedElements());
        ((CheckboxListener) checkboxListener.getListener()).checkedElementsChanged(checkboxEvent);
    }

    /**
     * Notifies any registered {@link ICheckStateListener}s of a change in the
     * check state of the specified element. Normally, there is no reason for
     * subclasses to call this method.
     *
     * @param element
     *        the element to notify listeners about (must not be
     *        <code>null</code>)
     * @param checked
     *        the check state of the element
     */
    protected final void notifyCheckStateListeners(final Object element, final boolean checked) {
        final ICheckStateListener listener = (ICheckStateListener) checkStateListener.getListener();
        listener.checkStateChanged(new CheckStateChangedEvent(this, element, checked));
    }

    /**
     * Notifies any registered {@link IDoubleClickListener}s of a double-click
     * of the specified element. Normally, there is no reason for subclasses to
     * call this method.
     *
     * @param element
     *        the element to notify listeners about (must not be
     *        <code>null</code>)
     */
    protected final void notifyDoubleClickListeners(final Object element) {
        final IDoubleClickListener listener = (IDoubleClickListener) doubleClickListener.getListener();
        listener.doubleClick(new DoubleClickEvent(getViewer(), new StructuredSelection(element)));
    }

    /**
     * @return a view data key that can be used to persist any view data (
     *         <code>null</code> to not persist any settings)
     */
    protected final String getViewDataKey() {
        if (!persistGeometry) {
            return null;
        }

        return viewDataKey;
    }

    protected void setOptionPersistGeometry(final boolean persistGeometry) {
        this.persistGeometry = persistGeometry;
    }

    protected boolean getOptionPersistGeometry() {
        return persistGeometry;
    }

    /**
     * <p>
     * Called to get transfer data for the specified transfer type and selected
     * elements in this {@link TableControl}. This method should be overridden
     * by the subclass to enable clipboard support for this {@link TableControl}
     * . In addition, the subclass must call
     * {@link #setClipboardTransferTypes(Transfer[])} to specify the transfer
     * types that this method will be called with. This method is called by
     * {@link #copySelectionToClipboard()}.
     * </p>
     *
     * <p>
     * <b>Important</b>: the subclass must not modify the selected elements
     * array.
     * </p>
     *
     * @param transferType
     *        the transfer type to get transfer data for, never
     *        <code>null</code>
     * @param selectedElements
     *        the selected elements to get the transfer data for, never
     *        <code>null</code>
     * @return transfer data for the selected elements
     */
    protected Object getTransferData(final Transfer transferType, final Object[] selectedElements) {
        return null;
    }

    /**
     * <p>
     * This method is invoked if the "default" label provider is used (by
     * calling {@link #setUseDefaultLabelProvider()}). The default
     * implementation of this method is to delegate to the column property-based
     * {@link #getColumnText(Object, String)} method.
     * </p>
     *
     * <p>
     * Subclasses should override this method if they are using the default
     * label provider but are not using column property names.
     * </p>
     *
     * @param element
     *        the element to get text for
     * @param columnIndex
     *        the index of the column to get text for
     * @return the column text, or <code>null</code> for no text in the column
     *         for this element
     */
    protected String getColumnText(final Object element, final int columnIndex) {
        return getColumnText(element, TableViewerUtils.columnIndexToColumnProperty(columnIndex, getViewer()));
    }

    /**
     * <p>
     * This method is invoked if the "default" label provider is used (by
     * calling {@link #setUseDefaultLabelProvider()}) and the subclass does not
     * override the column index-based {@link #getColumnText(Object, int)}
     * method. The default implementation of this method is to return
     * <code>null</code>, indicating no column text.
     * </p>
     *
     * <p>
     * Subclasses should override this method if they are using the default
     * label provider and are using column property names (which are preferred
     * over column indices).
     * </p>
     *
     * @param element
     *        the element to get text for
     * @param columnPropertyName
     *        the column property name of the column to get text for
     * @return the column text, or <code>null</code> for no text in the column
     *         for this element
     */
    protected String getColumnText(final Object element, final String columnPropertyName) {
        return null;
    }

    /**
     * <p>
     * This method is invoked if the "default" label provider is used (by
     * calling {@link #setUseDefaultLabelProvider()}). The default
     * implementation of this method is to delegate to the column property-based
     * {@link #getColumnImage(Object, String)} method.
     * </p>
     *
     * <p>
     * Subclasses should override this method if they are using the default
     * label provider but are not using column property names.
     * </p>
     *
     * @param element
     *        the element to get an image for
     * @param columnIndex
     *        the index of the column to get an image for
     * @return the column image, or <code>null</code> for no image in the column
     *         for this element
     */
    protected Image getColumnImage(final Object element, final int columnIndex) {
        return getColumnImage(element, TableViewerUtils.columnIndexToColumnProperty(columnIndex, getViewer()));
    }

    /**
     * <p>
     * This method is invoked if the "default" label provider is used (by
     * calling {@link #setUseDefaultLabelProvider()}) and the subclass does not
     * override the column index-based {@link #getColumnImage(Object, int)}
     * method. The default implementation of this method is to return
     * <code>null</code>, indicating no column image.
     * </p>
     *
     * <p>
     * Subclasses should override this method if they are using the default
     * label provider and are using column property names (which are preferred
     * over column indices).
     * </p>
     *
     * @param element
     *        the element to get an image for
     * @param columnIndex
     *        the index of the column to get an image for
     * @return the column image, or <code>null</code> for no image in the column
     *         for this element
     */
    protected Image getColumnImage(final Object element, final String columnPropertyName) {
        return null;
    }

    /**
     * <p>
     * This method is invoked if the "default" label provider is used (by
     * calling {@link #setUseDefaultLabelProvider()}). The default
     * implementation of this method is to return <code>null</code>, indicating
     * that the default foreground color should be used to decorate the object.
     * </p>
     *
     * <p>
     * Subclasses can override to provide a non-default foreground color for
     * elements in the table.
     * </p>
     *
     * @param element
     *        the element a color is being requested for
     * @return the foreground color for the element, or <code>null</code> to use
     *         the default foreground color
     */
    protected Color getForegroundColor(final Object element) {
        return null;
    }

    /**
     * <p>
     * This method is invoked if the "default" label provider is used (by
     * calling {@link #setUseDefaultLabelProvider()}). The default
     * implementation of this method is to return <code>null</code>, indicating
     * that the default background color should be used to decorate the object.
     * </p>
     *
     * <p>
     * Subclasses can override to provide a non-default background color for
     * elements in the table.
     * </p>
     *
     * @param element
     *        the element a color is being requested for
     * @return the background color for the element, or <code>null</code> to use
     *         the default background color
     */
    protected Color getBackgroundColor(final Object element) {
        return null;
    }

    /**
     * Called to get a per-element tooltip for this {@link TableControl}. Will
     * only be called if a subclass calls {@link #setEnableTooltips(boolean)}
     * with <code>true</code>.
     *
     * @param element
     *        an element currently in this {@link TableControl}
     * @return tooltip text for the element or <code>null</code> for no tooltip
     */
    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        return null;
    }

    /**
     * @return an error message for use with the {@link Validator} returned by
     *         {@link #getElementsValidator()}, or <code>null</code> for no
     *         specific error message
     */
    protected String getElementsValidatorErrorMessage() {
        return null;
    }

    /**
     * @return an error message for use with the {@link Validator} returned by
     *         {@link #getSelectionValidator()}, or <code>null</code> for no
     *         specific error message
     */
    protected String getSelectionValidatorErrorMessage() {
        return null;
    }

    /**
     * @return an error message for use with the {@link Validator} returned by
     *         {@link #getSingleSelectionValidator()}, or <code>null</code> for
     *         no specific error message
     */
    protected String getSingleSelectionValidatorErrorMessage() {
        return null;
    }

    /**
     * @return an error message for use with the {@link Validator} returned by
     *         {@link #getCheckboxValidator()}, or <code>null</code> for no
     *         specific error message
     */
    protected String getCheckboxValidatorErrorMessage() {
        return null;
    }

    /**
     * Called when this {@link TableControl} is disposed. This is an empty
     * convenience method, intended for overriding by the subclass.
     */
    protected void onDisposed() {

    }

    /**
     * Called when the selection has changed in the {@link TableViewer} backing
     * this {@link TableControl}. The base class implementation calls
     * {@link #computeSelectedElements()}.
     *
     * @param event
     *        the {@link SelectionChangedEvent}
     */
    protected void onSelectionChanged(final SelectionChangedEvent event) {
        computeSelectedElements(true);
    }

    /**
     * Called when the selection has changed (post-selection) in the
     * {@link TableViewer} backing this {@link TableControl}. The base class
     * implementation calls {@link #notifyPostSelectionChangedListeners()}.
     *
     * @param event
     *        the {@link SelectionChangedEvent}
     */
    protected void onPostSelectionChanged(final SelectionChangedEvent event) {
        notifyPostSelectionChangedListeners();
    }

    /**
     * Called when a {@link KeyEvent} occurs on the {@link Table} backing this
     * {@link TableControl}. The base class currently does nothing - subclasses
     * should still invoke super.onKeyPressed() in case functionality is added
     * to the base class in the future.
     *
     * @param e
     *        the {@link KeyEvent}
     */
    protected void onKeyPressed(final KeyEvent e) {
    }

    /**
     * Called when a {@link CheckStateChangedEvent} occurs on the
     * {@link CheckboxTableViewer} backing this {@link TableControl}.
     *
     * @param event
     *        the {@link CheckStateChangedEvent}
     */
    protected void onCheckStateChanged(final CheckStateChangedEvent event) {
        final boolean checked = event.getChecked();

        if (isChecksAffectSelection()) {
            /*
             * propagate checked state to all elements in the current selection
             */
            viewer.getTable().setRedraw(false);
            final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (selection.size() == viewer.getTable().getItemCount()) {
                ((CheckboxTableViewer) viewer).setAllChecked(checked);
            } else {
                /*
                 * Make sure the element being checked is part of the selection.
                 * Otherwise, we ignore the selected elements. Avoids the case
                 * where you have some elements selected, and check a
                 * non-selected element. The checkstate of the selected elements
                 * should NOT change in this case.
                 */
                boolean checkedElementInSelection = false;

                for (final Iterator it = selection.iterator(); it.hasNext();) {
                    if (it.next().equals(event.getElement())) {
                        checkedElementInSelection = true;
                        break;
                    }
                }

                if (checkedElementInSelection) {
                    for (final Iterator it = selection.iterator(); it.hasNext();) {
                        ((CheckboxTableViewer) viewer).setChecked(it.next(), checked);
                    }
                }
            }
            viewer.getTable().setRedraw(true);
        }

        computeCheckedElements(true);

        final Object element = event.getElement();
        if (!hideElementFromCollections(element)) {
            notifyCheckStateListeners(element, event.getChecked());
        }
    }

    /**
     * Called when a {@link DoubleClickEvent} occurs on the {@link TableViewer}
     * backing this {@link TableControl}.
     *
     * @param event
     *        the {@link DoubleClickEvent}
     */
    protected void onDoubleClick(final DoubleClickEvent event) {
        final Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
        if (!hideElementFromCollections(element)) {
            notifyDoubleClickListeners(element);
        }
    }

    /**
     * Called by the {@link ICellModifier#modify(Object, String, Object)} method
     * of the {@link ICellModifier} that is set on the underlying
     * {@link TableViewer} by default. Subclasses can override to support cell
     * editing.
     *
     * @param element
     *        the element that has been edited (never <code>null</code>)
     * @param columnPropertyName
     *        the identifier of the column that has been edited (never
     *        <code>null</code>)
     * @param value
     *        the cell-editor-specific value
     */
    protected void modifyElement(final Object element, final String columnPropertyName, final Object value) {

    }

    /**
     * Called by the {@link ICellModifier#getValue(Object, String)} method of
     * the {@link ICellModifier} that is set on the underlying
     * {@link TableViewer} by default. Subclasses can override to support cell
     * editing.
     *
     * @param element
     *        the element that is being edited (never <code>null</code>)
     * @param columnPropertyName
     *        the identifier of the column that is being edited (never
     *        <code>null</code>)
     * @return the cell-editor-specific value
     */
    protected Object getValueToModify(final Object element, final String columnPropertyName) {
        return null;
    }

    /**
     * Called by the {@link ICellModifier#canModify(Object, String)} method of
     * the {@link ICellModifier} that is set on the underlying
     * {@link TableViewer} by default. Subclasses can override to support cell
     * editing. If subclasses override, they should also install
     * {@link CellEditor}s by calling {@link #setCellEditor(String, CellEditor)}
     * for each column that can be edited. The default implementation of this
     * method returns <code>false</code>, effectively disabling cell editing.
     *
     * @param element
     *        the candidate element for cell editing (never <code>null</code>)
     * @param columnPropertyName
     *        the identifier of the candidate column for cell editing (never
     *        <code>null</code>)
     * @return <code>true</code> if cell editing should be enabled for the
     *         specified element and the specified column
     */
    protected boolean canModifyElement(final Object element, final String columnPropertyName) {
        return false;
    }

    /**
     * Computes an element collection by invoking the subclass hook
     * {@link #computeElementCollection(Object[], ElementCollectionType)}.
     *
     * @param candidates
     *        the candidate elements
     * @param type
     *        the element collection type
     * @return <code>null</code> if no elements are in the collection, or a
     *         typesafe array of elements if there is at least one element in
     *         the collection
     */
    private Object[] computeElementCollectionInternal(final Object[] candidates, final ElementCollectionType type) {
        final List list = computeElementCollection(candidates, type);

        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.toArray(newElementArray(list.size()));
        }
    }

    /**
     * Returns one of the internal element collections. No safe copy is made of
     * the returned collection.
     *
     * @param type
     *        the type of collection to return (must not be <code>null</code>)
     * @return the internal collection, or <code>null</code> if the internal
     *         collection is currently empty
     */
    private Object[] getElementCollectionInternal(final ElementCollectionType type) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        if (ElementCollectionType.CHECKED_ELEMENTS == type) {
            throwIfNotCheckboxTable();
        }

        if (ElementCollectionType.ALL_ELEMENTS == type) {
            return allElements;
        }

        if (ElementCollectionType.SELECTED_ELEMENTS == type) {
            return selectedElements;
        }

        if (ElementCollectionType.CHECKED_ELEMENTS == type) {
            return checkedElements;
        }

        final String messageFormat = "unknown element collection type: {0}"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, type);
        throw new IllegalArgumentException(message);
    }

    /**
     * @return a new, empty array of the component type specified by
     *         {@link #elementType}
     */
    private Object[] newEmptyArray() {
        return newElementArray(0);
    }

    /**
     * Creates anew array of the component type specified by
     * {@link #elementType}.
     *
     * @param length
     *        the length of the returned array
     * @return an element array
     */
    private Object[] newElementArray(final int length) {
        return (Object[]) Array.newInstance(elementType, length);
    }

    /**
     * Creates a new context menu (JFace {@link MenuManager}) on the specified
     * control. The {@link MenuManager} is set to use the "remove all when
     * shown" style. No listeners are added to the {@link MenuManager} by this
     * method.
     *
     * @param control
     *        the control to place the menu on
     * @return the JFace {@link MenuManager}
     */
    private MenuManager createContextMenu(final Control control) {
        final MenuManager menuManager = new MenuManager("#popup"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        control.setMenu(menuManager.createContextMenu(control));
        return menuManager;
    }

    /**
     * Called by the drag listener we've attached to this table's
     * {@link DragSource}.
     */
    private void dragSetData(final DragSourceEvent event) {
        final Transfer[] transferTypes = dragSource.getTransfer();
        for (int i = 0; i < transferTypes.length; i++) {
            if (transferTypes[i].isSupportedType(event.dataType)) {
                final Object[] elements = getSelectedElements();
                final Object transferData = getTransferData(transferTypes[i], elements);
                event.data = transferData;
                break;
            }
        }
    }

    /**
     * Hooks the SWT {@link Table} by adding appropriate listeners (selection,
     * checkbox, etc).
     *
     * @param table
     *        the SWT table to hook
     */
    private void hookTable(final Table table) {
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                TableControl.this.onSelectionChanged(event);
            }
        });

        viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                TableControl.this.onPostSelectionChanged(event);
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                TableControl.this.onDoubleClick(event);
            }
        });

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                TableControl.this.onKeyPressed(e);
            }
        });

        if (isCheckboxTable()) {
            final CheckboxTableViewer checkboxTableViewer = (CheckboxTableViewer) viewer;
            checkboxTableViewer.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(final CheckStateChangedEvent event) {
                    TableControl.this.onCheckStateChanged(event);
                }
            });
        }
    }

    /**
     * Called when this {@link TableControl} is disposed. We first call the
     * subclass hook method ({@link #onDisposed()}), then clean up private
     * objects.
     */
    private void widgetDisposed(final DisposeEvent e) {
        onDisposed();

        if (clipboard != null) {
            clipboard.dispose();
            clipboard = null;
        }

        dragSource.dispose();
    }

    private static abstract class DefaultLabelProvider extends LabelProvider
        implements ITableLabelProvider, IColorProvider {
    }
}
