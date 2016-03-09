// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action.keybinding;

import java.lang.reflect.Constructor;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class ActionKeyBindingSupportFactory {
    private static volatile boolean modernSupportAvailable = true;

    public static ActionKeyBindingSupport newInstance(final Shell shell) {
        final IWorkbench workbench = PlatformUI.getWorkbench();

        if (modernSupportAvailable) {
            try {
                /*
                 * Don't import this class, since it can't compile on Eclipse
                 * 3.0. This is a softer dependency that doesn't need to resolve
                 * when we load this Factory class.
                 */
                final Class modernClass =
                    com.microsoft.tfs.client.common.ui.framework.action.keybinding.ModernActionKeyBindingSupport.class;
                final Class adaptableClass = Class.forName("org.eclipse.core.runtime.IAdaptable"); //$NON-NLS-1$

                final Constructor modernConstructor = modernClass.getConstructor(new Class[] {
                    adaptableClass,
                    shell.getClass()
                });

                return (ActionKeyBindingSupport) modernConstructor.newInstance(new Object[] {
                    workbench,
                    shell
                });

            } catch (final Throwable e) {
                modernSupportAvailable = false;
            }
        }

        return new LegacyActionKeyBindingSupport(workbench, shell);
    }

    public static ActionKeyBindingSupport newInstance(final IWorkbenchPart part) {
        final IWorkbench workbench = PlatformUI.getWorkbench();

        if (modernSupportAvailable) {
            try {
                /*
                 * Don't import this class, since it can't compile on Eclipse
                 * 3.0. This is a softer dependency that doesn't need to resolve
                 * when we load this Factory class.
                 */
                final Class modernClass =
                    com.microsoft.tfs.client.common.ui.framework.action.keybinding.ModernActionKeyBindingSupport.class;
                final Class adaptableClass = Class.forName("org.eclipse.core.runtime.IAdaptable"); //$NON-NLS-1$

                final Constructor modernConstructor = modernClass.getConstructor(new Class[] {
                    adaptableClass,
                    IWorkbenchPart.class
                });

                return (ActionKeyBindingSupport) modernConstructor.newInstance(new Object[] {
                    workbench,
                    part
                });

            } catch (final Throwable e) {
                modernSupportAvailable = false;
            }
        }

        return new LegacyActionKeyBindingSupport(workbench, part);
    }
}
