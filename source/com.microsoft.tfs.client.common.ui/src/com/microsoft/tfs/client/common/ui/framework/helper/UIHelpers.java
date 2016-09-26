// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class UIHelpers {
    public static Font getTextFont(final Device device) {
        final FontData[] fontDatas = JFaceResources.getFontRegistry().getFontData(JFaceResources.TEXT_FONT);
        return new Font(device, fontDatas);
    }

    public static void copyToClipboard(final String text) {
        final Clipboard clipboard = new Clipboard(Display.getDefault());
        final TextTransfer textTransfer = TextTransfer.getInstance();
        final Transfer[] transfers = new Transfer[] {
            textTransfer
        };
        final Object[] data = new Object[] {
            text
        };
        clipboard.setContents(data, transfers);
        clipboard.dispose();
    }

    public static boolean isTabSelected(final TabFolder tabFolder, final TabItem tabItem) {
        final TabItem[] selection = tabFolder.getSelection();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i] == tabItem) {
                return true;
            }
        }
        return false;
    }

    public static String arrayToString(final Object[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }

        final StringBuffer sb = new StringBuffer("["); //$NON-NLS-1$

        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                sb.append("null"); //$NON-NLS-1$
            } else {
                sb.append(array[i].toString());
            }
            if (i < (array.length - 1)) {
                sb.append(", "); //$NON-NLS-1$
            }
        }

        sb.append("]"); //$NON-NLS-1$

        return sb.toString();
    }

    public static void printContributions(final IContributionManager mgr, final String name, final String indent) {
        final IContributionItem[] items = mgr.getItems();
        System.out.println(indent + "*" + name + " (size=" + items.length + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (int i = 0; i < items.length; i++) {
            System.out.println(indent + "\t" + items[i].getId() + ": " + items[i].getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
            if (items[i] instanceof IContributionManager) {
                printContributions((IContributionManager) items[i], "sub-" + name, indent + "\t"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    public static void setCompositeEnabled(final Composite comp, final boolean enabled) {
        final Control[] children = comp.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].setEnabled(enabled);
            if (children[i] instanceof Composite && children[i].isVisible()) {
                setCompositeEnabled((Composite) children[i], enabled);
            }
        }
    }

    public static Display getDisplay() {
        /*
         * The first attempt to get a Display to use is to call
         * Display.getCurrent(). This method is only useful in the case that the
         * current thread is a user interface thread for some Display. Most of
         * the times this method is called this isn't the case so this first
         * check will almost always return null. However, in the case that there
         * are multiple Displays (very rare) and the current thread just happens
         * to be a user interface thread for one of them (also rare), this line
         * ensures that we do the right thing.
         */
        Display display = Display.getCurrent();

        if (display == null) {
            /*
             * Display.getDefault() is almost always guaranteed to give us the
             * right Display to use. 99% of the time there is only one Display
             * and it can be treated as a singleton. But technically SWT does
             * support multiple Display objects so in very rare cases this
             * method may fail. It's probably best for clients to use one of the
             * runOnUIThread() overloads that explicitly passes in a Display
             * object if that object is easily available.
             *
             * See: http://dev.eclipse.org/newslists/news.eclipse.platform.swt/
             * msg22972 .html
             */
            display = Display.getDefault();
        }

        return display;
    }

    public static void syncExec(final Runnable runnable) {
        UIHelpers.runOnUIThread(false, runnable);
    }

    public static void asyncExec(final Runnable runnable) {
        UIHelpers.runOnUIThread(true, runnable);
    }

    /**
     * <p>
     * Runs the given runnable on the user interface thread associated with the
     * default Display. If the async flag is true then the runnable is executed
     * asynchronously to this method, and this method may return before the
     * runnable executes. The async=true mode is recommended to avoid deadlocks.
     * Important: even if the thread calling this message is the user interface
     * thread for the given Display, when the async flag is true the runnable
     * will still be executed asynchronously.
     * </p>
     * <p>
     * The vast majority of the time this method should be safe to call as there
     * is normally only one Display object and this method can retrieve it.
     * However SWT can technically support multiple Displays so when it is
     * convenient for client code to obtain a Display, it is probably better to
     * call runOnUIThread(Display, boolean, Runnable).
     * </p>
     *
     * @param asynch
     * @param runnable
     */
    public static void runOnUIThread(final boolean asynch, final Runnable runnable) {
        runOnUIThread(getDisplay(), asynch, runnable);
    }

    /**
     * <p>
     * Runs the given runnable on the user interface thread associated with the
     * given Shell's Display. If the async flag is true then the runnable is
     * executed asynchronously to this method, and this method may return before
     * the runnable executes. The async=true mode is recommended to avoid
     * deadlocks. Important: even if the thread calling this message is the user
     * interface thread for the given Display, when the async flag is true the
     * runnable will still be executed asynchronously.
     * </p>
     * <p>
     * Warning: care is needed when calling this method, and in many cases uses
     * of this method should be replaced by runOnUIThread(Display, boolean,
     * Runnable). This is because a common method for getting a Shell involves
     * calling Control.getShell(). However, Control.getShell() requires that the
     * caller is on the UI thread for the Control's Display. Therefore any code
     * which may not be on the UI thread and passes a Shell to this method by
     * calling Control.getShell() is broken. Such code should use
     * Widget.getDisplay() instead, which does not require that the caller be on
     * the Display's UI thread.
     * </p>
     *
     * @param shell
     *        used to get a Display to execute the Runnable with
     * @param asynch
     *        true to run asynchronously (recommended)
     * @param runnable
     *        the Runnable to run
     */
    public static void runOnUIThread(final Shell shell, final boolean asynch, final Runnable runnable) {
        if (!shell.isDisposed()) {
            runOnUIThread(shell.getDisplay(), asynch, runnable);
        }
    }

    /**
     * Runs the given runnable on the user interface thread associated with the
     * given Display. If the async flag is true then the runnable is executed
     * asynchronously to this method, and this method may return before the
     * runnable executes. The async=true mode is recommended to avoid deadlocks.
     * Important: even if the thread calling this message is the user interface
     * thread for the given Display, when the async flag is true the runnable
     * will still be executed asynchronously.
     *
     * @param display
     *        the Display that will run the runnable
     * @param asynch
     *        true to run asynchronously (recommended)
     * @param runnable
     *        the Runnable to run
     */
    public static void runOnUIThread(final Display display, final boolean asynch, final Runnable runnable) {
        if (display == null) {
            throw new IllegalArgumentException("display is null"); //$NON-NLS-1$
        }

        if (asynch) {
            display.asyncExec(runnable);
        } else {
            /*
             * Note that sync execute will just call .run() on the Runnable if
             * the current thread is the display's thread. Therefore there is no
             * need to check for that case here and optimize for it, as it would
             * have the same effect.
             */
            display.syncExec(runnable);
        }
    }

    public static int openOnUIThread(final Dialog dialog) {
        final AtomicInteger dialogStatus = new AtomicInteger();

        runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                dialogStatus.set(dialog.open());
            }
        });

        return dialogStatus.get();
    }
}
