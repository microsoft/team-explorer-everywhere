// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.ImageButton;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public abstract class AbstractPathCombo extends BaseControl {
    public static final int BROWSE = Integer.MAX_VALUE - 1;
    public static final int REFRESH = Integer.MAX_VALUE - 2;

    private final Label prompt;
    private final Combo combo;
    private final ImageButton refreshButton;
    protected final ImageButton upButton;
    private final Button browseButton;

    private AbstractPathComboItem[] hierarchy;
    private String path;

    private final SingleListenerFacade listeners = new SingleListenerFacade(SelectionListener.class);

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public AbstractPathCombo(final Composite parent, final int style) {
        super(parent, (style & ~(BROWSE | REFRESH)));

        final boolean browse = (style & BROWSE) == BROWSE;
        final boolean refresh = (style & REFRESH) == REFRESH;

        final int rows = 3 + (browse ? 1 : 0) + (refresh ? 1 : 0);

        final GridLayout layout = new GridLayout(rows, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalMargin();
        setLayout(layout);

        prompt = new Label(this, SWT.NONE);
        prompt.setText(Messages.getString("AbstractPathCombo.PromptText")); //$NON-NLS-1$

        combo = new Combo(this, SWT.DROP_DOWN);
        combo.setText(""); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(combo);

        if ((style & SWT.READ_ONLY) == SWT.READ_ONLY) {
            combo.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(final KeyEvent e) {
                    e.doit = false;
                }

                @Override
                public void keyReleased(final KeyEvent e) {
                }
            });
        }

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (combo.getSelectionIndex() >= 0) {
                    setPath(hierarchy[combo.getSelectionIndex()].getCanonicalName());
                    notifyListeners();
                } else {
                    setPath(combo.getText());
                    notifyListeners();
                }
            }
        });

        if (refresh) {
            refreshButton = new ImageButton(this, SWT.NONE);
            refreshButton.setEnabledImage(imageHelper.getImage("/images/common/refresh.gif")); //$NON-NLS-1$
            refreshButton.setDisabledImage(imageHelper.getImage("/images/common/refresh_disabled.gif")); //$NON-NLS-1$
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    refreshPressed();
                }
            });
        } else {
            refreshButton = null;
        }

        upButton = new ImageButton(this, SWT.NONE);
        upButton.setEnabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_UP));
        upButton.setDisabledImage(
            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_UP_DISABLED));
        upButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                upPressed();
            }
        });

        if (browse) {
            browseButton = new Button(this, SWT.PUSH);
            browseButton.setText(Messages.getString("AbstractPathCombo.BrowseButtonText")); //$NON-NLS-1$
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final String newPath = browsePressed();

                    if (newPath != null) {
                        setPath(newPath);
                        notifyListeners();
                    }
                }
            });
        } else {
            browseButton = null;
        }

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });
    }

    public void clear() {
        combo.removeAll();
        combo.setText(""); //$NON-NLS-1$

        this.hierarchy = null;
        this.path = null;
    }

    public void setPath(String path) {
        if (path == null || (path = validatePath(path)) == null) {
            clear();
            return;
        }

        hierarchy = getHierarchy(path);
        final String[] items = new String[hierarchy.length];

        for (int i = 0; i < hierarchy.length; i++) {
            final StringBuilder item = new StringBuilder();

            for (int j = 0; j < i; j++) {
                item.append("   "); //$NON-NLS-1$
            }

            item.append(hierarchy[i].getShortName());

            items[i] = item.toString();
        }

        combo.setItems(items);

        if (hierarchy.length > 0) {
            combo.setText(hierarchy[hierarchy.length - 1].getCanonicalName());
        }

        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void addSelectionListener(final SelectionListener listener) {
        listeners.addListener(listener);
    }

    public void removeSelectionListener(final SelectionListener listener) {
        listeners.removeListener(listener);
    }

    private void notifyListeners() {
        final Event e = new Event();
        e.widget = this;

        final SelectionEvent se = new SelectionEvent(e);

        final SelectionListener listener = (SelectionListener) listeners.getListener(false);
        listener.widgetSelected(se);
    }

    protected abstract String getParentPath(final String path);

    protected abstract String validatePath(final String path);

    protected abstract String browsePressed();

    protected void upPressed() {
        final String parentPath = getParentPath(path);

        if (parentPath == null) {
            return;
        }

        setPath(parentPath);
        notifyListeners();
    }

    protected void refreshPressed() {
        notifyListeners();
    }

    protected abstract AbstractPathComboItem[] getHierarchy(final String path);

    protected static class AbstractPathComboItem {
        private final String canonicalName;
        private final String shortName;

        public AbstractPathComboItem(final String canonicalName, final String shortName) {
            this.canonicalName = canonicalName;
            this.shortName = shortName;
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public String getShortName() {
            return shortName;
        }
    }
}
