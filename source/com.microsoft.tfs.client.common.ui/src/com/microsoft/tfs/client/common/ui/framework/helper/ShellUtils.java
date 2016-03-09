// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.util.Check;

/**
 * {@link ShellUtils} contains static utility methods for working with SWT
 * {@link Shell}s.
 */
public class ShellUtils {
    private static final Log log = LogFactory.getLog(ShellUtils.class);

    /**
     * The SWT modal style bits. If a Shell's style has any of these bits set,
     * we consider the shell to be modal.
     */
    private static int MODAL_STYLES = SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL | SWT.PRIMARY_MODAL;

    /**
     * <p>
     * Tests whether the given {@link Shell} is modal.
     * </p>
     *
     * @throws SWTException
     *         if this method is not called from the UI thread for the specified
     *         {@link Shell} (code {@link SWT#ERROR_THREAD_INVALID_ACCESS})
     *
     * @param shell
     *        a {@link Shell} to test for modality (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the given {@link Shell} is modal
     */
    public static boolean isModal(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$
        return (shell.getStyle() & MODAL_STYLES) != 0;
    }

    /**
     * <p>
     * Tests whether a {@link Shell} is a child of another {@link Shell}. The
     * test is actually a descendant test, since this method will return
     * <code>true</code> for both direct children and descendants.
     * </p>
     *
     * @throws SWTException
     *         if this method is not called from the UI thread for the specified
     *         {@link Shell} (code {@link SWT#ERROR_THREAD_INVALID_ACCESS})
     *
     * @param shell
     *        the potential child {@link Shell} (must not be <code>null</code>)
     * @param parent
     *        the potential parent {@link Shell} (must not be <code>null</code>)
     * @return <code>true</code> if there is a parent-child relationship
     */
    public static boolean isChild(final Shell shell, final Shell parent) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        Control control = shell;
        while (control != null && control != parent) {
            control = control.getParent();
        }

        return control == parent;
    }

    /**
     * <p>
     * Finds the first modal {@link Shell} of the given {@link Display}. If
     * there are no modal {@link Shell}s, returns <code>null</code>.
     * </p>
     *
     * @throws SWTException
     *         if this method is not called from the UI thread for the specified
     *         {@link Display} (code {@link SWT#ERROR_THREAD_INVALID_ACCESS})
     *
     * @param display
     *        the {@link Display} used to find active {@link Shell}s
     * @return the first modal {@link Shell} or <code>null</code> if there are
     *         no modal {@link Shell}s
     */
    public static Shell getFirstModalShell(final Display display) {
        Check.notNull(display, "display"); //$NON-NLS-1$

        final Shell[] shells = display.getShells();
        for (int i = 0; i < shells.length; i++) {
            if (isModal(shells[i])) {
                return shells[i];
            }
        }

        return null;
    }

    /**
     * <p>
     * Finds a {@link Shell} that is a modal blocker of the given {@link Shell}.
     * If such a {@link Shell} exists and the given {@link Shell} is modal, then
     * opening the given {@link Shell} would result in multiple modal
     * {@link Shell}s that do not have a parent-child relationship. This
     * condition should be avoided since it can result in a hung UI.
     * </p>
     *
     * @throws SWTException
     *         if this method is not called from the UI thread for the specified
     *         {@link Shell} (code {@link SWT#ERROR_THREAD_INVALID_ACCESS})
     *
     * @param shellToOpen
     *        a {@link Shell} that is being considered for opening (must not be
     *        <code>null</code>)
     * @return a modal blocker {@link Shell} (described above) or
     *         <code>null</code> if there is no modal blocker {@link Shell}
     */
    public static Shell getModalBlockingShell(final Shell shellToOpen) {
        Check.notNull(shellToOpen, "shellToOpen"); //$NON-NLS-1$

        final Shell[] shells = shellToOpen.getDisplay().getShells();

        for (int i = 0; i < shells.length; i++) {
            if (isModal(shells[i])) {
                if (shells[i] == shellToOpen) {
                    continue;
                }
                if (!shells[i].isVisible()) {
                    continue;
                }

                /*
                 * If the queried shell is a child of the shell about to be
                 * opened, consider this a blocking shell. This is a behavior
                 * change beginning post-TEE 2010 SP1. This was a rare case that
                 * caused problems on some windows managers on Linux. The most
                 * likely problem is that a command is being run in a
                 * DeferredProgressMonitor runnable context, which creates a
                 * Shell but does not open it. A different mechanism creates a
                 * new shell (parented off the DeferredProgressMonitor's Shell)
                 * and opens it. When the DeferredProgressMonitor is later
                 * opened, the window manager will be confused.
                 */
                if (isChild(shells[i], shellToOpen)) {
                    return shells[i];
                }
                if (isChild(shellToOpen, shells[i])) {
                    continue;
                }
                return shells[i];
            }
        }

        return null;
    }

    /**
     * <p>
     * Attempts to find the best {@link Shell} to use as the parent of a modal
     * dialog. You must pass in a {@link Shell} that would be used as a parent
     * in the absence of this method. If no better parent can be found, the
     * given {@link Shell} will be returned as the parent. If there are existing
     * modal {@link Shell}s, this method will return a parent that is possibly
     * safer to use than the given {@link Shell}. In particular, using this
     * method will avoid having multiple modal {@link Shell}s that do not have a
     * parent-child relationship.
     * </p>
     *
     * <p>
     * In order to make this method applicable in as many situations as
     * possible, you are allowed to pass <code>null</code> as the default parent
     * {@link Shell}. In this case, this method will return <code>null</code>.
     * </p>
     *
     * <p>
     * In order for this method to be useful, it must be called from the UI
     * thread for the given {@link Shell}. However, if it is not, it will not
     * throw an exception. In this case it will simply return the given
     * {@link Shell}.
     * </p>
     *
     * @param defaultParent
     *        the default parent {@link Shell}, or <code>null</code>
     * @return the best parent {@link Shell} to use, or <code>null</code> if
     *         <code>null</code> was passed as the argument
     */
    public static Shell getBestParent(final Shell defaultParent) {
        /*
         * Wrap the entire method in a try block. Errors in this method should
         * never bubble up to callers (but we will log them).
         */
        try {
            if (defaultParent == null) {
                return null;
            }

            final Display display = defaultParent.getDisplay();

            if (display.getThread() != Thread.currentThread()) {
                return defaultParent;
            }

            final Shell[] childrenOfDefaulParent = defaultParent.getShells();

            for (int i = 0; i < childrenOfDefaulParent.length; i++) {
                if (isModal(childrenOfDefaulParent[i])) {
                    return getBestParent(childrenOfDefaulParent[i]);
                }
            }

            return defaultParent;
        } catch (final Exception ex) {
            log.error("error in getBestParent", ex); //$NON-NLS-1$
            return defaultParent;
        }
    }

    /**
     * Returns the shell which contains the given control.
     *
     * @param control
     *        The control to test (not null)
     * @return The shell which contains the given control, or null if it is not
     *         contained in a Shell.
     */
    public static Shell getParentShell(final Control control) {
        Check.notNull(control, "control"); //$NON-NLS-1$

        for (Control testControl = control; testControl != null; testControl = testControl.getParent()) {
            if (testControl instanceof Shell) {
                return (Shell) testControl;
            }
        }

        return null;
    }

    public static Shell getWorkbenchShell() {
        final Shell shell[] = new Shell[1];
        final Display display = UIHelpers.getDisplay();

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell[0] = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            }
        });

        return shell[0];
    }
}
