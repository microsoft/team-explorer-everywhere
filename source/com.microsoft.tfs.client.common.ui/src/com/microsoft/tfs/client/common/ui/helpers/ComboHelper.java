// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Combo;

import com.microsoft.tfs.util.Check;

public class ComboHelper {
    public static final int MAX_VISIBLE_ITEM_COUNT = 14;

    /**
     * Populates the combo with the specified values. The specified initial
     * value is set as the initial selection it it appears in the array of
     * values, otherwise the first value is selected.
     *
     * @param combo
     *        The combo to populate.
     *
     * @param values
     *        The values for the combo drop down.
     *
     * @param initialValue
     *        The value which should be the initial selected value.
     *
     * @return The index of the selected item.
     */
    public static int populateCombo(final Combo combo, final String[] values, final String initialValue) {
        Check.notNull(combo, "combo"); //$NON-NLS-1$
        Check.notNull(values, "values"); //$NON-NLS-1$

        if (values.length == 0) {
            return -1;
        }

        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            final String value = values[i];
            if (value.equals(initialValue)) {
                selectedIndex = i;
            }
            combo.add(value);
        }

        combo.select(selectedIndex);
        setVisibleItemCount(combo);
        return selectedIndex;
    }

    /*
     * Sets the visible item count of the specified combo to the smaller of
     * MAX_VISIBLE_ITEM_COUNT or the current number of items in the combo. The
     * combo is guaranteed to have at least one visible item even if the combi
     * is empty.
     */
    public static void setVisibleItemCount(final Combo combo) {
        setVisibleItemCount(combo, combo.getItemCount(), MAX_VISIBLE_ITEM_COUNT);
    }

    /*
     * Sets the visible item count of the specified combo to the smaller of
     * MAX_VISIBLE_ITEM_COUNT or the current number of items in the combo. The
     * combo is guaranteed to have at least one visible item even if the combi
     * is empty.
     */
    public static void setVisibleItemCount(final CCombo combo) {
        setVisibleItemCount(combo, combo.getItemCount(), MAX_VISIBLE_ITEM_COUNT);
    }

    /*
     * Sets the visible item count of the specified combo to the smaller of
     * maxItemCount or itemCount. The combo is guaranteed to have at least one
     * visible item even if the combo is empty.
     */
    public static void setVisibleItemCount(final Combo combo, final int itemCount, final int maxItemCount) {
        combo.setVisibleItemCount(getVisibleItemCount(itemCount, maxItemCount));
    }

    /*
     * Sets the visible item count of the specified combo to the smaller of
     * maxItemCount or itemCount. The combo is guaranteed to have at least one
     * visible item even if the combo is empty.
     */
    public static void setVisibleItemCount(final CCombo combo, final int itemCount, final int maxItemCount) {
        combo.setVisibleItemCount(getVisibleItemCount(itemCount, maxItemCount));
    }

    private static int getVisibleItemCount(final int itemCount, final int maxItemCount) {
        int visibleItemCount = itemCount;
        visibleItemCount = Math.max(1, visibleItemCount);
        visibleItemCount = Math.min(maxItemCount, visibleItemCount);
        return visibleItemCount;
    }
}
