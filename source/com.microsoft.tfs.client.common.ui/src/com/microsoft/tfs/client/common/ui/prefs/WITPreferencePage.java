// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInfo;

public class WITPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public final static String LAUNCH_BROWSER_FOR_ATTACHMENT_RADIO_ID = "WITPreferencePage.launchBrowserAttachment"; //$NON-NLS-1$
    public final static String DOWNLOAD_ATTACHMENT_RADIO_ID = "WITPreferencePage.downloadAttachment"; //$NON-NLS-1$

    private RadioGroupFieldEditor openFileAttachmentPrefsEditor;
    private RadioGroupFieldEditor workItemEditorPrefsEditor;

    public WITPreferencePage() {
        super(GRID);
        setPreferenceStore(TFSCommonUIClientPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        openFileAttachmentPrefsEditor = new RadioGroupFieldEditor(
            UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT,
            Messages.getString("WITPreferencePage.SummaryLabelText"), //$NON-NLS-1$
            1,
            new String[][] {
                {
                    Messages.getString("WITPreferencePage.LaunchBrowserWithAttachment"), //$NON-NLS-1$
                    UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_BROWSER
            }, {
                Messages.getString("WITPreferencePage.DownloadAttachmentToTemp"), //$NON-NLS-1$
                UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_LOCAL
            }
        }, getFieldEditorParent(), true);

        workItemEditorPrefsEditor = new RadioGroupFieldEditor(
            UIPreferenceConstants.WORK_ITEM_EDITOR_ID,
            Messages.getString("WITPreferencePage.WorkItemEditorGroupLabel"), //$NON-NLS-1$
            1,
            getWorkItemEditors(),
            getFieldEditorParent(),
            true);

        addField(openFileAttachmentPrefsEditor);
        addField(workItemEditorPrefsEditor);

        /*
         * Get the controls back out of the field editor to set automation IDs.
         * There may be a better way to do this.
         */
        final Control[] radioButtons =
            openFileAttachmentPrefsEditor.getRadioBoxControl(getFieldEditorParent()).getChildren();
        AutomationIDHelper.setWidgetID(radioButtons[0], LAUNCH_BROWSER_FOR_ATTACHMENT_RADIO_ID);
        AutomationIDHelper.setWidgetID(radioButtons[1], DOWNLOAD_ATTACHMENT_RADIO_ID);
    }

    @Override
    public void init(final IWorkbench workbench) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        CodeMarkerDispatch.dispatch(
            visible ? BasePreferencePage.CODEMARKER_VISIBLE_TRUE : BasePreferencePage.CODEMARKER_VISIBLE_FALSE);
    }

    /**
     * Get the work item editor information in the String[][] format to be
     * consumed by a RadioGroupFieldEditor.
     *
     *
     * @return A two dimensional string array. Each row in the array represents
     *         a registered work item editor with the display name in the first
     *         column and the Eclipse editor ID in the second column.
     */
    private static String[][] getWorkItemEditors() {
        final List<WorkItemEditorInfo> editorInfos = WorkItemEditorHelper.getWorkItemEditors();

        if (editorInfos == null || editorInfos.size() == 0) {
            return new String[0][2];
        }

        final String[][] editors = new String[editorInfos.size()][2];
        for (int i = 0; i < editorInfos.size(); i++) {
            final WorkItemEditorInfo editorInfo = editorInfos.get(i);
            editors[i][0] = editorInfo.getDisplayName();
            editors[i][1] = editorInfo.getEditorID();
        }

        return editors;
    }
}
