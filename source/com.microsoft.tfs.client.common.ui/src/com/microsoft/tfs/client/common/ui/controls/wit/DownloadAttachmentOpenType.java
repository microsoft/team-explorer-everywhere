// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;

public class DownloadAttachmentOpenType {
    public static final DownloadAttachmentOpenType USE_PREFERENCE = new DownloadAttachmentOpenType();
    public static final DownloadAttachmentOpenType LOCAL = new DownloadAttachmentOpenType();
    public static final DownloadAttachmentOpenType BROWSER = new DownloadAttachmentOpenType();

    private DownloadAttachmentOpenType() {
    }

    public static DownloadAttachmentOpenType getPreferredOpenType() {
        final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        final String prefValue = preferences.getString(UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT);
        if (UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_BROWSER.equals(prefValue)) {
            return DownloadAttachmentOpenType.BROWSER;
        } else if (UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_LOCAL.equals(prefValue)) {
            return DownloadAttachmentOpenType.LOCAL;
        } else {
            return DownloadAttachmentOpenType.BROWSER;
        }
    }
}
