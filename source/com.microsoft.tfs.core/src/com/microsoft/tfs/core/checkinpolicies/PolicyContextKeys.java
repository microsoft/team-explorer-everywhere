// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

/**
 * <p>
 * {@link PolicyContextKeys} defines constant policy context property keys that
 * are used by the Plug-in for Eclipse and stand-alone Explorer to pass
 * information to policy implementations during evaluation.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyContextKeys {
    /**
     * This key identifies a property whose value is a SWT Shell object.
     */
    public static final String SWT_SHELL = "SWT_SHELL"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is a PendingChanges control.
     *
     * @deprecated TEE 10.0 no longer uses this control, so it is not
     *             initialized in {@link PolicyContext}s. Use the control
     *             returned by the {@link #CHECKIN_CONTROL} key instead.
     * @see #CHECKIN_CONTROL
     */
    @Deprecated
    public static final String PENDING_CHANGES_CONTROL = "PENDING_CHANGES_CONTROL"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is a
     * com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl
     * control.
     */
    @Deprecated
    public static final String CHECKIN_CONTROL = "CHECKIN_CONTROL"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is an Object (must not be
     * <code>null</code>) when the policy framework is running in the Plug-in
     * for Eclipse. It is not set when the framework runs in other products.
     */
    public static final String RUNNING_PRODUCT_ECLIPSE_PLUGIN = "RUNNING_PRODUCT_ECLIPSE_PLUGIN"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is an object (must not be
     * <code>null</code>) when the policy framework is running in the
     * stand-alone Team Explorer. It is not set when the framework runs in other
     * products.
     *
     * @deprecated
     */
    @Deprecated
    public static final String RUNNING_PRODUCT_EXPLORER = "RUNNING_PRODUCT_EXPLORER"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is an object (must not be
     * <code>null</code>) when the policy framework is running in the Command
     * Line Client (CLC). It is not set when the framework runs in other
     * products.
     */
    public static final String RUNNING_PRODUCT_CLC = "RUNNING_PRODUCT_CLC"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is a
     * com.microsoft.tfs.util.tasks.TaskMonitor when the policy framework is
     * running where a task monitor is appropriate.
     */
    public static final String TASK_MONITOR = "TASK_MONITOR"; //$NON-NLS-1$

    /**
     * This key identifies a property whose value is a
     * com.microsoft.tfs.core.TFSTeamProjectCollection.
     */
    public static final String TFS_TEAM_PROJECT_COLLECTION = "TFS_TEAM_PROJECT_COLLECTION"; //$NON-NLS-1$
}