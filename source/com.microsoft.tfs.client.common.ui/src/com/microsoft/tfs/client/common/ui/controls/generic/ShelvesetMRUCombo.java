// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.MRUPreferenceSerializer;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.util.MRUSet;

/**
 * This is an autocompleting Combo box with MRU control. This extends
 * AutocompleteCombo. Now only used for shelve set.
 */
public class ShelvesetMRUCombo extends AutocompleteCombo {

    /* The default max number of MRU items */
    public static final int MRU_NAME_MAX = 10;
    private final MRUSet shelvesetNameComboMRUSet;

    /**
     * @param parent
     * @param style
     */
    public ShelvesetMRUCombo(final Composite parent, final int style) {
        super(parent, style);
        shelvesetNameComboMRUSet =
            new MRUPreferenceSerializer(TFSCommonUIClientPlugin.getDefault().getPreferenceStore()).read(
                MRU_NAME_MAX,
                UIPreferenceConstants.SHELVE_DIALOG_NAME_MRU_PREFIX);
    }

    /**
     * Populate MRU control
     */
    public void populateMRU() {
        final List<String> mruItemsList = new ArrayList<String>(shelvesetNameComboMRUSet);
        Collections.reverse(mruItemsList);
        this.setItems(mruItemsList.toArray(new String[mruItemsList.size()]));
    }

    /**
     * Update MRU item list
     */
    public void updateMRU(final String shelveSetName) {
        if (this.shelvesetNameComboMRUSet.add(shelveSetName)) {
            new MRUPreferenceSerializer(TFSCommonUIClientPlugin.getDefault().getPreferenceStore()).write(
                this.shelvesetNameComboMRUSet,
                UIPreferenceConstants.SHELVE_DIALOG_NAME_MRU_PREFIX);
        }
    }
}
