// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.util.Check;

public class ShowConsoleAction extends Action implements IPropertyChangeListener {
    private final String preferenceKey;
    private boolean ignorePropertyChangeEvent = false;

    private final IPreferenceStore preferenceStore = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

    public ShowConsoleAction(final String name, final String preferenceKey, final ImageDescriptor imageDescriptor) {
        super(name, IAction.AS_CHECK_BOX);

        Check.notNull(preferenceKey, "preferenceKey"); //$NON-NLS-1$
        this.preferenceKey = preferenceKey;

        setToolTipText(name);
        setImageDescriptor(imageDescriptor);

        preferenceStore.addPropertyChangeListener(this);

        refresh();
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (ignorePropertyChangeEvent == false && preferenceKey.equals(event.getProperty())) {
            refresh();
        }
    }

    @Override
    public void run() {
        try {
            ignorePropertyChangeEvent = true;
            preferenceStore.setValue(preferenceKey, isChecked());
        } finally {
            ignorePropertyChangeEvent = false;
        }
    }

    private void refresh() {
        if (preferenceStore.getBoolean(preferenceKey)) {
            setChecked(true);
        } else {
            setChecked(false);
        }
    }

    public void dispose() {
        preferenceStore.removePropertyChangeListener(this);
    }
}
