// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

public class TeamExplorerPendingChangesPageState {
    private final boolean shelveCompositeVisible;
    private final String shelvesetNameComboText;
    private final boolean preserveCheckboxChecked;
    private final boolean evaluateCheckboxChecked;

    public TeamExplorerPendingChangesPageState(
        final boolean shelveCompositeVisible,
        final String shelvesetNameComboText,
        final boolean preserveButtonChecked,
        final boolean evaluateButtonChecked) {
        this.shelveCompositeVisible = shelveCompositeVisible;
        this.shelvesetNameComboText = shelvesetNameComboText;
        this.preserveCheckboxChecked = preserveButtonChecked;
        this.evaluateCheckboxChecked = evaluateButtonChecked;
    }

    public boolean isShelveCompositeVisible() {
        return shelveCompositeVisible;
    }

    public String getShelvesetNameComboText() {
        return shelvesetNameComboText;
    }

    public boolean isPreserveCheckboxChecked() {
        return preserveCheckboxChecked;
    }

    public boolean isEvaluateCheckboxChecked() {
        return evaluateCheckboxChecked;
    }
}
