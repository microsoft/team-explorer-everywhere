// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action.keybinding;

/**
 * <p>
 * {@link CommandIDs} defines common command IDs as constants. For each command,
 * the command name, description, category, and default handler (if any) are
 * given. Also, any default key bindings for that command are also given in the
 * form "sequence (schemeId contextId)".
 * </p>
 *
 * <p>
 * This information is copied from the plugins that define the commands. The
 * <code>org.eclipse.ui</code> plugin defines many common commands.
 * </p>
 */
public class CommandIDs {
    /**
     * The "Copy" command (description: "Copy the selection to the clipboard").
     * This command is in the "org.eclipse.ui.category.edit" category and has a
     * default handler
     * ("org.eclipse.ui.internal.handlers.WidgetMethodHandler:copy"). By
     * default, it is bound to M1+C (defaultAcceleratorConfiguration
     * dialogAndWindow), M1+INSERT (defaultAcceleratorConfiguration
     * dialogAndWindow), M3+W (emacsAcceleratorConfiguration dialogAndWindow),
     * and ESC W (emacsAcceleratorConfiguration dialogAndWindow).
     */
    public static final String COPY = "org.eclipse.ui.edit.copy"; //$NON-NLS-1$

    /**
     * The "Paste" command (description: "Paste from the clipboard"). This
     * command is in the "org.eclipse.ui.category.edit" category and has a
     * default handler
     * ("org.eclipse.ui.internal.handlers.WidgetMethodHandler:paste"). By
     * default, it is bound to M1+V (defaultAcceleratorConfiguration
     * dialogAndWindow), M2+INSERT (defaultAcceleratorConfiguration
     * dialogAndWindow), and CTRL+Y (emacsAcceleratorConfiguration
     * dialogAndWindow).
     */
    public static final String PASTE = "org.eclipse.ui.edit.paste"; //$NON-NLS-1$

    /**
     * The "Delete" command (description: "Delete the selection"). This command
     * is in the "org.eclipse.ui.category.edit" category and has no default
     * handler. By default, it is bound to DEL (defaultAcceleratorConfiguration
     * window).
     */
    public static final String DELETE = "org.eclipse.ui.edit.delete"; //$NON-NLS-1$

    /**
     * The "Cut" command (description: "Cut the selection to the clipboard").
     * This command is in the "org.eclipse.ui.category.edit" category and has a
     * default handler
     * ("org.eclipse.ui.internal.handlers.WidgetMethodHandler:cut"). By default,
     * it is bound to M1+X (defaultAcceleratorConfiguration dialogAndWindow),
     * M2+DEL (defaultAcceleratorConfiguration dialogAndWindow), and CTRL+W
     * (emacsAcceleratorConfiguration dialogAndWindow).
     */
    public static final String CUT = "org.eclipse.ui.edit.cut"; //$NON-NLS-1$

    /**
     * The "Select All" command (description: "Select all"). This command is in
     * the "org.eclipse.ui.category.edit" category and has a default handler
     * (org.eclipse.ui.internal.handlers.SelectAllHandler). By default, it is
     * bound to M1+A (defaultAcceleratorConfiguration dialogAndWindow) and
     * CTRL+X H (emacsAcceleratorConfiguration dialogAndWindow).
     */
    public static final String SELECT_ALL = "org.eclipse.ui.edit.selectAll"; //$NON-NLS-1$
}
