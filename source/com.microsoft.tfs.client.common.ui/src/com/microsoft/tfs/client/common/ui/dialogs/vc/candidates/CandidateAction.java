// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.CandidatesTable;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.action.SelectionProviderAction;

public abstract class CandidateAction extends SelectionProviderAction {
    protected final PromoteCandidateChangesDialog dialog;
    protected final CandidatesTable table;
    protected final TFSRepository repository;

    public CandidateAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog.getTable());
        this.dialog = dialog;
        this.table = dialog.getTable();
        this.repository = dialog.getRepository();
    }
}