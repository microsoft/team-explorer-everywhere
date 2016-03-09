// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.client.common.ui.wit.form.VerticalStackControl;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTab;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTabGroup;

public class TabGroupControl extends BaseWorkItemControl {
    private TabFolder tabFolder;
    private WIFormTabGroup tabGroupDescription;

    @Override
    protected void hookInit() {
        tabGroupDescription = (WIFormTabGroup) getFormElement();
    }

    @Override
    public int getMinimumRequiredColumnCount() {
        return 1;
    }

    @Override
    public void addToComposite(final Composite parent) {
        populate(parent);

        final int numColumns = ((GridLayout) parent.getLayout()).numColumns;
        if (wantsVerticalFill()) {
            tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1));
        } else {
            tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numColumns, 1));
        }
    }

    @Override
    public boolean wantsVerticalFill() {
        return isFormElementLastAmongSiblings();
    }

    private void populate(final Composite parent) {
        tabFolder = new TabFolder(parent, SWT.NONE);
        getDebuggingContext().debug(tabFolder, tabGroupDescription);
        getWorkItemEditorContextMenu().setMenuOnControl(tabFolder);

        final WIFormTab[] tabs = tabGroupDescription.getTabChildren();

        for (int i = 0; i < tabs.length; i++) {
            final TabItem tab = new TabItem(tabFolder, SWT.NONE);
            tab.setText(tabs[i].getLabel());

            getFormContext().pushFieldTracker(new FieldTracker(getFormContext().getFieldTracker()));
            final VerticalStackControl vsc = new VerticalStackControl(tabFolder, SWT.NONE, tabs[i], getFormContext());
            final FieldTracker trackerForTab = getFormContext().popFieldTracker();

            final TabDecoratingWorkItemStateListener stateListener =
                new TabDecoratingWorkItemStateListener(trackerForTab, tab);
            stateListener.updateTab();

            getWorkItem().addWorkItemStateListener(stateListener);
            tab.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    getWorkItem().removeWorkItemStateListener(stateListener);
                }
            });

            tab.setControl(vsc);
        }
    }

    private static class TabDecoratingWorkItemStateListener extends WorkItemStateAdapter {
        private final FieldTracker fieldTracker;
        private final TabItem tab;
        private final boolean imageMode;
        private static final String KEY = "text-mode-key"; //$NON-NLS-1$

        public TabDecoratingWorkItemStateListener(final FieldTracker fieldTracker, final TabItem tab) {
            this.fieldTracker = fieldTracker;
            this.tab = tab;
            imageMode = !SWT.getPlatform().equals("win32"); //$NON-NLS-1$
        }

        @Override
        public void validStateChanged(final boolean isValid, final WorkItem workItem) {
            /*
             * We can get invoked on a non-UI thread.
             */
            UIHelpers.runOnUIThread(tab.getDisplay(), true, new Runnable() {
                @Override
                public void run() {
                    if (tab.isDisposed()) {
                        return;
                    }

                    updateTab();
                }
            });
        }

        public void updateTab() {
            if (imageMode) {
                updateTabDecorationImage();
            } else {
                updateTabDecorationText();
            }
        }

        public void updateTabDecorationImage() {
            final Field f = fieldTracker.findFirstInvalidField();
            final Image existingImage = tab.getImage();
            if (f != null && existingImage == null) {
                tab.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
            } else if (f == null && existingImage != null) {
                tab.setImage(null);
            }
        }

        public void updateTabDecorationText() {
            final Field f = fieldTracker.findFirstInvalidField();
            String oldText = (String) tab.getData(KEY);

            if (f != null && oldText == null) {
                oldText = tab.getText();
                tab.setText(Messages.getString("TabGroupControl.TabDecorationText") + oldText); //$NON-NLS-1$
                tab.setData(KEY, oldText);
            } else if (f == null && oldText != null) {
                tab.setText(oldText);
                tab.setData(KEY, null);
            }
        }

    }
}
