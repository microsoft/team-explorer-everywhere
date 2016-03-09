// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.notifications;

/**
 * The version of the notification messages supported by this notification
 * window.
 * <p>
 * Due to how Orcas SP1 checks the window flags, all later versions must OR in
 * the 1 bit. In other words, all version numbers Dev10+ must be odd. Here's how
 * Orcas did the checK:
 * <p>
 *
 * <pre>
 * uint value = (uint) NativeMethods.GetWindowLong(hwnd, NativeMethods.GWL_USERDATA).ToInt32();
 * return ((value &amp; flag) == flag); // flag == 1
 * </pre>
 */
interface NotificationWindowVersion {
    public final int ORCAS_RTM = 0x00;

    public final int ORCAS_SP1 = 0x01;

    // Must OR in the OrcasSP1 bit (see remarks)
    public final int DEV10_RTM = 0x03;
    public final int DEV12_RTM = 0x05;

    public final int LATEST = DEV12_RTM; // Should always be the latest version
}