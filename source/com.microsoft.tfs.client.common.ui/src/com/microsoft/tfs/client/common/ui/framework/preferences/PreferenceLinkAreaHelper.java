// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.preferences;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.controls.generic.EmptyControl;

/**
 * This will build a PreferenceLinkArea on Eclipse >= 3.1. It uses reflection so
 * that Eclipse 3.0 can still compile.
 */
public class PreferenceLinkAreaHelper {
    /**
     * Builds a PreferenceLinkArea if the platform supports it. On platforms
     * that do not support a PreferenceLinkArea, you will be returned an empty
     * control which has no size and does nothing.
     *
     * @return A Control that can be used to link to the preference area, if
     *         supported, or an empty control if the platform does not support
     *         preference links
     */
    public static Control createPreferenceLinkArea(
        final Composite parent,
        final int style,
        final String pageId,
        final String message,
        final IPreferencePageContainer container,
        final Object pageData) {
        try {
            final Class preferenceLinkClass = Class.forName("org.eclipse.ui.dialogs.PreferenceLinkArea"); //$NON-NLS-1$
            final Class workbenchPrefContainerClass =
                Class.forName("org.eclipse.ui.preferences.IWorkbenchPreferenceContainer"); //$NON-NLS-1$

            final Constructor preferenceLinkCtor = preferenceLinkClass.getConstructor(new Class[] {
                Composite.class,
                int.class,
                String.class,
                String.class,
                workbenchPrefContainerClass,
                Object.class
            });

            final Object preferenceLinkArea = preferenceLinkCtor.newInstance(new Object[] {
                parent,
                new Integer(style),
                pageId,
                message,
                container,
                pageData
            });

            final Method getControlMethod = preferenceLinkClass.getMethod("getControl", (Class[]) null); //$NON-NLS-1$
            return (Control) getControlMethod.invoke(preferenceLinkArea, (Object[]) null);
        } catch (final Exception e) {
            return new EmptyControl(parent, style);
        }
    }
}
