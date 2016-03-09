// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import org.eclipse.jface.action.IContributionManager;

import com.microsoft.tfs.client.common.ui.Messages;

public class CheckinControlOptions {
    private IContributionManager externalContributionManager;
    private boolean forDialog = false;
    private boolean forShelveset = false;
    private boolean sourceFilesCommentReadOnly = false;
    private boolean sourceFilesCheckboxes = false;
    private String changesText = Messages.getString("CheckinControlOptions.DefaultPendingChangesText"); //$NON-NLS-1$
    private boolean policyDisplayed = true;
    private boolean policyEvaluationEnabled = true;
    private boolean workItemSearchEnabled = true;
    private boolean workItemReadOnly = false;
    private boolean checkinNotesReadOnly = false;
    private boolean checkinNotesHistoric = false;

    /*
     * Do the initial work item query on load (typically true, false for
     * shelveset dialog since the wit association comes from the pending changes
     * view selection.)
     */
    private boolean workItemInitialQuery = true;

    /* Show "Check-in Action" column when readonly (for shelveset details) */
    private boolean workItemShowAction = false;

    public CheckinControlOptions() {
    }

    public CheckinControlOptions(final CheckinControlOptions other) {
        externalContributionManager = other.externalContributionManager;
        forDialog = other.forDialog;
        forShelveset = other.forShelveset;
        sourceFilesCommentReadOnly = other.sourceFilesCommentReadOnly;
        sourceFilesCheckboxes = other.sourceFilesCheckboxes;
        changesText = other.changesText;
        policyDisplayed = other.policyDisplayed;
        policyEvaluationEnabled = other.policyEvaluationEnabled;
        workItemSearchEnabled = other.workItemSearchEnabled;
        workItemReadOnly = other.workItemReadOnly;
        workItemInitialQuery = other.workItemInitialQuery;
        workItemShowAction = other.workItemShowAction;
        checkinNotesReadOnly = other.checkinNotesReadOnly;
        checkinNotesHistoric = other.checkinNotesHistoric;
    }

    public boolean isForDialog() {
        return forDialog;
    }

    public void setForDialog(final boolean forDialog) {
        this.forDialog = forDialog;
    }

    public void setForShelveset(final boolean forShelveset) {
        this.forShelveset = forShelveset;
    }

    public boolean isForShelveset() {
        return forShelveset;
    }

    public IContributionManager getExternalContributionManager() {
        return externalContributionManager;
    }

    public void setExternalContributionManager(final IContributionManager externalContributionManager) {
        this.externalContributionManager = externalContributionManager;
    }

    public boolean isSourceFilesCommentReadOnly() {
        return sourceFilesCommentReadOnly;
    }

    public void setSourceFilesCommentReadOnly(final boolean sourceFilesCommentReadOnly) {
        this.sourceFilesCommentReadOnly = sourceFilesCommentReadOnly;
    }

    public boolean isSourceFilesCheckboxes() {
        return sourceFilesCheckboxes;
    }

    public void setSourceFilesCheckboxes(final boolean sourceFilesCheckboxes) {
        this.sourceFilesCheckboxes = sourceFilesCheckboxes;
    }

    public String getChangesText() {
        return changesText;
    }

    public void setChangesText(final String changesText) {
        this.changesText = changesText;
    }

    public boolean isPolicyEvaluationEnabled() {
        return policyEvaluationEnabled;
    }

    public void setPolicyEvaluationEnabled(final boolean enabled) {
        policyEvaluationEnabled = enabled;
    }

    public boolean isPolicyDisplayed() {
        return policyDisplayed;
    }

    public void setPolicyDisplayed(final boolean policyDisplayed) {
        this.policyDisplayed = policyDisplayed;
    }

    public boolean isWorkItemSearchEnabled() {
        return workItemSearchEnabled;
    }

    public void setWorkItemSearchEnabled(final boolean enabled) {
        workItemSearchEnabled = enabled;
    }

    public boolean isWorkItemReadOnly() {
        return workItemReadOnly;
    }

    public void setWorkItemReadOnly(final boolean readonly) {
        workItemReadOnly = readonly;
    }

    public void setWorkItemInitialQuery(final boolean workItemInitialQuery) {
        this.workItemInitialQuery = workItemInitialQuery;
    }

    public boolean getWorkItemInitialQuery() {
        return workItemInitialQuery;
    }

    /**
     * Show the check-in action column field even when work items are read only.
     * (Defaults false.)
     */
    public void setWorkItemShowAction(final boolean workItemShowAction) {
        this.workItemShowAction = workItemShowAction;
    }

    public boolean getWorkItemShowAction() {
        return workItemShowAction;
    }

    public void setCheckinNotesReadOnly(final boolean checkinNotesReadOnly) {
        this.checkinNotesReadOnly = checkinNotesReadOnly;
    }

    public boolean getCheckinNotesReadOnly() {
        return checkinNotesReadOnly;
    }

    public void setCheckinNotesHistoric(final boolean checkinNotesHistoric) {
        this.checkinNotesHistoric = checkinNotesHistoric;
    }

    public boolean getCheckinNotesHistoric() {
        return checkinNotesHistoric;
    }
}
