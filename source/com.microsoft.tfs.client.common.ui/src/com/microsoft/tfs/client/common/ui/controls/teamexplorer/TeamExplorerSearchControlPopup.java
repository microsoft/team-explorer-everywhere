// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.teamexplorer;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;
import com.microsoft.tfs.util.CollatorFactory;
import com.microsoft.tfs.util.MRUSet;

class TeamExplorerSearchControlPopup extends PopupDialog {
    /**
     * Popup sizes itself to the width of the {@link TeamExplorerSearchControl}
     * 's text box, but we won't go smaller than this.
     */
    private static final int MIN_WIDTH_PIXELS = 100;

    /**
     * The maximum number of searches to show in the control (a greater number
     * may be saved to disk).
     */
    private static final int SHOW_SEARCHES_MAX_COUNT = 5;

    private final TeamExplorerSearchControl searchControl;

    private ListViewer mruControl;
    private Label separator;

    public TeamExplorerSearchControlPopup(final TeamExplorerSearchControl searchControl) {
        // HOVER_SHELLSTYLE cannot get focus, which is nice in this case
        super(searchControl.getShell(), PopupDialog.HOVER_SHELLSTYLE, false, false, false, false, false, null, null);

        this.searchControl = searchControl;
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);

        // Update this popup when the user types
        final ModifyListener textModifiedListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (getShell().isDisposed()) {
                    return;
                }

                if (!searchControl.isDisposed() && mruControl != null && !mruControl.getList().isDisposed()) {
                    mruControl.refresh();
                    separator.setVisible(mruControl.getList().getItemCount() > 0);
                    autoSize();
                }
            }
        };

        // Close the popup if the parent shell loses focus (commonly this is
        // alt-tab away from the app)
        final Listener deactivateListener = new Listener() {
            @Override
            public void handleEvent(final Event event) {
                final Shell activeShell = Display.getDefault().getActiveShell();

                if (activeShell == getShell() || activeShell == getParentShell()) {
                    return;
                }

                close();
            }
        };

        final KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.ESC:
                        close();
                        break;
                }
            }
        };

        searchControl.getSearchText().addModifyListener(textModifiedListener);
        searchControl.addListener(SWT.Deactivate, deactivateListener);
        shell.addKeyListener(keyListener);

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                searchControl.removeListener(SWT.Deactivate, deactivateListener);
                searchControl.getSearchText().removeModifyListener(textModifiedListener);
                shell.removeKeyListener(keyListener);
            }
        });
    }

    @Override
    public int open() {
        ClientTelemetryHelper.sendDialogOpened(this);

        final int ret = super.open();

        // Put the focus back on the text box every time
        searchControl.getSearchText().setFocus();

        return ret;
    }

    /**
     * Call to automatically size the popup and its shell to its content.
     */
    private void autoSize() {
        // Let the variable size controls (listviewer) calculate new sizes and
        // positions
        getShell().layout();

        // Set the overall shell size (seems to need to be done separately)
        getShell().setSize(getDefaultSize());
    }

    @Override
    protected Point getDefaultLocation(final Point initialSize) {
        final Point location = searchControl.getSearchText().toDisplay(0, 0);
        location.y += searchControl.getSearchText().getBounds().height;

        // Add a small gap so we don't draw over the border
        location.y += 1;

        return location;
    }

    @Override
    protected Point getDefaultSize() {
        final int preferredWidth = Math.max(MIN_WIDTH_PIXELS, searchControl.getSearchText().getSize().x);
        return getShell().computeSize(preferredWidth, SWT.DEFAULT, true);
    }

    @Override
    protected Color getForeground() {
        return searchControl.getToolkit().getColors().getForeground();
    }

    @Override
    protected Color getBackground() {
        return searchControl.getToolkit().getColors().getBackground();
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        // Superclass recommends this pattern
        final Composite composite = (Composite) super.createDialogArea(parent);

        final FormToolkit toolkit = searchControl.getToolkit();

        SWTUtil.gridLayout(composite, 1, true, 0, 0);

        mruControl = new ListViewer(composite, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(mruControl.getList());
        mruControl.setLabelProvider(new MRULabelProvider());
        mruControl.setContentProvider(new PrefixMatchingContentProvider(searchControl));
        mruControl.setInput(searchControl.getRecentSearches());

        mruControl.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                search();
            }
        });

        mruControl.getList().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.CR:
                        search();
                        break;
                }
            }
        });

        separator = toolkit.createSeparator(composite, SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(separator);

        final Label instructions =
            toolkit.createLabel(composite, Messages.getString("TeamExplorerSearchControlPopup.AddASearchFilter")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().hIndent(3).applyTo(instructions);
        instructions.setEnabled(false);

        final Composite filterBar = toolkit.createComposite(composite);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(filterBar);
        filterBar.setLayout(new RowLayout(SWT.HORIZONTAL));

        createFilterHyperlink(
            filterBar,
            toolkit,
            Messages.getString("TeamExplorerSearchControlPopup.AssignedTo"), //$NON-NLS-1$
            "A", //$NON-NLS-1$
            WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_ME),
            Messages.getString("TeamExplorerSearchControlPopup.AssignedToTooltip")); //$NON-NLS-1$
        createFilterHyperlink(
            filterBar,
            toolkit,
            Messages.getString("TeamExplorerSearchControlPopup.CreatedBy"), //$NON-NLS-1$
            "C", //$NON-NLS-1$
            WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_ME),
            Messages.getString("TeamExplorerSearchControlPopup.CreatedByTooltip")); //$NON-NLS-1$
        createFilterHyperlink(
            filterBar,
            toolkit,
            Messages.getString("TeamExplorerSearchControlPopup.State"), //$NON-NLS-1$
            "S", //$NON-NLS-1$
            "", //$NON-NLS-1$
            Messages.getString("TeamExplorerSearchControlPopup.StateTooltip")); //$NON-NLS-1$
        createFilterHyperlink(
            filterBar,
            toolkit,
            Messages.getString("TeamExplorerSearchControlPopup.WorkItemType"), //$NON-NLS-1$
            "T", //$NON-NLS-1$
            "", //$NON-NLS-1$
            Messages.getString("TeamExplorerSearchControlPopup.WorkItemTypeTooltip")); //$NON-NLS-1$

        return composite;
    }

    private Hyperlink createFilterHyperlink(
        final Composite parent,
        final FormToolkit toolkit,
        final String text,
        final String filter,
        final String localizedValue,
        final String tooltip) {
        final Hyperlink link = toolkit.createHyperlink(parent, text, SWT.NONE);
        link.setToolTipText(tooltip);

        // Pack the data in the href object
        link.setHref(new String[] {
            filter,
            localizedValue
        });

        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                if (searchControl.isDisposed()) {
                    return;
                }

                final String[] values = (String[]) e.getHref();
                searchControl.appendFilter(values[0], values[1]);
            }
        });

        return link;
    }

    protected void search() {
        final String text = (String) SelectionUtils.adaptSelectionFirstElement(mruControl.getSelection(), String.class);

        searchControl.getSearchText().setText(text);
        searchControl.getSearchText().setSelection(text.length());
        close();
        searchControl.search();
    }

    /**
     * Content provider for an input that is a {@link MRUSet}. Provides the
     * {@link String}s from the set that start with what's been typed in a
     * {@link Text} control.
     */
    private static class PrefixMatchingContentProvider extends ContentProviderAdapter {
        private final static Collator collator = CollatorFactory.getCaseInsensitiveCollator();
        private final TeamExplorerSearchControl searchControl;

        private PrefixMatchingContentProvider(final TeamExplorerSearchControl searchControl) {
            this.searchControl = searchControl;
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            final MRUSet mruSet = (MRUSet) inputElement;

            // Find the best matching strings for what was typed in the text
            // control
            final String prefix = searchControl.getSearchText().getText().trim();

            // MRU keeps most recent at the end, we want them at the front
            final List<String> candidates = new ArrayList<String>(mruSet);
            Collections.reverse(candidates);

            final List<String> matches = new ArrayList<String>();
            for (final String candidate : candidates) {
                if (candidate != null && matches(candidate.trim(), prefix)) {
                    matches.add(candidate);
                }

                if (matches.size() == SHOW_SEARCHES_MAX_COUNT) {
                    break;
                }
            }

            return matches.toArray(new String[matches.size()]);
        }

        private boolean matches(final String candidate, final String prefix) {
            if (prefix.length() > candidate.length()) {
                return false;
            }

            return collator.equals(prefix, candidate.substring(0, prefix.length()));
        }
    }

    private static class MRULabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            return ((String) element).trim();
        }
    }
}