// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.valid.Validator;

/**
 * <p>
 * A simple abstract table-based control that provides ISelectionProvider and
 * double-click handling behavior.
 * </p>
 * <p>
 * Note: The only time you want to base a control on this class is when you:
 * <ul>
 * <li>need a SWT Table-based control, and</li>
 * <li>need ISelectionProvider and/or double-click handling behavior, and</li>
 * <li>cannot use TableViewer for some reason</li>
 * </ul>
 * </p>
 * <p>
 * In other words use TableViewer! If TableViewer cannot be used, then this
 * class is a substitute for some of the functionality in TableViewer.
 * </p>
 * <p>
 * For instance, SWT virtual tables were introduced in Eclipse 3.0, but were
 * only made compatible with the JFace viewer layer in Eclispe 3.1. Therefore if
 * you want to use a virtual table and retain compatibility with Eclipse 3.0,
 * you must not use a <code>TableViewer</code>. This class could be used as a
 * substitute in this case.
 * </p>
 * <p>
 * Note: for double-click handling, this class cannot re-use
 * <code>org.eclipse.jface.viewers.IDoubleClickListener</code> (unforuntately).
 * That interface uses <code>org.eclipse.jface.viewers.DoubleClickEvent</code>,
 * which requires a non-null <code>Viewer</code> reference. Instead,
 * <code>TableBasedControl</code> uses
 * <code>com.microsoft.tfs.client.common.ui.shared.DoubleClickListener</code>
 * and <code>com.microsoft.tfs.client.common.ui.shared.DoubleClickEvent</code>
 * which have similar functionality.
 * </p>
 */
public abstract class CompatibilityVirtualTable extends Composite implements ISelectionProvider {
    /**
     * SWT table styles that will always be used.
     */
    private static int TABLE_STYLES = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL;

    /**
     * SWT table styles that will only be used if the client passes them in.
     */
    private static int OPTIONAL_TABLE_STYLES = SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.HIDE_SELECTION;

    /**
     * The SWT style bits this {@link CompatibilityVirtualTable}'s {@link Table}
     * was created with.
     */
    private final int tableStyles;

    /**
     * The SWT Table this control is based around.
     */
    private final Table table;

    /**
     * Manages DoubleClickListeners registered for this control.
     */
    private final SingleListenerFacade doubleClickListener = new SingleListenerFacade(DoubleClickListener.class);

    /**
     * Manages ISelectionChangedListeners registered for this control
     */
    private final SingleListenerFacade selectionChangedListener =
        new SingleListenerFacade(ISelectionChangedListener.class);

    /**
     * The context menu attached to this {@link CompatibilityVirtualTable}'s
     * {@link Table}.
     */
    private final MenuManager contextMenu;

    private final Class elementType;

    private Object[] selectedElements;
    private int[] selectedIndices;

    private Transfer[] clipboardTransferTypes;
    private Clipboard clipboard;

    public CompatibilityVirtualTable(final Composite parent, final int style, final Class elementType) {
        super(parent, style);

        Check.notNull(elementType, "elementType"); //$NON-NLS-1$
        this.elementType = elementType;

        setLayout(new FillLayout());

        tableStyles = TABLE_STYLES | (OPTIONAL_TABLE_STYLES & style);
        table = new Table(this, tableStyles);

        table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                fireDoubleClick();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedElementsChanged();
            }
        });

        table.addListener(SWT.SetData, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                final TableItem tableItem = (TableItem) event.item;
                populateTableItem(tableItem);
            }
        });

        contextMenu = new MenuManager("#popup"); //$NON-NLS-1$
        contextMenu.setRemoveAllWhenShown(true);
        table.setMenu(contextMenu.createContextMenu(table));

        contextMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillMenu(manager);
            }
        });

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                CompatibilityVirtualTable.this.onTableDisposed(e);
            }
        });
    }

    public Table getTable() {
        return table;
    }

    public MenuManager getContextMenu() {
        return contextMenu;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        table.setEnabled(enabled);
    }

    public void addDoubleClickListener(final DoubleClickListener listener) {
        doubleClickListener.addListener(listener);
    }

    public void removeDoubleClickListener(final DoubleClickListener listener) {
        doubleClickListener.removeListener(listener);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListener.addListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListener.removeListener(listener);
    }

    @Override
    public ISelection getSelection() {
        if (selectedElements == null) {
            return StructuredSelection.EMPTY;
        }

        return new StructuredSelection(selectedElements.clone());
    }

    @Override
    public void setSelection(final ISelection selection) {
        throw new UnsupportedOperationException();
    }

    public void clearSelection() {
        getTable().clearAll();
        selectedElementsChanged();
    }

    @Override
    public boolean setFocus() {
        return table.setFocus();
    }

    public Object[] getSelectedElements() {
        return (selectedElements == null ? (Object[]) Array.newInstance(elementType, 0)
            : (Object[]) selectedElements.clone());
    }

    public Object getSelectedElement() {
        if (selectedElements == null || selectedElements.length == 0) {
            return null;
        }
        return selectedElements[0];
    }

    public int[] getSelectedIndices() {
        return selectedIndices == null ? new int[0] : (int[]) selectedIndices.clone();
    }

    public Validator getSelectionValidator() {
        return new SelectionProviderValidator(this, NumericConstraint.ONE_OR_MORE, null);
    }

    public Validator getSingleSelectionValidator() {
        return new SelectionProviderValidator(this, NumericConstraint.EXACTLY_ONE, null);
    }

    protected abstract void populateTableItem(TableItem tableItem);

    protected void fillMenu(final IMenuManager manager) {

    }

    protected void selectedElementsChanged() {
        selectedIndices = table.getSelectionIndices();
        if (selectedIndices.length == 0) {
            selectedElements = null;
        } else {
            /*
             * GTK tends to select a single element on paint, which may happen
             * before the table data is loaded. You can't have a structured
             * selection with any nulls in it (or
             *
             * @link{StructuredSelection#equals} will throw.)
             */
            final List selectedElementsList = new ArrayList();

            for (int i = 0; i < selectedIndices.length; i++) {
                final Object data = table.getItem(selectedIndices[i]).getData();
                if (data != null) {
                    selectedElementsList.add(data);
                }
            }

            selectedElements = selectedElementsList.toArray(new Object[selectedElementsList.size()]);
        }

        fireSelectionChanged();
    }

    protected void fireSelectionChanged() {
        final SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
        ((ISelectionChangedListener) selectionChangedListener.getListener()).selectionChanged(event);
    }

    protected void fireDoubleClick() {
        final DoubleClickEvent event = new DoubleClickEvent(table, getSelection());
        ((DoubleClickListener) doubleClickListener.getListener()).doubleClick(event);
    }

    private void onTableDisposed(final DisposeEvent e) {
        if (clipboard != null) {
            clipboard.dispose();
            clipboard = null;
        }
    }

    protected final Clipboard getClipboard() {
        if (clipboard == null) {
            clipboard = new Clipboard(getDisplay());
        }
        return clipboard;
    }

    protected final void setClipboardTransferTypes(final Transfer[] transferTypes) {
        clipboardTransferTypes = transferTypes;
    }

    protected final void setClipboardTransferType(final Transfer transferType) {
        setClipboardTransferTypes(new Transfer[] {
            transferType
        });
    }

    protected Object getTransferData(
        final Transfer transferType,
        final int[] selectedIndices,
        final IProgressMonitor progressMonitor) {
        return null;
    }

    public void copyAllToClipboard() {
        final int[] itemIndices = new int[table.getItemCount()];
        for (int i = 0; i < itemIndices.length; i++) {
            itemIndices[i] = i;
        }
        copyToClipboard(itemIndices);
    }

    public void copySelectionToClipboard() {
        copyToClipboard(getSelectedIndices());
    }

    private void copyToClipboard(final int[] itemIndices) {
        final Display display = getDisplay();

        final TFSCommand command = new TFSCommand() {
            @Override
            public String getName() {
                return Messages.getString("CompatibilityVirtualTable.CopyCommandText"); //$NON-NLS-1$
            }

            @Override
            public String getErrorDescription() {
                return Messages.getString("CompatibilityVirtualTable.CopyCommandErrorText"); //$NON-NLS-1$
            }

            @Override
            public String getLoggingDescription() {
                return Messages.getString("CompatibilityVirtualTable.CopyCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
            }

            @Override
            public boolean isCancellable() {
                return true;
            }

            @Override
            protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
                final Object[] clipboardData = getClipboardTransferData(itemIndices, progressMonitor);
                if (clipboardData != null) {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            getClipboard().setContents(clipboardData, clipboardTransferTypes);
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        UICommandExecutorFactory.newUICommandExecutor(getShell(), 2000).execute(command);
    }

    protected final Object[] getClipboardTransferData(final int[] itemIndices, final IProgressMonitor progressMonitor) {
        if (itemIndices.length == 0) {
            return null;
        }

        final Transfer[] transferTypes = clipboardTransferTypes;

        if (transferTypes == null || transferTypes.length == 0) {
            return null;
        }

        if (progressMonitor != null) {
            progressMonitor.beginTask(
                Messages.getString("CompatibilityVirtualTable.ProgressLoadingDetails"), //$NON-NLS-1$
                10 + itemIndices.length * transferTypes.length);
        }

        final Object[] transferData = new Object[transferTypes.length];
        for (int i = 0; i < transferTypes.length; i++) {
            transferData[i] = getTransferData(transferTypes[i], itemIndices, progressMonitor);
            if (transferData[i] == null) {
                return null;
            }
        }

        return transferData;
    }
}