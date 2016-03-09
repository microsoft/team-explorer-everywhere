// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.util.Check;

/**
 * Helper methods for dealing with editors.
 *
 * @threadsafety unknown
 */
public final class EditorHelper {
    private static final Log log = LogFactory.getLog(EditorHelper.class);

    public EditorHelper() {
    }

    /**
     * Saves all dirty editors matching the given saveable filter.
     *
     * @param filter
     *        the {@link WorkbenchPartSaveableFilter} to use to determine if the
     *        given workbench parts should be saved (not <code>null</code>)
     *
     * @return <code>true</code> if successful, <code>false</code> if the user
     *         cancelled the command.
     */
    public static boolean saveAllDirtyEditors(final WorkbenchPartSaveableFilter filter) {
        /*
         * Attempt to save all dirty editors. Eclipse 3.3+ offers "saveAll"
         * which allows us to pick an choose which dirty items will be saved.
         * Older versions of Eclipse only offer "saveAllEditors" which saves
         * every dirty editor. Pre-10.0 versions of Team Explorer used
         * "saveAllEditors" which remains as the fallback.
         *
         * As an additional headache, Eclipse 3.0 (RAD 6.0) does not have some
         * of the interface types the method uses, so we have to load those via
         * reflection before finding the "saveAll" method.
         */

        final IWorkbench workbench = PlatformUI.getWorkbench();
        boolean saveResult = false;
        boolean reflectionError = false;

        try {
            // Since Eclipse 3.1
            final Class iShellProviderClass =
                CheckinControl.class.getClassLoader().loadClass("org.eclipse.jface.window.IShellProvider"); //$NON-NLS-1$

            // Since Eclipse 3.3
            final Class iSaveableFilterClass =
                CheckinControl.class.getClassLoader().loadClass("org.eclipse.ui.ISaveableFilter"); //$NON-NLS-1$

            final Class[] parameters = new Class[4];
            parameters[0] = iShellProviderClass;
            parameters[1] = IRunnableContext.class;
            parameters[2] = iSaveableFilterClass;
            parameters[3] = Boolean.TYPE;

            final Method m = workbench.getClass().getMethod("saveAll", parameters); //$NON-NLS-1$

            final Object[] arguments = new Object[4];
            arguments[0] = workbench.getActiveWorkbenchWindow();
            arguments[1] = workbench.getActiveWorkbenchWindow();
            arguments[2] = new WorkbenchPartSaveableFilterAdapter(filter);
            arguments[3] = Boolean.TRUE;

            try {
                final Boolean result = (Boolean) m.invoke(workbench, arguments);
                saveResult = result.booleanValue();
            } catch (final Exception e) {
                saveResult = false;
            }
        } catch (final ClassNotFoundException e) {
            reflectionError = true;
        } catch (final NoSuchMethodException e) {
            reflectionError = true;
        }

        if (reflectionError) {
            saveResult = workbench.saveAllEditors(true);
        }

        return saveResult;
    }

    /**
     * Try to focus an editor that is already open.
     *
     * @param editor
     */
    public static boolean focusEditor(final IEditorPart editor) {
        Check.notNull(editor, "editor"); //$NON-NLS-1$

        if (editor.getEditorInput() == null) {
            log.warn("Asked to focus editor with no editor input"); //$NON-NLS-1$
            return false;
        }

        if (editor.getEditorSite() == null || editor.getEditorSite().getId() == null) {
            log.warn("Asked to focus editor with no editor site"); //$NON-NLS-1$
            return false;
        }

        Check.notNull(editor.getEditorInput(), "editor.getEditorInput"); //$NON-NLS-1$

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        if (page == null) {
            log.warn(MessageFormat.format(
                "Could not locate workbench page to focus editor '{0}'", //$NON-NLS-1$
                editor.getEditorInput().getName()));
            return false;
        }

        try {
            page.openEditor(editor.getEditorInput(), editor.getEditorSite().getId());
        } catch (final PartInitException e) {
            log.warn(MessageFormat.format("Could not focus editor '{0}'", editor.getEditorInput().getName()), e); //$NON-NLS-1$
            return false;
        }

        return true;
    }
}
