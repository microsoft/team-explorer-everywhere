// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.teamexplorer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.search.WorkItemSearchCommand;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.prefs.MRUPreferenceSerializer;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.MRUSet;

public class TeamExplorerSearchControl extends BaseControl {
    private static final int SAVED_SEARCHES_MAX = 100;

    /*
     * MRU management is handled here instead of in the popup to avoid
     * deserializing on each popup.
     */
    private final MRUPreferenceSerializer recentSearchesSerializer =
        new MRUPreferenceSerializer(TFSCommonUIClientPlugin.getDefault().getPreferenceStore());
    private final MRUSet recentSearches;

    private final TeamExplorerContext context;
    private final FormToolkit toolkit;
    private final Text searchText;
    private final Button menuButton;
    private final TeamExplorerSearchControlPopup popup;

    public TeamExplorerSearchControl(
        final Composite parent,
        final TeamExplorerContext context,
        final FormToolkit toolkit,
        final int style) {
        super(parent, style);
        this.context = context;
        this.toolkit = toolkit;

        // Text controls present in this composite, enable form-style borders,
        // must have at least 1 pixel margins
        toolkit.paintBordersFor(this);

        SWTUtil.gridLayout(this, 2, false, 1, 2);

        searchText = toolkit.createText(this, "", SWT.NONE); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(searchText);
        searchText.setMessage(Messages.getString("TeamExplorerSearchControl.SearchWorkItems")); //$NON-NLS-1$
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.ESC:
                        popup.close();
                        break;
                    case SWT.ARROW_DOWN:
                        popup.open();
                        popup.getShell().setFocus();
                        break;
                    default:
                        if (e.character != '\0'
                            && !Character.isWhitespace(e.character)
                            && !Character.isISOControl(e.character)) {
                            popup.open();
                        }
                        break;
                }
            }
        });
        searchText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                // SWT doesn't let us know which control is getting the focus at
                // event time, so we'll do the test after the transition
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }

                        // Don't close the popup if the new focus holder is this
                        // text or inside our popup
                        final Control focusControl = getDisplay().getFocusControl();

                        // It's possible for there to be no focus holder for
                        // types of controls like Labels, which might appear in
                        // the popup, so don't close
                        if (focusControl == null) {
                            return;
                        }

                        if (focusControl == searchText || focusControl.getShell() == popup.getShell()) {
                            return;
                        }

                        popup.close();
                    }
                });
            }
        });
        searchText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                popup.close();
                search();
            }
        });

        popup = new TeamExplorerSearchControlPopup(this);

        final Point textSize = searchText.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        menuButton = toolkit.createButton(this, null, SWT.ARROW | SWT.DOWN);
        GridDataBuilder.newInstance().hHint(textSize.y).applyTo(menuButton);
        menuButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                popup.open();
            }
        });

        this.setTabList(new Control[] {
            searchText,
            menuButton
        });

        recentSearches =
            recentSearchesSerializer.read(SAVED_SEARCHES_MAX, UIPreferenceConstants.TEAM_EXPLORER_RECENT_SEARCHES);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                recentSearchesSerializer.write(recentSearches, UIPreferenceConstants.TEAM_EXPLORER_RECENT_SEARCHES);
            }
        });
    }

    public Text getSearchText() {
        return searchText;
    }

    public FormToolkit getToolkit() {
        return toolkit;
    }

    public MRUSet getRecentSearches() {
        return recentSearches;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled) {
            popup.close();
        }

        searchText.setEnabled(enabled);
        menuButton.setEnabled(enabled);
    }

    public void clear() {
        searchText.setText(""); //$NON-NLS-1$
    }

    public void closePopup() {
        popup.close();
    }

    public void search() {
        final String text = searchText.getText().trim();
        if (text.length() == 0) {
            return;
        }

        final Project project = context.getCurrentProject();
        if (project == null) {
            return;
        }

        final WorkItemSearchCommand command =
            new WorkItemSearchCommand(context.getServer(), context.getDefaultRepository(), project, text);

        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(command);

        if (status.isOK()) {
            recentSearches.add(text);
        }
    }

    /**
     * Appends the text to the current text contents in the format and grabs
     * focus:
     * <p>
     *
     * <pre>
     * filter:"defaultText"
     * </pre>
     * <p>
     * And sets the selection to contain the sample text.
     *
     * @param filter
     *        the filter to add (probably won't contain spaces)
     * @param defaultText
     *        the default text to put in quotes and select
     */
    public void appendFilter(final String filter, final String defaultText) {
        final StringBuilder sb = new StringBuilder(searchText.getText());

        if (sb.length() > 0) {
            if (!Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.append(' ');
            }
        }

        sb.append(filter);
        sb.append(':');
        sb.append('"');
        final int selStart = sb.length();
        sb.append(defaultText);
        final int selEnd = selStart + defaultText.length();
        sb.append('"');

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                searchText.setText(sb.toString());
                searchText.setSelection(selStart, selEnd);
                searchText.forceFocus();
            }
        });
    }
}
