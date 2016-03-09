// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action;

import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Contains constants used to implement action contributions among controls.
 */
public class StandardActionConstants {
    /**
     * <p>
     * Used as an identifier of a contribution group ID, often in context menus.
     * Controls that provide a context menu should ensure that a contribution
     * group with this ID is present in the menu. Hosting controls that
     * contribute into a sub-control's context menu should append contributions
     * to this group.
     * </p>
     *
     * <p>
     * The meaning of this group ID is different from that of
     * {@link IWorkbenchActionConstants#MB_ADDITIONS}. This group is meant to be
     * used internally by your controls, while the more general
     * <code>additions</code> group is used for workbench (e.g. external)
     * contributions to controls.
     * </p>
     */
    public static final String HOSTING_CONTROL_CONTRIBUTIONS = "hosting-control-contributions"; //$NON-NLS-1$

    /**
     * Used as an identifier of a contribution group ID, often in context menus.
     * Controls that provide a context menu can use this group ID to identify a
     * contribution group that the control itself will contribute to. Hosting
     * controls should never contribute into this group.
     */
    public static final String PRIVATE_CONTRIBUTIONS = "private-contributions"; //$NON-NLS-1$
}
